package service.idm.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifySessionRequestModel extends RequestModel{
    private String email;
    private String sessionID;

    @JsonCreator
    public VerifySessionRequestModel(@JsonProperty(value = "email", required = true) String email,
                                     @JsonProperty(value = "sessionID", required = true) String sessionID)
    {
        this.email = email;
        this.sessionID = sessionID;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("sessionID")
    public String getSessionID() {
        return sessionID;
    }


}
