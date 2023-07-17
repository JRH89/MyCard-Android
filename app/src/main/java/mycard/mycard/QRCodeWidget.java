package mycard.mycard;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class QRCodeWidget extends AppWidgetProvider {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Called when the widget is updated

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Fetch the QR code data from Firebase Firestore
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DocumentReference docRef = db.collection(user.getEmail()).document("userInfo");
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String qrImageData = document.getString("qrImage");

                            // Convert the base64PNG data URL string to a Bitmap
                            Bitmap qrCodeBitmap = convertBase64ToBitmap(qrImageData);

                            // Update the widget views
                            for (int appWidgetId : appWidgetIds) {
                                updateAppWidget(context, appWidgetManager, appWidgetId, qrCodeBitmap);
                            }
                        }
                    } else {
                        // Handle any potential errors here
                        Exception e = task.getException();
                        if (e != null) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private Bitmap convertBase64ToBitmap(String base64String) {
        // Remove the data URL prefix (e.g., "data:image/png;base64,")
        String imageData = base64String.substring(base64String.indexOf(",") + 1);

        byte[] decodedBytes = Base64.decode(imageData, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bitmap qrCodeBitmap) {
        // Create the remote views for the widget layout
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.q_r_code_widget);


        // Set the generated QR code bitmap to the ImageView in the widget layout
        views.setImageViewBitmap(R.id.qr_code_image_widget, qrCodeBitmap);

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
