package tn.esprit.myapplication.ui.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;

import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.data.Role;
import tn.esprit.myapplication.data.User;
import tn.esprit.myapplication.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQ_PICK_IMAGE = 1001;

    private ActivityProfileBinding binding;
    private String imageUrl = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseManager.init(this);

        binding.btnChooseImage.setOnClickListener(v -> pickImage());
        binding.btnRefresh.setOnClickListener(v -> loadProfile());

        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser fu = FirebaseManager.auth().getCurrentUser();
        if (fu == null) {
            Toast.makeText(this, "Not signed in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setLoading(true);
        FirebaseManager.users().document(fu.getUid()).get()
                .addOnSuccessListener(snap -> {
                    setLoading(false);
                    User u = snap.toObject(User.class);
                    if (u == null) {
                        Toast.makeText(this, "No profile data.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    binding.tvName.setText(u.getFirstName() + " " + u.getLastName());
                    binding.tvSex.setText(u.getSex());
                    Role role = u.getRole();
                    binding.tvRole.setText(role != null ? role.name() : "");
                    imageUrl = u.getImageUrl() == null ? "" : u.getImageUrl();
                    if (imageUrl.isEmpty()) {
                        binding.avatar.setImageResource(android.R.drawable.sym_def_app_icon);
                    } else {
                        fetchAndShowImage(imageUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void fetchAndShowImage(String downloadUrl) {
        try {
            StorageReference ref = FirebaseManager.storage().getReferenceFromUrl(downloadUrl);
            ref.getBytes(1024 * 1024)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        binding.avatar.setImageBitmap(bmp);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show()
                    );
        } catch (Exception e) {
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(downloadUrl);
                    InputStream is = url.openStream();
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    runOnUiThread(() -> binding.avatar.setImageBitmap(bmp));
                } catch (Exception ignored) {
                }
            }).start();
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), REQ_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                uploadImage(uri);
            }
        }
    }

    private void uploadImage(Uri uri) {
        FirebaseUser fu = FirebaseManager.auth().getCurrentUser();
        if (fu == null) {
            Toast.makeText(this, "Not signed in.", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);

        String path = "profileImages/" + fu.getUid() + ".jpg";
        StorageReference ref = FirebaseManager.storage().getReference().child(path);

        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(this::saveImageUrl)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveImageUrl(Uri downloadUri) {
        FirebaseUser fu = FirebaseManager.auth().getCurrentUser();
        if (fu == null) return;

        Task<Void> t = FirebaseManager.users()
                .document(fu.getUid())
                .update("imageUrl", downloadUri.toString());

        t.addOnSuccessListener(aVoid -> {
            setLoading(false);
            imageUrl = downloadUri.toString();
            fetchAndShowImage(imageUrl);
            Toast.makeText(this, "Profile image updated.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnChooseImage.setEnabled(!loading);
        binding.btnRefresh.setEnabled(!loading);
    }
}
