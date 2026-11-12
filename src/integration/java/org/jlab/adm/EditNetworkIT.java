package org.jlab.adm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.JsonObject;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Admins change the inventory only from the networks in ADM_INVENTORY_EDIT_NETWORKS, which
 * compose.yaml sets to the private ones the tests connect from. Elsewhere they get the read-only
 * pages, with the reason.
 */
class EditNetworkIT {

  /** An address in none of those networks (a documentation address, RFC 5737) */
  private static final String ELSEWHERE = "203.0.113.9";

  private static final String REFUSAL_START = "The inventory can be edited only from ";
  private static final String REFUSAL_END = ", and you are connecting from " + ELSEWHERE;

  private static final String NOTICE = "id=\"edit-refusal\"";
  private static final String EDITABLE = "editable-row-table";

  @BeforeAll
  static void awaitReady() throws Exception {
    Adm.awaitReady();
  }

  @Test
  void adminElsewhereCannotAdd() throws Exception {
    String name = "it-net-" + UUID.randomUUID().toString().substring(0, 8);

    assertRefused(
        "Unable to add App: ", post("add-app", Map.of("name", name, "docUrl", ""), ELSEWHERE));
    assertFalse(appsPage(name, null).contains("data-id="), "app added: " + name);

    assertRefused("Unable to add App: ", post("add-app-env", envForm(name), ELSEWHERE));
    assertFalse(allowlistPage(name, null).contains("data-id="), "env added: " + name);
  }

  @Test
  void adminElsewhereCannotChangeOrRemoveAnEnv() throws Exception {
    String env = Adm.addAppEnv(Adm.SERVICE_ACCOUNT, "sshd", "echo");

    try {
      String id = id(allowlistPage(env, null));

      Map<String, String> edit = envForm(env);
      edit.put("appEnvId", id);
      edit.put("deployCommand", "changed");

      assertRefused("Unable to edit App: ", post("edit-app-env", edit, ELSEWHERE));
      assertRefused(
          "Unable to remove App: ", post("remove-app-env", Map.of("appEnvId", id), ELSEWHERE));

      String page = allowlistPage(env, null);

      assertTrue(page.contains("data-deploy-command=\"echo\""), "env changed or removed: " + env);
    } finally {
      Adm.removeAppEnv(env);
    }
  }

  @Test
  void adminElsewhereCannotChangeOrRemoveAnApp() throws Exception {
    String name = "it-net-" + UUID.randomUUID().toString().substring(0, 8);

    assertEquals("ok", post("add-app", Map.of("name", name, "docUrl", ""), null).getString("stat"));

    String id = id(appsPage(name, null));

    try {
      assertRefused(
          "Unable to edit App: ",
          post("edit-app", Map.of("appId", id, "name", name + "-x", "docUrl", ""), ELSEWHERE));
      assertRefused("Unable to remove App: ", post("remove-app", Map.of("appId", id), ELSEWHERE));

      assertTrue(appsPage(name, null).contains("data-name=\"" + name + "\""), "app changed");
    } finally {
      assertEquals("ok", post("remove-app", Map.of("appId", id), null).getString("stat"));
    }
  }

  @Test
  void adminElsewhereSeesReadOnlyPagesAndWhy() throws Exception {
    for (String page :
        new String[] {allowlistPage(Adm.DEMO_ENV, ELSEWHERE), appsPage(Adm.APP, ELSEWHERE)}) {
      assertTrue(page.contains(NOTICE), "no notice");
      assertTrue(page.contains(REFUSAL_START), "no reason");
      assertTrue(page.contains(REFUSAL_END), "no address");
      assertFalse(page.contains(EDITABLE), "editable");
      // The records are still listed
      assertTrue(page.contains("data-id="), "no records");
    }
  }

  @Test
  void adminInTheNetworksSeesEditablePages() throws Exception {
    for (String page : new String[] {allowlistPage(Adm.DEMO_ENV, null), appsPage(Adm.APP, null)}) {
      assertFalse(page.contains(NOTICE), "notice shown");
      assertTrue(page.contains(EDITABLE), "not editable");
    }
  }

  /** Someone who could not edit anyway is not told about networks. */
  @Test
  void userElsewhereSeesNoNotice() throws Exception {
    HttpResponse<String> page =
        Adm.get("/inventory/app-envs?envName=" + Adm.DEMO_ENV, Adm.userToken("jadams"), ELSEWHERE);

    assertEquals(200, page.statusCode(), page.body());
    assertFalse(page.body().contains(NOTICE), "notice shown");
    assertFalse(page.body().contains(EDITABLE), "editable");
  }

  /** Only inventory edits are limited: CI deploys from outside those networks. */
  @Test
  void deployIsNotLimitedToTheNetworks() throws Exception {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("env", Adm.DEMO_ENV);
    form.put("app", Adm.APP);
    form.put("ver", "1.0.0");

    // The demo env takes deploys from admins; who may deploy is not what this checks
    String token = Adm.adminToken();
    HttpResponse<String> response = Adm.post("/deploy", token, form, ELSEWHERE);

    assertEquals(200, response.statusCode(), response.body());

    JsonObject result = Adm.json(response);

    assertTrue(result.containsKey("jobId"), result.toString());

    Adm.awaitJob(result.getJsonNumber("jobId").bigIntegerValue(), token);
  }

  private static void assertRefused(String prefix, JsonObject result) {
    assertEquals("fail", result.getString("stat"), result.toString());

    String error = result.getString("error");

    assertTrue(error.startsWith(prefix + REFUSAL_START), error);
    assertTrue(error.endsWith(REFUSAL_END), error);
  }

  private static JsonObject post(String action, Map<String, String> form, String clientAddress)
      throws IOException, InterruptedException {
    return Adm.json(Adm.post("/inventory/ajax/" + action, Adm.adminToken(), form, clientAddress));
  }

  /** An env of the demo app with this name, as the add and edit dialogs send it */
  private static Map<String, String> envForm(String envName) {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("appName", Adm.APP);
    form.put("envName", envName);
    form.put("requestUsername", Adm.SERVICE_ACCOUNT);
    form.put("deployUsername", "testuser");
    form.put("deployHostname", "sshd");
    form.put("deployPort", "22");
    form.put("deployCommand", "echo");

    return form;
  }

  private static String allowlistPage(String envName, String clientAddress)
      throws IOException, InterruptedException {
    return page("/inventory/app-envs?envName=" + envName, clientAddress);
  }

  private static String appsPage(String appName, String clientAddress)
      throws IOException, InterruptedException {
    return page("/inventory/apps?appName=" + appName, clientAddress);
  }

  private static String page(String path, String clientAddress)
      throws IOException, InterruptedException {
    HttpResponse<String> page = Adm.get(path, Adm.adminToken(), clientAddress);

    assertEquals(200, page.statusCode(), page.body());

    return page.body();
  }

  private static String id(String page) {
    Matcher id = Pattern.compile("data-id=\"(\\d+)\"").matcher(page);

    assertTrue(id.find(), "no record on the page");

    return id.group(1);
  }
}
