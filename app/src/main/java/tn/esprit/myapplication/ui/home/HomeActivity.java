package tn.esprit.myapplication.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.core.SeedData;
import tn.esprit.myapplication.ui.auth.AuthHostActivity;

public class HomeActivity extends AppCompatActivity {

    private FrameLayout container;
    private BottomNavigationView bottomNav;
    private MaterialToolbar toolbar;

    private final Fragment indicatorsFragment = new IndicatorsFragment();
    private final Fragment suivieFragment = new SuivieFragment();
    private final Fragment medicationFragment = new MedicationFragment();
    private final Fragment profileFragment = new ProfileFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // One-shot demo data seed (safe if already present)
        SeedData.run(getApplicationContext());

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        container = findViewById(R.id.home_container);
        bottomNav = findViewById(R.id.bottom_nav);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.home_container, indicatorsFragment, "indicators")
                    .commit();
            setTitle(getString(R.string.menu_indicators_title));
        }

        bottomNav.setOnItemSelectedListener(this::onBottomItemSelected);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Session guard: if user is null, go to Auth
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent i = new Intent(this, AuthHostActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }
    }

    private boolean onBottomItemSelected(@NonNull MenuItem item) {
        Fragment target = null;
        String title = null;

        int id = item.getItemId();
        if (id == R.id.menu_indicators) {
            target = indicatorsFragment; title = getString(R.string.menu_indicators_title);
        } else if (id == R.id.menu_suivie) {
            target = suivieFragment; title = getString(R.string.menu_suivie_title);
        } else if (id == R.id.menu_medication) {
            target = medicationFragment; title = getString(R.string.menu_medication_title);
        } else if (id == R.id.menu_profile) {
            target = profileFragment; title = getString(R.string.menu_profile_title);
        }

        if (target == null) return false;

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.home_container, target)
                .commit();

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home_top, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, AuthHostActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Lightweight inline placeholder for Profile tab
    public static class ProfileFragment extends Fragment {
        public ProfileFragment() { super(); }
        @Override public android.view.View onCreateView(@NonNull android.view.LayoutInflater inflater,
                                                        android.view.ViewGroup container,
                                                        Bundle savedInstanceState) {
            android.content.Context ctx = requireContext();
            android.widget.FrameLayout root = new android.widget.FrameLayout(ctx);
            root.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
            ));
            int pad = (int) (24 * ctx.getResources().getDisplayMetrics().density);
            root.setPadding(pad,pad,pad,pad);

            com.google.android.material.textview.MaterialTextView tv = new com.google.android.material.textview.MaterialTextView(ctx);
            tv.setText(getString(R.string.menu_profile_title));
            tv.setTextAppearance(ctx, com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall);
            root.addView(tv);
            return root;
        }
    }
}
