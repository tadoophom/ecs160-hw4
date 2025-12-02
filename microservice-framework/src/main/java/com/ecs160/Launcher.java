package com.ecs160;

import com.ecs160.annotations.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class Launcher {
    private Map<String, EndpointInfo> endpoints = new HashMap<>();

    public void scanForMicroservices(String pkg) throws Exception {
        ClassLoaderHelper classLoader = new ClassLoaderHelper();
        List<Class<?>> allClasses = classLoader.listClassesInAllJarsInOwnDirectory();
        
        for (Class<?> c : allClasses) {
            
            if (c.isAnnotationPresent(Microservice.class) && c.getName().startsWith(pkg)) {
                Object obj = c.getDeclaredConstructor().newInstance();
                
                for (Method m : c.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(Endpoint.class)) {
                        Endpoint endpointAnnotation = m.getAnnotation(Endpoint.class);
                        String url = endpointAnnotation.url();
                        
                        if (!url.startsWith("/")) {
                            url = "/" + url;
                        }
                        
                        EndpointInfo endpoint = new EndpointInfo(obj, m);
                        endpoints.put(url, endpoint);
                    }
                }
            }
        }
    }

    public boolean launch(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            
            
            for (Map.Entry<String, EndpointInfo> entry : endpoints.entrySet()) {
                
                String path = entry.getKey();
                EndpointInfo endpoint = entry.getValue();
                server.createContext(path, new EndpointHandler(endpoint));
            }
            
            server.setExecutor(null);
            server.start();
            System.out.println("Microservice server launched on port " + port);
            
            while (true) {

                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class EndpointHandler implements HttpHandler {
        public final EndpointInfo endpoint;

        public EndpointHandler(EndpointInfo endpoint) {
            this.endpoint = endpoint;
        }

        public void handle(HttpExchange exchange) throws IOException {

            try {
                String requestBody = readRequestBody(exchange);
                String result = (String) endpoint.method.invoke(endpoint.obj, requestBody);
                
                byte[] response = result.getBytes();
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            } 
            catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        public String readRequestBody(HttpExchange exchange) throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody()))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    public static class EndpointInfo {

        public final Object obj;
        public final Method method;
        public EndpointInfo(Object obj, Method method) {
            this.obj = obj;
            this.method = method;
        }
    }
}
