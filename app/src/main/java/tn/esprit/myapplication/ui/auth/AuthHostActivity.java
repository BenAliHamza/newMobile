package tn.esprit.myapplication.ui.auth;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.core.FirebaseManager;

/**
 * Host activity for the auth NavGraph.
 * If a user is already logged in, we skip auth and go directly to Home.
 */
public class AuthHostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure Firebase is initialized
        FirebaseManager.init(this);
        FirebaseUser current = FirebaseManager.auth().getCurrentUser();

        if (current != null) {
            // User already authenticated -> go directly to Home
            AuthUiNavigator.goToHomeAndClearTask(this);
            return;
        }

        // No user -> show auth flow (NavHost in this layout)
        setContentView(R.layout.activity_auth_host);
    }
}
