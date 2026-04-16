package com.hookerhillstudios.mycard;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import com.hookerhillstudios.mycard.workers.CombinedUpdateWorker;

public class CombinedWidget extends AppWidgetProvider {

    private static final String WORK_NAME = "CombinedUpdateWork";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Initial loading state
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.combined_widget);
            views.setTextViewText(R.id.temperature, context.getString(R.string.loading));
            views.setTextViewText(R.id.todoListTextView, context.getString(R.string.loading));
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        scheduleUpdates(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) || action.equals(Intent.ACTION_BOOT_COMPLETED))) {
                scheduleUpdates(context);
            }
        }
    }

    private void scheduleUpdates(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Periodic update every hour
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                CombinedUpdateWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
        );

        // Immediate update
        WorkManager.getInstance(context).enqueue(
                new OneTimeWorkRequest.Builder(CombinedUpdateWorker.class).build()
        );
    }
}
