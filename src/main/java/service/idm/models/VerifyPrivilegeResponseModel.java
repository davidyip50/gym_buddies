package service.idm.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifyPrivilegeResponseModel {
    private int resultCode;
    private String message;

    @JsonCreator
    public VerifyPrivilegeResponseModel(@JsonProperty(value = "resultCode", required = true) int resultCode,
                                       @JsonProperty(value = "message", required = true) String message)
    {
        this.resultCode = resultCode;
        this.message = message;
    }

    @JsonProperty("resultCode")
    public int getresultCode() {
        return resultCode;
    }

    @JsonProperty("message")
    public String getmessage() {
        return message;
    }
}
