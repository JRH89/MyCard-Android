package mycard.mycard;
// ToDoWidgetUpdateService.java

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.widget.RemoteViews;

public class ToDoWidgetUpdateService extends Service {

    private Handler handler;
    private Runnable runnable;
    private static final long UPDATE_INTERVAL_MS = 60000; // Update every 1 minute

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                updateWidget();
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        handler.post(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Return START_STICKY to keep the service running until explicitly stopped
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Not used for this service
        return null;
    }

    private void updateWidget() {
        // Get the todos from your Firestore database and update the widget UI
        // Similar to your existing code in ToDoWidget class (fetch todos and update the widget)
        // ...

        // Use AppWidgetManager to update the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName componentName = new ComponentName(this, ToDoWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.to_do_widget);
            // Update the views here with the fetched todos
            // ...

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
