package tn.esprit.myapplication.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Locale;

import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.data.Role;
import tn.esprit.myapplication.data.User;
import tn.esprit.myapplication.data.auth.AuthErrorMapper;
import tn.esprit.myapplication.data.auth.AuthRepository;

/**
 * LoginViewModel (MVVM)
 * - Orchestrates email/password & Google sign-in through AuthRepository.
 * - Exposes minimal state to UI to keep Fragment thin.
 *
 * Google sign-in flow:
 *  1) Firebase Auth sign-in with credential.
 *  2) Check Firestore "users/{uid}".
 *     - If exists -> done -> go Home.
 *     - If missing -> UI must ask for role (Doctor / Patient),
 *       then we create the user profile in Firestore.
 */
public class LoginViewModel extends ViewModel {

    private final AuthRepository repo = new AuthRepository();

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> loading = _loading;

    private final MutableLiveData<String> _message = new MutableLiveData<>();
    public LiveData<String> message = _message;

    private final MutableLiveData<Boolean> _signedIn = new MutableLiveData<>(false);
    public LiveData<Boolean> signedIn = _signedIn;

    // For Google path: when true, Fragment should show role picker dialog.
    private final MutableLiveData<Boolean> _needsGoogleProfile = new MutableLiveData<>(false);
    public LiveData<Boolean> needsGoogleProfile = _needsGoogleProfile;

    // Remember last Google FirebaseUser so we can build profile after role selection.
    private FirebaseUser lastGoogleUser;

    // -------- Email / password sign-in -------- //

    public void signInWithEmail(@NonNull String email, @NonNull String password) {
        if (Boolean.TRUE.equals(_loading.getValue())) return;
        _loading.setValue(true);

        repo.signInWithEmail(email, password)
                .addOnSuccessListener(this::onAuthSuccessEmailPassword)
                .addOnFailureListener(e -> {
                    _loading.setValue(false);
                    _message.setValue(AuthErrorMapper.toMessage(e));
                });
    }

    private void onAuthSuccessEmailPassword(AuthResult result) {
        FirebaseUser user = result != null ? result.getUser() : null;
        if (user == null) {
            _loading.setValue(false);
            _message.setValue("Authentication failed: null user.");
            return;
        }
        // Email/password path assumes profile already created at registration.
        _loading.setValue(false);
        _signedIn.setValue(true);
    }

    // -------- Google sign-in -------- //

    public void signInWithCredential(@NonNull AuthCredential credential) {
        if (Boolean.TRUE.equals(_loading.getValue())) return;
        _loading.setValue(true);

        repo.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result != null ? result.getUser() : null;
                    if (user == null) {
                        _loading.setValue(false);
                        _message.setValue("Authentication failed: null user.");
                        return;
                    }
                    lastGoogleUser = user;
                    checkGoogleUserProfile(user);
                })
                .addOnFailureListener(e -> {
                    _loading.setValue(false);
                    _message.setValue(AuthErrorMapper.toMessage(e));
                });
    }

    /**
     * Checks if the Firestore user profile exists for this Google user.
     * If yes -> we're done.
     * If no -> UI must ask user to choose a role (Doctor / Patient).
     */
    private void checkGoogleUserProfile(@NonNull FirebaseUser firebaseUser) {
        FirebaseManager.userDoc(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(this::handleGoogleProfileSnapshot)
                .addOnFailureListener(e -> {
                    _loading.setValue(false);
                    _message.setValue(AuthErrorMapper.toMessage(e));
                });
    }

    private void handleGoogleProfileSnapshot(DocumentSnapshot snap) {
        if (snap != null && snap.exists()) {
            // Profile already exists, nothing else to do.
            _loading.setValue(false);
            _needsGoogleProfile.setValue(false);
            _signedIn.setValue(true);
        } else {
            // No profile -> let Fragment show role dialog.
            _loading.setValue(false);
            _needsGoogleProfile.setValue(true);
        }
    }

    /**
     * Called by Fragment AFTER user chooses a role from the Google role dialog.
     *
     * @param roleLabel usually "DOCTOR" or "PATIENT"
     *                  coming from getString(R.string.role_doctor / role_patient).
     */
    public void completeGoogleProfileWithRole(@NonNull String roleLabel) {
        if (lastGoogleUser == null) {
            _message.setValue("Google user info missing. Please sign in again.");
            return;
        }

        Role role = Role.fromString(roleLabel);
        if (role == null) {
            _message.setValue("Invalid role selected.");
            return;
        }

        String email = lastGoogleUser.getEmail();
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        } else {
            email = "";
        }

        String displayName = lastGoogleUser.getDisplayName();
        String firstName = "";
        String lastName = "";

        if (displayName != null && !displayName.trim().isEmpty()) {
            String[] parts = displayName.trim().split("\\s+", 2);
            firstName = parts[0];
            if (parts.length > 1) {
                lastName = parts[1];
            }
        }

        if (firstName.isEmpty()) {
            firstName = "Unknown";
        }

        // Build profile model (matches what RegisterViewModel uses).
        User profile = new User();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setRole(role);
        profile.setEmail(email);
        profile.setIsFirstLogin(true);
        profile.setSex(null); // not known from Google by default
        if (lastGoogleUser.getPhotoUrl() != null) {
            profile.setImageUrl(lastGoogleUser.getPhotoUrl().toString());
        } else {
            profile.setImageUrl(null);
        }

        _loading.setValue(true);
        repo.createUserProfile(lastGoogleUser.getUid(), profile)
                .addOnSuccessListener(aVoid -> {
                    _loading.setValue(false);
                    _needsGoogleProfile.setValue(false);
                    _signedIn.setValue(true);
                })
                .addOnFailureListener(e -> {
                    _loading.setValue(false);
                    _message.setValue(AuthErrorMapper.toMessage(e));
                });
    }
}
