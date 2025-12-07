package com.ticketblitz.registry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Service Registry
 *
 * WHAT WE'RE TESTING :
 * ====================
 *
 * 1. Application starts successfully
 * 2. Eureka Server is running
 * 3. Dashboard is accessible
 * 4. Health endpoint works
 *
 * WHY INTEGRATION TEST (not unit test) :
 * ======================================
 *
 * - We're testing the entire application stack
 * - Real HTTP server starts
 * - Actual Eureka server runs
 * - Tests real behaviour, not mocks
 *
 * @SpringbootTest(webEnvironment=RANDORM_PORT)
 * - Starts full application
 * - Uses random port (avoids conflicts)
 * - Injects TestRestTemplate for HTTP calls
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServiceRegistryApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Test 1: Context Loads
     *
     * Basic smoke test - does the application start?
     * If this fails, something is fundamentally broken
     */
    @Test
    void contextLoads() {
        // If we get here, Spring context loaded successfully
        assertThat(port).isGreaterThan(0);
    }

    /**
     * Test 2 : Eureka dashboard is accessible
     *
     * verifies the web UI is working
     * GET http://localhost:{port}/
     *
     */
    @Test
    void eurekaDashboardIsAccessible() {
        // call the eureka homepage
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/",
                String.class
        );

        // should return 200 OK
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // should contain "Eureka" in the HTML
        assertThat(response.getBody()).contains("Eureka");
    }

    /**
     * Test 3 : Health check Endpoint works
     *
     * Spring Boot Actuator exposes /actuator/health
     * Should return {"status":"UP"}
     */
    @Test
    void healthEndpointReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    /**
     * Test 4 : Eureka registry Endpoint works
     *
     * The /eureka/apps endpoint is used by clients to fetch registry
     * Should return XML with registered applications (empty for now)
     */
    @Test
    void eurekaRegistryEndpointExists() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/eureka/apps",
                String.class
        );

        // Should return 200 OK (even if no apps registered)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}