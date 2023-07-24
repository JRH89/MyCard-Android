package mycard.mycard;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class ToDoWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Called when the widget is updated
        Intent updateServiceIntent = new Intent(context, ToDoWidgetUpdateService.class);
        context.startService(updateServiceIntent);
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
                            // For example, you can join the strings into a single string to display in the widget
                            String todosString = TextUtils.join("\n", todos);
                            updateToDoWidgetUI(context, appWidgetManager, appWidgetIds, todosString);
                        } else {
                            // Handle the case when the 'todos' field is not an array or empty
                            updateToDoWidgetUI(context, appWidgetManager, appWidgetIds, "");
                        }
                    } else {
                        // Document does not exist
                        // Handle the case when the document doesn't exist or todos array is empty
                        updateToDoWidgetUI(context, appWidgetManager, appWidgetIds, "");
                    }
                } else {
                    // Error fetching document
                    FirebaseFirestoreException exception = (FirebaseFirestoreException) task.getException();
                    if (exception != null) {
                        // Handle the error here
                        updateToDoWidgetUI(context, appWidgetManager, appWidgetIds, "");
                    }
                }
            });

        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
                // Update the widget UI whenever the broadcast is received
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                if (appWidgetIds != null && appWidgetIds.length > 0) {
                    onUpdate(context, appWidgetManager, appWidgetIds);
                }
            }
        }
    }

    private void updateToDoWidgetUI(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String todos) {
        // Update the widget UI here with the fetched To-Do items (todos)
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.to_do_widget);
        views.setTextViewText(R.id.todoListTextView, todos);

        // Update all instances of the widget with the updated RemoteViews
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

}
