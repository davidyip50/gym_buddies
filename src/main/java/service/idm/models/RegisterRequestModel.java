package service.idm.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterRequestModel extends RequestModel{
    private String email;
    private char[] password;
    private String location;
    private String gym;
    private String biography;
    private String fitness;
    private String music;


    @JsonCreator
    public RegisterRequestModel(@JsonProperty(value = "email", required = true) String email,
                                @JsonProperty(value = "password", required = true) char[] password,
                                @JsonProperty(value = "location") String location,
                                @JsonProperty(value = "gym") String gym,
                                @JsonProperty(value = "biography") String biography,
                                @JsonProperty(value = "fitness") String fitness,
                                @JsonProperty(value = "music") String music)
    {
        this.email = email;
        this.password = password;
        this.location = location;
        this.gym = gym;
        this.biography = biography;
        this.fitness = fitness;
        this.music = music;
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

    @JsonProperty("location")
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @JsonProperty("gym")
    public String getGym() {
        return gym;
    }

    public void setGym(String gym) {
        this.gym = gym;
    }

    @JsonProperty("biography")
    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    @JsonProperty("fitness")
    public String getFitness() {
        return fitness;
    }

    public void setFitness(String fitness) {
        this.fitness = fitness;
    }

    @JsonProperty("music")
    public String getMusic() {
        return music;
    }

    public void setMusic(String music) {
        this.music = music;
    }
}
