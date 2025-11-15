package tn.esprit.myapplication.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

import tn.esprit.myapplication.data.auth.AuthErrorMapper;
import tn.esprit.myapplication.data.auth.AuthRepository;

/**
 * LoginViewModel (MVVM)
 * - Orchestrates email/password & Google sign-in through AuthRepository.
 * - Exposes minimal state to UI to keep Fragment thin.
 *
 * Notes for Firebase Free Tier:
 * - Uses one-shot Tasks only (no realtime listeners).
 * - Avoids extra reads/writes beyond sign-in.
 */
public class LoginViewModel extends ViewModel {

    private final AuthRepository repo = new AuthRepository();

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> loading = _loading;

    private final MutableLiveData<String> _message = new MutableLiveData<>();
    public LiveData<String> message = _message;

    private final MutableLiveData<Boolean> _signedIn = new MutableLiveData<>(false);
    public LiveData<Boolean> signedIn = _signedIn;

    public void signInWithEmail(@NonNull String email, @NonNull String password) {
        if (Boolean.TRUE.equals(_loading.getValue())) return;
        _loading.setValue(true);

        repo.signInWithEmail(email, password)
                .addOnSuccessListener(this::onAuthSuccess)
                .addOnFailureListener(e -> {
                    _loading.setValue(false);
                    _message.setValue(AuthErrorMapper.toMessage(e));
                });
    }

    public void signInWithCredential(@NonNull AuthCredential credential) {
        if (Boolean.TRUE.equals(_loading.getValue())) return;
        _loading.setValue(true);

        repo.signInWithCredential(credential)
                .addOnSuccessListener(this::onAuthSuccess)
                .addOnFailureListener(e -> {
                    _loading.setValue(false);
                    _message.setValue(AuthErrorMapper.toMessage(e));
                });
    }

    private void onAuthSuccess(AuthResult result) {
        FirebaseUser user = result != null ? result.getUser() : null;
        if (user == null) {
            _loading.setValue(false);
            _message.setValue("Authentication failed: null user.");
            return;
        }
        _loading.setValue(false);
        _signedIn.setValue(true);
    }
}
