package tn.esprit.myapplication.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.databinding.ActivityForgotPasswordBinding;
import tn.esprit.myapplication.util.NetworkUtil;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseManager.init(this);

        binding.btnSend.setOnClickListener(v -> sendReset());
        binding.btnBackToLogin.setOnClickListener(v -> finish());
    }

    private void sendReset() {
        String email = String.valueOf(binding.inputEmail.getText()).trim();

        if (TextUtils.isEmpty(email)) {
            binding.inputLayoutEmail.setError(getString(tn.esprit.myapplication.R.string.error_email_required));
            return;
        } else {
            binding.inputLayoutEmail.setError(null);
        }

        if (!NetworkUtil.isOnline(this)) {
            Toast.makeText(this, getString(tn.esprit.myapplication.R.string.offline_message), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> {
                    setLoading(false);
                    Toast.makeText(this, getString(tn.esprit.myapplication.R.string.msg_reset_email_sent), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, getString(tn.esprit.myapplication.R.string.msg_reset_email_failed), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSend.setEnabled(!loading);
        binding.btnBackToLogin.setEnabled(!loading);
        binding.inputEmail.setEnabled(!loading);
    }
}
