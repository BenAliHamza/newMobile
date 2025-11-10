package tn.esprit.myapplication.ui.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.core.FirebaseManager;

public class AddVisitDialogFragment extends DialogFragment {

    private TextInputLayout tilTitle, tilDoctor, tilSpecialty, tilConclusion;
    private TextInputEditText etTitle, etDoctor, etSpecialty, etConclusion;
    private View btnCancel, btnSave, progress;

    public static AddVisitDialogFragment newInstance() {
        return new AddVisitDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        setStyle(DialogFragment.STYLE_NORMAL,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog);
        return inflater.inflate(R.layout.dialog_add_visit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        tilTitle = root.findViewById(R.id.tilTitle);
        tilDoctor = root.findViewById(R.id.tilDoctor);
        tilSpecialty = root.findViewById(R.id.tilSpecialty);
        tilConclusion = root.findViewById(R.id.tilConclusion);

        etTitle = root.findViewById(R.id.etTitle);
        etDoctor = root.findViewById(R.id.etDoctor);
        etSpecialty = root.findViewById(R.id.etSpecialty);
        etConclusion = root.findViewById(R.id.etConclusion);

        btnCancel = root.findViewById(R.id.btnCancel);
        btnSave = root.findViewById(R.id.btnSave);
        progress = root.findViewById(R.id.progress);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String title = String.valueOf(etTitle.getText()).trim();
        String doctor = String.valueOf(etDoctor.getText()).trim();
        String specialty = String.valueOf(etSpecialty.getText()).trim();
        String conclusion = String.valueOf(etConclusion.getText()).trim();

        boolean invalid = false;
        if (TextUtils.isEmpty(title)) { tilTitle.setError("Required"); invalid = true; } else tilTitle.setError(null);
        if (TextUtils.isEmpty(doctor)) { tilDoctor.setError("Required"); invalid = true; } else tilDoctor.setError(null);
        if (TextUtils.isEmpty(specialty)) { tilSpecialty.setError("Required"); invalid = true; } else tilSpecialty.setError(null);
        if (invalid) return;

        FirebaseUser user = FirebaseManager.auth().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not signed in.", Toast.LENGTH_SHORT).show();
            return;
        }

        toggleLoading(true);

        Map<String, Object> doc = new HashMap<>();
        doc.put("uid", user.getUid());
        doc.put("title", title);
        doc.put("doctorName", doctor);
        doc.put("specialty", specialty);
        doc.put("conclusion", conclusion);
        doc.put("createdAt", FieldValue.serverTimestamp());

        FirebaseManager.db().collection("visits")
                .add(doc)
                .addOnSuccessListener(r -> {
                    toggleLoading(false);
                    getParentFragmentManager().setFragmentResult("visit_added", new Bundle());
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    toggleLoading(false);
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void toggleLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        btnCancel.setEnabled(!show);
        etTitle.setEnabled(!show);
        etDoctor.setEnabled(!show);
        etSpecialty.setEnabled(!show);
        etConclusion.setEnabled(!show);
    }
}
