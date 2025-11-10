package tn.esprit.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.core.FirebaseManager;

public class SuivieFragment extends Fragment {

    private RecyclerView recycler;
    private View emptyView;
    private VisitsAdapter adapter;

    public SuivieFragment() { /* required */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_suivie, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener("visit_added", this, (reqKey, bundle) -> loadVisits());
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        recycler = root.findViewById(R.id.recyclerVisits);
        emptyView = root.findViewById(R.id.emptyView);

        adapter = new VisitsAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        recycler.setAdapter(adapter);

        FloatingActionButton fab = root.findViewById(R.id.fabAddVisit);
        fab.setOnClickListener(v -> AddVisitDialogFragment.newInstance()
                .show(getParentFragmentManager(), "add_visit"));

        loadVisits();
    }

    private void loadVisits() {
        FirebaseUser user = FirebaseManager.auth().getCurrentUser();
        if (user == null) {
            showEmpty(true);
            return;
        }
        CollectionReference col = FirebaseManager.db().collection("visits");
        Query q = col.whereEqualTo("uid", user.getUid()).orderBy("createdAt", Query.Direction.DESCENDING);
        q.get().addOnSuccessListener(snaps -> {
            List<VisitItem> items = new ArrayList<>();
            for (QueryDocumentSnapshot d : snaps) {
                VisitItem it = new VisitItem();
                it.title = d.getString("title");
                it.doctorName = d.getString("doctorName");
                it.specialty = d.getString("specialty");
                it.conclusion = d.getString("conclusion");
                items.add(it);
            }
            adapter.setData(items);
            showEmpty(items.isEmpty());
        }).addOnFailureListener(e -> showEmpty(true));
    }

    private void showEmpty(boolean empty) {
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ---- Recycler ----
    static class VisitItem {
        String title;
        String doctorName;
        String specialty;
        String conclusion;
    }

    static class VisitsAdapter extends RecyclerView.Adapter<VisitVH> {
        private final List<VisitItem> data = new ArrayList<>();
        void setData(List<VisitItem> items) {
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }
        @NonNull @Override public VisitVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_visit, parent, false);
            return new VisitVH(v);
        }
        @Override public void onBindViewHolder(@NonNull VisitVH holder, int position) {
            holder.bind(data.get(position));
        }
        @Override public int getItemCount() { return data.size(); }
    }

    static class VisitVH extends RecyclerView.ViewHolder {
        private final com.google.android.material.textview.MaterialTextView tvTitle;
        private final com.google.android.material.textview.MaterialTextView tvSub;

        VisitVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvVisitTitle);
            tvSub   = itemView.findViewById(R.id.tvVisitSub);
        }

        void bind(VisitItem it) {
            tvTitle.setText(it.title == null ? "Visit" : it.title);
            String sub = (it.doctorName == null ? "-" : it.doctorName)
                    + (it.specialty == null ? "" : " · " + it.specialty)
                    + (it.conclusion == null ? "" : "\n" + it.conclusion);
            tvSub.setText(sub);
        }
    }
}
