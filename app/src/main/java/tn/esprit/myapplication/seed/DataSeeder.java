package tn.esprit.myapplication.seed;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.data.Role;
import tn.esprit.myapplication.data.User;
import tn.esprit.myapplication.data.profile.DoctorProfile;
import tn.esprit.myapplication.data.profile.PatientProfile;

/**
 * DataSeeder
 *
 * One-shot seeder for domain data (NOT auth accounts).
 * - Seeds ~30 generic doctors and ~20 generic patients.
 * - Writes into collections: "users", "doctors", "patients".
 * - Uses a meta marker document so seeding runs only once.
 *
 * Seeded users are demo data for lists, appointments, etc.
 * They do NOT have Firebase Auth accounts.
 */
public final class DataSeeder {

    private static final String TAG = "DataSeeder";

    private static final String META_COLLECTION = "meta";
    private static final String META_DOC_SEED_USERS = "seed_users_v1";

    private static final int DOCTOR_COUNT = 30;
    private static final int PATIENT_COUNT = 20;

    private DataSeeder() {
        // no-op
    }

    /**
     * Runs seeding only if marker document does not exist.
     * Can be safely called from App.onCreate(); it is idempotent.
     */
    public static Task<Void> runIfNeeded() {
        FirebaseFirestore db = FirebaseManager.db();
        DocumentReference marker = db.collection(META_COLLECTION).document(META_DOC_SEED_USERS);

        return marker.get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Failed to check seed marker.");
                    }

                    if (task.getResult() != null && task.getResult().exists()) {
                        // Already seeded -> nothing to do.
                        Log.d(TAG, "Seed marker exists. Skipping seeding.");
                        return Tasks.forResult(null);
                    }

                    List<Task<?>> writes = new ArrayList<>();

                    seedDoctors(db, writes);
                    seedPatients(db, writes);

                    Task<Void> allWrites = Tasks.whenAll(writes);
                    return allWrites.continueWithTask(allTask -> {
                        if (!allTask.isSuccessful()) {
                            throw allTask.getException() != null
                                    ? allTask.getException()
                                    : new IllegalStateException("Seeding writes failed.");
                        }

                        java.util.Map<String, Object> markerData = new java.util.HashMap<>();
                        markerData.put("at", System.currentTimeMillis());
                        markerData.put("countDoctors", DOCTOR_COUNT);
                        markerData.put("countPatients", PATIENT_COUNT);
                        markerData.put("by", "system"); // no specific user, seeding at startup
                        return marker.set(markerData);
                    });
                })
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Seed completed."))
                .addOnFailureListener(e -> Log.e(TAG, "Seed failed", e));
    }

    // --------------------------------
    // Seeding helpers
    // --------------------------------

    private static void seedDoctors(FirebaseFirestore db, List<Task<?>> writes) {
        String[] cities = new String[]{
                "Tunis", "Ariana", "Ben Arous", "Manouba", "Sousse",
                "Sfax", "Nabeul", "Bizerte", "Gabes", "Kairouan"
        };

        String[] specialties = new String[]{
                "Cardiology", "Endocrinology", "General Medicine", "Pediatrics",
                "Gynecology", "Dermatology", "Neurology", "Orthopedics",
                "Psychiatry", "Ophthalmology"
        };

        String[] clinics = new String[]{
                "Clinique La Soukra",
                "Hôpital Charles Nicolle",
                "Polyclinique Les Jasmins",
                "Clinique El Manar",
                "Clinique Hannibal",
                "Clinique Pasteur",
                "Clinique Ennasr",
                "Clinique Ibn Khaldoun",
                "Centre Médical Lac 2",
                "Clinique Al Hayat"
        };

        for (int i = 1; i <= DOCTOR_COUNT; i++) {
            String id = String.format("seed_doctor_%02d", i);
            String fullName = "Dr Demo Doctor " + i;
            String email = "seed_doctor_" + i + "@demo.local";
            String phone = "+216 55 1" + String.format("%03d", i);
            String city = cities[(i - 1) % cities.length];
            String speciality = specialties[(i - 1) % specialties.length];
            String clinicName = clinics[(i - 1) % clinics.length];
            String avatarUrl = "https://i.pravatar.cc/150?img=" + (10 + (i % 40));
            int yearsOfExperience = 3 + (i % 15);

            // Base user
            User user = new User();
            String[] nameParts = splitName(fullName);
            user.setFirstName(nameParts[0]);
            user.setLastName(nameParts[1]);
            user.setSex("Other");
            user.setRole(Role.DOCTOR);
            user.setIsFirstLogin(false);
            user.setImageUrl(avatarUrl);
            user.setEmail(email);
            user.setPhone(phone);
            user.setCity(city);

            writes.add(db.collection("users").document(id).set(user));

            // Doctor profile
            java.util.List<String> acts = Arrays.asList("Consultation", "Follow-up");
            DoctorProfile profile = new DoctorProfile(
                    id,
                    speciality,
                    clinicName,
                    city,
                    phone,
                    avatarUrl,
                    yearsOfExperience,
                    acts
            );
            writes.add(db.collection("doctors").document(id).set(profile));
        }
    }

    private static void seedPatients(FirebaseFirestore db, List<Task<?>> writes) {
        String[] cities = new String[]{
                "Tunis", "Ariana", "Ben Arous", "Manouba", "Sousse",
                "Sfax", "Nabeul", "Bizerte", "Gabes", "Kairouan"
        };

        String[] baseNames = new String[]{
                "Demo Patient",
                "Sample Patient",
                "Test Patient",
                "Example Patient"
        };

        for (int i = 1; i <= PATIENT_COUNT; i++) {
            String id = String.format("seed_patient_%02d", i);
            String baseName = baseNames[(i - 1) % baseNames.length];
            String fullName = baseName + " " + i;
            String email = "seed_patient_" + i + "@demo.local";
            String phone = "+216 55 2" + String.format("%03d", i);
            String city = cities[(i - 1) % cities.length];

            // Rough DOB between ~1975 and 2005
            long year = 1975 + (i % 30);
            long dobMillis = yearToApproxMillis((int) year);

            double heightCm = 155 + (i % 25);  // ~155-179
            double weightKg = 55 + (i % 25);   // ~55-79

            // Base user
            User user = new User();
            String[] nameParts = splitName(fullName);
            user.setFirstName(nameParts[0]);
            user.setLastName(nameParts[1]);
            user.setSex("Other");
            user.setRole(Role.PATIENT);
            user.setIsFirstLogin(false);
            user.setImageUrl(null);
            user.setEmail(email);
            user.setPhone(phone);
            user.setCity(city);

            writes.add(db.collection("users").document(id).set(user));

            // Patient profile
            java.util.List<String> chronic = new ArrayList<>();
            chronic.add("None"); // simple demo value

            PatientProfile profile = new PatientProfile(
                    id,
                    dobMillis,
                    city,
                    phone,
                    heightCm,
                    weightKg,
                    null,
                    chronic
            );
            writes.add(db.collection("patients").document(id).set(profile));
        }
    }

    // --------------------------------
    // Small helpers
    // --------------------------------

    private static String[] splitName(@androidx.annotation.NonNull String fullName) {
        String trimmed = fullName.trim();
        if (trimmed.isEmpty()) {
            return new String[]{"", ""};
        }
        String[] parts = trimmed.split("\\s+", 2);
        String first = parts[0];
        String last = parts.length > 1 ? parts[1] : "";
        return new String[]{first, last};
    }

    private static long yearToApproxMillis(int year) {
        Calendar cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
