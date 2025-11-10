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

/** Dialog to add an indicator (type, value, unit). */
public class AddIndicatorDialogFragment extends DialogFragment {

    private TextInputLayout tilType, tilValue, tilUnit;
    private TextInputEditText etType, etValue, etUnit;
    private View btnCancel, btnSave, progress;

    public static AddIndicatorDialogFragment newInstance() {
        return new AddIndicatorDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Use the Material library's theme overlay (correct constant from Material package)
        setStyle(DialogFragment.STYLE_NORMAL,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog);
        return inflater.inflate(R.layout.dialog_add_indicator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        tilType = root.findViewById(R.id.tilType);
        tilValue = root.findViewById(R.id.tilValue);
        tilUnit = root.findViewById(R.id.tilUnit);
        etType = root.findViewById(R.id.etType);
        etValue = root.findViewById(R.id.etValue);
        etUnit = root.findViewById(R.id.etUnit);
        btnCancel = root.findViewById(R.id.btnCancel);
        btnSave = root.findViewById(R.id.btnSave);
        progress = root.findViewById(R.id.progress);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String type = String.valueOf(etType.getText()).trim();
        String value = String.valueOf(etValue.getText()).trim();
        String unit = String.valueOf(etUnit.getText()).trim();

        boolean invalid = false;
        if (TextUtils.isEmpty(type)) { tilType.setError("Required"); invalid = true; } else tilType.setError(null);
        if (TextUtils.isEmpty(value)) { tilValue.setError("Required"); invalid = true; } else tilValue.setError(null);
        if (invalid) return;

        FirebaseUser user = FirebaseManager.auth().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not signed in.", Toast.LENGTH_SHORT).show();
            return;
        }

        toggleLoading(true);

        Map<String, Object> doc = new HashMap<>();
        doc.put("uid", user.getUid());
        doc.put("type", type);
        doc.put("value", value);
        doc.put("unit", unit);
        doc.put("createdAt", FieldValue.serverTimestamp());

        FirebaseManager.db().collection("indicators")
                .add(doc)
                .addOnSuccessListener(ref -> {
                    toggleLoading(false);
                    getParentFragmentManager().setFragmentResult("indicator_added", new Bundle());
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
        etType.setEnabled(!show);
        etValue.setEnabled(!show);
        etUnit.setEnabled(!show);
    }
}
