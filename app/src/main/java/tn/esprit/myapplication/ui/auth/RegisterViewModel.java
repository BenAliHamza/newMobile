package tn.esprit.myapplication.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

import tn.esprit.myapplication.data.User;
import tn.esprit.myapplication.data.auth.AuthRepository;

/**
 * RegisterViewModel
 * - Handles email/password account creation and initial profile write.
 * - Sends a verification email after registration.
 */
public class RegisterViewModel extends ViewModel {

    private final AuthRepository repo = new AuthRepository();

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> loading = _loading;

    private final MutableLiveData<String> _message = new MutableLiveData<>();
    public LiveData<String> message = _message;

    private final MutableLiveData<Boolean> _registered = new MutableLiveData<>(false);
    public LiveData<Boolean> registered = _registered;

    public void register(@NonNull String email,
                         @NonNull String password,
                         @NonNull User profile) {
        if (_loading.getValue() != null && _loading.getValue()) return;
        _loading.setValue(true);

        repo.registerWithEmail(email, password)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    AuthResult ar = task.getResult();
                    FirebaseUser fu = ar != null ? ar.getUser() : null;
                    if (fu == null) {
                        throw new IllegalStateException("User not created.");
                    }

                    // Normalize profile email to the one from auth (source of truth)
                    profile.setEmail(fu.getEmail() == null ? email : fu.getEmail());

                    // Fire off verification email (do not fail registration if this fails).
                    fu.sendEmailVerification()
                            .addOnFailureListener(e -> _message.postValue(e.getMessage()));

                    // Persist user profile in Firestore.
                    return repo.createUserProfile(fu.getUid(), profile);
                })
                .addOnSuccessListener(aVoid -> {
                    _loading.setValue(false);
                    _registered.setValue(true);
                })
                .addOnFailureListener(e -> {
                    _loading.setValue(false);
                    _message.setValue(e.getMessage());
                });
    }
}
