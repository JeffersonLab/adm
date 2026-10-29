package org.jlab.adm.business.session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jlab.smoothness.business.exception.UserFriendlyException;
import org.junit.jupiter.api.Test;

class AppEnvFacadeTest {

  private static final String PORT_MESSAGE =
      "Deploy Port must be a whole number from 1 to 65535 (SSH usually uses 22)";

  @Test
  void acceptsPortsSshCanUse() {
    assertDoesNotThrow(() -> validateWithPort(1));
    assertDoesNotThrow(() -> validateWithPort(22));
    assertDoesNotThrow(() -> validateWithPort(65535));
  }

  @Test
  void refusesPortsOutOfRange() {
    for (Integer port : new Integer[] {null, -22, 0, 65536}) {
      UserFriendlyException e =
          assertThrows(UserFriendlyException.class, () -> validateWithPort(port));
      assertEquals(PORT_MESSAGE, e.getUserMessage());
    }
  }

  @Test
  void refusesBlankFields() {
    UserFriendlyException e =
        assertThrows(
            UserFriendlyException.class,
            () -> AppEnvFacade.validate("app", "env", " ", "deployer", "host", 22, "echo"));
    assertEquals("Request Username cannot be empty", e.getUserMessage());

    e =
        assertThrows(
            UserFriendlyException.class,
            () -> AppEnvFacade.validate("app", "env", "requester", "deployer", "host", 22, null));
    assertEquals("Deploy Command cannot be empty", e.getUserMessage());
  }

  private static void validateWithPort(Integer port) throws UserFriendlyException {
    AppEnvFacade.validate("app", "env", "requester", "deployer", "host", port, "echo");
  }
}
