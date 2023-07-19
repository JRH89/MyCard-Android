package mycard.mycard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MenuActivity extends AppCompatActivity {

    Button buttonEditCard;
    Button buttonLogout;
    Button buttonCloseMenu;
    FirebaseAuth auth;
    FirebaseUser user;
    Button buttonDelete;
    Button buttonSupport;
    FirebaseFirestore db;

    ImageView qrCode;

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

        buttonDelete = findViewById(R.id.deleteAcct);
        buttonDelete.setOnClickListener(view -> {
            showDeleteAccountConfirmationDialog();
        });

        buttonEditCard = findViewById(R.id.editCard);
        buttonEditCard.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), EditActivity.class);
            startActivity(intent);
        });

        buttonCloseMenu = findViewById(R.id.btnCloseMenu);
        buttonCloseMenu.setOnClickListener(view -> onBackPressed());

        buttonSupport = findViewById(R.id.support);
        buttonSupport.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), SupportActivity.class);
            startActivity(intent);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Account Deletion");
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteAccount();
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.black));
            positiveButton.setBackgroundColor(getResources().getColor(R.color.white));

            // You can also apply other customizations to the positive button here
        }

        Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.black));
            negativeButton.setBackgroundColor(getResources().getColor(R.color.white));

            // You can also apply other customizations to the negative button here
        }
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
