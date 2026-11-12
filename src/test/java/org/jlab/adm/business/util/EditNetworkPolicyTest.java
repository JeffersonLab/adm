package org.jlab.adm.business.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jlab.smoothness.business.exception.UserFriendlyException;
import org.junit.jupiter.api.Test;

class EditNetworkPolicyTest {

  @Test
  void allowsEveryAddressWhenUnset() {
    for (String setting : new String[] {null, "", "  "}) {
      EditNetworkPolicy policy = new EditNetworkPolicy(setting);

      assertFalse(policy.isRestricted());
      assertTrue(policy.allows("203.0.113.9"));
      assertTrue(policy.allows(null));
      assertNull(policy.refusal("203.0.113.9"));
    }
  }

  @Test
  void allowsOnlyAddressesInTheBlock() {
    EditNetworkPolicy policy = new EditNetworkPolicy("129.57.0.0/16");

    assertTrue(policy.isRestricted());
    assertTrue(policy.allows("129.57.0.0"));
    assertTrue(policy.allows("129.57.95.151"));
    assertTrue(policy.allows("129.57.255.255"));
    assertFalse(policy.allows("129.56.255.255"));
    assertFalse(policy.allows("129.58.0.0"));
    assertFalse(policy.allows("203.0.113.9"));
  }

  @Test
  void blockNeedNotEndOnAByte() {
    EditNetworkPolicy policy = new EditNetworkPolicy("172.16.0.0/12");

    assertTrue(policy.allows("172.16.0.1"));
    assertTrue(policy.allows("172.31.255.255"));
    assertFalse(policy.allows("172.15.255.255"));
    assertFalse(policy.allows("172.32.0.0"));
  }

  @Test
  void allowsAnyOfSeveralNetworks() {
    EditNetworkPolicy policy = new EditNetworkPolicy(" 10.0.0.0/8,192.168.1.7\n2001:db8::/32 ");

    assertTrue(policy.allows("10.200.3.4"));
    // A bare address is that one host
    assertTrue(policy.allows("192.168.1.7"));
    assertFalse(policy.allows("192.168.1.8"));
    assertTrue(policy.allows("2001:db8:0:1::5"));
    assertFalse(policy.allows("2001:db9::5"));
  }

  @Test
  void matchesIpv4WrittenAsIpv6() {
    EditNetworkPolicy policy = new EditNetworkPolicy("129.57.0.0/16");

    assertTrue(policy.allows("::ffff:129.57.1.2"));
    assertFalse(policy.allows("::ffff:203.0.113.9"));
    // An IPv6 client is in no IPv4 block
    assertFalse(policy.allows("::1"));
  }

  @Test
  void refusesWhatIsNotAnAddress() {
    EditNetworkPolicy policy = new EditNetworkPolicy("0.0.0.0/0, ::/0");

    assertTrue(policy.allows("203.0.113.9"));
    assertTrue(policy.allows("2001:db8::1"));

    for (String address :
        new String[] {
          null,
          "",
          "unknown",
          "localhost",
          "129.57.1",
          "129.57.1.256",
          "129.57.1.2.3",
          "1:2",
          "fe80::1%eth0"
        }) {
      assertFalse(policy.allows(address), "allowed: " + address);
    }
  }

  @Test
  void neverLooksANameUp() {
    assertNull(EditNetworkPolicy.parseAddress("localhost"));
    assertNull(EditNetworkPolicy.parseAddress("example.com"));
    assertNull(EditNetworkPolicy.parseAddress("bad:cafe.example"));
    assertArrayEquals(new byte[] {127, 0, 0, 1}, EditNetworkPolicy.parseAddress(" 127.0.0.1 "));
  }

  @Test
  void invalidNetworkIsIgnoredWithoutOpeningEdits() {
    // A mistyped setting must not allow everyone
    EditNetworkPolicy none = new EditNetworkPolicy("129.57.0.0/33");

    assertTrue(none.isRestricted());
    assertFalse(none.allows("129.57.1.2"));
    assertEquals(
        "The inventory can be edited only from an approved network,"
            + " and you are connecting from 129.57.1.2",
        none.refusal("129.57.1.2"));

    EditNetworkPolicy some = new EditNetworkPolicy("onsite, 129.57.0.0/16, 10.0.0.0/x");

    assertTrue(some.allows("129.57.1.2"));
    assertFalse(some.allows("10.1.2.3"));
  }

  @Test
  void refusalNamesTheNetworksAndTheClient() {
    EditNetworkPolicy policy = new EditNetworkPolicy("129.57.0.0/16, 10.0.0.0/8");

    assertNull(policy.refusal("129.57.1.2"));
    assertEquals(
        "The inventory can be edited only from 129.57.0.0/16, 10.0.0.0/8,"
            + " and you are connecting from 203.0.113.9",
        policy.refusal("203.0.113.9"));
    assertEquals(
        "The inventory can be edited only from 129.57.0.0/16, 10.0.0.0/8,"
            + " and you are connecting from an unknown address",
        policy.refusal(null));
  }

  @Test
  void configuredPolicyRefusesOnlyWhenRestricted() {
    // Unit tests run outside a request, so the client's address is unknown
    assertNull(EditNetworkPolicy.clientAddress());

    EditNetworkPolicy configured = EditNetworkPolicy.configured();

    if (configured.isRestricted()) {
      assertThrows(UserFriendlyException.class, configured::checkCurrentRequest);
    } else {
      assertDoesNotThrow(configured::checkCurrentRequest);
    }
  }
}
