package mycard.mycard;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import mycard.mycard.workers.ToDoUpdateWorker;

public class ToDoWidget extends AppWidgetProvider {

    private static final String WORK_NAME = "ToDoUpdateWorker";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Initial loading state
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.to_do_widget);
            views.setTextViewText(R.id.todoListTextView, context.getString(R.string.loading));
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        // Schedule periodic updates
        schedulePeriodicUpdate(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        schedulePeriodicUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }

    private void schedulePeriodicUpdate(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ToDoUpdateWorker.class,
                15, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }
}
