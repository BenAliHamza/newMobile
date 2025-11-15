package tn.esprit.myapplication.data.auth;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;


public final class AuthErrorMapper {

    private AuthErrorMapper() {
        // no-op
    }

    @NonNull
    public static String toMessage(@NonNull Throwable throwable) {
        // Network problems
        if (throwable instanceof FirebaseNetworkException) {
            return "Network error. Please check your connection and try again.";
        }

        // Invalid credentials (wrong password, malformed email, etc.)
        if (throwable instanceof FirebaseAuthInvalidCredentialsException) {
            return "Invalid email or password.";
        }

        // Unknown user / account disabled / etc.
        if (throwable instanceof FirebaseAuthInvalidUserException) {
            FirebaseAuthInvalidUserException ex = (FirebaseAuthInvalidUserException) throwable;
            String code = ex.getErrorCode();
            if ("ERROR_USER_DISABLED".equals(code)) {
                return "This account has been disabled.";
            }
            if ("ERROR_USER_NOT_FOUND".equals(code)) {
                return "No account found for this email.";
            }
            if ("ERROR_USER_TOKEN_EXPIRED".equals(code)) {
                return "Your session has expired. Please sign in again.";
            }
            return "We couldn't find an account for this email.";
        }

        // Other Firebase auth errors with known codes
        if (throwable instanceof FirebaseAuthException) {
            FirebaseAuthException ex = (FirebaseAuthException) throwable;
            String code = ex.getErrorCode();
            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code)) {
                return "An account already exists with this email.";
            }
            if ("ERROR_WEAK_PASSWORD".equals(code)) {
                return "The password is too weak. Please use at least 6 characters.";
            }
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return "Enter a valid email address.";
            }
            // Fallback for any other auth-specific error:
            return "Authentication failed: " + code;
        }

        // Generic fallback
        String msg = throwable.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return "Something went wrong. Please try again.";
        }
        return msg;
    }
}
