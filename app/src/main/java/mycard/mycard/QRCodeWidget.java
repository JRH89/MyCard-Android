package mycard.mycard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
            updateFromFirestore(context, appWidgetManager, appWidgetIds, user.getEmail());
        } else {
            // User not logged in or session not yet restored
            for (int appWidgetId : appWidgetIds) {
                updateAppWidgetWithError(context, appWidgetManager, appWidgetId, "Please log in to sync");
            }
        }
    }

    private void updateFromFirestore(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String email) {
        DocumentReference docRef = db.collection(email).document("userInfo");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        String qrImageData = document.getString("qrImage");
                        if (qrImageData != null && !qrImageData.isEmpty()) {
                            // Convert the base64PNG data URL string to a Bitmap
                            Bitmap qrCodeBitmap = convertBase64ToBitmap(qrImageData);
                            // Update the widget views
                            for (int appWidgetId : appWidgetIds) {
                                updateAppWidget(context, appWidgetManager, appWidgetId, qrCodeBitmap);
                            }
                        } else {
                            for (int appWidgetId : appWidgetIds) {
                                updateAppWidgetWithError(context, appWidgetManager, appWidgetId, "No QR code found");
                            }
                        }
                    } else {
                        for (int appWidgetId : appWidgetIds) {
                            updateAppWidgetWithError(context, appWidgetManager, appWidgetId, "User info not found");
                        }
                    }
                } else {
                    // Handle any potential errors here
                    Exception e = task.getException();
                    if (e != null) {
                        e.printStackTrace();
                    }
                    for (int appWidgetId : appWidgetIds) {
                        updateAppWidgetWithError(context, appWidgetManager, appWidgetId, "Sync error");
                    }
                }
            }
        });
    }

    private void updateAppWidgetWithError(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String errorMessage) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.q_r_code_widget);
        // We could change an image or show a toast, for now just update UI to show we tried
        // views.setImageViewResource(R.id.qr_code_image_widget, R.drawable.error_icon); 
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) || action.equals(Intent.ACTION_BOOT_COMPLETED))) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, QRCodeWidget.class));
                if (appWidgetIds != null && appWidgetIds.length > 0) {
                    onUpdate(context, appWidgetManager, appWidgetIds);
                }
            }
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

        // Create an Intent to launch MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.qr_widget_layout, pendingIntent);

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
