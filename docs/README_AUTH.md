# Authentication Flow — ReadyDocs (Android)

This document explains how the authentication flow works in the ReadyDocs Android app: login, registration, password reset, session handling, and navigation between auth and home screens.

---

## 1. Overview

The app uses **Firebase Authentication** (email/password + Google Sign-In) and **Cloud Firestore** to:

- Create and authenticate users.
- Store basic profile data (`User` document per account).
- Persist sessions locally so users stay signed in across app restarts.

Primary auth features:

- Login with email + password.
- Login with Google (if `default_web_client_id` is configured).
- Registration with profile info (name, sex, role, optional phone).
- Forgot password (reset email).
- Auto-login if a user is already authenticated.
- Centralized navigation between auth and home, and safe sign-out.

There is **no email verification requirement** implemented right now.

---

## 2. Architecture & Responsibilities

### 2.1 Layers

- **UI (Activities / Fragments)**
    - `AuthHostActivity` — hosts the auth navigation graph.
    - `LoginFragment` — email/password + Google login screen.
    - `RegisterFragment` — registration form screen.
    - `ForgotPasswordFragment` — password reset screen.
    - `HomeActivity` — main screen after successful auth.
- **ViewModels**
    - `LoginViewModel` — handles login logic and exposes `loading`, `message`, `signedIn`.
    - `RegisterViewModel` — handles account creation + profile write, exposes `loading`, `message`, `registered`.
    - `ForgotPasswordViewModel` — handles sending password reset emails, exposes `loading`, `message`, `sent`.
- **Data / Core**
    - `AuthRepository` — wraps Firebase Auth and Firestore calls (sign in, register, reset, sign out, profile write).
    - `AuthErrorMapper` — converts Firebase exceptions into user-friendly error messages.
    - `User` & `Role` — Firestore user model and enum for roles.
    - `FirebaseManager` — helper for Firebase Auth/Firestore/Storage access.
- **Navigation / Utilities**
    - `AuthUiNavigator` — central place to:
        - Enforce auth for protected screens.
        - Navigate to home with a cleared back stack.
        - Sign out and return to auth.
    - `NetworkUtil` — checks if the device is online.

This keeps Fragments thin, with ViewModels and Repository handling most of the logic.

---

## 3. User Flows

### 3.1 App launch / Auto-login

1. App starts in `AuthHostActivity` (declared as `MAIN`/`LAUNCHER` in the manifest).
2. `AuthHostActivity` checks Firebase for an already signed-in user.
3. If a user exists:
    - It immediately navigates to `HomeActivity` using `AuthUiNavigator.goToHomeAndClearTask(...)`.
    - The auth screens are skipped.
4. If no user exists:
    - `AuthHostActivity` inflates `activity_auth_host.xml` which hosts the `nav_auth` navigation graph.
    - The user sees the `LoginFragment` as the start destination.

This effectively gives a “remember me / auto-login” behavior with Firebase’s built-in session persistence.

---

### 3.2 Login (Email + Password)

**UI:** `LoginFragment` + `fragment_login.xml`

Steps:

1. User enters email and password.
2. The fragment validates:
    - Email is not empty and matches a standard email pattern.
    - Password is not empty and has at least 6 characters.
3. If validation fails:
    - Field-specific error messages are displayed via `TextInputLayout` errors.
4. If validation passes:
    - A network check is performed via `NetworkUtil.isOnline(...)`.
    - If offline, a toast informs the user and the call is aborted.
5. If online:
    - `LoginViewModel.signInWithEmail(email, password)` is called.
    - ViewModel triggers `AuthRepository.signInWithEmail(...)`.
    - `loading` LiveData switches to `true`, showing a full-screen loading overlay and disabling inputs.
6. On success:
    - `LoginViewModel` sets `signedIn` to `true`.
    - The fragment observes `signedIn` and, if true and the current Firebase user is not null, calls `AuthUiNavigator.goToHomeAndClearTask(...)`.
7. On failure:
    - `AuthErrorMapper` converts the Firebase exception into a human-readable message.
    - The message is exposed through `message` LiveData and displayed as a toast.
    - `loading` is reset to `false`.

