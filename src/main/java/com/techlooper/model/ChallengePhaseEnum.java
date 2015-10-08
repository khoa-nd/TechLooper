package com.techlooper.model;

/**
 * Created by NguyenDangKhoa on 9/22/15.
 */
public enum ChallengePhaseEnum {

  REGISTRATION("startDateTime", "registrationDateTime", "REGISTRATION"),
  IN_PROGRESS("registrationDateTime", "submissionDateTime", "IN_PROGRESS"),
  IDEA("registrationDateTime", "ideaSubmissionDateTime", "IDEA"),
  UIUX("ideaSubmissionDateTime", "uxSubmissionDateTime", "UIUX"),
  PROTOTYPE("uxSubmissionDateTime", "prototypeSubmissionDateTime", "PROTOTYPE"),
  FINAL("prototypeSubmissionDateTime", "submissionDateTime", "FINAL");

  public static ChallengePhaseEnum CHALLENGE_TIMELINE[] = {
    REGISTRATION, IDEA, UIUX, PROTOTYPE, FINAL
  };

  private String fromDateTimeField;

  private String toDateTimeField;

  private String value;

  ChallengePhaseEnum(String fromDateTimeField, String toDateTimeField, String value) {
    this.fromDateTimeField = fromDateTimeField;
    this.toDateTimeField = toDateTimeField;
    this.value = value;
  }

  public String getFromDateTimeField() {
    return fromDateTimeField;
  }

  public void setFromDateTimeField(String fromDateTimeField) {
    this.fromDateTimeField = fromDateTimeField;
  }

  public String getToDateTimeField() {
    return toDateTimeField;
  }

  public void setToDateTimeField(String toDateTimeField) {
    this.toDateTimeField = toDateTimeField;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
