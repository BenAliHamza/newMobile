package tn.esprit.myapplication.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.data.Role;
import tn.esprit.myapplication.data.User;
import tn.esprit.myapplication.databinding.FragmentRegisterBinding;
import tn.esprit.myapplication.util.NetworkUtil;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(this).get(RegisterViewModel.class);

        // Adapters for exposed dropdowns
        ArrayAdapter<String> sexAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.sex_options)
        );
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.role_options)
        );

        binding.sexDropdown.setAdapter(sexAdapter);
        binding.roleDropdown.setAdapter(roleAdapter);

        binding.btnCreateAccount.setOnClickListener(v -> submit());

        // Back to login link
        binding.signInLink.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        // Observers
        vm.loading.observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = loading != null && loading;
            setLoadingOverlay(isLoading);

            binding.inputEmail.setEnabled(!isLoading);
            binding.inputPassword.setEnabled(!isLoading);
            binding.inputFirstName.setEnabled(!isLoading);
            binding.inputLastName.setEnabled(!isLoading);
            binding.sexDropdown.setEnabled(!isLoading);
            binding.roleDropdown.setEnabled(!isLoading);
            binding.btnCreateAccount.setEnabled(!isLoading);
            binding.signInLink.setEnabled(!isLoading);
        });

        vm.message.observe(getViewLifecycleOwner(), msg -> {
            if (!TextUtils.isEmpty(msg)) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        vm.registered.observe(getViewLifecycleOwner(), ok -> {
            if (ok != null && ok) {
                Toast.makeText(requireContext(), getString(R.string.msg_account_created), Toast.LENGTH_SHORT).show();
                // Go back to login screen
                NavHostFragment.findNavController(this).popBackStack();
            }
        });
    }

    private void submit() {
        clearFieldErrors();

        String email     = binding.inputEmail.getText() != null ? binding.inputEmail.getText().toString().trim() : "";
        String password  = binding.inputPassword.getText() != null ? binding.inputPassword.getText().toString() : "";
        String firstName = binding.inputFirstName.getText() != null ? binding.inputFirstName.getText().toString().trim() : "";
        String lastName  = binding.inputLastName.getText() != null ? binding.inputLastName.getText().toString().trim() : "";

        String sex       = binding.sexDropdown.getText() != null ? binding.sexDropdown.getText().toString().trim() : "";
        String roleStr   = binding.roleDropdown.getText() != null ? binding.roleDropdown.getText().toString().trim() : "";

        boolean valid = true;

        if (TextUtils.isEmpty(firstName)) {
            binding.inputLayoutFirstName.setError(getString(R.string.error_required));
            valid = false;
        }

        if (TextUtils.isEmpty(lastName)) {
            binding.inputLayoutLastName.setError(getString(R.string.error_required));
            valid = false;
        }

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

        if (TextUtils.isEmpty(roleStr)) {
            binding.inputLayoutRole.setError(getString(R.string.error_required));
            valid = false;
        }

        if (TextUtils.isEmpty(sex)) {
            binding.inputLayoutSex.setError(getString(R.string.error_required));
            valid = false;
        }

        if (!valid) {
            return;
        }

        if (!NetworkUtil.isOnline(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.offline_message), Toast.LENGTH_SHORT).show();
            return;
        }

        Role role = Role.PATIENT;
        if ("DOCTOR".equalsIgnoreCase(roleStr) || getString(R.string.role_doctor).equalsIgnoreCase(roleStr)) {
            role = Role.DOCTOR;
        }

        User u = new User();
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setSex(sex);
        u.setRole(role);

        vm.register(email, password, u);
    }

    private void clearFieldErrors() {
        binding.inputLayoutFirstName.setError(null);
        binding.inputLayoutLastName.setError(null);
        binding.inputLayoutEmail.setError(null);
        binding.inputLayoutPassword.setError(null);
        binding.inputLayoutRole.setError(null);
        binding.inputLayoutSex.setError(null);
    }

    private void setLoadingOverlay(boolean show) {
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
