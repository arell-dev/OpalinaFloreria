package com.floral.opalina;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import java.net.CookieManager;
import java.net.CookiePolicy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.profiles.active=railway",
            "DEMO_ACCESS_USERNAME=demo",
            "DEMO_ACCESS_PASSWORD=test-password",
            "opalina.demo-data.enabled=false"
        })
class RailwaySecurityIntegrationTests {

    @SuppressWarnings("resource")
    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4")
            .withDatabaseName("opalina_railway_test")
            .withUsername("opalina")
            .withPassword("opalina_test");

    @Value("${local.server.port}")
    private int port;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Test
    void readinessIsPublicWhileStoreRequiresDemoCredentials() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> readiness = client.send(
                request("/actuator/health/readiness", null), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> anonymousStore = client.send(
                request("/catalogo", null), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> authenticatedStore = client.send(
                request("/catalogo", demoAuthorization()), HttpResponse.BodyHandlers.ofString());

        assertThat(readiness.statusCode()).isEqualTo(200);
        assertThat(anonymousStore.statusCode()).isEqualTo(401);
        assertThat(authenticatedStore.statusCode()).isEqualTo(200);
    }

    @Test
    void apiStaysBlockedEvenWithDemoCredentials() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request("/api/productos", demoAuthorization()), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void productUploadsAreDisabledInTheRailwayDemo() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        HttpResponse<String> loginPage = client.send(
                request("/login", demoAuthorization()), HttpResponse.BodyHandlers.ofString());
        Matcher csrfToken = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(loginPage.body());
        assertThat(loginPage.statusCode()).isEqualTo(200);
        assertThat(csrfToken.find()).isTrue();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/admin/productos"))
                .header("Authorization", demoAuthorization())
                .header("X-CSRF-TOKEN", csrfToken.group(1))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(403);
    }

    private HttpRequest request(String path, String authorization) {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET();
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return request.build();
    }

    private String demoAuthorization() {
        String credentials = Base64.getEncoder()
                .encodeToString("demo:test-password".getBytes(StandardCharsets.UTF_8));
        return "Basic " + credentials;
    }
}
