package tn.esprit.myapplication.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseUser;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.data.Role;
import tn.esprit.myapplication.data.User;
import tn.esprit.myapplication.databinding.FragmentRegisterBinding;
import tn.esprit.myapplication.ui.home.HomeActivity;
import tn.esprit.myapplication.util.NetworkUtil;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        FirebaseManager.init(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        ArrayAdapter<CharSequence> sexAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.sex_options, android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSex.setAdapter(sexAdapter);

        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.role_options, android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRole.setAdapter(roleAdapter);

        binding.btnCreate.setOnClickListener(view -> tryRegister());
        binding.btnBackToLogin.setOnClickListener(
                view -> NavHostFragment.findNavController(this).popBackStack());
    }

    private void tryRegister() {
        String firstName = String.valueOf(binding.inputFirstName.getText()).trim();
        String lastName  = String.valueOf(binding.inputLastName.getText()).trim();
        String email     = String.valueOf(binding.inputEmail.getText()).trim();
        String password  = String.valueOf(binding.inputPassword.getText());

        String sex       = binding.spinnerSex.getSelectedItem() != null ? binding.spinnerSex.getSelectedItem().toString() : "";
        String roleStr   = binding.spinnerRole.getSelectedItem() != null ? binding.spinnerRole.getSelectedItem().toString() : "";

        boolean invalid = false;
        if (TextUtils.isEmpty(firstName)) { binding.inputLayoutFirstName.setError(getString(R.string.error_required)); invalid = true; } else binding.inputLayoutFirstName.setError(null);
        if (TextUtils.isEmpty(lastName))  { binding.inputLayoutLastName.setError(getString(R.string.error_required)); invalid = true; } else binding.inputLayoutLastName.setError(null);
        if (TextUtils.isEmpty(email))     { binding.inputLayoutEmail.setError(getString(R.string.error_required)); invalid = true; } else binding.inputLayoutEmail.setError(null);
        if (TextUtils.isEmpty(password))  { binding.inputLayoutPassword.setError(getString(R.string.error_required)); invalid = true; } else binding.inputLayoutPassword.setError(null);
        if (TextUtils.isEmpty(sex))       { Toast.makeText(requireContext(), getString(R.string.prompt_select_sex), Toast.LENGTH_SHORT).show(); invalid = true; }
        if (TextUtils.isEmpty(roleStr))   { Toast.makeText(requireContext(), getString(R.string.prompt_select_role), Toast.LENGTH_SHORT).show(); invalid = true; }
        if (invalid) return;

        if (!NetworkUtil.isOnline(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.offline_message), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        FirebaseManager.auth()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser fu = result.getUser();
                    if (fu == null) {
                        setLoading(false);
                        Toast.makeText(requireContext(), getString(R.string.error_user_not_created), Toast.LENGTH_LONG).show();
                        return;
                    }
                    String uid = fu.getUid();

                    Role role = Role.fromString(roleStr);
                    User user = new User(
                            firstName,
                            lastName,
                            sex,
                            role,
                            true,
                            "",
                            email
                    );

                    FirebaseManager.users()
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(aVoid -> {
                                setLoading(false);
                                Toast.makeText(requireContext(), getString(R.string.msg_account_created), Toast.LENGTH_SHORT).show();
                                Intent i = new Intent(requireContext(), HomeActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(i);
                                requireActivity().finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        if (binding == null) return;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
