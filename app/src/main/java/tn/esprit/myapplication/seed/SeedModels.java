package tn.esprit.myapplication.seed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SeedModels
 *
 * Simple POJOs used only for generating seed data.
 * They are NOT the main domain models; they are helpers to build Firestore docs.
 */
public final class SeedModels {

    private SeedModels() {
        // no-op
    }

    public static class DoctorSeed {
        public String id;          // assigned by DataSeeder (matches users/doctors doc id)
        public String fullName;
        public String email;
        public String phone;
        public String city;

        public String speciality;
        public String clinicName;
        public String avatarUrl;   // optional
        public int    yearsOfExperience;

        public List<String> acts = new ArrayList<>();

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("fullName", fullName);
            m.put("email", email);
            m.put("phone", phone);
            m.put("city", city);
            m.put("speciality", speciality);
            m.put("clinicName", clinicName);
            m.put("avatarUrl", avatarUrl);
            m.put("yearsOfExperience", yearsOfExperience);
            m.put("acts", new ArrayList<>(acts));
            m.put("userId", id);
            return m;
        }
    }

    public static class PatientSeed {
        public String id;          // assigned by DataSeeder (matches users/patients doc id)
        public String fullName;
        public String email;
        public String phone;
        public String city;

        public long   dateOfBirthEpochMillis;
        public double heightCm;
        public double weightKg;
        public List<String> chronicDiseases = new ArrayList<>();

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("fullName", fullName);
            m.put("email", email);
            m.put("phone", phone);
            m.put("city", city);
            m.put("dateOfBirth", dateOfBirthEpochMillis);
            m.put("heightCm", heightCm);
            m.put("weightKg", weightKg);
            m.put("chronicDiseases", new ArrayList<>(chronicDiseases));
            m.put("userId", id);
            return m;
        }
    }

    /**
     * Kept for possible future appointment seeding or examples.
     * Not used by the current DataSeeder implementation.
     */
    public static class AppointmentSeed {
        public String id;          // generated
        public String doctorId;
        public String patientId;
        public long   startAtEpochMillis;
        public long   endAtEpochMillis;
        public String title;
        public String notes;
        public String status;      // e.g. "PENDING", "CONFIRMED", ...

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("doctorId", doctorId);
            m.put("patientId", patientId);
            m.put("startAt", startAtEpochMillis);
            m.put("endAt", endAtEpochMillis);
            m.put("title", title);
            m.put("notes", notes);
            m.put("status", status);
            return m;
        }
    }
}
