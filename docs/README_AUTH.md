# Authentication Flow — ReadyDocs (Android)

This document explains how the authentication flow works in the ReadyDocs Android app: login, registration, password reset, session handling, and navigation between auth and home screens.

---

## 1. Overview

The app uses **Firebase Authentication** (email/password + Google Sign-In) and **Cloud Firestore** to:

- Create and authenticate users.
- Store basic profile data (`users` collection: one `User` document per account).
- Persist sessions locally so users stay signed in across app restarts.

Primary auth features:

- Login with email + password.
- Login with Google (if `default_web_client_id` is configured).
- Registration with profile info (full name → split into first/last, sex, role, optional phone).
- Forgot password (reset email via Firebase).
- Auto-login if a user is already authenticated.
- Centralized navigation between auth and home, and safe sign-out.
- Ensuring **Google sign-in users also have a Firestore `users` document** with an explicit role.

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
    - `LoginViewModel` — handles login logic (email/password + Google) and exposes:
        - `loading`, `message`, `signedIn`, and (for Google) internal flow to ensure a Firestore `User` doc exists.
    - `RegisterViewModel` — handles account creation + profile write, exposes:
        - `loading`, `message`, `registered`.
    - `ForgotPasswordViewModel` — handles sending password reset emails, exposes:
        - `loading`, `message`, `sent`/`done`.

- **Data / Core**
    - `AuthRepository` — wraps Firebase Auth and Firestore calls:
        - sign in (email/password, Google credential),
        - registration (email/password),
        - password reset,
        - user profile read/write.
    - `AuthErrorMapper` — converts Firebase exceptions into user-friendly error messages.
    - `User` & `Role` — Firestore user model and enum for roles.
    - `FirebaseManager` — helper for Firebase Auth/Firestore/Storage access (no static Context leaks).

- **Navigation / Utilities**
    - `AuthUiNavigator` — central place to:
        - Enforce auth for protected screens.
        - Navigate to home with a cleared back stack.
        - Sign out and return to auth.
    - `NetworkUtil` — checks if the device is online.

This keeps Fragments thin, with ViewModels and the Repository handling most of the logic.

---

### 2.2 User Model & Role

**Collection:** `users`  
**Document id:** Firebase Auth `uid`.

The `User` model is stored in Firestore and used for both **email/password** users and **Google sign-in** users.  
Fields (simplified):

- `email` — user email (string).
- `firstName` — first name (string).
- `lastName` — last name (string).
- `sex` — e.g. "Male", "Female", "Other" (string).
- `role` — enum `Role`:
    - `PATIENT`
    - `DOCTOR`
- `isFirstLogin` — boolean flag used for future onboarding.
- `imageUrl` — optional profile photo URL (currently set `null` on registration, may be used later).

**Important:**

- For **registration**, the user **selects the role** in the form.
- For **Google sign-in**, if a Firestore `User` document does not exist yet, the app forces the user to pick a role **once** via a dialog, and then creates the `users/{uid}` document.

This role is what tells you whether a given user is a doctor or a patient.

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
    - The fragment observes `signedIn` and, if `true` and the current Firebase user is not null, calls `AuthUiNavigator.goToHomeAndClearTask(...)`.
7. On failure:
    - `AuthErrorMapper` converts the Firebase exception into a human-readable message.
    - The message is exposed through `message` LiveData and displayed as a toast.
    - `loading` is reset to `false`.

---

### 3.3 Login (Google Sign-In) **(+ Firestore user + role prompt)**

**UI:** same `LoginFragment`, with a “Continue with Google” button and a Google icon.

High-level behavior:

1. On view creation, `LoginFragment` attempts to read `default_web_client_id` from resources.
2. If the client ID is valid (non-empty and not a placeholder):
    - A `GoogleSignInClient` is built with email + ID token requests.
3. On Google button click:
    - If offline → a toast shows an offline message and returns.
    - If configuration is invalid or `default_web_client_id` is missing → a toast explains that SHA-1 / config is missing and returns (instead of crashing).
    - Otherwise, the fragment starts the Google sign-in intent using an `ActivityResultLauncher`.
4. When the result returns:
    - The fragment retrieves the `GoogleSignInAccount` and its ID token.
    - If the account or token is null, an explanatory toast is shown.
    - If token is present, it creates a Firebase `AuthCredential` and calls `LoginViewModel.signInWithCredential(credential)`.
5. The `LoginViewModel`:
    - Signs in via `AuthRepository.signInWithCredential(...)`.
    - Then checks Firestore `users/{uid}`:
        - **If the document exists:** we assume the user already has profile + role; `signedIn` is set to true and we navigate to `HomeActivity` as usual.
        - **If the document does NOT exist (first Google login):**
            1. The UI shows a **blocking dialog** asking the user to choose a role: **Doctor** or **Patient**.
            2. Using the selected role and the Google account data, the app builds a minimal `User` profile:
                - `firstName` / `lastName` are derived from `displayName` (fallbacks applied if needed).
                - `email` is taken from the Google account.
                - `role` is the user’s choice (DOCTOR or PATIENT).
                - `sex` is left unspecified for now.
                - `isFirstLogin` is set to `true`.
                - `imageUrl` can later be tied to Google photo if needed.
            3. This profile is written to Firestore as `users/{uid}`.
            4. On success, the app proceeds to `HomeActivity`.

