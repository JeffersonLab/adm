package org.jlab.adm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Adding a deployment allowlist record (app env), with its app chosen or created (issue #36). */
class AllowlistIT {

  @BeforeAll
  static void awaitReady() throws Exception {
    Adm.awaitReady();
  }

  @Test
  void inventoryOpensAllowlist() throws Exception {
    HttpResponse<String> page = Adm.get("/inventory/apps", Adm.userToken("jadams"));

    assertEquals(200, page.statusCode());
    assertTrue(page.body().contains("/inventory/app-envs\">Inventory</a>"), page.body());
  }

  @Test
  void adminAddsNewAppWithFirstEnv() throws Exception {
    String app = uniqueAppName();
    try {
      JsonObject result =
          addAppEnv(Adm.adminToken(), app, true, "https://example.com/docs", "dev", "echo");

      assertEquals("ok", result.getString("stat"), result.toString());

      String apps = appsPage(app);
      assertTrue(apps.contains("data-url=\"https://example.com/docs\""), apps);

      HttpResponse<String> envs =
          Adm.get("/inventory/app-envs?appName=" + app + "&envName=dev", Adm.adminToken());
      assertTrue(envs.body().contains("data-env-name=\"dev\""), envs.body());
    } finally {
      removeApp(app);
    }
  }

  @Test
  void newAppIsNotKeptWhenItsEnvFails() throws Exception {
    String app = uniqueAppName();
    // Longer than the deploy command column, so the env fails after the app is persisted
    String command = "echo " + "x".repeat(300);

    JsonObject result = addAppEnv(Adm.adminToken(), app, true, null, "dev", command);

    assertEquals("fail", result.getString("stat"), result.toString());
    assertFalse(appsPage(app).contains("data-id="), "orphan app left: " + app);
  }

  @Test
  void mistypedAppIsRefused() throws Exception {
    JsonObject result = addAppEnv(Adm.adminToken(), "TestApp", false, null, "it-typo", "echo");

    assertEquals("fail", result.getString("stat"));
    assertEquals(
        "Unable to add App: App not found with name: TestApp (did you mean " + Adm.APP + "?)",
        result.getString("error"));
  }

  @Test
  void unknownAppIsRefusedAndNotCreated() throws Exception {
    String app = uniqueAppName();

    JsonObject result = addAppEnv(Adm.adminToken(), app, false, null, "dev", "echo");

    assertEquals("fail", result.getString("stat"));
    assertEquals(
        "Unable to add App: App not found with name: "
            + app
            + " (choose an existing app, or Create new App to add it)",
        result.getString("error"));
    assertFalse(appsPage(app).contains("data-id="), "app created: " + app);
  }

  @Test
  void newAppWithExistingNameIsRefused() throws Exception {
    JsonObject result = addAppEnv(Adm.adminToken(), "TESTAPP", true, null, "it-dup", "echo");

    assertEquals("fail", result.getString("stat"));
    assertEquals(
        "Unable to add App: App already exists with name: " + Adm.APP, result.getString("error"));
  }

  @Test
  void duplicateEnvIsRefused() throws Exception {
    JsonObject result = addAppEnv(Adm.adminToken(), Adm.APP, false, null, Adm.DEMO_ENV, "echo");

    assertEquals("fail", result.getString("stat"));
    assertEquals(
        "Unable to add App: App " + Adm.APP + " already has an env named " + Adm.DEMO_ENV,
        result.getString("error"));
  }

  @Test
  void userCannotAddNewApp() throws Exception {
    String app = uniqueAppName();

    JsonObject result = addAppEnv(Adm.userToken("jadams"), app, true, null, "dev", "echo");

    assertEquals("fail", result.getString("stat"));
    assertEquals(
        "Unable to add App: Not authenticated / authorized (do you need to re-login?)",
        result.getString("error"));
    assertFalse(appsPage(app).contains("data-id="), "app created: " + app);
  }

  @Test
  void searchFindsAppsIgnoringCase() throws Exception {
    HttpResponse<String> response =
        Adm.get("/inventory/ajax/search-apps?term=ESTAP", Adm.userToken("jadams"));

    assertEquals(200, response.statusCode(), response.body());

    try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
      JsonArray names = reader.readArray();
      assertTrue(names.contains(Json.createValue(Adm.APP)), names.toString());
      assertTrue(
          names.getValuesAs(JsonString.class).stream()
              .allMatch(n -> n.getString().toLowerCase().contains("estap")),
          names.toString());
    }
  }

  private static String uniqueAppName() {
    return "it-app-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private static JsonObject addAppEnv(
      String token, String app, boolean newApp, String docUrl, String env, String deployCommand)
      throws IOException, InterruptedException {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("appName", app);
    form.put("newApp", newApp ? "Y" : "N");
    if (docUrl != null) {
      form.put("docUrl", docUrl);
    }
    form.put("envName", env);
    form.put("requestUsername", Adm.SERVICE_ACCOUNT);
    form.put("deployUsername", "testuser");
    form.put("deployHostname", "sshd");
    form.put("deployPort", "22");
    form.put("deployCommand", deployCommand);

    return Adm.json(Adm.post("/inventory/ajax/add-app-env", token, form));
  }

  private static String appsPage(String app) throws IOException, InterruptedException {
    HttpResponse<String> page =
        Adm.get(
            "/inventory/apps?appName=" + URLEncoder.encode(app, StandardCharsets.UTF_8),
            Adm.adminToken());

    assertEquals(200, page.statusCode(), page.body());

    return page.body();
  }

  /** Removes an app, which removes its envs with it. */
  private static void removeApp(String app) throws IOException, InterruptedException {
    Matcher id = Pattern.compile("data-id=\"(\\d+)\"").matcher(appsPage(app));

    assertTrue(id.find(), "app not found: " + app);

    JsonObject result =
        Adm.json(
            Adm.post("/inventory/ajax/remove-app", Adm.adminToken(), Map.of("appId", id.group(1))));

    assertEquals("ok", result.getString("stat"), result.toString());
  }
}
