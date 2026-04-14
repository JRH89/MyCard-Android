package mycard.mycard;

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

import mycard.mycard.workers.WeatherUpdateWorker;

public class WeatherWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Initial loading state
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
            views.setTextViewText(R.id.temperature, context.getString(R.string.loading));
            views.setViewVisibility(R.id.description, android.view.View.GONE);
            views.setViewVisibility(R.id.humidity, android.view.View.GONE);
            views.setViewVisibility(R.id.lastUpdated, android.view.View.GONE);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        schedulePeriodicUpdates(context);
        // Trigger an immediate update
        WorkManager.getInstance(context).enqueue(
                new OneTimeWorkRequest.Builder(WeatherUpdateWorker.class).build()
        );
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) || action.equals(Intent.ACTION_BOOT_COMPLETED))) {
                schedulePeriodicUpdates(context);
                WorkManager.getInstance(context).enqueue(
                        new OneTimeWorkRequest.Builder(WeatherUpdateWorker.class).build()
                );
            }
        }
    }

    private void schedulePeriodicUpdates(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest weatherWork = new PeriodicWorkRequest.Builder(
                WeatherUpdateWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "WeatherUpdateWork",
                ExistingPeriodicWorkPolicy.KEEP,
                weatherWork
        );
    }
}
