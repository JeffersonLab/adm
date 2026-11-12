package org.jlab.adm.business.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jlab.smoothness.business.exception.UserFriendlyException;
import org.jlab.smoothness.presentation.filter.AuditContext;

/**
 * The networks from which admins may edit the inventory: a second check after the adm-admin role,
 * so a stolen admin login is not enough from elsewhere.
 *
 * <p>The environment variable ADM_INVENTORY_EDIT_NETWORKS lists them as addresses or CIDR blocks,
 * separated by commas or spaces, such as {@code 129.57.0.0/16}. Unset or blank, edits are allowed
 * from anywhere. It is an environment variable, and not a setting on the Settings page, so that an
 * admin outside those networks cannot widen it.
 *
 * <p>The client's address is the one Smoothness records for the request: the first X-Forwarded-For
 * entry, else the connection's. That is only as trustworthy as the proxy in front of the app, which
 * must replace the header instead of adding to the one a client sent.
 */
public final class EditNetworkPolicy {

  public static final String ENV_NAME = "ADM_INVENTORY_EDIT_NETWORKS";

  private static final Logger logger = Logger.getLogger(EditNetworkPolicy.class.getName());

  private static final Pattern SEPARATOR = Pattern.compile("[,\\s]+");
  private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

  // Only what an IPv6 literal can hold, so InetAddress never looks a name up
  private static final Pattern IPV6 = Pattern.compile("[0-9A-Fa-f:][0-9A-Fa-f:.]*");

  private static final EditNetworkPolicy CONFIGURED =
      new EditNetworkPolicy(System.getenv(ENV_NAME));

  private final boolean restricted;
  private final List<Network> networks = new ArrayList<>();

  /**
   * @param setting the networks, as ADM_INVENTORY_EDIT_NETWORKS holds them; null or blank for none
   */
  public EditNetworkPolicy(String setting) {
    restricted = setting != null && !setting.isBlank();

    if (restricted) {
      for (String entry : SEPARATOR.split(setting.trim())) {
        Network network = Network.parse(entry);

        if (network == null) {
          // The rest still apply; with none left, edits are refused from everywhere
          logger.log(
              Level.SEVERE, "Ignoring invalid network in {0}: {1}", new Object[] {ENV_NAME, entry});
        } else {
          networks.add(network);
        }
      }
    }
  }

  /** The policy this installation is configured with. */
  public static EditNetworkPolicy configured() {
    return CONFIGURED;
  }

  /** The address of the client making the current request, or null outside a request. */
  public static String clientAddress() {
    AuditContext context = AuditContext.getCurrentInstance();

    return context == null ? null : context.getIp();
  }

  /** Whether edits are limited to some networks. */
  public boolean isRestricted() {
    return restricted;
  }

  /**
   * Whether a client at this address may edit the inventory. An address that can't be read, such as
   * null, is refused when edits are restricted.
   */
  public boolean allows(String address) {
    if (!restricted) {
      return true;
    }

    byte[] bytes = parseAddress(address);

    return bytes != null && networks.stream().anyMatch(n -> n.contains(bytes));
  }

  /**
   * Why a client at this address may not edit the inventory, for that admin to read.
   *
   * @return the reason, or null if the address is allowed
   */
  public String refusal(String address) {
    if (allows(address)) {
      return null;
    }

    String allowed = networks.stream().map(Network::toString).collect(Collectors.joining(", "));

    return "The inventory can be edited only from "
        + (networks.isEmpty() ? "an approved network" : allowed)
        + ", and you are connecting from "
        + (address == null || address.isBlank() ? "an unknown address" : address);
  }

  /**
   * Refuses the current request's edit if its client is outside the networks.
   *
   * @throws UserFriendlyException with the reason
   */
  public void checkCurrentRequest() throws UserFriendlyException {
    String refusal = refusal(clientAddress());

    if (refusal != null) {
      throw new UserFriendlyException(refusal);
    }
  }

  /**
   * Reads an IPv4 or IPv6 address literal, without looking a name up.
   *
   * @return its 4 or 16 bytes (4 for an IPv4-mapped IPv6 address), or null if it isn't one
   */
  static byte[] parseAddress(String text) {
    if (text == null) {
      return null;
    }

    text = text.trim();

    if (IPV4.matcher(text).matches()) {
      byte[] bytes = new byte[4];
      String[] parts = text.split("\\.");

      for (int i = 0; i < 4; i++) {
        int part = Integer.parseInt(parts[i]);

        if (part > 255) {
          return null;
        }

        bytes[i] = (byte) part;
      }

      return bytes;
    }

    if (text.indexOf(':') >= 0 && IPV6.matcher(text).matches()) {
      try {
        return InetAddress.getByName(text).getAddress();
      } catch (UnknownHostException e) {
        return null;
      }
    }

    return null;
  }

  /** An address block: the addresses that share the first {@code bits} bits of {@code prefix}. */
  record Network(byte[] prefix, int bits, String text) {

    /**
     * @param text an address, or a CIDR block such as 129.57.0.0/16
     * @return the network, or null if the text is neither
     */
    static Network parse(String text) {
      int slash = text.indexOf('/');
      byte[] prefix = parseAddress(slash < 0 ? text : text.substring(0, slash));

      if (prefix == null) {
        return null;
      }

      int bits = prefix.length * 8;

      if (slash >= 0) {
        String length = text.substring(slash + 1);

        if (!length.matches("\\d{1,3}") || Integer.parseInt(length) > bits) {
          return null;
        }

        bits = Integer.parseInt(length);
      }

      return new Network(prefix, bits, text);
    }

    boolean contains(byte[] address) {
      if (address.length != prefix.length) {
        return false;
      }

      for (int bit = 0; bit < bits; bit++) {
        int mask = 0x80 >> (bit % 8);

        if ((address[bit / 8] & mask) != (prefix[bit / 8] & mask)) {
          return false;
        }
      }

      return true;
    }

    @Override
    public String toString() {
      return text;
    }
  }
}
