package tn.esprit.myapplication.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

import tn.esprit.myapplication.data.User;
import tn.esprit.myapplication.data.auth.AuthErrorMapper;
import tn.esprit.myapplication.data.auth.AuthRepository;

/**
 * RegisterViewModel
 * - Handles email/password account creation and initial profile write.
 * - Minor tweak: ensure profile email equals auth email (source of truth).
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
        if (Boolean.TRUE.equals(_loading.getValue())) return;
        _loading.setValue(true);

        repo.registerWithEmail(email, password)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Registration failed.");
                    }
                    AuthResult ar = task.getResult();
                    FirebaseUser fu = ar != null ? ar.getUser() : null;
                    if (fu == null) {
                        throw new IllegalStateException("User not created.");
                    }
                    // Normalize profile email to the one from auth
                    profile.setEmail(fu.getEmail() == null ? email : fu.getEmail());
                    return repo.createUserProfile(fu.getUid(), profile);
                })
                .addOnSuccessListener(aVoid -> {
                    _loading.setValue(false);
                    _registered.setValue(true);
                })
                .addOnFailureListener(e -> {
                    _loading.setValue(false);
                    _message.setValue(AuthErrorMapper.toMessage(e));
                });
    }
}