---

### 3.3 Login (Google Sign-In)

**UI:** same `LoginFragment`, with a “Continue with Google” button and a Google icon.

High-level behavior:

1. On view creation, `LoginFragment` attempts to read `default_web_client_id` from resources.
2. If the client ID is valid (non-empty and not a placeholder):
    - A `GoogleSignInClient` is built with email + ID token requests.
    - A flag marks Google Sign-In as correctly configured.
3. On Google button click:
    - If offline → a toast shows an offline message and returns.
    - If configuration is invalid or client is null → a toast explains that SHA-1 / config is missing and returns.
    - Otherwise, the fragment starts the Google sign-in intent using an `ActivityResultLauncher`.
4. When the result returns:
    - The fragment retrieves the `GoogleSignInAccount` and its ID token.
    - If token is present, it creates a Firebase `AuthCredential` and calls `LoginViewModel.signInWithCredential(credential)`.
    - The ViewModel then signs in via `AuthRepository.signInWithCredential(...)`, using the same `loading` and `message` patterns as email/password.

If correctly configured, users can sign in with their Google account; if configuration is incomplete, they get clear toasts instead of a crash.

---

### 3.4 Registration

**UI:** `RegisterFragment` + `fragment_register.xml`

The registration screen collects:

- Full name (later split into first/last).
- Email.
- Optional phone number.
- Password and confirm password.
- Sex (dropdown, values from `sex_options` array).
- Role (dropdown, values from `role_options` array, mapped to `Role` enum).

Steps:

1. User fills in the form and taps the register button.
2. Fragment validations:
    - Full name is required.
    - Email is required and must be valid.
    - Password is required and at least 6 characters.
    - Confirm password is required and must match password.
    - Sex and role must be selected from dropdowns.
3. If any validation fails:
    - The relevant `TextInputLayout` shows an error message.
4. If all fields are valid:
    - The full name is split into first and last name (simple split on first space).
    - A `User` object is created with:
        - First name, last name, sex, role.
        - `isFirstLogin` set to `true`.
        - `imageUrl` set to `null` (not used yet).
        - `email` set to the entered email.
    - `RegisterViewModel.register(email, password, user)` is called.
    - `loading` LiveData shows the full-screen loading overlay and disables all inputs.
5. The ViewModel:
    - Uses `AuthRepository.registerWithEmail(...)` to create the Firebase Auth user.
    - On success, it retrieves the actual `FirebaseUser`, normalizes the profile’s email to match `FirebaseUser.getEmail()`, and writes the user profile to Firestore via `AuthRepository.createUserProfile(...)`.
6. On final success:
    - `registered` LiveData is set to `true`.
    - The fragment observes this, shows a toast using `msg_account_created`, and navigates back (typically to the login screen).
7. On failure:
    - `AuthErrorMapper` produces an error message for the cause (e.g., email already in use, weak password).
    - `message` LiveData delivers it to the fragment, which shows it in a toast.
    - `loading` is reset to `false`.

---

### 3.5 Forgot Password

**UI:** `ForgotPasswordFragment` + `fragment_forgot_password.xml`

Steps:

1. User enters their email and taps the “Send reset email” button.
2. Fragment validates:
    - Email not empty.
    - Email format is valid.
3. If validation fails:
    - The email input layout shows an error.
4. If validation passes:
    - A network check (`NetworkUtil.isOnline(...)`) ensures the device is online.
5. If offline:
    - A toast informs the user and the request is cancelled.
6. If online:
    - `ForgotPasswordViewModel.sendReset(email)` is called.
    - `loading` LiveData shows the full-screen overlay, and all controls are disabled.
7. The ViewModel sends the reset email through `AuthRepository.sendPasswordReset(...)`.
8. On success:
    - `sent` LiveData is set to `true`.
    - The fragment shows a success toast using `msg_reset_email_sent` and navigates back to the previous screen (usually login).
9. On failure:
    - `AuthErrorMapper` generates a friendly error string.
    - The fragment displays it via toast.
    - `loading` is set back to `false`.

