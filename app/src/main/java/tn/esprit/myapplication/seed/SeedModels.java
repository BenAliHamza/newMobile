package tn.esprit.myapplication.seed;

import java.util.HashMap;
import java.util.Map;

public final class SeedModels {

    private SeedModels() {}

    public static class DoctorSeed {
        public String id;          // generated
        public String fullName;
        public String specialty;
        public String hospital;
        public String avatarUrl;   // optional

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("fullName", fullName);
            m.put("specialty", specialty);
            m.put("hospital", hospital);
            m.put("avatarUrl", avatarUrl);
            return m;
        }
    }

    public static class PatientSeed {
        public String id;          // generated
        public String fullName;
        public String email;
        public String phone;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("fullName", fullName);
            m.put("email", email);
            m.put("phone", phone);
            return m;
        }
    }

    public static class AppointmentSeed {
        public String id;          // generated
        public String doctorId;
        public String patientId;
        public long   startAtEpochMillis;
        public long   endAtEpochMillis;
        public String title;
        public String notes;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("doctorId", doctorId);
            m.put("patientId", patientId);
            m.put("startAt", startAtEpochMillis);
            m.put("endAt", endAtEpochMillis);
            m.put("title", title);
            m.put("notes", notes);
            return m;
        }
    }
}
