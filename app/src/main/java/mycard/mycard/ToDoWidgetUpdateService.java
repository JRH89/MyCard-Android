package mycard.mycard;
// ToDoWidgetUpdateService.java
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.List;

public class ToDoWidgetUpdateService extends Service {

    private static final long UPDATE_INTERVAL = 3600000; // 1hour
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            updateWidget();
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler.post(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWidget();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    private void updateWidget() {
        // Fetch the current user from Firebase Auth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Check if the user is logged in
        if (currentUser != null) {
            // Get the email of the current user
            String userEmail = currentUser.getEmail();

            // Access Firestore instance and get the document reference for the user's data
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userDocRef = db.collection(userEmail).document("ToDo");

            // Fetch the data from the document
            userDocRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Document exists, fetch the todos and update the widget UI
                        Object todosObject = document.get("todos");
                        if (todosObject instanceof List<?>) {

                            List<String> todos = (List<String>) todosObject;
                            // Now 'todos' contains the array of strings, you can process it further
                            // Create a StringBuilder to hold the formatted todos with hyphens
                            StringBuilder formattedTodos = new StringBuilder();
                            for (String todo : todos) {
                                if (todo != null && !todo.trim().isEmpty()) {
                                    formattedTodos.append("- ").append(todo).append("\n");
                                }
                            }
                            updateToDoWidgetUI(formattedTodos.toString().trim());
                        } else {
                            // Handle the case when the 'todos' field is not an array or empty
                            updateToDoWidgetUI("");
                        }
                    } else {
                        // Document does not exist
                        // Handle the case when the document doesn't exist or todos array is empty
                        updateToDoWidgetUI("");
                    }
                } else {
                    // Error fetching document
                    FirebaseFirestoreException exception = (FirebaseFirestoreException) task.getException();
                    if (exception != null) {
                        // Handle the error here
                        updateToDoWidgetUI("");
                    }
                }
            });
        }
    }

    private void updateToDoWidgetUI(String todosText) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, ToDoWidget.class));

        if (appWidgetIds.length > 0) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.to_do_widget);
            views.setTextViewText(R.id.todoListTextView, todosText);

            // Create an Intent to launch ManageTodosActivity
            Intent intent = new Intent(this, ManageTodosActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetIds, views);
        }
    }
}
