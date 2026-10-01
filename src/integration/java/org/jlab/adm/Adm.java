package org.jlab.adm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * The running app and its Keycloak, as started by {@code docker compose -f build.yaml up}.
 *
 * <p>The environment variables ADM_URL and KEYCLOAK_URL point the tests at other ports.
 */
final class Adm {

  static final String ADM_URL = env("ADM_URL", "https://localhost:8443/adm");
  static final String KEYCLOAK_URL = env("KEYCLOAK_URL", "http://localhost:8081/auth");

  /** The app in the demo data */
  static final String APP = "testapp";

  /** The env in the demo data, deploying to the sshd container */
  static final String DEMO_ENV = "local-demo";

  /** The demo users (container/keycloak) all have this password */
  static final String PASSWORD = "password";

  /** The adm client's service account, which deploys like CI does */
  static final String SERVICE_ACCOUNT = "service-account-adm";

  private static final String CLIENT_ID = "adm";
  private static final String CLIENT_SECRET = "yHi6W2raPmLvPXoxqMA7VWbLAA2WN0eB";
  private static final String TOKEN_URL =
      KEYCLOAK_URL + "/realms/test-realm/protocol/openid-connect/token";

  private static final Duration READY_TIMEOUT =
      Duration.ofSeconds(Long.parseLong(env("ADM_READY_TIMEOUT_SECONDS", "300")));
  private static final Duration JOB_TIMEOUT = Duration.ofSeconds(60);

  private static final HttpClient HTTP =
      HttpClient.newBuilder()
          .sslContext(sslContext())
          .connectTimeout(Duration.ofSeconds(5))
          .build();

  private static boolean ready = false;

  private Adm() {}

  /** A finished deploy job, as shown on the log page */
  record Job(Integer exitCode, String out, String err, String stackTrace) {}

  /** Waits for the app, its database, and Keycloak, as they may still be starting. */
  static synchronized void awaitReady() throws InterruptedException {
    Instant end = Instant.now().plus(READY_TIMEOUT);
    String problem = null;

    while (!ready && Instant.now().isBefore(end)) {
      try {
        // The admin's password login works once Keycloak's setup scripts have all run
        HttpResponse<String> response = get("/log", adminToken());
        ready = response.statusCode() == 200;
        problem = "GET /log returned " + response.statusCode();
      } catch (IOException | RuntimeException | AssertionError e) {
        // Such as a failed token request while Keycloak is still creating the realm
        problem = e.toString();
      }

      if (!ready) {
        Thread.sleep(5_000);
      }
    }

    if (!ready) {
      fail("ADM at " + ADM_URL + " not ready after " + READY_TIMEOUT + ": " + problem);
    }
  }

  /** Gets a token for a demo user with their password. */
  static String userToken(String username) throws IOException, InterruptedException {
    return token(Map.of("grant_type", "password", "username", username, "password", PASSWORD));
  }

  /** Gets a token for the adm client's service account, as CI does. */
  static String serviceAccountToken() throws IOException, InterruptedException {
    return token(Map.of("grant_type", "client_credentials"));
  }

  private static String token(Map<String, String> form) throws IOException, InterruptedException {
    String basic =
        Base64.getEncoder()
            .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

    HttpRequest request =
        HttpRequest.newBuilder(URI.create(TOKEN_URL))
            .header("Authorization", "Basic " + basic)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(encode(form)))
            .build();

    HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode(), "token request failed: " + response.body());

    return json(response).getString("access_token");
  }

  /** Requests a deploy; null values leave the parameter out. */
  static JsonObject deploy(String token, String env, String app, String ver)
      throws IOException, InterruptedException {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("env", env);
    form.put("app", app);
    form.put("ver", ver);
    form.values().removeIf(v -> v == null);

    HttpResponse<String> response = post("/deploy", token, form);

    assertEquals(200, response.statusCode(), response.body());

    return json(response);
  }

  /** Requests a deploy that is expected to start, and waits for its job to finish. */
  static Job deployAndWait(String token, String env, String ver)
      throws IOException, InterruptedException {
    JsonObject result = deploy(token, env, APP, ver);

    assertTrue(result.containsKey("jobId"), result.toString());

    return awaitJob(result.getJsonNumber("jobId").bigIntegerValue(), token);
  }

  /** Waits for a deploy job to finish, reading it from the log page. */
  static Job awaitJob(BigInteger jobId, String token) throws IOException, InterruptedException {
    Pattern row =
        Pattern.compile(
            "<tr data-out=\"(.*?)\" data-err=\"(.*?)\" data-trace=\"(.*?)\">(.*?)</tr>",
            Pattern.DOTALL);
    // The period shows an end time once the job has finished
    Pattern finished = Pattern.compile("\\d{2}:\\d{2}:\\d{2} -\\s+\\d{2}-");
    Pattern exitCode = Pattern.compile("Exit Code:\\s*(\\S+)");

    Instant end = Instant.now().plus(JOB_TIMEOUT);

    while (Instant.now().isBefore(end)) {
      HttpResponse<String> response = get("/log?jobId=" + jobId, token);

      assertEquals(200, response.statusCode(), response.body());

      Matcher m = row.matcher(response.body());

      if (m.find() && finished.matcher(m.group(4)).find()) {
        Matcher code = exitCode.matcher(m.group(4));
        assertTrue(code.find(), m.group(4));

        return new Job(
            "None".equals(code.group(1)) ? null : Integer.valueOf(code.group(1)),
            unescape(m.group(1)),
            unescape(m.group(2)),
            unescape(m.group(3)));
      }

      Thread.sleep(500);
    }

    return fail("Job " + jobId + " did not finish within " + JOB_TIMEOUT);
  }

  /**
   * Adds an env of the demo app, as an admin, with a unique name.
   *
   * @return the env name
   */
  static String addAppEnv(String requestUsername, String hostname, String deployCommand)
      throws IOException, InterruptedException {
    String envName = "it-" + UUID.randomUUID().toString().substring(0, 8);

    Map<String, String> form = new LinkedHashMap<>();
    form.put("appName", APP);
    form.put("envName", envName);
    form.put("requestUsername", requestUsername);
    form.put("deployUsername", "testuser");
    form.put("deployHostname", hostname);
    form.put("deployPort", "22");
    form.put("deployCommand", deployCommand);

    JsonObject result = json(post("/inventory/ajax/add-app-env", adminToken(), form));

    assertEquals("ok", result.getString("stat"), result.toString());

    return envName;
  }

  /** Removes an env added by addAppEnv, with its deploy jobs. */
  static void removeAppEnv(String envName) throws IOException, InterruptedException {
    String token = adminToken();

    HttpResponse<String> page =
        get("/inventory/app-envs?appName=" + APP + "&envName=" + envName, token);
    Matcher id = Pattern.compile("data-id=\"(\\d+)\"").matcher(page.body());

    assertTrue(id.find(), "env not found: " + envName);

    JsonObject result =
        json(post("/inventory/ajax/remove-app-env", token, Map.of("appEnvId", id.group(1))));

    assertEquals("ok", result.getString("stat"), result.toString());
  }

  /** tbrown has the adm-admin role */
  static String adminToken() throws IOException, InterruptedException {
    return userToken("tbrown");
  }

  static HttpResponse<String> get(String path, String token)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(ADM_URL + path));

    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }

    return HTTP.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
  }

  static HttpResponse<String> post(String path, String token, Map<String, String> form)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(ADM_URL + path))
            .header("Content-Type", "application/x-www-form-urlencoded");

    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }

    return HTTP.send(
        builder.POST(HttpRequest.BodyPublishers.ofString(encode(form))).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  static JsonObject json(HttpResponse<String> response) {
    try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
      return reader.readObject();
    }
  }

  private static String encode(Map<String, String> form) {
    return form.entrySet().stream()
        .map(
            e ->
                URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }

  /** Reverses the JSTL fn:escapeXml used on the log page */
  private static String unescape(String text) {
    return text.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&#039;", "'")
        .replace("&#034;", "\"")
        .replace("&amp;", "&");
  }

  /**
   * Trusts the usual CAs, plus the self-signed certificate of the jeffersonlab/wildfly image that
   * the app runs in. If that image changes its certificate, save the new one with: {@code openssl
   * s_client -connect localhost:8443 </dev/null | openssl x509 >
   * src/integration/resources/wildfly-localhost.crt}
   */
  private static SSLContext sslContext() {
    Path cacerts = Path.of(System.getProperty("java.home"), "lib", "security", "cacerts");

    try (InputStream defaults = Files.newInputStream(cacerts);
        InputStream wildfly = Adm.class.getResourceAsStream("/wildfly-localhost.crt")) {
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(defaults, null);
      trustStore.setCertificateEntry(
          "wildfly-localhost",
          CertificateFactory.getInstance("X.509").generateCertificate(wildfly));

      TrustManagerFactory trustManagers =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagers.init(trustStore);

      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, trustManagers.getTrustManagers(), null);
      return context;
    } catch (IOException | GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String env(String name, String defaultValue) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? defaultValue : value;
  }
}
