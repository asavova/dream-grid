package com.dreamgrid.dto;

import com.dreamgrid.api.ApiErrorCode;

public class ErrorResponse {
  private String error;
  private String message;

  public ErrorResponse(ApiErrorCode error, String message) {
    this.error = error.name();
    this.message = message;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
