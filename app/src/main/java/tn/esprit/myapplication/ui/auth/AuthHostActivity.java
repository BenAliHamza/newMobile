package tn.esprit.myapplication.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.ui.home.HomeActivity;

public class AuthHostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_MyApplication);
        super.onCreate(savedInstanceState);

        // If already signed in AND email is verified, go straight to Home
        FirebaseUser current = FirebaseManager.auth().getCurrentUser();
        if (current != null) {
            if (current.isEmailVerified()) {
                Intent i = new Intent(this, HomeActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
                return;
            } else {
                // Do not keep unverified sessions around
                FirebaseManager.signOut();
            }
        }

        setContentView(R.layout.activity_auth_host);
    }
}
