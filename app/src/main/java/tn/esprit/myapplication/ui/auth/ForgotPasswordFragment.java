package tn.esprit.myapplication.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.databinding.FragmentForgotPasswordBinding;
import tn.esprit.myapplication.util.NetworkUtil;

public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private ForgotPasswordViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);

        // Observers
        vm.loading.observe(getViewLifecycleOwner(), this::setLoading);
        vm.message.observe(getViewLifecycleOwner(), msg -> {
            if (!TextUtils.isEmpty(msg)) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
        vm.resetSent.observe(getViewLifecycleOwner(), sent -> {
            if (sent != null && sent) {
                // After a successful reset request, go back to Login
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

        // Actions
        binding.btnSend.setOnClickListener(v -> attemptReset());
        binding.btnBackToLogin.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());
    }

    private void attemptReset() {
        if (binding == null) return;

        String email = String.valueOf(binding.inputEmail.getText()).trim();

        boolean hasError = false;

        if (TextUtils.isEmpty(email)) {
            binding.inputLayoutEmail.setError(getString(R.string.error_email_required));
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEmail.setError(getString(R.string.error_invalid_email));
            hasError = true;
        } else {
            binding.inputLayoutEmail.setError(null);
        }

        if (hasError) return;

        if (!NetworkUtil.isOnline(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.offline_message), Toast.LENGTH_SHORT).show();
            return;
        }

        vm.requestPasswordReset(email);
    }

    private void setLoading(boolean show) {
        if (binding == null) return;
        binding.loadingOverlay.getRoot().setVisibility(show ? View.VISIBLE : View.GONE);
        binding.inputEmail.setEnabled(!show);
        binding.btnSend.setEnabled(!show);
        binding.btnBackToLogin.setEnabled(!show);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
