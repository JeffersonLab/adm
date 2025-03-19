package org.jlab.adm.persistence.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(schema = "ADM_OWNER", name = "REMOTE_COMMAND_RESULT")
public class RemoteCommandResult implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @SequenceGenerator(
      name = "remoteCommandResultId",
      sequenceName = "REMOTE_COMMAND_RESULT_ID",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "remoteCommandResultId")
  @Basic(optional = false)
  @NotNull
  @Column(name = "REMOTE_COMMAND_RESULT_ID", nullable = false, precision = 22, scale = 0)
  private BigDecimal remoteCommandResultId;

  @NotNull
  @JoinColumn(name = "ENV_ID", referencedColumnName = "ENV_ID", nullable = false)
  @ManyToOne(optional = false)
  private AppEnv appEnv;

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

  public RemoteCommandResult() {}

  public RemoteCommandResult(String stackTrace) {
    this(null, null, null, stackTrace);
  }

  public RemoteCommandResult(Integer exitCode, String out, String err) {
    this(exitCode, out, err, null);
  }

  public RemoteCommandResult(Integer exitCode, String out, String err, String stackTrace) {
    this.exitCode = exitCode;
    this.out = out;
    this.err = err;
    this.stackTrace = stackTrace;
  }

  public BigDecimal getRemoteCommandResultId() {
    return remoteCommandResultId;
  }

  public void setRemoteCommandResultId(BigDecimal remoteCommandResultId) {
    this.remoteCommandResultId = remoteCommandResultId;
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
}
