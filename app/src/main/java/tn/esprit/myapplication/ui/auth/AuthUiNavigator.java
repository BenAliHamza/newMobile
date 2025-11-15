package tn.esprit.myapplication.ui.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import tn.esprit.myapplication.ui.home.HomeActivity;

/**
 * AuthUiNavigator
 * - Centralizes navigation related to authentication:
 *   * Guarding screens that require a signed-in user.
 *   * Signing out and returning to the auth flow.
 *   * Going to Home with a cleared back stack.
 *
 * Keeps this logic out of Activities/Fragments to avoid duplication.
 */
public final class AuthUiNavigator {

    private AuthUiNavigator() {
        // no-op
    }

    /**
     * Ensures there is a signed-in user.
     *
     * @return true if user is signed in and the caller can continue,
     *         false if we redirected to Auth and finished the Activity.
     */
    public static boolean requireAuthOrFinish(@NonNull AppCompatActivity activity) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return true;
        }

        Intent intent = new Intent(activity, AuthHostActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return false;
    }

    /**
     * Signs out and sends the user back to the Auth flow,
     * clearing the back stack to prevent "back into Home" issues.
     */
    public static void performSignOutAndGoToAuth(@NonNull Context context) {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(context, AuthHostActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    /**
     * Optional helper: go to Home after successful login/registration,
     * with a clean back stack.
     */
    public static void goToHomeAndClearTask(@NonNull Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }
}
