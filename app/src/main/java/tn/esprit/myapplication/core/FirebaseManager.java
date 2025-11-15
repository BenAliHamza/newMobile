package tn.esprit.myapplication.core;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * FirebaseManager
 *
 * ✅ Free of static Context-holding singletons (addresses memory-leak lint).
 * We do NOT keep FirebaseAuth/Firestore/Storage in static fields anymore.
 * Instead, we always fetch instances from the SDK (which manages them safely).
 *
 * App-level initialization happens in App.onCreate().
 */
public final class FirebaseManager {

    public static final String COLLECTION_USERS = "users";

    private FirebaseManager() { }

    /** Backward-compatible no-op. Keep for old callers; safe to remove later. */
    public static void init(@NonNull Context context) {
        // Ensure default app exists; safe to call multiple times.
        try {
            FirebaseApp.getInstance();
        } catch (IllegalStateException ignore) {
            FirebaseApp.initializeApp(context.getApplicationContext());
        }
    }

    public static FirebaseAuth auth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    public static FirebaseStorage storage() {
        return FirebaseStorage.getInstance();
    }

    public static CollectionReference users() {
        return db().collection(COLLECTION_USERS);
    }

    // Auth helpers (one-shot Tasks)
    public static Task<AuthResult> signInWithEmail(@NonNull String email, @NonNull String password) {
        return auth().signInWithEmailAndPassword(email.trim(), password);
    }

    public static DocumentReference userDoc(@NonNull String uid) {
        return users().document(uid);
    }

    public static void signOut() {
        auth().signOut();
    }
}
