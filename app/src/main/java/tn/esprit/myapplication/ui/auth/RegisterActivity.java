package tn.esprit.myapplication.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;

import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.data.Role;
import tn.esprit.myapplication.data.User;
import tn.esprit.myapplication.databinding.ActivityRegisterBinding;
import tn.esprit.myapplication.util.NetworkUtil;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseManager.init(this);

        List<String> sexes = Arrays.asList("Male", "Female", "Other");
        List<String> roles = Arrays.asList("DOCTOR", "PATIENT");
        binding.spinnerSex.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sexes));
        binding.spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles));

        binding.btnCreate.setOnClickListener(v -> tryRegister());
        binding.btnBackToLogin.setOnClickListener(v -> finish());
    }

    private void tryRegister() {
        String firstName = String.valueOf(binding.inputFirstName.getText()).trim();
        String lastName  = String.valueOf(binding.inputLastName.getText()).trim();
        String email     = String.valueOf(binding.inputEmail.getText()).trim();
        String password  = String.valueOf(binding.inputPassword.getText());
        String sex       = binding.spinnerSex.getSelectedItem() != null ? binding.spinnerSex.getSelectedItem().toString() : "";
        String roleStr   = binding.spinnerRole.getSelectedItem() != null ? binding.spinnerRole.getSelectedItem().toString() : "";

        boolean invalid = false;
        if (TextUtils.isEmpty(firstName)) { binding.inputLayoutFirstName.setError("Required"); invalid = true; } else binding.inputLayoutFirstName.setError(null);
        if (TextUtils.isEmpty(lastName))  { binding.inputLayoutLastName.setError("Required"); invalid = true; } else binding.inputLayoutLastName.setError(null);
        if (TextUtils.isEmpty(email))     { binding.inputLayoutEmail.setError("Required"); invalid = true; } else binding.inputLayoutEmail.setError(null);
        if (TextUtils.isEmpty(password))  { binding.inputLayoutPassword.setError("Required"); invalid = true; } else binding.inputLayoutPassword.setError(null);
        if (TextUtils.isEmpty(sex))       { Toast.makeText(this, "Select sex", Toast.LENGTH_SHORT).show(); invalid = true; }
        if (TextUtils.isEmpty(roleStr))   { Toast.makeText(this, "Select role", Toast.LENGTH_SHORT).show(); invalid = true; }
        if (invalid) return;

        if (!NetworkUtil.isOnline(this)) {
            Toast.makeText(this, "You are offline. Check your connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        FirebaseManager.auth()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        setLoading(false);
                        Toast.makeText(this, "User not created.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String uid = result.getUser().getUid();

                    Role role = Role.fromString(roleStr);
                    User user = new User(
                            firstName,
                            lastName,
                            sex,
                            role,
                            true,
                            "",     // imageUrl default
                            email
                    );

                    FirebaseManager.users()
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(aVoid -> {
                                setLoading(false);
                                Toast.makeText(this, "Account created.", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnCreate.setEnabled(!loading);
        binding.btnBackToLogin.setEnabled(!loading);
        binding.inputFirstName.setEnabled(!loading);
        binding.inputLastName.setEnabled(!loading);
        binding.inputEmail.setEnabled(!loading);
        binding.inputPassword.setEnabled(!loading);
        binding.spinnerSex.setEnabled(!loading);
        binding.spinnerRole.setEnabled(!loading);
    }
}
