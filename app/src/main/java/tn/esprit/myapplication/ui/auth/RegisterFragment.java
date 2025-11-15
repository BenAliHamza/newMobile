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

        // Dropdown adapters
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

        // Actions
        binding.registerButton.setOnClickListener(v -> submit());
        binding.signInLink.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        // Observers
        vm.loading.observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = loading != null && loading;
            setLoading(isLoading);
        });

        vm.message.observe(getViewLifecycleOwner(), msg -> {
            if (!TextUtils.isEmpty(msg)) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        vm.registered.observe(getViewLifecycleOwner(), ok -> {
            if (ok != null && ok) {
                Toast.makeText(requireContext(), getString(R.string.msg_account_created), Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            }
        });
    }

    private void submit() {
        if (binding == null) return;

        String firstName   = textOf(binding.firstNameEdit);
        String lastName    = textOf(binding.lastNameEdit);
        String email       = textOf(binding.emailEdit);
        String phone       = textOf(binding.phoneEdit); // optional, not yet persisted
        String password    = textOf(binding.passwordEdit);
        String confirmPass = textOf(binding.confirmPasswordEdit);
        String sexText     = textOf(binding.sexDropdown);
        String roleText    = textOf(binding.roleDropdown);

        boolean hasError = false;

        // First name (required)
        if (TextUtils.isEmpty(firstName)) {
            binding.firstNameLayout.setError(getString(R.string.error_required));
            hasError = true;
        } else {
            binding.firstNameLayout.setError(null);
        }

        // Last name (required)
        if (TextUtils.isEmpty(lastName)) {
            binding.lastNameLayout.setError(getString(R.string.error_required));
            hasError = true;
        } else {
            binding.lastNameLayout.setError(null);
        }

        // Email (required)
        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError(getString(R.string.error_email_required));
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(getString(R.string.error_invalid_email));
            hasError = true;
        } else {
            binding.emailLayout.setError(null);
        }

        // Password
        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.setError(getString(R.string.error_required));
            hasError = true;
        } else if (password.length() < 6) {
            binding.passwordLayout.setError(getString(R.string.error_password_min));
            hasError = true;
        } else {
            binding.passwordLayout.setError(null);
        }

        // Confirm password
        if (TextUtils.isEmpty(confirmPass)) {
            binding.confirmPasswordLayout.setError(getString(R.string.error_required));
            hasError = true;
        } else if (!password.equals(confirmPass)) {
            binding.confirmPasswordLayout.setError(getString(R.string.error_password_mismatch));
            hasError = true;
        } else {
            binding.confirmPasswordLayout.setError(null);
        }

        // Role (required)
        if (TextUtils.isEmpty(roleText)) {
            binding.roleLayout.setError(getString(R.string.prompt_select_role));
            hasError = true;
        } else {
            binding.roleLayout.setError(null);
        }

        // Sex (required for now)
        if (TextUtils.isEmpty(sexText)) {
            binding.sexLayout.setError(getString(R.string.prompt_select_sex));
            hasError = true;
        } else {
            binding.sexLayout.setError(null);
        }

        if (hasError) {
            return;
        }

        Role role = Role.fromString(roleText);

        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setSex(sexText);
        u.setRole(role);
        u.setIsFirstLogin(true);
        u.setImageUrl(null); // not set yet, kept explicit
        u.setEmail(email);   // will be normalized inside ViewModel if needed

        vm.register(email, password, u);
    }

    private void setLoading(boolean isLoading) {
        if (binding == null) return;

        binding.loadingOverlay.getRoot().setVisibility(isLoading ? View.VISIBLE : View.GONE);

        binding.firstNameLayout.setEnabled(!isLoading);
        binding.lastNameLayout.setEnabled(!isLoading);
        binding.emailLayout.setEnabled(!isLoading);
        binding.phoneLayout.setEnabled(!isLoading);
        binding.passwordLayout.setEnabled(!isLoading);
        binding.confirmPasswordLayout.setEnabled(!isLoading);
        binding.roleLayout.setEnabled(!isLoading);
        binding.sexLayout.setEnabled(!isLoading);
        binding.registerButton.setEnabled(!isLoading);
        binding.signInLink.setEnabled(!isLoading);
    }

    private String textOf(@Nullable android.widget.TextView tv) {
        return tv != null && tv.getText() != null
                ? tv.getText().toString().trim()
                : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
