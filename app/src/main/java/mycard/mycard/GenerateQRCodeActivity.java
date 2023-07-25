package mycard.mycard;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class GenerateQRCodeActivity extends AppCompatActivity {

    private EditText editText;
    private Button generateButton;
    private ImageView qrCodeImageView;

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
                String textToEncode = editText.getText().toString();
                if (!textToEncode.isEmpty()) {
                    try {
                        Bitmap qrCodeBitmap = generateQRCode(textToEncode, 512, 512);
                        qrCodeImageView.setImageBitmap(qrCodeBitmap);
                    } catch (WriterException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private Bitmap generateQRCode(String text, int width, int height) throws WriterException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);
        int matrixWidth = bitMatrix.getWidth();
        int matrixHeight = bitMatrix.getHeight();
        int[] pixels = new int[matrixWidth * matrixHeight];

        for (int y = 0; y < matrixHeight; y++) {
            int offset = y * matrixWidth;
            for (int x = 0; x < matrixWidth; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight);
        return bitmap;
    }
}
