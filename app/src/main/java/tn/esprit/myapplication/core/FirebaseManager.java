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

/** Centralized, minimal Firebase access. */
public final class FirebaseManager {

    public static final String COLLECTION_USERS = "users";

    private static FirebaseAuth auth;
    private static FirebaseFirestore firestore;
    private static FirebaseStorage storage;

    private FirebaseManager() {}

    public static void init(@NonNull Context context) {
        FirebaseApp.initializeApp(context.getApplicationContext());
        if (auth == null) auth = FirebaseAuth.getInstance();
        if (firestore == null) firestore = FirebaseFirestore.getInstance();
        if (storage == null) storage = FirebaseStorage.getInstance();
    }

    public static FirebaseAuth auth() {
        if (auth == null) auth = FirebaseAuth.getInstance();
        return auth;
    }

    public static FirebaseFirestore db() {
        if (firestore == null) firestore = FirebaseFirestore.getInstance();
        return firestore;
    }

    public static FirebaseStorage storage() {
        if (storage == null) storage = FirebaseStorage.getInstance();
        return storage;
    }

    public static CollectionReference users() {
        return db().collection(COLLECTION_USERS);
    }

    // Auth helpers
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
