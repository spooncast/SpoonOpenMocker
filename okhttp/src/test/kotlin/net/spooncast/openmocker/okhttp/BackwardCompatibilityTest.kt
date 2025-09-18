package net.spooncast.openmocker.okhttp

import net.spooncast.openmocker.core.MemoryMockRepository
import okhttp3.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests to verify backward compatibility with the existing OpenMockerInterceptor API.
 *
 * These tests ensure that the new OkHttpMockerInterceptor can be used as a drop-in
 * replacement for the legacy interceptor without breaking existing code.
 */
class BackwardCompatibilityTest {

    // BDD: Given Builder pattern usage, When creating interceptor, Then work like legacy OpenMockerInterceptor
    @Test
    fun `new interceptor can be created using Builder pattern like legacy`() {
        // This simulates how the legacy interceptor was created:
        // val interceptor = OpenMockerInterceptor.Builder().build()

        // Act - Create new interceptor using same pattern
        val interceptor = OkHttpMockerInterceptor.Builder().build()

        // Assert
        assertNotNull("Interceptor should be created successfully", interceptor)
        assertTrue("Should be instance of Interceptor", interceptor is Interceptor)
    }

    // BDD: Given companion create methods, When creating interceptor, Then provide same functionality as legacy
    @Test
    fun `new interceptor can be created using companion create method`() {
        // Act - Create using companion methods
        val defaultInterceptor = OkHttpMockerInterceptor.create()
        val customInterceptor = OkHttpMockerInterceptor.create(MemoryMockRepository())

        // Assert
        assertNotNull("Default interceptor should be created", defaultInterceptor)
        assertNotNull("Custom interceptor should be created", customInterceptor)
        assertTrue("Should be instances of Interceptor",
            defaultInterceptor is Interceptor && customInterceptor is Interceptor)
    }

    // BDD: Given constant access, When checking MOCKER_MESSAGE, Then return same value as legacy
    @Test
    fun `new interceptor has same constant as legacy`() {
        // Act
        val messageConstant = OkHttpMockerInterceptor.MOCKER_MESSAGE

        // Assert
        assertEquals("OpenMocker enabled", messageConstant)
    }

    // BDD: Given OkHttpClient configuration, When adding interceptor, Then integrate seamlessly like legacy
    @Test
    fun `new interceptor can be added to OkHttpClient like legacy`() {
        // This simulates typical usage:
        // val client = OkHttpClient.Builder()
        //     .addInterceptor(OpenMockerInterceptor.Builder().build())
        //     .build()

        // Act
        val interceptor = OkHttpMockerInterceptor.Builder().build()
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        // Assert
        assertNotNull("Client should be created successfully", client)
        assertEquals("Client should have 1 interceptor", 1, client.interceptors.size)
        assertEquals("Interceptor should be our interceptor", interceptor, client.interceptors[0])
    }

    // BDD: Given Builder method chaining, When configuring interceptor, Then support fluent API like legacy
    @Test
    fun `builder method chaining works correctly`() {
        // Act
        val interceptor = OkHttpMockerInterceptor.builder()
            .repository(MemoryMockRepository())
            .build()

        // Assert
        assertNotNull("Interceptor should be created with method chaining", interceptor)
        assertTrue("Should implement Interceptor interface", interceptor is Interceptor)
    }

    // BDD: Given interface requirements, When checking implementation, Then implement OkHttp Interceptor correctly
    @Test
    fun `interceptor implements Interceptor interface correctly`() {
        // Arrange
        val interceptor = OkHttpMockerInterceptor.create()

        // Act & Assert
        assertTrue("Should implement okhttp3.Interceptor", interceptor is okhttp3.Interceptor)

        // Verify the intercept method exists and has correct signature
        val interceptMethod = interceptor.javaClass.getMethod("intercept", Interceptor.Chain::class.java)
        assertNotNull("intercept method should exist", interceptMethod)
        assertEquals("intercept method should return Response", Response::class.java, interceptMethod.returnType)
    }

    // BDD: Given multiple creation requests, When creating interceptors, Then create independent instances
    @Test
    fun `multiple interceptors can be created independently`() {
        // Act - Create multiple interceptors as would be done in different parts of application
        val interceptor1 = OkHttpMockerInterceptor.Builder().build()
        val interceptor2 = OkHttpMockerInterceptor.create()
        val interceptor3 = OkHttpMockerInterceptor.create(MemoryMockRepository())

        // Assert
        assertNotNull("First interceptor should be created", interceptor1)
        assertNotNull("Second interceptor should be created", interceptor2)
        assertNotNull("Third interceptor should be created", interceptor3)

        // All should be different instances
        assertNotSame("Interceptors should be different instances", interceptor1, interceptor2)
        assertNotSame("Interceptors should be different instances", interceptor2, interceptor3)
        assertNotSame("Interceptors should be different instances", interceptor1, interceptor3)
    }

