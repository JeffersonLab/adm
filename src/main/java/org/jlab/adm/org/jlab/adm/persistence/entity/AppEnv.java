package org.jlab.adm.org.jlab.adm.persistence.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(schema = "ADM_OWNER", name = "APP_ENV")
public class AppEnv implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "ENV_ID", nullable = false, precision = 22, scale = 0)
    private BigDecimal envId;
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
    @Column(name = "SERVICE_USERNAME", nullable = false, length = 128)
    private String serviceUsername;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(nullable = false, length = 128)
    private String hostname;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "DEPLOY_COMMAND", nullable = false, length = 128)
    private String deployCommand;

    public BigDecimal getEnvId() {
        return envId;
    }

    public void setEnvId(BigDecimal envId) {
        this.envId = envId;
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

    public String getServiceUsername() {
        return serviceUsername;
    }

    public void setServiceUsername(String serviceUsername) {
        this.serviceUsername = serviceUsername;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
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
