package io.a2a.extras.opentelemetry;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class SpanInterceptorTest {

    private SpanInterceptor interceptor = new SpanInterceptor();

    /**
     * Test the stripCdiProxySuffix method using reflection since it's private.
     */
    private String invokeStripCdiProxySuffix(String className) throws Exception {
        Method method = SpanInterceptor.class.getDeclaredMethod("stripCdiProxySuffix", String.class);
        method.setAccessible(true);
        return (String) method.invoke(interceptor, className);
    }

    @Test
    void testStripCdiProxySuffix_Subclass() throws Exception {
        assertEquals("com.example.MyService",
                invokeStripCdiProxySuffix("com.example.MyService_Subclass"));
    }

    @Test
    void testStripCdiProxySuffix_ClientProxy() throws Exception {
        assertEquals("com.example.MyService",
                invokeStripCdiProxySuffix("com.example.MyService_ClientProxy"));
    }

    @Test
    void testStripCdiProxySuffix_WeldSubclass() throws Exception {
        assertEquals("com.example.MyService",
                invokeStripCdiProxySuffix("com.example.MyService$$_WeldSubclass"));
    }

    @Test
    void testStripCdiProxySuffix_Javassist() throws Exception {
        assertEquals("com.example.MyService",
                invokeStripCdiProxySuffix("com.example.MyService_$$_javassist_1"));
    }

    @Test
    void testStripCdiProxySuffix_ProxyPattern() throws Exception {
        assertEquals("com.example.MyService",
                invokeStripCdiProxySuffix("com.example.MyService$Proxy$_$$_123"));
    }

    @Test
    void testStripCdiProxySuffix_NoSuffix() throws Exception {
        // Class without CDI proxy suffix should remain unchanged
        assertEquals("com.example.MyService",
                invokeStripCdiProxySuffix("com.example.MyService"));
    }

    @Test
    void testStripCdiProxySuffix_Null() throws Exception {
        // Null input should return null
        assertNull(invokeStripCdiProxySuffix(null));
    }

    @Test
    void testStripCdiProxySuffix_EmptyString() throws Exception {
        // Empty string should remain unchanged
        assertEquals("", invokeStripCdiProxySuffix(""));
    }

    @Test
    void testStripCdiProxySuffix_MultipleSuffixes() throws Exception {
        // Only the first matching suffix should be removed
        // This tests that we stop at the first match
        assertEquals("com.example.MyService",
                invokeStripCdiProxySuffix("com.example.MyService_Subclass_ClientProxy"));
    }

    @Test
    void testStripCdiProxySuffix_InnerClass() throws Exception {
        // Inner class with proxy suffix
        assertEquals("com.example.OuterClass$InnerService",
                invokeStripCdiProxySuffix("com.example.OuterClass$InnerService_Subclass"));
    }

    @Test
    void testStripCdiProxySuffix_PackageWithSimilarNames() throws Exception {
        // Ensure we don't accidentally strip part of the package name
        assertEquals("com.example.subclass.MyService",
                invokeStripCdiProxySuffix("com.example.subclass.MyService_Subclass"));
    }

    @Test
    void testStripCdiProxySuffix_ShortClassName() throws Exception {
        // Very short class name
        assertEquals("A",
                invokeStripCdiProxySuffix("A_Subclass"));
    }
}
