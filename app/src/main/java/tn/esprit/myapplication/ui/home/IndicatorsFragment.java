package tn.esprit.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultCaller;
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

public class IndicatorsFragment extends Fragment {

    private RecyclerView recycler;
    private View emptyView;
    private IndicatorsAdapter adapter;

    public IndicatorsFragment() { /* required */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_indicators, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // refresh after dialog saves
        getParentFragmentManager().setFragmentResultListener("indicator_added", this, (reqKey, bundle) -> loadIndicators());
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        recycler = root.findViewById(R.id.recyclerIndicators);
        emptyView = root.findViewById(R.id.emptyView);

        adapter = new IndicatorsAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        recycler.setAdapter(adapter);

        FloatingActionButton fab = root.findViewById(R.id.fabAddIndicator);
        fab.setOnClickListener(v -> AddIndicatorDialogFragment.newInstance()
                .show(getParentFragmentManager(), "add_indicator"));

        loadIndicators();
    }

    private void loadIndicators() {
        FirebaseUser user = FirebaseManager.auth().getCurrentUser();
        if (user == null) {
            showEmpty(true);
            return;
        }
        CollectionReference col = FirebaseManager.db().collection("indicators");
        Query q = col.whereEqualTo("uid", user.getUid()).orderBy("createdAt", Query.Direction.DESCENDING);
        q.get().addOnSuccessListener(snaps -> {
            List<IndicatorItem> items = new ArrayList<>();
            for (QueryDocumentSnapshot d : snaps) {
                IndicatorItem it = new IndicatorItem();
                it.type = d.getString("type");
                it.value = d.getString("value");
                it.unit = d.getString("unit");
                items.add(it);
            }
            adapter.setData(items);
            showEmpty(items.isEmpty());
        }).addOnFailureListener(e -> showEmpty(true));
    }

    private void showEmpty(boolean empty) {
        if (emptyView != null) emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recycler != null) recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // -------- Recycler bits --------

    static class IndicatorItem {
        String type;
        String value;
        String unit;
    }

    static class IndicatorsAdapter extends RecyclerView.Adapter<IndicatorsVH> {
        private final List<IndicatorItem> data = new ArrayList<>();
        void setData(List<IndicatorItem> items) {
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }
        @NonNull @Override public IndicatorsVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_indicator, parent, false);
            return new IndicatorsVH(v);
        }
        @Override public void onBindViewHolder(@NonNull IndicatorsVH holder, int position) {
            holder.bind(data.get(position));
        }
        @Override public int getItemCount() { return data.size(); }
    }

    static class IndicatorsVH extends RecyclerView.ViewHolder {
        private final com.google.android.material.textview.MaterialTextView tvTitle;
        private final com.google.android.material.textview.MaterialTextView tvSub;

        IndicatorsVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvIndicatorTitle);
            tvSub   = itemView.findViewById(R.id.tvIndicatorSub);
        }

        void bind(IndicatorItem it) {
            String t = it.type == null ? "Indicator" : it.type;
            tvTitle.setText(t);
            String sub = (it.value == null ? "-" : it.value) + (it.unit == null ? "" : " " + it.unit);
            tvSub.setText(sub);
        }
    }
}
