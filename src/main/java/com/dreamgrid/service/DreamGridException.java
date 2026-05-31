package com.dreamgrid.service;

import com.dreamgrid.api.ApiErrorCode;

public class DreamGridException extends RuntimeException {
  private final ApiErrorCode errorCode;

  public DreamGridException(ApiErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ApiErrorCode getErrorCode() {
    return errorCode;
  }
}
