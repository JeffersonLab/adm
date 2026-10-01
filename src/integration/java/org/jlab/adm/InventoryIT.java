package org.jlab.adm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.json.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Changing the inventory needs the adm-admin role. */
class InventoryIT {

  @BeforeAll
  static void awaitReady() throws Exception {
    Adm.awaitReady();
  }

  @Test
  void adminAddsAndRemovesAppEnv() throws Exception {
    // Each step asserts that it succeeded
    String env = Adm.addAppEnv(Adm.SERVICE_ACCOUNT, "sshd", "echo");
    Adm.removeAppEnv(env);
  }

  @Test
  void userCannotAddAppEnv() throws Exception {
    JsonObject result =
        Adm.json(
            Adm.post(
                "/inventory/ajax/add-app-env",
                Adm.userToken("jadams"),
                Map.of(
                    "appName", Adm.APP,
                    "envName", "it-refused",
                    "requestUsername", Adm.SERVICE_ACCOUNT,
                    "deployUsername", "testuser",
                    "deployHostname", "sshd",
                    "deployPort", "22",
                    "deployCommand", "echo")));

    assertEquals("fail", result.getString("stat"));
    assertEquals(
        "Unable to add App: Not authenticated / authorized (do you need to re-login?)",
        result.getString("error"));
  }
}
