package org.jlab.adm.persistence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Entity
@Table(schema = "ADM_OWNER", name = "APP_ENV")
public class AppEnv implements Serializable {
  private static final long serialVersionUID = 1L;

  /** The HOSTNAME column's length, which holds all of an env's hosts */
  public static final int MAX_HOSTNAME_LENGTH = 1024;

  @Id
  @SequenceGenerator(name = "AppEnvId", sequenceName = "APP_ENV_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AppEnvId")
  @Basic(optional = false)
  @NotNull
  @Column(name = "APP_ENV_ID", nullable = false, precision = 22, scale = 0)
  private BigInteger appEnvId;

  @NotNull
  @JoinColumn(name = "APP_ID", referencedColumnName = "APP_ID", nullable = false)
  @ManyToOne(optional = false)
  private App app;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 128)
  @Column(nullable = false, length = 128)
  private String name;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 128)
  @Column(name = "REQUEST_SERVICE_USERNAME", nullable = false, length = 128)
  private String requestServiceUsername;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 128)
  @Column(name = "RUN_SERVICE_USERNAME", nullable = false, length = 128)
  private String runServiceUsername;

  // One or more hosts, separated by commas or spaces; the deploy command runs on each in turn
  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = MAX_HOSTNAME_LENGTH)
  @Column(nullable = false, length = MAX_HOSTNAME_LENGTH)
  private String hostname;

  @Basic(optional = false)
  @NotNull
  @Column(nullable = false)
  private int port;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 128)
  @Column(name = "DEPLOY_COMMAND", nullable = false, length = 128)
  private String deployCommand;

  public AppEnv() {}

  public AppEnv(
      App app,
      String envName,
      String requestUsername,
      String deployUsername,
      String deployHostname,
      Integer deployPort,
      String deployCommand) {
    this.app = app;
    this.name = envName;
    this.requestServiceUsername = requestUsername;
    this.runServiceUsername = deployUsername;
    this.hostname = deployHostname;
    this.port = deployPort;
    this.deployCommand = deployCommand;
  }

  public BigInteger getAppEnvId() {
    return appEnvId;
  }

  public void setAppEnvId(BigInteger envId) {
    this.appEnvId = envId;
  }

  public App getApp() {
    return app;
  }

  public void setApp(App app) {
    this.app = app;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRequestServiceUsername() {
    return requestServiceUsername;
  }

  public void setRequestServiceUsername(String authorizedGroupname) {
    this.requestServiceUsername = authorizedGroupname;
  }

  public String getRunServiceUsername() {
    return runServiceUsername;
  }

  public void setRunServiceUsername(String serviceUsername) {
    this.runServiceUsername = serviceUsername;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  /** The hosts to deploy to, in order */
  public List<String> getHostnames() {
    return parseHostnames(hostname);
  }

  /** Splits a list of hosts separated by commas or whitespace, neither of which a host name has */
  public static List<String> parseHostnames(String hostnames) {
    if (hostnames == null) {
      return List.of();
    }

    return Arrays.stream(hostnames.split("[,\\s]+")).filter(h -> !h.isEmpty()).toList();
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getDeployCommand() {
    return deployCommand;
  }

  public void setDeployCommand(String deployCommand) {
    this.deployCommand = deployCommand;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AppEnv)) return false;
    AppEnv appEnv = (AppEnv) o;
    return Objects.equals(app, appEnv.app) && Objects.equals(name, appEnv.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(app, name);
  }
}
