package tn.esprit.myapplication.core;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * One-shot seeding for demo data. Safe to call multiple times; it won't duplicate
 * if collections already contain documents.
 *
 * Usage (e.g. from HomeActivity on first app open):
 *   SeedData.run(getApplicationContext());
 */
public final class SeedData {

    private static final String TAG = "SeedData";
    private static final String DOCTORS_COL = "doctors";
    private static final String PATIENTS_COL = "patients";

    private SeedData() {}

    public static void run(Context ctx) {
        FirebaseFirestore db = FirebaseManager.db();

        // Seed doctors if empty
        try {
            CollectionReference doctorsRef = db.collection(DOCTORS_COL);
            int existingDoctors = Tasks.await(doctorsRef.limit(1).get()).size();
            if (existingDoctors == 0) {
                JSONArray arr = new JSONArray(readAsset(ctx, "fake_doctors.json"));
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("fullName", o.optString("fullName", ""));
                    doc.put("specialty", o.optString("specialty", ""));
                    doc.put("bio", o.optString("bio", ""));
                    doc.put("services", jsonArrayToList(o.optJSONArray("services")));
                    doc.put("prescriptions", jsonArrayToList(o.optJSONArray("prescriptions")));
                    doctorsRef.add(doc);
                }
                Log.i(TAG, "Seeded doctors collection.");
            } else {
                Log.i(TAG, "Doctors collection already populated. Skipping.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed seeding doctors", e);
        }

        // Seed sample patients if empty (NOTE: not linked to Auth users)
        try {
            CollectionReference patientsRef = db.collection(PATIENTS_COL);
            int existingPatients = Tasks.await(patientsRef.limit(1).get()).size();
            if (existingPatients == 0) {
                JSONArray arr = new JSONArray(readAsset(ctx, "fake_patients.json"));
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("firstName", o.optString("firstName", ""));
                    doc.put("lastName", o.optString("lastName", ""));
                    doc.put("sex", o.optString("sex", ""));
                    doc.put("role", "PATIENT");
                    doc.put("isFirstLogin", o.optBoolean("isFirstLogin", false));
                    doc.put("imageUrl", o.optString("imageUrl", ""));
                    doc.put("email", o.optString("email", ""));
                    patientsRef.add(doc);
                }
                Log.i(TAG, "Seeded patients collection.");
            } else {
                Log.i(TAG, "Patients collection already populated. Skipping.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed seeding patients", e);
        }
    }

    // ---- helpers ----

    private static String readAsset(Context ctx, String name) throws Exception {
        AssetManager am = ctx.getAssets();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(am.open(name), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String ln;
            while ((ln = br.readLine()) != null) sb.append(ln);
            return sb.toString();
        }
    }

    private static java.util.List<String> jsonArrayToList(JSONArray arr) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            out.add(arr.optString(i, ""));
        }
        return out;
    }
}
