package com.hookerhillstudios.mycard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import com.google.android.material.button.MaterialButton;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MenuActivity extends AppCompatActivity {

    MaterialButton buttonEditCard;
    MaterialButton buttonLogout;
    MaterialButton buttonCloseMenu;
    FirebaseAuth auth;
    FirebaseUser user;
    MaterialButton buttonDelete;


    TextView webButton;
    FirebaseFirestore db;

    ImageView qrCode;

    MaterialButton buttonTodo;
    MaterialButton buttonSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        buttonLogout = findViewById(R.id.logout);
        buttonLogout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });

        buttonTodo = findViewById(R.id.todoButton);
        buttonTodo.setOnClickListener(view -> {
                    Intent intent = new Intent(getApplicationContext(), ManageTodosActivity.class);
                    startActivity(intent);
                });

        buttonDelete = findViewById(R.id.deleteAcct);
        buttonDelete.setOnClickListener(view -> {
            showDeleteAccountConfirmationDialog();
        });

        setupNavigation();
    }

    private void setupNavigation() {
        View navBar = findViewById(R.id.buttonRow);
        if (navBar != null) {
            View homeBtn = navBar.findViewById(R.id.home_button);
            if (homeBtn != null) {
                homeBtn.setOnClickListener(v -> {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            }

            View editBtn = navBar.findViewById(R.id.edit_button);
            if (editBtn != null) {
                editBtn.setOnClickListener(v -> {
                    startActivity(new Intent(this, EditActivity.class));
                });
            }

            View shareBtn = navBar.findViewById(R.id.share_button);
            if (shareBtn != null) {
                shareBtn.setOnClickListener(v -> {
                    Toast.makeText(this, "Go to Home to share your card", Toast.LENGTH_SHORT).show();
                });
            }

            View menuBtn = navBar.findViewById(R.id.menu_button);
            if (menuBtn != null) {
                menuBtn.setOnClickListener(v -> {
                    Toast.makeText(this, "You are in the menu", Toast.LENGTH_SHORT).show();
                });
            }
        }
        
        buttonSupport = findViewById(R.id.supportButton);
        buttonSupport.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:")); // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"hookerhillstudios@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });

        TextView webButton = findViewById(R.id.websiteButton);

        // Set a click listener on the "websiteButton" TextView
        webButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Define the URL you want to navigate to
                String websiteUrl = "https://have-mycard.vercel.app";

                // Create an intent to open the URL in the device's web browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl));

                // Start the activity (web browser) to open the URL
                startActivity(intent);
            }
        });

        qrCode = findViewById(R.id.qr_image);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Fetch the QR image data from Firestore
        DocumentReference docRef = db.collection(user.getEmail()).document("userInfo");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String qrImageData = document.getString("qrImage");
                        if (qrImageData != null && !qrImageData.isEmpty()) {
                            // Convert the base64PNG data URL string to a Bitmap or load it directly into the ImageView
                            Bitmap qrBitmap = convertBase64ToBitmap(qrImageData);
                            qrCode.setImageBitmap(qrBitmap);
                            qrCode.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            qrCode.setAdjustViewBounds(true);
                            qrCode.setMaxHeight(500);
                            qrCode.setMaxWidth(500);
                        } else {
                            // Handle the case where qrImage data is null or empty
                        }
                    } else {
                        // Document doesn't exist or doesn't contain the qrImage field
                    }
                } else {
                    // Failed to fetch the document or the qrImage field
                    // Handle the error as needed
                }
            }
        });
    }

        private Bitmap convertBase64ToBitmap(String base64String) {
        // Remove the data URL prefix (e.g., "data:image/png;base64,")
        String imageData = base64String.substring(base64String.indexOf(",") + 1);

        byte[] decodedBytes = Base64.decode(imageData, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private void showDeleteAccountConfirmationDialog() {
        // Create a LayoutInflater object to inflate the custom layout
        LayoutInflater inflater = getLayoutInflater();
        View customView = inflater.inflate(R.layout.custom_dialog_layout, null);

        // Get references to views in the custom layout
        TextView dialogTitle = customView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = customView.findViewById(R.id.dialogMessage);
        MaterialButton btnPositive = customView.findViewById(R.id.btnPositive);
        MaterialButton btnNegative = customView.findViewById(R.id.btnNegative);

        dialogTitle.setText("Confirm Account Deletion");
        dialogMessage.setText("Are you sure you want to delete your account? This action cannot be undone.");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(customView);

        AlertDialog alertDialog = builder.create();

        btnPositive.setOnClickListener(view -> {
            deleteAccount();
            alertDialog.dismiss();
        });

        btnNegative.setOnClickListener(view -> alertDialog.dismiss());

        alertDialog.show();
    }


    private void deleteAccount() {
        if (user != null) {
            user.delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Account deletion successful
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(getApplicationContext(), Login.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Account deletion failed
                            // Display an error message or handle the error as needed
                        }
                    });
        }
    }
}
