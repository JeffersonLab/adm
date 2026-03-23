package org.jlab.adm.persistence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;

@Entity
@Table(schema = "ADM_OWNER")
public class App implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @SequenceGenerator(name = "AppId", sequenceName = "APP_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AppId")
  @Basic(optional = false)
  @NotNull
  @Column(name = "APP_ID", nullable = false, precision = 22, scale = 0)
  private BigInteger appId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 128)
  @Column(nullable = false, length = 128)
  private String name;

  @Basic(optional = true)
  @Column(name = "DOC_URL", nullable = true, length = 512)
  private String docUrl;

  public BigInteger getAppId() {
    return appId;
  }

  public void setAppId(BigInteger appId) {
    this.appId = appId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDocUrl() {
    return docUrl;
  }

  public void setDocUrl(String docUrl) {
    this.docUrl = docUrl;
  }

  public App(){}

  public App(String name, String docUrl) {
    this.name = name;
    this.docUrl = docUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof App)) return false;
    App app = (App) o;
    return Objects.equals(name, app.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
