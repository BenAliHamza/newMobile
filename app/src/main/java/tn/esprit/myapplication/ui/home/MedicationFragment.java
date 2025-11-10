package tn.esprit.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import tn.esprit.myapplication.R;

public class MedicationFragment extends Fragment {

    public MedicationFragment() { /* required */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_medication, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        View fab = root.findViewById(R.id.fabAddMedication);
        fab.setOnClickListener(v ->
                com.google.android.material.snackbar.Snackbar
                        .make(root, "Add Medication (to be implemented)", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                        .show()
        );
    }
}
