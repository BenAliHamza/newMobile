package tn.esprit.myapplication.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import tn.esprit.myapplication.data.auth.AuthRepository;

/**
 * ForgotPasswordViewModel
 * - Sends password reset email via AuthRepository.
 */
public class ForgotPasswordViewModel extends ViewModel {

    private final AuthRepository repo = new AuthRepository();

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> loading = _loading;

    private final MutableLiveData<String> _message = new MutableLiveData<>();
    public LiveData<String> message = _message;

    private final MutableLiveData<Boolean> _sent = new MutableLiveData<>(false);
    public LiveData<Boolean> sent = _sent;

    public void sendReset(@NonNull String email) {
        if (_loading.getValue() != null && _loading.getValue()) return;
        _loading.setValue(true);

        repo.sendPasswordReset(email)
                .addOnSuccessListener(v -> {
                    _loading.setValue(false);
                    _sent.setValue(true);
                })
                .addOnFailureListener(e -> {
                    _loading.setValue(false);
                    _message.setValue(e.getMessage());
                });
    }
}
