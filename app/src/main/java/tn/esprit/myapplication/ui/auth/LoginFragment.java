package tn.esprit.myapplication.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
import tn.esprit.myapplication.ui.home.HomeActivity;
import tn.esprit.myapplication.util.NetworkUtil;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    // GoogleSignInClient is deprecated in newer Play Services; we keep it for now but
    // isolate and suppress the warning so the rest of the codebase stays clean.
    @SuppressWarnings("deprecation")
    private GoogleSignInClient googleClient;

    private LoginViewModel viewModel;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) {
                    setLoading(false);
                    Toast.makeText(requireContext(), getString(R.string.google_cancelled), Toast.LENGTH_SHORT).show();
                    return;
                }
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account == null) {
                        setLoading(false);
                        Toast.makeText(requireContext(), getString(R.string.google_null_account), Toast.LENGTH_LONG).show();
                        return;
                    }
                    String idToken = account.getIdToken();
                    if (TextUtils.isEmpty(idToken)) {
                        setLoading(false);
                        Toast.makeText(requireContext(), getString(R.string.google_missing_token), Toast.LENGTH_LONG).show();
                        return;
                    }
                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
                    viewModel.signInWithCredential(credential);
                } catch (ApiException e) {
                    setLoading(false);
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
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Focus email immediately for better UX
        binding.inputEmail.requestFocus();

        // Observers
        viewModel.loading.observe(getViewLifecycleOwner(), this::setLoading);
        viewModel.message.observe(getViewLifecycleOwner(), msg -> {
            if (!TextUtils.isEmpty(msg)) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.signedIn.observe(getViewLifecycleOwner(), ok -> {
            if (ok != null && ok) {
                FirebaseUser fu = FirebaseManager.auth().getCurrentUser();
                if (fu != null) {
                    Intent i = new Intent(requireContext(), HomeActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    requireActivity().finish();
                }
            }
        });

        // Email/Password
        binding.btnLogin.setOnClickListener(v1 -> attemptLogin());
        binding.inputPassword.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        // Google Sign-In
        String webClientId;
        try {
            webClientId = getString(R.string.default_web_client_id);
        } catch (Resources.NotFoundException ex) {
            webClientId = null;
        }

        //noinspection deprecation
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(webClientId)
                .build();

        //noinspection deprecation
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
            hideKeyboard();
            setLoading(true);
            //noinspection deprecation
            googleLauncher.launch(googleClient.getSignInIntent());
        });

        // Nav actions
        binding.btnGoToRegister.setOnClickListener(
                v13 -> NavHostFragment.findNavController(this).navigate(R.id.action_login_to_register));
        binding.btnForgot.setOnClickListener(
                v14 -> NavHostFragment.findNavController(this).navigate(R.id.action_login_to_forgotPassword));
    }

    private void attemptLogin() {
        // Clear previous errors
        binding.inputLayoutEmail.setError(null);
        binding.inputLayoutPassword.setError(null);

        String email = String.valueOf(binding.inputEmail.getText()).trim();
        String password = String.valueOf(binding.inputPassword.getText());

        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            binding.inputLayoutEmail.setError(getString(R.string.error_email_required));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEmail.setError(getString(R.string.error_invalid_email));
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.inputLayoutPassword.setError(getString(R.string.error_required));
            valid = false;
        } else if (password.length() < 6) {
            binding.inputLayoutPassword.setError(getString(R.string.error_password_min));
            valid = false;
        }

        if (!valid) {
            return;
        }

        if (!NetworkUtil.isOnline(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.offline_message), Toast.LENGTH_SHORT).show();
            return;
        }

        hideKeyboard();
        setLoading(true);
        viewModel.signInWithEmail(email, password);
    }

    private void setLoading(boolean show) {
        if (binding == null) return;
        View overlay = binding.loadingOverlay.getRoot();

        if (show) {
            overlay.setClickable(true);
            if (overlay.getVisibility() != View.VISIBLE) {
                overlay.setAlpha(0f);
                overlay.setVisibility(View.VISIBLE);
                overlay.animate()
                        .alpha(1f)
                        .setDuration(200L)
                        .setListener(null);
            }
        } else {
            if (overlay.getVisibility() == View.VISIBLE) {
                overlay.animate()
                        .alpha(0f)
                        .setDuration(200L)
                        .withEndAction(() -> {
                            overlay.setVisibility(View.GONE);
                            overlay.setAlpha(1f);
                        });
            }
        }

        binding.btnLogin.setEnabled(!show);
        binding.btnGoogle.setEnabled(!show);
        binding.btnGoToRegister.setEnabled(!show);
        binding.inputEmail.setEnabled(!show);
        binding.inputPassword.setEnabled(!show);
    }

    private void hideKeyboard() {
        if (getActivity() == null) return;
        View view = getActivity().getCurrentFocus();
        if (view == null && binding != null) {
            view = binding.getRoot();
        }
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
