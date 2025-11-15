package tn.esprit.myapplication.core;

import android.app.Application;

import com.google.firebase.FirebaseApp;

/**
 * App
 * - Single place to initialize Firebase (no Activities/Fragments should do this).
 * - Keeps ProcessLifecycle clean and avoids static Context leaks elsewhere.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase once per process; safe to call repeatedly.
        try {
            FirebaseApp.getInstance();
        } catch (IllegalStateException e) {
            FirebaseApp.initializeApp(this);
        }
    }
}
