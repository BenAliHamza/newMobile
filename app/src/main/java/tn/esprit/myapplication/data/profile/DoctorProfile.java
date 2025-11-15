package tn.esprit.myapplication.data.profile;

import java.util.ArrayList;
import java.util.List;

/**
 * DoctorProfile
 *
 * Doctor-specific data that extends the base User identity.
 * Stored in the "doctors" collection with the same ID as the User document.
 */
public class DoctorProfile {

    private String userId;
    private String speciality;
    private String clinicName;
    private String city;
    private String phone;
    private String avatarUrl;
    private Integer yearsOfExperience;
    private List<String> acts = new ArrayList<>();

    // Empty constructor required by Firestore
    public DoctorProfile() {
    }

    public DoctorProfile(String userId,
                         String speciality,
                         String clinicName,
                         String city,
                         String phone,
                         String avatarUrl,
                         Integer yearsOfExperience,
                         List<String> acts) {
        this.userId = userId;
        this.speciality = speciality;
        this.clinicName = clinicName;
        this.city = city;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.yearsOfExperience = yearsOfExperience;
        if (acts != null) {
            this.acts = new ArrayList<>(acts);
        }
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public List<String> getActs() { return acts; }
    public void setActs(List<String> acts) {
        this.acts = acts != null ? new ArrayList<>(acts) : new ArrayList<>();
    }
}
