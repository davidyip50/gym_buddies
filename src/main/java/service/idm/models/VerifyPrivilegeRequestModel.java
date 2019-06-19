package service.idm.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifyPrivilegeRequestModel extends RequestModel{
    private String email;
    private int plevel;

/*
    @JsonCreator
    public VerifyPrivilegeRequestModel(@JsonProperty(value = "email", required = true) String email,
                                       @JsonProperty(value = "plevel", required = true) int plevel)
    {
        this.email = email;
        this.plevel = plevel;
    }
*/
    @JsonCreator
    public VerifyPrivilegeRequestModel(@JsonProperty(value = "email", required = true) String email,
                                       @JsonProperty(value = "plevel", required = true) int plevel)
    {
        this.email = email;
        this.plevel = plevel;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("plevel")
    public int getPlevel() {
        return plevel;
    }

}
