package org.jlab.adm.persistence.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(schema = "ADM_OWNER")
public class App implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "APP_ID", nullable = false, precision = 22, scale = 0)
  private BigDecimal appId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 128)
  @Column(nullable = false, length = 128)
  private String name;

  @Basic(optional = true)
  @Column(name = "DOC_URL", nullable = true, length = 512)
  private String docUrl;

  public BigDecimal getAppId() {
    return appId;
  }

  public void setAppId(BigDecimal appId) {
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
