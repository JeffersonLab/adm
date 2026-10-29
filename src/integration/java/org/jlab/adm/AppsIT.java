package org.jlab.adm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.JsonObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Finding an app by name on the Apps page (issue #35). */
class AppsIT {

  private static final String PREFIX = "it-find-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String FIRST = PREFIX + "-a";
  private static final String SECOND = PREFIX + "-b";

  @BeforeAll
  static void addApps() throws Exception {
    Adm.awaitReady();

    for (String app : List.of(FIRST, SECOND)) {
      JsonObject result =
          Adm.json(
              Adm.post(
                  "/inventory/ajax/add-app", Adm.adminToken(), Map.of("name", app, "docUrl", "")));

      assertEquals("ok", result.getString("stat"), result.toString());
    }
  }

  @AfterAll
  static void removeApps() throws Exception {
    for (String id : ids(appsPage(Adm.adminToken(), PREFIX + "*", 0, 100))) {
      Adm.post("/inventory/ajax/remove-app", Adm.adminToken(), Map.of("appId", id));
    }
  }

  @Test
  void userFindsAppsIgnoringCase() throws Exception {
    String filter = PREFIX.toUpperCase() + "*";
    String page = appsPage(Adm.userToken("jadams"), filter, 0, 10);

    assertEquals(List.of(FIRST, SECOND), names(page), page);
    assertTrue(page.contains("Name \"" + filter + "\" {2}"), page);
    // The form keeps the filter, so paging and Max Per Page submit it again
    assertTrue(page.contains("name=\"appName\" value=\"" + filter + "\""), page);
  }

  @Test
  void pagesThroughFilteredApps() throws Exception {
    String filter = PREFIX + "*";
    String page = appsPage(Adm.userToken("jadams"), filter, 1, 1);

    assertEquals(List.of(SECOND), names(page), page);
    assertTrue(page.contains("Name \"" + filter + "\" {2 - 2 of 2}"), page);
    assertTrue(page.contains("name=\"appName\" value=\"" + filter + "\""), page);
  }

  @Test
  void exactNameFindsOneApp() throws Exception {
    String page = appsPage(Adm.userToken("jadams"), FIRST, 0, 10);

    assertEquals(List.of(FIRST), names(page), page);
  }

  private static String appsPage(String token, String appName, int offset, int max)
      throws IOException, InterruptedException {
    HttpResponse<String> page =
        Adm.get(
            "/inventory/apps?appName="
                + URLEncoder.encode(appName, StandardCharsets.UTF_8)
                + "&offset="
                + offset
                + "&max="
                + max,
            token);

    assertEquals(200, page.statusCode(), page.body());

    return Adm.unescape(page.body());
  }

  private static List<String> names(String page) {
    return matches(page, "data-name=\"([^\"]*)\"");
  }

  private static List<String> ids(String page) {
    return matches(page, "data-id=\"(\\d+)\"");
  }

  private static List<String> matches(String page, String regex) {
    List<String> values = new ArrayList<>();
    Matcher matcher = Pattern.compile(regex).matcher(page);

    while (matcher.find()) {
      values.add(matcher.group(1));
    }

    return values;
  }
}
