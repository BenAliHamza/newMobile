package tn.esprit.myapplication.seed;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.seed.SeedModels.AppointmentSeed;
import tn.esprit.myapplication.seed.SeedModels.DoctorSeed;
import tn.esprit.myapplication.seed.SeedModels.PatientSeed;

/**
 * One-shot seeder. Creates fake doctors, a sample patient, and a few appointments.
 * Idempotent: uses a marker doc to avoid duplicating data.
 */
public final class DataSeeder {

    private static final String TAG = "DataSeeder";
    private static final String MARKER_COLLECTION = "_internal";
    private static final String MARKER_DOC = "seed_v1_done";

    private DataSeeder() {}

    public static Task<Void> runIfNeeded(@NonNull String currentUserUid, @NonNull String currentUserEmail) {
        FirebaseFirestore db = FirebaseManager.db();
        DocumentReference marker = db.collection(MARKER_COLLECTION).document(MARKER_DOC);

        return marker.get().continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    if (task.getResult() != null && task.getResult().exists()) {
                        // Already seeded
                        return com.google.android.gms.tasks.Tasks.forResult(null);
                    }
                    // Build seeds
                    List<DoctorSeed> doctors = new ArrayList<>();
                    doctors.add(makeDoctor("Dr. Lina Ben Salah", "Cardiology", "Clinique La Soukra",
                            "https://i.pravatar.cc/150?img=12"));
                    doctors.add(makeDoctor("Dr. Amine Trabelsi", "Endocrinology", "Hôpital Charles Nicolle",
                            "https://i.pravatar.cc/150?img=15"));
                    doctors.add(makeDoctor("Dr. Sara Gharbi", "General Medicine", "Polyclinique Les Jasmins",
                            "https://i.pravatar.cc/150?img=8"));

                    PatientSeed demoPatient = new PatientSeed();
                    demoPatient.fullName = "Demo Patient";
                    demoPatient.email = currentUserEmail;
                    demoPatient.phone = "+216 55 000 111";

                    // Write in a simple chain: doctors -> patient -> appointments -> marker
                    List<Task<?>> writes = new ArrayList<>();

                    for (DoctorSeed d : doctors) {
                        DocumentReference ref = db.collection("doctors").document();
                        d.id = ref.getId();
                        writes.add(ref.set(d.toMap()));
                    }

                    DocumentReference patientRef = db.collection("patients").document();
                    demoPatient.id = patientRef.getId();
                    writes.add(patientRef.set(demoPatient.toMap()));
                    // Link this patient to current user UID for convenience
                    writes.add(db.collection("users").document(currentUserUid).update("seedPatientId", demoPatient.id));

                    // Appointments: next 3 days at 10:00 for 45 minutes
                    Calendar base = Calendar.getInstance();
                    base.set(Calendar.MINUTE, 0);
                    base.set(Calendar.SECOND, 0);
                    base.set(Calendar.MILLISECOND, 0);

                    List<AppointmentSeed> appts = new ArrayList<>();
                    for (int i = 1; i <= 3; i++) {
                        AppointmentSeed a = new AppointmentSeed();
                        a.doctorId = doctors.get(i % doctors.size()).id;
                        a.patientId = demoPatient.id;

                        Calendar start = (Calendar) base.clone();
                        start.add(Calendar.DATE, i);
                        start.set(Calendar.HOUR_OF_DAY, 10);

                        Calendar end = (Calendar) start.clone();
                        end.add(Calendar.MINUTE, 45);

                        a.startAtEpochMillis = start.getTimeInMillis();
                        a.endAtEpochMillis = end.getTimeInMillis();
                        a.title = "Consultation " + i;
                        a.notes = "Follow-up & prescription review.";
                        appts.add(a);
                    }

                    for (AppointmentSeed a : appts) {
                        DocumentReference ref = db.collection("appointments").document();
                        a.id = ref.getId();
                        writes.add(ref.set(a.toMap()));
                    }

                    // Wait all, then set the marker
                    AtomicInteger remaining = new AtomicInteger(writes.size());
                    com.google.android.gms.tasks.TaskCompletionSource<Void> tcs = new com.google.android.gms.tasks.TaskCompletionSource<>();

                    for (Task<?> w : writes) {
                        w.addOnCompleteListener(done -> {
                            if (remaining.decrementAndGet() == 0) {
                                marker.set(new java.util.HashMap<String, Object>() {{
                                            put("at", System.currentTimeMillis());
                                            put("by", currentUserUid);
                                        }}).addOnSuccessListener(v -> tcs.setResult(null))
                                        .addOnFailureListener(tcs::setException);
                            }
                        });
                    }
                    return tcs.getTask();
                }).addOnSuccessListener(aVoid -> Log.d(TAG, "Seed completed."))
                .addOnFailureListener(e -> Log.e(TAG, "Seed failed", e));
    }

    private static DoctorSeed makeDoctor(String name, String spec, String hospital, String avatar) {
        DoctorSeed d = new DoctorSeed();
        d.fullName = name;
        d.specialty = spec;
        d.hospital = hospital;
        d.avatarUrl = avatar;
        return d;
    }
}
