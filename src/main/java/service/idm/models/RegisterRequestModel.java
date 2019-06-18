package service.idm.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterRequestModel extends RequestModel{
    private String email;
    private char[] password;

    @JsonCreator
    public RegisterRequestModel(@JsonProperty(value = "email", required = true) String email,
                                @JsonProperty(value = "password", required = true) char[] password)
    {
        this.email = email;
        this.password = password;
    }

    public RegisterRequestModel() {
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    @JsonProperty("password")
    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }
}
