package org.jlab.adm.persistence.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(schema = "ADM_OWNER", name = "APP_ENV")
public class AppEnv implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "APP_ENV_ID", nullable = false, precision = 22, scale = 0)
  private BigDecimal appEnvId;

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

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 128)
  @Column(nullable = false, length = 128)
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

  public BigDecimal getAppEnvId() {
    return appEnvId;
  }

  public void setAppEnvId(BigDecimal envId) {
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