---

## 4. Session & Navigation Rules

### 4.1 Guarding the home screen

`HomeActivity` enforces that only authenticated users can access it:

- In `onStart()`, it calls `AuthUiNavigator.requireAuthOrFinish(this)`.
- If no current Firebase user exists:
    - The helper navigates to `AuthHostActivity` with flags that clear the back stack.
    - `HomeActivity` is finished, preventing the user from going “back” into home.

### 4.2 Global sign-out

From the top-right app bar menu in `HomeActivity`:

- The sign-out action triggers `AuthUiNavigator.performSignOutAndGoToAuth(...)`.
- This:
    - Signs out of Firebase Auth.
    - Starts `AuthHostActivity` with flags that clear the back stack.
    - Finishes the current activity when possible.

This ensures logging out always returns the user to the auth flow and prevents navigating back into `HomeActivity`.

### 4.3 Auth navigation graph

The auth screens are driven by `nav_auth.xml`:

- Start destination: `LoginFragment`.
- Actions:
    - From login → register.
    - From login → forgot password.
- Simple fade-in/fade-out animations provide smooth transitions.

---

## 5. Error Handling & Networking

### 5.1 Error mapping

`AuthErrorMapper` converts low-level Firebase exceptions into clear, user-facing messages:

- Network issues (e.g., `FirebaseNetworkException`) → “Network error, check your connection.”
- Invalid credentials, weak passwords, email already in use, invalid email, user not found, etc.
- Unknown or generic failures fall back to a safe generic message.

The ViewModels use this for all auth operations and expose messages via `message` LiveData.

### 5.2 Offline handling

Before any network-dependent operation (login, register, Google sign-in, forgot password):

- The UI layer calls `NetworkUtil.isOnline(context)`.
- If no network is available:
    - The operation is skipped.
    - A short toast explains that the user is offline.

---

## 6. Loading / UX & Accessibility

Each auth screen uses a **full-screen loading overlay** (`view_loading_overlay.xml`):

- Semi-transparent dimmed background.
- Centered circular progress indicator with a descriptive text (using `loading_message` string).
- Overlay is shown via `loadingOverlay.getRoot().setVisibility(...)` observed from ViewModel `loading` state.
- When visible:
    - Key inputs and buttons are disabled to prevent duplicate actions.
    - The overlay is clickable/focusable, blocking interactions with underlying views.

Layouts use:

- Material 3 components for consistent look and feel.
- Clear labels and hints for inputs.
- Adequate touch target sizes and spacing around buttons and fields.
- Simple, readable background gradients and panel drawables to keep contrast and readability high.

---

## 7. How to Test the Auth Flow

Suggested manual test cases:

1. **Fresh install / first launch**
    - Expect to see the login screen.
2. **Successful registration**
    - Fill all fields correctly and register.
    - Expect a success toast and navigation back to login.
3. **Registration errors**
    - Try registering with:
        - Missing fields.
        - Invalid email.
        - Short password.
        - Mismatched passwords.
        - Already-used email.
4. **Login (email/password)**
    - Wrong email → should show appropriate error from Firebase.
    - Wrong password → should show appropriate error.
    - Correct credentials → should navigate to `HomeActivity`.
5. **Auto-login**
    - After logging in, kill and relaunch the app.
    - Expect to go directly to `HomeActivity`.
6. **Forgot password**
    - Use a registered email → check that reset email is sent (using Firebase console).
    - Use an unregistered email → confirm the app handles errors gracefully.
7. **Sign-out**
    - From `HomeActivity`, use the menu action to sign out.
    - Expect to return to auth and not be able to navigate back into the home screen.

---

## 8. Possible Future Enhancements

Not implemented now, but easy to add later:

- Email verification (send verification on register, require verified email on login).
- Real-time input validation as the user types.
- Distinct error displays under each input instead of only toasts.
- More advanced session handling (e.g., explicit “remember me” toggle or multi-account support).

For the current university project scope, the existing implementation is intentionally kept clear and straightforward while following good practices (MVVM, repository pattern, centralized navigation, and separation of concerns).
