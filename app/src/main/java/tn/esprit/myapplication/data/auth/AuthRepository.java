package tn.esprit.myapplication.data.auth;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.data.User;

/**
 * AuthRepository
 * - Uses SDK singletons (no static Context references).
 * - Free tier friendly: one-shot Tasks only.
 */
public final class AuthRepository {

    public Task<AuthResult> signInWithEmail(@NonNull String email, @NonNull String password) {
        return FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), password);
    }

    public Task<AuthResult> signInWithCredential(@NonNull AuthCredential credential) {
        return FirebaseAuth.getInstance().signInWithCredential(credential);
    }

    public Task<AuthResult> registerWithEmail(@NonNull String email, @NonNull String password) {
        return FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.trim(), password);
    }

    public Task<Void> createUserProfile(@NonNull String uid, @NonNull User user) {
        DocumentReference doc = FirebaseFirestore.getInstance()
                .collection(FirebaseManager.COLLECTION_USERS)
                .document(uid);
        return doc.set(user);
    }

    /** Send password reset email (one-shot). */
    public Task<Void> sendPasswordReset(@NonNull String email) {
        return FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim());
    }

    public FirebaseUser currentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
    }
}
