package org.jlab.adm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.json.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Logging in as a person does: redirected to Keycloak's login form, and back with a session cookie.
 * The other tests send bearer tokens, which the server checks differently (such as their audience).
 */
class BrowserLoginIT {

  @BeforeAll
  static void awaitReady() throws Exception {
    Adm.awaitReady();
  }

  @Test
  void adminLogsInAtKeycloakAndEditsWithTheSession() throws Exception {
    Browser browser = new Browser();
    String allowlist = Adm.ADM_URL + "/inventory/app-envs";

    HttpResponse<String> form = browser.get(allowlist);

    assertEquals(200, form.statusCode(), form.body());
    assertTrue(
        form.uri().getPath().endsWith("/protocol/openid-connect/auth"), form.uri().toString());

    Matcher action = Pattern.compile("<form[^>]*action=\"([^\"]+)\"").matcher(form.body());

    assertTrue(action.find(), "no login form");

    Map<String, String> login = new LinkedHashMap<>();
    login.put("username", "tbrown");
    login.put("password", Adm.PASSWORD);

    HttpResponse<String> page = browser.post(Adm.unescape(action.group(1)), login);

    assertEquals(200, page.statusCode(), page.body());
    assertTrue(page.uri().toString().startsWith(allowlist), page.uri().toString());
    assertTrue(page.body().contains("editable-row-table"), "not editable as tbrown");

    // The session alone, with no token, is enough to edit
    String app = "it-login-" + UUID.randomUUID().toString().substring(0, 8);
    String ajax = Adm.ADM_URL + "/inventory/ajax/";

    JsonObject added = Adm.json(browser.post(ajax + "add-app", Map.of("name", app, "docUrl", "")));

    assertEquals("ok", added.getString("stat"), added.toString());

    Matcher id =
        Pattern.compile("data-id=\"(\\d+)\"")
            .matcher(browser.get(Adm.ADM_URL + "/inventory/apps?appName=" + app).body());

    assertTrue(id.find(), "app not found: " + app);

    JsonObject removed = Adm.json(browser.post(ajax + "remove-app", Map.of("appId", id.group(1))));

    assertEquals("ok", removed.getString("stat"), removed.toString());
  }

  /**
   * Follows redirects and returns each server's cookies to it, whatever their attributes: Keycloak
   * marks its cookies Secure though the demo serves it over http, which browsers allow on localhost
   * and Java's CookieManager does not.
   */
  private static final class Browser {

    private static final int MAX_REDIRECTS = 10;

    /** Cookies by the server (host and port) that set them */
    private final Map<String, Map<String, String>> cookies = new HashMap<>();

    HttpResponse<String> get(String url) throws IOException, InterruptedException {
      return send(URI.create(url), null);
    }

    HttpResponse<String> post(String url, Map<String, String> form)
        throws IOException, InterruptedException {
      return send(URI.create(url), form);
    }

    private HttpResponse<String> send(URI uri, Map<String, String> form)
        throws IOException, InterruptedException {
      for (int i = 0; i <= MAX_REDIRECTS; i++) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        Map<String, String> jar = cookies.computeIfAbsent(uri.getAuthority(), a -> new HashMap<>());

        if (!jar.isEmpty()) {
          builder.header(
              "Cookie",
              jar.entrySet().stream()
                  .map(c -> c.getKey() + "=" + c.getValue())
                  .collect(Collectors.joining("; ")));
        }

        if (form == null) {
          builder.GET();
        } else {
          builder
              .header("Content-Type", "application/x-www-form-urlencoded")
              .POST(HttpRequest.BodyPublishers.ofString(encode(form)));
        }

        HttpResponse<String> response =
            Adm.HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        for (String cookie : response.headers().allValues("Set-Cookie")) {
          String[] pair = cookie.split(";", 2)[0].split("=", 2);

          if (pair.length == 2 && !pair[1].isEmpty()) {
            jar.put(pair[0].trim(), pair[1]);
          } else {
            jar.remove(pair[0].trim());
          }
        }

        String location = response.headers().firstValue("Location").orElse(null);

        if (response.statusCode() / 100 != 3 || location == null) {
          return response;
        }

        // As a browser does after a form post
        uri = uri.resolve(location);
        form = null;
      }

      return fail("more than " + MAX_REDIRECTS + " redirects, last to " + uri);
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
  }
}