**Result:**

- Every **Google account** that signs in will:
    - Have a corresponding **Firestore `User` document**.
    - Have an explicit **role** (doctor or patient), chosen by the user the first time.
- The role prompt is shown only once per account; afterwards, the existing document is reused.

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
        - `firstName`, `lastName`, `sex`, `role`.
        - `isFirstLogin` set to `true`.
        - `imageUrl` set to `null` (not used yet).
        - `email` set to the entered email.
    - `RegisterViewModel.register(email, password, user)` is called.
    - `loading` LiveData shows the full-screen loading overlay and disables all inputs.
5. The ViewModel:
    - Uses `AuthRepository.registerWithEmail(...)` to create the Firebase Auth user.
    - On success, it retrieves the actual `FirebaseUser`, ensures the profile email is aligned with `FirebaseUser.getEmail()` (source of truth), and writes the user profile to Firestore via `AuthRepository.createUserProfile(...)`.
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
    - The app intentionally relies on the **default Firebase behavior**:
        - Even if the email does **not** correspond to an existing account, Firebase still returns success (to avoid leaking whether an address is registered).
8. On success:
    - `sent` (or `done`) LiveData is set to `true`.
    - The fragment shows a success toast using `msg_reset_email_sent` and typically navigates back to the previous screen (usually login).
9. On failure (network issues, misconfiguration, etc.):
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
- Invalid credentials.
- Weak passwords.
- Email already in use.
- Invalid email.
- User not found.
- Generic/unknown failures → safe generic message.

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
    - Verify in Firebase:
        - New user appears in **Authentication** tab.
        - Corresponding `users/{uid}` document exists in Firestore with correct role and basic profile.

3. **Registration errors**
    - Try registering with:
        - Missing fields.
        - Invalid email.
        - Short password.
        - Mismatched passwords.
        - Already-used email → should show a mapped error message.

4. **Login (email/password)**
    - Wrong email → should show appropriate error from Firebase.
    - Wrong password → should show appropriate error.
    - Correct credentials → should navigate to `HomeActivity`.

5. **Login (Google)**
    - With correct configuration (`default_web_client_id` and SHA-1):
        - Tap “Continue with Google”.
        - On first ever login with that Google account:
            - Expect a role selection dialog (Doctor / Patient).
            - After choosing, a new `users/{uid}` document is created in Firestore with that role and profile fields.
            - User is then navigated to `HomeActivity`.
        - On subsequent logins:
            - No role dialog (existing profile is reused).
            - Direct navigation to `HomeActivity`.
    - If configuration is missing (no SHA-1, wrong web client ID):
        - Tap the Google button:
            - Expect a clear toast explaining configuration is incomplete (no crash).

6. **Auto-login**
    - After logging in (email or Google), kill and relaunch the app.
    - Expect to go directly to `HomeActivity` if the session is still valid.

7. **Forgot password**
    - Use a registered email:
        - Expect a success message (`msg_reset_email_sent`).
        - Confirm reset email is sent via your email or Firebase console.
    - Use an unregistered email:
        - By design, Firebase still reports success (to avoid leaking whether accounts exist).
        - The app still shows the generic success message.

8. **Sign-out**
    - From `HomeActivity`, use the menu action to sign out.
    - Expect to return to auth (`AuthHostActivity`) and not be able to navigate back into the home screen.

---

## 8. Possible Future Enhancements / Easy Wins (Not Implemented Yet)

The following items are **not implemented yet** but are good candidates for quick future improvements:

- **Real-time input validation as the user types**  
  (e.g. clear `TextInputLayout` errors as the user edits email/password/role/sex fields).

- **Prefill forgot-password email from login**
    - When the user taps “Forgot password?” on the login screen, pass the current email field as an argument to `ForgotPasswordFragment` and prefill it there.

- **Email normalization everywhere**
    - Normalize all emails to `trim().lowercase(Locale.ROOT)`:
        - Before calling `signInWithEmail`, `registerWithEmail`, and `sendPasswordReset`.
        - When storing `User.email` in Firestore.
    - This avoids duplicates like `Test@Gmail.com` vs `test@gmail.com`.

- **Light logging / metrics around auth flows**
    - Log how long registration and Google sign-in flows take end-to-end.
    - Makes it easier to debug slow network or rule issues.

- **More advanced error UX**
    - Surface common errors directly under fields (e.g. “Password too short”) in addition to toasts.

- **Richer profile data from Google**
    - Optionally pull photo URL from Google and store it in `imageUrl`.
    - Allow user to edit their profile (sex, phone, etc.) after first login.

For the current university project scope, the existing implementation is intentionally kept clear and straightforward while following good practices (MVVM, repository pattern, centralized navigation, and separation of concerns). The items above are **nice-to-have** refinements that can be added if there is extra time.
