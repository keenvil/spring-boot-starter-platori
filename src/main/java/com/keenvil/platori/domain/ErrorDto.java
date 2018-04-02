package com.keenvil.platori.domain;

public class ErrorDto {
  
  private int httpStatus;
  
  private String code;
  
  private String title;
  
  private String detail;
  
  public ErrorDto() {
  }
  
  public int getHttpStatus() {
    return httpStatus;
  }
  
  public String getCode() {
    return code;
  }
  
  public String getTitle() {
    return title;
  }
  
  public String getDetail() {
    return detail;
  }
}
