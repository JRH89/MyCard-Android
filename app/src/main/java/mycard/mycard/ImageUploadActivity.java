package mycard.mycard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.widget.TextView;
import android.widget.Toast;

import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class ImageUploadActivity extends AppCompatActivity {

    private static final int REQUEST_GALLERY_PERMISSION = 100;
    private static final int REQUEST_PICK_IMAGE = 101;

    private ImageView imageView;
    private Bitmap selectedBitmap;
    private String userId;

    private TextView buttonClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_upload);

        imageView = findViewById(R.id.imageView);

        // Check for gallery permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, do nothing
            retrieveUserIdAndSetVariable(); // Call the method to retrieve the user ID
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_GALLERY_PERMISSION);
        }

        buttonClose = findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an explicit Intent to go back to the previous activity
                // Here, assuming the previous activity is MainActivity, but it may vary in your case
                Intent intent = new Intent(ImageUploadActivity.this, MainActivity.class);
                startActivity(intent);
                // Finish the current activity to remove it from the activity stack
                finish();
            }
        });

    }


    // Retrieve the 'id' field from the 'userInfo' document in the 'user.getEmail()' collection
    private void retrieveUserIdAndSetVariable() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userInfoRef = db.collection(user.getEmail()).document("userInfo");
            userInfoRef.get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists() && documentSnapshot.contains("id")) {
                                userId = documentSnapshot.getString("id");
                                Log.d(userId, "onSuccess: ");

                                // Check if the document contains the "base64Image" field
                                if (documentSnapshot.contains("base64Image")) {
                                    String base64Image = documentSnapshot.getString("base64Image");
                                    // Convert the Base64 string back to a Bitmap
                                    Bitmap userImageBitmap = decodeBase64ToBitmap(base64Image);
                                    // Display the Bitmap in the ImageView
                                    imageView.setImageBitmap(userImageBitmap);
                                }
                            } else {
                                // Handle the case where 'userInfo' document or 'id' field doesn't exist
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Handle the failure to retrieve 'userInfo' document
                        }
                    });
        }
    }
    // Decode the Base64 string to a Bitmap
    private Bitmap decodeBase64ToBitmap(String base64Image) {
        byte[] decodedByteArray = Base64.decode(base64Image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.length);
    }


    // Handle the permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_GALLERY_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open gallery
                openGallery();
            } else {
                // Handle permission denied
            }
        }
    }

    // Open the gallery to select an image when the button is clicked
    public void onPickImageClick(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_GALLERY_PERMISSION);
        }
    }

    // Open the gallery to select an image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    // Handle the selected image from the gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                // Get the image URI
                Uri imageUri = data.getData();
                try {
                    // Load the selected image into a bitmap with correct orientation
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    // Rotate the bitmap if needed to correct the orientation
                    int rotation = getRotationFromExif(imageUri);
                    if (rotation != 0) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotation);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }

                    // Perform the circular cropping and resize to 300x300 pixels
                    selectedBitmap = getCircularCroppedBitmap(bitmap, 400);

                    // Set the cropped image to the ImageView for preview
                    imageView.setImageBitmap(selectedBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Get the image rotation from EXIF data
    private int getRotationFromExif(Uri imageUri) throws IOException {
        int orientation = 0;
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        ExifInterface exifInterface = new ExifInterface(inputStream);
        int exifRotation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (exifRotation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                orientation = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                orientation = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                orientation = 270;
                break;
        }
        inputStream.close();
        return orientation;
    }

    // Auto-crop the bitmap to the specified width and height
    // Custom method to perform circular cropping and resizing
    private Bitmap getCircularCroppedBitmap(Bitmap bitmap, int diameter) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, diameter, diameter, true);
        Bitmap outputBitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, diameter, diameter);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaledBitmap, rect, rect, paint);

        return outputBitmap;
    }

    // Handle the "Upload Image" button click to upload the cropped image to Firestore
    public void onUploadImageClick(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Check if an image is selected before proceeding with the upload
        if (selectedBitmap == null) {
            // Show a message to select an image first
            return;
        }

        // Convert the cropped image to a Base64-encoded string
        String base64Image = encodeBitmapToBase64(selectedBitmap);

        if (userId == null) {
            // Show a message to indicate that the user ID is not available yet
            return;
        }

        String userDocumentId = user.getEmail();
        uploadImageToFirestore(base64Image, userDocumentId, "userInfo");

        // Navigate back to the previous activity
        finish();
    }


    private void uploadImageToFirestore(String base64Image, String collectionName, String documentId) {
        // Create an ImageData object with the updated "base64Image" field
        ImageData imageData = new ImageData(base64Image);

        // TODO: Upload the imageData object to Firestore as a document in the specified collection
        // and document ID.

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(collectionName)
                .document(documentId)
                .set(imageData, SetOptions.merge()) // Use SetOptions.merge() to update only the specified field
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Image upload success
                        // Show a success message using a toast
                        Toast.makeText(ImageUploadActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                        Log.d("ImageUploadActivity", "Image uploaded successfully to " + collectionName + "/" + documentId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle the failure to upload the image data
                        // Show an error message using a toast
                        Toast.makeText(ImageUploadActivity.this, "Failed to upload image.", Toast.LENGTH_SHORT).show();
                        Log.e("ImageUploadActivity", "Error uploading image to " + collectionName + "/" + documentId, e);
                    }
                });
    }


    // Encode the Bitmap image to a Base64 string
    private String encodeBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public class ImageData {
        private String base64Image;

        public ImageData() {
            // Default constructor required for Firestore
        }

        public ImageData(String base64Image) {
            this.base64Image = base64Image;
        }

        public String getBase64Image() {
            return base64Image;
        }

        public void setBase64Image(String base64Image) {
            this.base64Image = base64Image;
        }
    }

}