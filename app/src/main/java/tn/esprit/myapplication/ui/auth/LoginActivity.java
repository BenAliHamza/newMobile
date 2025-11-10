package tn.esprit.myapplication.ui.auth;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

import java.util.Arrays;
import java.util.List;

import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.data.Role;
import tn.esprit.myapplication.data.User;
import tn.esprit.myapplication.databinding.ActivityLoginBinding;
import tn.esprit.myapplication.seed.DataSeeder;
import tn.esprit.myapplication.ui.home.HomeActivity;
import tn.esprit.myapplication.ui.auth.RegisterActivity;
import tn.esprit.myapplication.util.NetworkUtil;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private GoogleSignInClient googleClient;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) {
                    showLoading(false);
                    Toast.makeText(this, "Google Sign-In canceled.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account == null) {
                        showLoading(false);
                        Toast.makeText(this, "Google account is null.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String idToken = account.getIdToken();
                    if (TextUtils.isEmpty(idToken)) {
                        showLoading(false);
                        Toast.makeText(this, "Missing ID token. Add SHA-1 in Firebase and re-sync.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
                    FirebaseManager.auth().signInWithCredential(credential)
                            .addOnSuccessListener(authResult -> onGoogleSignedIn(account))
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                } catch (ApiException e) {
                    showLoading(false);
                    Toast.makeText(this, "Google Sign-In failed: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseManager.init(this);

        // Email/Password
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
        binding.inputPassword.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { attemptLogin(); return true; }
            return false;
        });

        // Google
        String webClientId;
        try {
            webClientId = getString(tn.esprit.myapplication.R.string.default_web_client_id);
        } catch (Resources.NotFoundException ex) {
            webClientId = null;
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(webClientId)
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        binding.btnGoogle.setOnClickListener(v -> {
            if (!NetworkUtil.isOnline(this)) {
                Toast.makeText(this, "You are offline. Check your connection.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String check = getString(tn.esprit.myapplication.R.string.default_web_client_id);
                if (TextUtils.isEmpty(check) || "REPLACE_WITH_WEB_CLIENT_ID".equalsIgnoreCase(check)) {
                    Toast.makeText(this, "Configure SHA-1 in Firebase & re-download google-services.json.", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Resources.NotFoundException e) {
                Toast.makeText(this, "Missing default_web_client_id. Add SHA-1 & re-sync.", Toast.LENGTH_LONG).show();
                return;
            }
            showLoading(true);
            googleLauncher.launch(googleClient.getSignInIntent());
        });
    }

    // ----- Email/Password -----
    private void attemptLogin() {
        String email = String.valueOf(binding.inputEmail.getText()).trim();
        String password = String.valueOf(binding.inputPassword.getText());

        if (TextUtils.isEmpty(email)) { binding.inputLayoutEmail.setError("Email required"); return; }
        else binding.inputLayoutEmail.setError(null);

        if (TextUtils.isEmpty(password)) { binding.inputLayoutPassword.setError("Password required"); return; }
        else binding.inputLayoutPassword.setError(null);

        if (!NetworkUtil.isOnline(this)) {
            Toast.makeText(this, "You are offline. Check your connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        FirebaseManager.signInWithEmail(email, password)
                .addOnSuccessListener(result -> seedThenEnter())
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ----- Google: create-or-login with REQUIRED role selection -----
    private void onGoogleSignedIn(GoogleSignInAccount account) {
        FirebaseUser fu = FirebaseManager.auth().getCurrentUser();
        if (fu == null) {
            showLoading(false);
            Toast.makeText(this, "No Firebase user after Google sign-in.", Toast.LENGTH_LONG).show();
            return;
        }
        String uid = fu.getUid();

        FirebaseManager.users().document(uid).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        seedThenEnter();
                    } else {
                        List<String> roles = Arrays.asList("DOCTOR", "PATIENT");
                        final int[] selected = {1}; // default PATIENT
                        new AlertDialog.Builder(this)
                                .setTitle("Select role")
                                .setSingleChoiceItems(roles.toArray(new String[0]), selected[0], (d, which) -> selected[0] = which)
                                .setPositiveButton("Continue", (d, w) -> {
                                    d.dismiss();
                                    String displayName = account.getDisplayName() == null ? "" : account.getDisplayName();
                                    String firstName = displayName.contains(" ") ? displayName.substring(0, displayName.indexOf(' ')) : displayName;
                                    String lastName = displayName.contains(" ") ? displayName.substring(displayName.indexOf(' ') + 1) : "";
                                    String email = account.getEmail();
                                    String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";

                                    Role role = Role.fromString(roles.get(selected[0]));
                                    User user = new User(
                                            firstName,
                                            lastName,
                                            "",           // sex unknown
                                            role,
                                            true,
                                            photoUrl,
                                            email == null ? "" : email
                                    );
                                    FirebaseManager.users().document(uid).set(user)
                                            .addOnSuccessListener(aVoid -> seedThenEnter())
                                            .addOnFailureListener(e -> {
                                                showLoading(false);
                                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                })
                                .setNegativeButton("Cancel", (d, w) -> {
                                    d.dismiss();
                                    showLoading(false);
                                    FirebaseManager.signOut();
                                })
                                .setCancelable(false)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void seedThenEnter() {
        FirebaseUser fu = FirebaseManager.auth().getCurrentUser();
        if (fu == null) {
            showLoading(false);
            return;
        }
        String email = fu.getEmail() == null ? "" : fu.getEmail();
        DataSeeder.runIfNeeded(fu.getUid(), email)
                .addOnCompleteListener(done -> {
                    showLoading(false);
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                });
    }

    private void showLoading(boolean show) {
        binding.progress.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
        binding.btnGoogle.setEnabled(!show);
        binding.btnGoToRegister.setEnabled(!show);
        binding.inputEmail.setEnabled(!show);
        binding.inputPassword.setEnabled(!show);
    }
}
