package lk.ijse.dep10.shared.singup;

import javafx.scene.image.Image;

import java.io.Serializable;

public class SignUpUser implements Serializable {

    private String name;
    private String userName;
    private Gender gender;
    private String country;
    private String birthDay;
    private String password;





    public SignUpUser(String name, String userName,
                      Gender gender, String country, String birthDay, String password) {
        this.name = name;
        this.userName = userName;
        this.gender = gender;
        this.country = country;
        this.birthDay = birthDay;
        this.password = password;


    }

    public SignUpUser() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setBirthDay(String birthDay) {
        this.birthDay = birthDay;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getUserName() {
        return userName;
    }

    public Gender getGender() {
        return gender;
    }

    public String getCountry() {
        return country;
    }

    public String getBirthDay() {
        return birthDay;
    }

    public String getPassword() {
        return password;
    }

}
