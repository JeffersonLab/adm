package org.jlab.adm.persistence.entity;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(schema = "ADM_OWNER", name = "DEPLOY_JOB")
public class DeployJob implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @SequenceGenerator(name = "deployJobId", sequenceName = "DEPLOY_JOB_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deployJobId")
  @Basic(optional = false)
  @NotNull
  @Column(name = "DEPLOY_JOB_ID", nullable = false, precision = 22, scale = 0)
  private BigInteger deployJobId;

  @NotNull
  @JoinColumn(name = "APP_ENV_ID", referencedColumnName = "APP_ENV_ID", nullable = false)
  @ManyToOne(optional = false)
  private AppEnv appEnv;

  @NotNull
  @Column(name = "VERSION")
  private String version;

  @Lob
  @Column(name = "STACK_TRACE")
  private String stackTrace;

  @Lob
  @Column(name = "OUT")
  private String out;

  @Lob
  @Column(name = "ERR")
  private String err;

  @Column(name = "EXIT_CODE")
  private Integer exitCode;

  // I'm aware using Date as local time with JPA doesn't handle DST, but doing it "correctly" is
  // enormously
  // complicated using legacy JPA APIs and this is "good enough".  Sorry double hour in the Fall.
  @Column(name = "JOB_START")
  @Temporal(TemporalType.TIMESTAMP)
  private Date start;

  @Column(name = "JOB_END")
  @Temporal(TemporalType.TIMESTAMP)
  private Date end;

  public DeployJob() {}

  public DeployJob(AppEnv appEnv, String version) {
    this(appEnv, version, new Date(), null, null, null, null, null);
  }

  public DeployJob(
      AppEnv appEnv,
      String version,
      Date start,
      Date end,
      Integer exitCode,
      String out,
      String err,
      String stackTrace) {
    this.appEnv = appEnv;
    this.version = version;
    this.start = start;
    this.end = end;
    this.exitCode = exitCode;
    this.out = out;
    this.err = err;
    this.stackTrace = stackTrace;
  }

  public BigInteger getDeployJobId() {
    return deployJobId;
  }

  public void setDeployJobId(BigInteger remoteCommandResultId) {
    this.deployJobId = remoteCommandResultId;
  }

  public void setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
  }

  public void setOut(String out) {
    this.out = out;
  }

  public void setErr(String err) {
    this.err = err;
  }

  public void setExitCode(Integer exitCode) {
    this.exitCode = exitCode;
  }

  public Date getStart() {
    return start;
  }

  public void setStart(Date start) {
    this.start = start;
  }

  public Date getEnd() {
    return end;
  }

  public void setEnd(Date end) {
    this.end = end;
  }

  public Integer getExitCode() {
    return exitCode;
  }

  public String getOut() {
    return out;
  }

  public String getErr() {
    return err;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public AppEnv getAppEnv() {
    return appEnv;
  }

  public void setAppEnv(AppEnv appEnv) {
    this.appEnv = appEnv;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
