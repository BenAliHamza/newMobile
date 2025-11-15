# --- Firebase / GMS (safe minimal rules) ---

# Keep Firebase Auth internal models accessed by reflection
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep your Firestore POJOs used via reflection (User, Role)
-keepclassmembers class tn.esprit.myapplication.data.User {
    <init>();
    *;
}
-keep class tn.esprit.myapplication.data.Role { *; }

# (Optional) Reduce warnings for Firebase / Play Services
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# --- General best practice ---
# Keep class/method names for AndroidX navigation safe args (if used later)
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
