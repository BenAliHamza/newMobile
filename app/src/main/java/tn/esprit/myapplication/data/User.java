package tn.esprit.myapplication.data;

public class User {
    private String firstName;
    private String lastName;
    private String sex;
    private Role role;            // Enum: DOCTOR or PATIENT
    private Boolean isFirstLogin;
    private String imageUrl;
    private String email;         // unique identifier in auth + stored in Firestore

    // Empty constructor required by Firestore
    public User() {}

    public User(String firstName, String lastName, String sex, Role role, Boolean isFirstLogin, String imageUrl, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.sex = sex;
        this.role = role;
        this.isFirstLogin = isFirstLogin;
        this.imageUrl = imageUrl;
        this.email = email;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Boolean getIsFirstLogin() { return isFirstLogin; }
    public void setIsFirstLogin(Boolean firstLogin) { isFirstLogin = firstLogin; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
