package com.ecs160;

import com.ecs160.annotations.Endpoint;
import com.ecs160.annotations.Microservice;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.Assert.*;

public class FrameworkTest {

    @Microservice
    public static class DummyService {
        @Endpoint(url = "/dummy")
        public String handleDummy(String body) {
            return "Dummy Response";
        }
    }

    @Microservice
    public static class HelloService {
        @Endpoint(url = "/hello")
        public String hello(String body) {
            return "Hello World";
        }
    }

    public static class RegularClass {
        @Endpoint(url = "/regular")
        public String regular(String body) {
            return "this is a regular class";
        }
    }
    
    @Test
    public void testMicroserviceCall() {
        try {
            Launcher launcher = new Launcher();
            launcher.scanForMicroservices("com.ecs160");
            Field field = Launcher.class.getDeclaredField("endpoints");
            field.setAccessible(true);
            Map<String, ?> endpoints = (Map<String, ?>) field.get(launcher);

            System.out.println("Endpoints found: " + endpoints.keySet());

            assertTrue("Should contain /dummy endpoint", endpoints.containsKey("/dummy"));
            assertTrue("Should contain /hello endpoint", endpoints.containsKey("/hello"));

            assertFalse("Should NOT contain /regular endpoint", endpoints.containsKey("/regular"));

            assertEquals("Should have found exactly 2 endpoints", 2, endpoints.size());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed" + e.getMessage());
        }
    }
}
