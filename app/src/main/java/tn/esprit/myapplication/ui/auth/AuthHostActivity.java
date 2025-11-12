package tn.esprit.myapplication.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.ui.home.HomeActivity;

/**
 * Hosts the auth fragments and guards session.
 * If a user session exists, forward to Home immediately.
 */
public class AuthHostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_MyApplication); // ensure theme applied before setContentView
        super.onCreate(savedInstanceState);

        // Session guard: if already logged in -> go Home
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent i = new Intent(this, HomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_auth_host);
        // NavHostFragment takes over; startDestination is LoginFragment
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If user signed in from another screen (rare), finish and route to Home
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent i = new Intent(this, HomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }
    }
}
