package org.jlab.adm.persistence.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(schema = "ADM_OWNER", name = "REMOTE_COMMAND_RESULT")
public class RemoteCommandResult implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "REMOTE_COMMAND_RESULT_ID", nullable = false, precision = 22, scale = 0)
  private BigDecimal remoteCommandResultId;

  private String stackTrace;
  private String out;
  private String err;
  private int exitCode;
  private Instant start;
  private Instant end;

  public RemoteCommandResult() {}

  public RemoteCommandResult(String stackTrace) {
    this(-1, null, null, stackTrace);
  }

  public RemoteCommandResult(int exitCode, String out, String err) {
    this(exitCode, out, err, null);
  }

  public RemoteCommandResult(int exitCode, String out, String err, String stackTrace) {
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

  public void setExitCode(int exitCode) {
    this.exitCode = exitCode;
  }

  public Instant getStart() {
    return start;
  }

  public void setStart(Instant start) {
    this.start = start;
  }

  public Instant getEnd() {
    return end;
  }

  public void setEnd(Instant end) {
    this.end = end;
  }

  public int getExitCode() {
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
}
