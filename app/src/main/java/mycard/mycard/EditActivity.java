package mycard.mycard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditActivity extends AppCompatActivity {

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
    // Add other form fields here as needed
    private String userId; // Store the user ID

    Button uploadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

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
        uploadButton = findViewById(R.id.uploadButton);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ImageUploadActivity.class);
                startActivity(intent);
            }
        });

        // Call a method to retrieve the user's data and populate the form fields
        loadUserData();

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
                submitForm(userId); // Pass the user ID to the submitForm method
            }
        });

    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference docRef = db.collection(user.getEmail()).document("userInfo");

            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        // Retrieve the user's data from the document
                        String userName = documentSnapshot.getString("name");
                        String userJob = documentSnapshot.getString("jobTitle");
                        String userPhone = documentSnapshot.getString("phone");
                        String userEmail = documentSnapshot.getString("email");
                        String userLabel1 = documentSnapshot.getString("social1Label");
                        String userLink1 = documentSnapshot.getString("social1");
                        String userLabel2 = documentSnapshot.getString("social2Label");
                        String userLink2 = documentSnapshot.getString("social2");
                        String userLabel3 = documentSnapshot.getString("social3Label");
                        String userLink3 = documentSnapshot.getString("social3");
                        String userLabel4 = documentSnapshot.getString("social4Label");
                        String userLink4 = documentSnapshot.getString("social4");
                        String userTheme = documentSnapshot.getString("theme");
                        String userCity = documentSnapshot.getString("favoriteCity");
                        userId = documentSnapshot.getString("id"); // Store the custom user ID

                        // Populate the form fields with the retrieved values
                        name.setText(userName);
                        job.setText(userJob);
                        phone.setText(userPhone);
                        email.setText(userEmail);
                        label1.setText(userLabel1);
                        link1.setText(userLink1);
                        label2.setText(userLabel2);
                        link2.setText(userLink2);
                        label3.setText(userLabel3);
                        link3.setText(userLink3);
                        label4.setText(userLabel4);
                        link4.setText(userLink4);

                        // Set the spinner to the correct position based on the userTheme
                        int themePosition = getThemePosition(userTheme);
                        theme.setSelection(themePosition);

                        // Populate the city edit text with the retrieved value
                        city.setText(userCity);
                    } else {
                        Toast.makeText(EditActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(EditActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private int getThemePosition(String userTheme) {
        String[] themesArray = getResources().getStringArray(R.array.choices);
        for (int i = 0; i < themesArray.length; i++) {
            if (userTheme.equals(themesArray[i])) {
                return i;
            }
        }
        // If the userTheme does not match any of the array items, set it to the default position (e.g., Original)
        return 1;
    }

    private void submitForm(String userId) {
        // Retrieve form field values
        String updatedName = name.getText().toString();
        String updatedJob = job.getText().toString();
        String updatedPhone = phone.getText().toString();
        String updatedEmail = email.getText().toString();
        String updatedLabel1 = label1.getText().toString();
        String updatedLink1 = link1.getText().toString();
        String updatedLabel2 = label2.getText().toString();
        String updatedLink2 = link2.getText().toString();
        String updatedLabel3 = label3.getText().toString();
        String updatedLink3 = link3.getText().toString();
        String updatedLabel4 = label4.getText().toString();
        String updatedLink4 = link4.getText().toString();
        String updatedTheme = theme.getSelectedItem().toString();
        String updatedCity = city.getText().toString();

        // Perform form validation
        boolean isValid = validateForm(updatedName, updatedJob);

        if (isValid) {
            // Submit the form data to update the user's data in the database
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference docRef = db.collection(user.getEmail()).document("userInfo");
                DocumentReference userRef = db.collection("users").document(userId); // Use the custom user ID

                // Create a map to hold the updated form values
                Map<String, Object> updatedData = new HashMap<>();
                updatedData.put("name", updatedName);
                updatedData.put("jobTitle", updatedJob);
                updatedData.put("phone", updatedPhone);
                updatedData.put("email", updatedEmail);
                updatedData.put("social1Label", updatedLabel1);
                updatedData.put("social1", updatedLink1);
                updatedData.put("social2Label", updatedLabel2);
                updatedData.put("social2", updatedLink2);
                updatedData.put("social3Label", updatedLabel3);
                updatedData.put("social3", updatedLink3);
                updatedData.put("social4Label", updatedLabel4);
                updatedData.put("social4", updatedLink4);
                updatedData.put("theme", updatedTheme);
                updatedData.put("favoriteCity", updatedCity);

                // Update the document with the updated form data
                docRef.update(updatedData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(EditActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                                // After successful update, you can navigate back to the MainActivity
                                Intent intent = new Intent(EditActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(EditActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                            }
                        });
                userRef.set(updatedData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(EditActivity.this, "Card updated", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(EditActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(EditActivity.this, "Failed to update card", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    private boolean validateForm(String name, String job) {
        // You can implement your form validation logic here
        // For example, check if the name and job fields are not empty
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (job.isEmpty()) {
            Toast.makeText(this, "Please enter your job title", Toast.LENGTH_SHORT).show();
            return false;
        }

        // You can add more validation rules as needed

        return true;
    }
}
