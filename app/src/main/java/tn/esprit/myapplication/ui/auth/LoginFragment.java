package tn.esprit.myapplication.ui.auth;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.databinding.FragmentLoginBinding;
import tn.esprit.myapplication.seed.DataSeeder;
import tn.esprit.myapplication.ui.home.HomeActivity;
import tn.esprit.myapplication.util.NetworkUtil;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private GoogleSignInClient googleClient;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) {
                    showLoading(false);
                    Toast.makeText(requireContext(), getString(R.string.google_cancelled), Toast.LENGTH_SHORT).show();
                    return;
                }
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account == null) {
                        showLoading(false);
                        Toast.makeText(requireContext(), getString(R.string.google_null_account), Toast.LENGTH_LONG).show();
                        return;
                    }
                    String idToken = account.getIdToken();
                    if (TextUtils.isEmpty(idToken)) {
                        showLoading(false);
                        Toast.makeText(requireContext(), getString(R.string.google_missing_token), Toast.LENGTH_LONG).show();
                        return;
                    }
                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
                    FirebaseManager.auth().signInWithCredential(credential)
                            .addOnSuccessListener(authResult -> seedThenEnter())
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                } catch (ApiException e) {
                    showLoading(false);
                    Toast.makeText(requireContext(), getString(R.string.google_failed, e.getStatusCode()), Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        FirebaseManager.init(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Email/Password
        binding.btnLogin.setOnClickListener(v1 -> attemptLogin());
        binding.inputPassword.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { attemptLogin(); return true; }
            return false;
        });

        // Google
        String webClientId;
        try {
            webClientId = getString(R.string.default_web_client_id);
        } catch (Resources.NotFoundException ex) {
            webClientId = null;
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(webClientId)
                .build();
        googleClient = GoogleSignIn.getClient(requireContext(), gso);

        binding.btnGoogle.setOnClickListener(v12 -> {
            if (!NetworkUtil.isOnline(requireContext())) {
                Toast.makeText(requireContext(), getString(R.string.offline_message), Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String check = getString(R.string.default_web_client_id);
                if (TextUtils.isEmpty(check) || "REPLACE_WITH_WEB_CLIENT_ID".equalsIgnoreCase(check)) {
                    Toast.makeText(requireContext(), getString(R.string.google_configure_sha1), Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Resources.NotFoundException e) {
                Toast.makeText(requireContext(), getString(R.string.google_missing_res_id), Toast.LENGTH_LONG).show();
                return;
            }
            showLoading(true);
            googleLauncher.launch(googleClient.getSignInIntent());
        });

        // Nav actions
        binding.btnGoToRegister.setOnClickListener(
                v13 -> NavHostFragment.findNavController(this).navigate(R.id.action_login_to_register));
        binding.btnForgot.setOnClickListener(
                v14 -> NavHostFragment.findNavController(this).navigate(R.id.action_login_to_forgotPassword));
    }

    private void attemptLogin() {
        String email = String.valueOf(binding.inputEmail.getText()).trim();
        String password = String.valueOf(binding.inputPassword.getText());

        if (TextUtils.isEmpty(email)) { binding.inputLayoutEmail.setError(getString(R.string.error_email_required)); return; }
        else binding.inputLayoutEmail.setError(null);

        if (TextUtils.isEmpty(password)) { binding.inputLayoutPassword.setError(getString(R.string.error_required)); return; }
        else binding.inputLayoutPassword.setError(null);

        if (!NetworkUtil.isOnline(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.offline_message), Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        FirebaseManager.signInWithEmail(email, password)
                .addOnSuccessListener(result -> seedThenEnter())
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void seedThenEnter() {
        FirebaseUser fu = FirebaseManager.auth().getCurrentUser();
        if (fu == null) {
            showLoading(false);
            return;
        }
        String email = fu.getEmail() == null ? "" : fu.getEmail();
        tn.esprit.myapplication.seed.DataSeeder.runIfNeeded(fu.getUid(), email)
                .addOnCompleteListener(done -> {
                    showLoading(false);
                    Intent i = new Intent(requireContext(), HomeActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    requireActivity().finish();
                });
    }

    private void showLoading(boolean show) {
        if (binding == null) return;
        binding.progress.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
        binding.btnGoogle.setEnabled(!show);
        binding.btnGoToRegister.setEnabled(!show);
        binding.inputEmail.setEnabled(!show);
        binding.inputPassword.setEnabled(!show);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
