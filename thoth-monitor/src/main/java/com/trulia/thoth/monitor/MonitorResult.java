package com.trulia.thoth.monitor;

/**
 * Created by sgudla on 10/21/14.
 */
public class MonitorResult {
  private String statusMessage;
  private boolean success;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String status) {
    this.statusMessage = status;
  }
}