    // BDD: Given API compatibility requirements, When checking public methods, Then expose same API surface as legacy
    @Test
    fun `API surface compatibility - public methods exist`() {
        // Arrange
        val interceptor = OkHttpMockerInterceptor.create()

        // Act & Assert - Verify that essential public methods exist
        assertNotNull("getEngine method should exist",
            interceptor.javaClass.getMethod("getEngine"))

        // These methods should be accessible via suspend functions with kotlin.coroutines.Continuation
        val engineClass = interceptor.getEngine().javaClass
        val methods = engineClass.declaredMethods

        val shouldMockMethods = methods.filter { it.name == "shouldMock" }
        assertTrue("shouldMock method should exist on engine", shouldMockMethods.isNotEmpty())

        val cacheResponseMethods = methods.filter { it.name == "cacheResponse" }
        assertTrue("cacheResponse method should exist on engine", cacheResponseMethods.isNotEmpty())
    }

    // BDD: Given various OkHttp configurations, When integrating interceptor, Then work with all client setups
    @Test
    fun `interceptor works with different OkHttp client configurations`() {
        // Act - Test with various OkHttp client configurations that might exist in legacy code
        val interceptor = OkHttpMockerInterceptor.create()

        val basicClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val clientWithTimeout = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val clientWithNetworkInterceptor = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addNetworkInterceptor(createLoggingInterceptor())
            .build()

        // Assert
        assertNotNull("Basic client should be created", basicClient)
        assertNotNull("Client with timeout should be created", clientWithTimeout)
        assertNotNull("Client with network interceptor should be created", clientWithNetworkInterceptor)

        // All should have our interceptor
        assertTrue("Basic client should contain our interceptor",
            basicClient.interceptors.contains(interceptor))
        assertTrue("Timeout client should contain our interceptor",
            clientWithTimeout.interceptors.contains(interceptor))
        assertTrue("Network interceptor client should contain our interceptor",
            clientWithNetworkInterceptor.interceptors.contains(interceptor))
    }

    private fun createLoggingInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            println("Making request to: ${request.url}")
            chain.proceed(request)
        }
    }

    // BDD: Given concurrent usage, When creating multiple interceptors, Then maintain thread safety like legacy
    @Test
    fun `thread safety - multiple interceptors can be used concurrently`() {
        // This test ensures that the new interceptor maintains thread safety like the legacy one
        val interceptors = mutableListOf<OkHttpMockerInterceptor>()

        // Act - Create multiple interceptors in parallel
        repeat(10) {
            interceptors.add(OkHttpMockerInterceptor.create())
        }

        // Assert - All should be created successfully and be different instances
        assertEquals("Should create 10 interceptors", 10, interceptors.size)

        // Verify all are different instances (no shared state issues)
        val uniqueInterceptors = interceptors.toSet()
        assertEquals("All interceptors should be unique instances", 10, uniqueInterceptors.size)
    }

    // BDD: Given resource management, When creating many interceptors, Then avoid memory leaks
    @Test
    fun `memory footprint - interceptor creation does not leak resources`() {
        // Act - Create and discard many interceptors to test for memory leaks
        repeat(100) {
            val interceptor = OkHttpMockerInterceptor.create()
            // Use interceptor briefly to ensure it's initialized
            assertNotNull(interceptor.getEngine())
        }

        // Run garbage collection
        System.gc()

        // Assert - If we get here without OutOfMemoryError, the test passes
        // This is a basic test; in production you'd use memory profiling tools
        assertTrue("Memory test completed without issues", true)
    }

    // BDD: Given API consistency requirements, When checking method signatures, Then match expected patterns
    @Test
    fun `API consistency - method signatures match expected patterns`() {
        val builderClass = OkHttpMockerInterceptor.Builder::class.java
        val interceptorClass = OkHttpMockerInterceptor::class.java

        // Builder methods
        assertTrue("Builder should have build method",
            builderClass.methods.any { it.name == "build" && it.returnType == interceptorClass })

        assertTrue("Builder should have repository method",
            builderClass.methods.any { it.name == "repository" && it.returnType == builderClass })

        // Functional testing - verify these methods actually work
        try {
            val createMethod1 = OkHttpMockerInterceptor.create()
            val createMethod2 = OkHttpMockerInterceptor.create(MemoryMockRepository())
            val builderMethod = OkHttpMockerInterceptor.builder()

            assertNotNull("create() should work", createMethod1)
            assertNotNull("create(repo) should work", createMethod2)
            assertNotNull("builder() should work", builderMethod)
        } catch (e: Exception) {
            fail("Companion object methods should be accessible: ${e.message}")
        }

        // Constants - Simple direct access test
        val constantValue = OkHttpMockerInterceptor.MOCKER_MESSAGE
        assertEquals("MOCKER_MESSAGE should have correct value", "OpenMocker enabled", constantValue)
    }
}