package tn.esprit.myapplication.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.databinding.FragmentForgotPasswordBinding;
import tn.esprit.myapplication.util.NetworkUtil;

public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;

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

        binding.btnSend.setOnClickListener(view -> sendReset());
        binding.btnBackToLogin.setOnClickListener(view ->
                NavHostFragment.findNavController(this).popBackStack());
    }

    private void sendReset() {
        String email = String.valueOf(binding.inputEmail.getText()).trim();

        if (TextUtils.isEmpty(email)) {
            binding.inputLayoutEmail.setError(getString(R.string.error_email_required));
            return;
        } else {
            binding.inputLayoutEmail.setError(null);
        }

        if (!NetworkUtil.isOnline(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.offline_message), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), getString(R.string.msg_reset_email_sent), Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), getString(R.string.msg_reset_email_failed), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        if (binding == null) return;
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
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
