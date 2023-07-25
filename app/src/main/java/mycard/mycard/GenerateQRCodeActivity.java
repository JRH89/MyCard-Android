package mycard.mycard;

import android.content.Context;
import android.graphics.Bitmap;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class GenerateQRCodeActivity extends AppCompatActivity {

    private EditText editText;
    private Button generateButton, foregroundButton, backgroundButton;
    private ImageView qrCodeImageView;
    private int foregroundColor = Color.BLACK; // Default foreground color is black
    private int backgroundColor = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qrcode);

        editText = findViewById(R.id.editText);
        generateButton = findViewById(R.id.generateButton);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);

        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateQRCodeAndShow();
            }
        });

        qrCodeImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                saveQRCodeToGallery();
                return true;
            }
        });
    }

    private void generateQRCodeAndShow() {
        // Close the keyboard before generating QR code
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

        String textToEncode = editText.getText().toString().trim();
        if (!textToEncode.isEmpty()) {
            try {
                Bitmap qrCodeBitmap = generateQRCode(textToEncode, 512, 512);
                qrCodeImageView.setImageBitmap(qrCodeBitmap);
                editText.setText("");
            } catch (WriterException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap generateQRCode(String text, int width, int height) throws WriterException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);
        int matrixWidth = bitMatrix.getWidth();
        int matrixHeight = bitMatrix.getHeight();
        int[] pixels = new int[matrixWidth * matrixHeight];

        for (int y = 0; y < matrixHeight; y++) {
            int offset = y * matrixWidth;
            for (int x = 0; x < matrixWidth; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? foregroundColor : backgroundColor;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight);
        return bitmap;
    }

    private void saveQRCodeToGallery() {
        // Get the current bitmap from qrImageView
        qrCodeImageView.setDrawingCacheEnabled(true);
        Bitmap qrBitmap = Bitmap.createBitmap(qrCodeImageView.getDrawingCache());
        qrCodeImageView.setDrawingCacheEnabled(false);

        // Save the bitmap to the device's gallery
        String displayName = "QR_Code_Image"; // Set a display name for the image
        String mimeType = "image/png"; // Set the image MIME type (change if needed)
        String uriString = MediaStore.Images.Media.insertImage(
                getContentResolver(), qrBitmap, displayName, null
        );

        if (uriString != null) {
            // Image saved successfully
            Uri imageUri = Uri.parse(uriString);
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } else {
            // Failed to save the image
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

}
