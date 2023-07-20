package mycard.mycard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;

public class FormActivity extends AppCompatActivity {

    private EditText name;
    private EditText job;
    private EditText phone;
    private EditText email;
    private EditText label1;
    private EditText link1;
    private EditText label2;
    private EditText link2;
    private EditText label3;
    private EditText link3;
    private EditText label4;
    private EditText link4;
    private Spinner theme;
    private EditText city;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        name = findViewById(R.id.name);
        job = findViewById(R.id.job);
        phone = findViewById(R.id.phone);
        email = findViewById(R.id.email);
        label1 = findViewById(R.id.Link1Label);
        link1 = findViewById(R.id.Link1);
        label2 = findViewById(R.id.Link2Label);
        link2 = findViewById(R.id.Link2);
        label3 = findViewById(R.id.Link3Label);
        link3 = findViewById(R.id.Link3);
        label4 = findViewById(R.id.Link4Label);
        link4 = findViewById(R.id.Link4);
        theme = findViewById(R.id.theme);
        city = findViewById(R.id.city);
        submitButton = findViewById(R.id.submit_button);


        TextView urlTextView = findViewById(R.id.exampleLink);
        String url = "https://have-mycard.vercel.app/Samples";
        String displayText = "View Theme Examples";

        SpannableString spannableString = new SpannableString(displayText);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                // Handle URL click event here
                // For example, open the URL in a browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        };

        spannableString.setSpan(clickableSpan, 0, displayText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        urlTextView.setText(spannableString);
        urlTextView.setMovementMethod(LinkMovementMethod.getInstance());

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });
    }
    private void submitForm() {
        // Retrieve form field values
        String value1 = name.getText().toString();
        String value2 = job.getText().toString();
        String value3 = phone.getText().toString();
        String value4 = email.getText().toString();
        String value5 = label1.getText().toString();
        String value6 = link1.getText().toString();
        String value7 = label2.getText().toString();
        String value8 = link2.getText().toString();
        String value9 = label3.getText().toString();
        String value10 = link3.getText().toString();
        String value11 = label4.getText().toString();
        String value12 = link4.getText().toString();
        String selectedChoice = theme.getSelectedItem().toString();
        String value13 = city.getText().toString();

        // Generate the random slug
        String slug = generateRandomSlug();

        // Generate the QR code value
        String qrCodeValue = "https://have-mycard.vercel.app/api/" + slug;

        // Generate the QR code bitmap
        Bitmap qrCodeBitmap = generateQRCode(qrCodeValue);

        // Convert the bitmap to base64
        String qrCodeBase64 = convertBitmapToBase64(qrCodeBitmap);

        // Perform form validation
        boolean isValid = validateForm(value1, value2, value3, value4, value5, value6);

        if (isValid) {
            // Submit the form data to the database
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference docRef = db.collection(user.getEmail()).document("userInfo");
                DocumentReference userRef = db.collection("users").document(slug);
                // Create a map to hold the form values
                Map<String, Object> formData = new HashMap<>();
                formData.put("name", value1);
                formData.put("short", qrCodeValue);
                formData.put("email", value4);
                formData.put("qrImage", qrCodeBase64);
                formData.put("id", slug);
                formData.put("jobTitle", value2);
                formData.put("phone", value3);
                formData.put("social1", value6);
                formData.put("social1Label", value5);
                formData.put("social2", value8);
                formData.put("social2Label", value7);
                formData.put("social3", value10);
                formData.put("social3Label", value9);
                formData.put("social4", value12);
                formData.put("social4Label", value11);
                formData.put("theme", selectedChoice);
                formData.put("favoriteCity", value13);

                // Update the document with the form data
                docRef.set(formData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(FormActivity.this, "Profile Created", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(FormActivity.this, "Failed to submit form", Toast.LENGTH_SHORT).show();
                            }
                        });
                userRef.set(formData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(FormActivity.this, "Card created", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(FormActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(FormActivity.this, "Failed to submit form", Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        } else {
            // Display error message or handle invalid form input
            Toast.makeText(this, "Form validation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateRandomSlug() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    private boolean validateForm(String value1, String value2, String value3, String value4, String value5, String value6) {
        if (TextUtils.isEmpty(value1)
                || TextUtils.isEmpty(value2)
                || TextUtils.isEmpty(value3)
                || TextUtils.isEmpty(value4)
                || TextUtils.isEmpty(value5)
                || TextUtils.isEmpty(value6)) {

            showToast("Please fill in all required fields.");
            return false;
        }

        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private Bitmap generateQRCode(String value) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 500, 500);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? getResources().getColor(R.color.black) : getResources().getColor(R.color.white);
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}
