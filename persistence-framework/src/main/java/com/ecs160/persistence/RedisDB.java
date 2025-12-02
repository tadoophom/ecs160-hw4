package com.ecs160.persistence;

import com.ecs160.persistence.annotations.Id;
import com.ecs160.persistence.annotations.LazyLoad;
import com.ecs160.persistence.annotations.PersistableField;
import com.ecs160.persistence.annotations.PersistableObject;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisDB {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6379;

    private final Jedis jedisSession;

    public RedisDB() {
        this.jedisSession = new Jedis(DEFAULT_HOST, DEFAULT_PORT);
    }

    private String serializeList(List<?> list) throws Exception {
        List<String> ids = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                persist(item);
                Field idField = getIdField(item.getClass());
                idField.setAccessible(true);
                ids.add(idField.get(item).toString());
            }
        }
        return String.join(",", ids);
    }

    private List<Object> deserializeList(Field field, String data) throws Exception {
        List<Object> list = new ArrayList<>();
        if (data.isEmpty()) return list;

        ParameterizedType pt = (ParameterizedType) field.getGenericType();
        Class<?> itemClass = (Class<?>) pt.getActualTypeArguments()[0];
        Field idField = getIdField(itemClass);
        idField.setAccessible(true);

        for (String id : data.split(",")) {
            if (!id.trim().isEmpty()) {
                Object item = itemClass.getDeclaredConstructor().newInstance();
                idField.set(item, convert(idField.getType(), id.trim()));
                Object loaded = load(item);
                if (loaded != null) list.add(loaded);
            }
        }
        return list;
    }

    private Field getIdField(Class<?> tempClass) {
        for (Field f : tempClass.getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class)) return f;
        }
        throw new IllegalArgumentException("No @Id field found in " + tempClass.getName());
    }

    private record LazyMetadata(Map<String, Field> fields, Map<String, String> mappings) {}

    private LazyMetadata resolveLazyMetadata(Class<?> tempClass) {
        Map<String, Field> fields = new HashMap<>();
        Map<String, String> mappings = new HashMap<>();

        for (Method method : tempClass.getDeclaredMethods()) {
            LazyLoad lazyLoad = method.getAnnotation(LazyLoad.class);
            if (lazyLoad == null) continue;

            String fieldName = lazyLoad.field();
            if (fieldName == null || fieldName.trim().isEmpty()) {
                throw new IllegalArgumentException("@LazyLoad on method " + method.getName() + " must reference a persistable field");
            }

            try {
                Field field = tempClass.getDeclaredField(fieldName);
                if (!field.isAnnotationPresent(PersistableField.class)) {
                    throw new IllegalArgumentException("Field '" + fieldName + "' must be annotated with @PersistableField");
                }
                field.setAccessible(true);
                fields.putIfAbsent(fieldName, field);
                mappings.put(method.getName(), fieldName);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Field '" + fieldName + "' not found in " + tempClass.getName(), e);
            }
        }
        return new LazyMetadata(fields, mappings);
    }

    public boolean persist(Object obj) {
        if (obj == null) throw new IllegalArgumentException("Cannot persist null object");

        Class<?> tempClass = obj.getClass();
        if (!tempClass.isAnnotationPresent(PersistableObject.class)) {
            throw new IllegalArgumentException("Class " + tempClass.getName() + " must be annotated with @PersistableObject");
        }

        try {
            Field idField = getIdField(tempClass);
            idField.setAccessible(true);
            Object idVal = idField.get(obj);
            if (idVal == null) throw new IllegalArgumentException("Id field cannot be null");
            String key = idVal.toString();

            Map<String, String> data = new HashMap<>();
            for (Field field : tempClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(PersistableField.class)) {
                    field.setAccessible(true);
                    Object val = field.get(obj);
                    if (val != null) {
                        if (List.class.isAssignableFrom(field.getType())) {
                            data.put(field.getName(), serializeList((List<?>) val));
                        } else {
                            data.put(field.getName(), val.toString());
                        }
                    }
                }
            }

            jedisSession.hset(key, data);
            return true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error persisting object", e);
        }
    }

    public Object load(Object template) {
        if (template == null) throw new IllegalArgumentException("Cannot load into null object");

        Class<?> tempClass = template.getClass();
        if (!tempClass.isAnnotationPresent(PersistableObject.class)) {
            throw new IllegalArgumentException("Class " + tempClass.getName() + " must be annotated with @PersistableObject");
        }

        try {
            Field idField = getIdField(tempClass);
            idField.setAccessible(true);
            Object idVal = idField.get(template);
            if (idVal == null) throw new IllegalArgumentException("Id field cannot be null");
            String key = idVal.toString();

            Map<String, String> rawData = jedisSession.hgetAll(key);
            if (rawData == null || rawData.isEmpty()) return null;

            Map<String, String> data = new HashMap<>();
            rawData.forEach((k, v) -> {
                data.putIfAbsent(k, v);
                data.putIfAbsent(k.toLowerCase(), v);
            });

            LazyMetadata lazyMetadata = resolveLazyMetadata(tempClass);
            Object target = template;

            if (!lazyMetadata.fields().isEmpty()) {
                ProxyFactory factory = new ProxyFactory();
                factory.setSuperclass(tempClass);
                try {
                    target = factory.create(new Class<?>[0], new Object[0]);
                    ((Proxy) target).setHandler(new LazyMethodHandler(lazyMetadata.mappings(), lazyMetadata.fields(), data));
                    idField.set(target, idVal);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create proxy for " + tempClass.getName(), e);
                }
            }

            for (Field field : tempClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(PersistableField.class)) {
                    if (lazyMetadata.fields().containsKey(field.getName())) continue;

                    setFieldValue(target, field, data.get(field.getName()));
                }
            }
            return target;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error loading object", e);
        }
    }

    private void setFieldValue(Object target, Field field, String value) throws Exception {
        if (value != null) {
            field.setAccessible(true);
            if (List.class.isAssignableFrom(field.getType())) {
                field.set(target, deserializeList(field, value));
            } else {
                field.set(target, convert(field.getType(), value));
            }
        }
    }

    private Object convert(Class<?> type, String val) {
        if (type == String.class) return val;
        if (type == int.class || type == Integer.class) return Integer.parseInt(val);
        if (type == long.class || type == Long.class) return Long.parseLong(val);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(val);
        if (type == double.class || type == Double.class) return Double.parseDouble(val);
        throw new IllegalArgumentException("Unsupported type: " + type.getName());
    }

    private class LazyMethodHandler implements MethodHandler {
        private final Map<String, String> methodMappings;
        private final Map<String, Field> lazyFields;
        private final Map<String, String> storedValues;
        private final Map<String, Boolean> loadedFlags = new HashMap<>();

        public LazyMethodHandler(Map<String, String> methodMappings,
                                 Map<String, Field> lazyFields,
                                 Map<String, String> storedValues) {
            this.methodMappings = methodMappings;
            this.lazyFields = lazyFields;
            this.storedValues = storedValues;
        }

        @Override
        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
            String fieldName = methodMappings.get(thisMethod.getName());

            if (fieldName != null && !loadedFlags.getOrDefault(fieldName, false)) {
                Field field = lazyFields.get(fieldName);
                String value = storedValues.get(fieldName);
                
                setFieldValue(self, field, value);
                loadedFlags.put(fieldName, true);
            }

            return proceed.invoke(self, args);
        }
    }
}
