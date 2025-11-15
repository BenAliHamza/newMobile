package tn.esprit.myapplication.data.profile;

import java.util.ArrayList;
import java.util.List;

/**
 * PatientProfile
 *
 * Patient-specific data that extends the base User identity.
 * Stored in the "patients" collection with the same ID as the User document.
 */
public class PatientProfile {

    private String userId;
    private Long dateOfBirth; // epoch millis (optional demo value)
    private String city;
    private String phone;
    private Double heightCm;
    private Double weightKg;
    private String bloodType;
    private List<String> chronicDiseases = new ArrayList<>();

    // Empty constructor required by Firestore
    public PatientProfile() {
    }

    public PatientProfile(String userId,
                          Long dateOfBirth,
                          String city,
                          String phone,
                          Double heightCm,
                          Double weightKg,
                          String bloodType,
                          List<String> chronicDiseases) {
        this.userId = userId;
        this.dateOfBirth = dateOfBirth;
        this.city = city;
        this.phone = phone;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.bloodType = bloodType;
        if (chronicDiseases != null) {
            this.chronicDiseases = new ArrayList<>(chronicDiseases);
        }
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Long getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Long dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public List<String> getChronicDiseases() { return chronicDiseases; }
    public void setChronicDiseases(List<String> chronicDiseases) {
        this.chronicDiseases =
                chronicDiseases != null ? new ArrayList<>(chronicDiseases) : new ArrayList<>();
    }
}
