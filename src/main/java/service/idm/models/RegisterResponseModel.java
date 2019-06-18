package service.idm.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterResponseModel {
    private Integer resultCode;
    private String message;

    public RegisterResponseModel() {
    }

    @JsonCreator
    public RegisterResponseModel(@JsonProperty(value = "resultCode", required = true) int resultCode,
                                 @JsonProperty(value = "message", required = true) String message)
    {
        this.resultCode = resultCode;
        this.message = message;
    }

    @JsonProperty("resultCode")
    public Integer getResultCode() {
        return resultCode;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }
}
