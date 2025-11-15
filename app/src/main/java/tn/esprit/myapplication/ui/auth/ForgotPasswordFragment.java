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
    private ForgotPasswordViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);
        viewModel.loading.observe(getViewLifecycleOwner(), this::setLoading);
        viewModel.message.observe(getViewLifecycleOwner(), msg -> {
            if (!TextUtils.isEmpty(msg)) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.sent.observe(getViewLifecycleOwner(), ok -> {
            if (ok != null && ok) {
                Toast.makeText(requireContext(), getString(R.string.msg_reset_email_sent), Toast.LENGTH_LONG).show();
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

        binding.btnSend.setOnClickListener(view -> sendReset());
        binding.btnBackToLogin.setOnClickListener(view ->
                NavHostFragment.findNavController(this).popBackStack());
    }

    private void sendReset() {
        if (binding == null) return;

        String email = String.valueOf(binding.inputEmail.getText()).trim();

        if (TextUtils.isEmpty(email)) {
            binding.inputLayoutEmail.setError(getString(R.string.error_email_required));
            return;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEmail.setError(getString(R.string.error_invalid_email));
            return;
        } else {
            binding.inputLayoutEmail.setError(null);
        }

        if (!NetworkUtil.isOnline(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.offline_message), Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.sendReset(email);
    }

    private void setLoading(boolean loading) {
        if (binding == null) return;
        binding.loadingOverlay.getRoot().setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSend.setEnabled(!loading);
        binding.btnBackToLogin.setEnabled(!loading);
        binding.inputEmail.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
