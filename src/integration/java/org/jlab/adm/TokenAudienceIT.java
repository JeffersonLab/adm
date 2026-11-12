package org.jlab.adm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.StringReader;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * ADM takes only the bearer tokens meant for it: those whose audience names its client. A realm
 * shared with other applications issues tokens to them for the same users, with the same roles.
 */
class TokenAudienceIT {

  /** The second client in the test realm (container/keycloak) */
  private static final String OTHER_CLIENT = "other-app";

  private static final String OTHER_SECRET = "other-app-test-only-secret";

  @BeforeAll
  static void awaitReady() throws Exception {
    Adm.awaitReady();
  }

  @Test
  void tokenForAdmNamesItAsAudience() throws Exception {
    assertTrue(audience(Adm.adminToken()).contains(Adm.CLIENT_ID), "user's token");
    assertTrue(audience(Adm.serviceAccountToken()).contains(Adm.CLIENT_ID), "service account's");
  }

  @Test
  void otherClientsServiceAccountIsRefused() throws Exception {
    String token =
        Adm.token(OTHER_CLIENT, OTHER_SECRET, Map.of("grant_type", "client_credentials"));

    assertFalse(audience(token).contains(Adm.CLIENT_ID), "the test needs a token not for adm");
    assertRefused(Adm.get("/log", token));
    assertRefused(
        Adm.post("/deploy", token, Map.of("env", Adm.DEMO_ENV, "app", Adm.APP, "ver", "1.0.0")));
  }

  /** The admin's token from another application would otherwise carry the admin role here. */
  @Test
  void adminsTokenFromOtherClientIsRefused() throws Exception {
    String token =
        Adm.token(
            OTHER_CLIENT,
            OTHER_SECRET,
            Map.of("grant_type", "password", "username", "tbrown", "password", Adm.PASSWORD));

    assertFalse(audience(token).contains(Adm.CLIENT_ID), "the test needs a token not for adm");
    assertRefused(Adm.get("/inventory/app-envs", token));
    assertRefused(Adm.job(token, "1"));
  }

  private static void assertRefused(HttpResponse<String> response) {
    assertEquals(401, response.statusCode(), response.body());

    String challenge = response.headers().firstValue("WWW-Authenticate").orElse("");

    assertTrue(challenge.contains("invalid_token"), challenge);
  }

  /** The aud claim of a token, which Keycloak writes as one name or a list of them. */
  private static List<String> audience(String token) {
    String payload =
        new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

    try (JsonReader reader = Json.createReader(new StringReader(payload))) {
      JsonObject claims = reader.readObject();
      JsonValue aud = claims.get("aud");

      if (aud == null) {
        return List.of();
      }

      if (aud.getValueType() == JsonValue.ValueType.STRING) {
        return List.of(((JsonString) aud).getString());
      }

      return claims.getJsonArray("aud").getValuesAs(JsonString.class).stream()
          .map(JsonString::getString)
          .toList();
    }
  }
}
