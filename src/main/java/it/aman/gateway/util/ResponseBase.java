package it.aman.gateway.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;


public class ResponseBase   {
  @JsonProperty("success")
  private Boolean success = null;

  @JsonProperty("resultCode")
  private Integer resultCode = null;

  @JsonProperty("internalCode")
  private Integer internalCode = null;

  @JsonProperty("message")
  private String message = null;

  @JsonProperty("errors")
  @Valid
  private List<String> errors = null;

  @JsonProperty("transactionId")
  private String transactionId = null;

  @JsonProperty("type")
  private String type = null;

  public ResponseBase success(Boolean success) {
    this.success = success;
    return this;
  }

  /**
   * Get success
   * @return success
  **/
  @NotNull


  public Boolean isSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public ResponseBase resultCode(Integer resultCode) {
    this.resultCode = resultCode;
    return this;
  }

  /**
   * Get resultCode
   * @return resultCode
  **/
  @NotNull


  public Integer getResultCode() {
    return resultCode;
  }

  public void setResultCode(Integer resultCode) {
    this.resultCode = resultCode;
  }

  public ResponseBase internalCode(Integer internalCode) {
    this.internalCode = internalCode;
    return this;
  }


  public Integer getInternalCode() {
    return internalCode;
  }

  public void setInternalCode(Integer internalCode) {
    this.internalCode = internalCode;
  }

  public ResponseBase message(String message) {
    this.message = message;
    return this;
  }


  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ResponseBase errors(List<String> errors) {
    this.errors = errors;
    return this;
  }

  public ResponseBase addErrorsItem(String errorsItem) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(errorsItem);
    return this;
  }


  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }

  public ResponseBase transactionId(String transactionId) {
    this.transactionId = transactionId;
    return this;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public ResponseBase type(String type) {
    this.type = type;
    return this;
  }


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResponseBase responseBase = (ResponseBase) o;
    return Objects.equals(this.success, responseBase.success) &&
        Objects.equals(this.resultCode, responseBase.resultCode) &&
        Objects.equals(this.internalCode, responseBase.internalCode) &&
        Objects.equals(this.message, responseBase.message) &&
        Objects.equals(this.errors, responseBase.errors) &&
        Objects.equals(this.transactionId, responseBase.transactionId) &&
        Objects.equals(this.type, responseBase.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(success, resultCode, internalCode, message, errors, transactionId, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResponseBase {\n");
    
    sb.append("    success: ").append(toIndentedString(success)).append("\n");
    sb.append("    resultCode: ").append(toIndentedString(resultCode)).append("\n");
    sb.append("    internalCode: ").append(toIndentedString(internalCode)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
    sb.append("    transactionId: ").append(toIndentedString(transactionId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

