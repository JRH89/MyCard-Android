package mycard.mycard.workers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import mycard.mycard.ManageTodosActivity;
import mycard.mycard.R;
import mycard.mycard.ToDoWidget;

public class ToDoUpdateWorker extends Worker {
    private static final String TAG = "ToDoUpdateWorker";

    public ToDoUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            updateWidgetWithError(context, context.getString(R.string.login_required));
            return Result.failure();
        }

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentSnapshot document = Tasks.await(db.collection(user.getEmail()).document("ToDo").get());

            if (document.exists()) {
                Object todosObject = document.get("todos");
                if (todosObject instanceof List<?>) {
                    List<String> todos = (List<String>) todosObject;
                    updateWidget(context, todos);
                    return Result.success();
                }
            }
            updateWidget(context, null);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error updating todos", e);
            return Result.retry();
        }
    }

    private void updateWidget(Context context, List<String> todos) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ToDoWidget.class));

        StringBuilder formattedTodos = new StringBuilder();
        if (todos != null && !todos.isEmpty()) {
            for (String todo : todos) {
                if (todo != null && !todo.trim().isEmpty()) {
                    formattedTodos.append("- ").append(todo).append("\n");
                }
            }
        } else {
            formattedTodos.append(context.getString(R.string.no_todos));
        }

        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.to_do_widget);
            views.setTextViewText(R.id.todoListTextView, formattedTodos.toString().trim());

            String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
            views.setTextViewText(R.id.lastUpdated, context.getString(R.string.updated_format, currentTime));
            views.setViewVisibility(R.id.lastUpdated, android.view.View.VISIBLE);

            Intent intent = new Intent(context, ManageTodosActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

            appWidgetManager.updateAppWidget(id, views);
        }
    }

    private void updateWidgetWithError(Context context, String error) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ToDoWidget.class));

        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.to_do_widget);
            views.setTextViewText(R.id.todoListTextView, error);
            appWidgetManager.updateAppWidget(id, views);
        }
    }
}
