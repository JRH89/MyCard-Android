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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import mycard.mycard.BuildConfig;
import mycard.mycard.R;
import mycard.mycard.WeatherActivity;
import mycard.mycard.WeatherWidget;

public class WeatherUpdateWorker extends Worker {
    private static final String TAG = "WeatherUpdateWorker";

    public WeatherUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Log.e(TAG, "No user logged in");
            return Result.failure();
        }

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentSnapshot document = Tasks.await(db.collection(user.getEmail()).document("userInfo").get());

            if (document.exists()) {
                String favoriteCity = document.getString("favoriteCity");
                if (favoriteCity != null) {
                    WeatherData weatherData = fetchWeatherData(favoriteCity);
                    if (weatherData != null) {
                        updateWidget(context, weatherData, favoriteCity);
                        return Result.success();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating weather", e);
            return Result.retry();
        }

        return Result.failure();
    }

    private WeatherData fetchWeatherData(String city) {
        String apiKey = BuildConfig.OPEN_WEATHER_API_KEY;
        String units = "imperial";
        String urlString = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey + "&units=" + units;

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                if (json.getInt("cod") == 200) {
                    JSONObject main = json.getJSONObject("main");
                    double temp = main.getDouble("temp");
                    int humidity = main.getInt("humidity");

                    JSONArray weatherArray = json.getJSONArray("weather");
                    String desc = weatherArray.length() > 0 ? weatherArray.getJSONObject(0).getString("description") : "";

                    return new WeatherData(temp, desc, humidity);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Fetch failed", e);
        }
        return null;
    }

    private void updateWidget(Context context, WeatherData data, String city) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, WeatherWidget.class));

        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
            views.setTextViewText(R.id.temperature, context.getString(R.string.temp_format, Math.round(data.temperature)));
            views.setTextViewText(R.id.description, data.description);
            views.setViewVisibility(R.id.description, android.view.View.VISIBLE);
            views.setTextViewText(R.id.favoriteCity, city);
            views.setViewVisibility(R.id.favoriteCity, android.view.View.VISIBLE);
            views.setTextViewText(R.id.humidity, context.getString(R.string.humidity_format, data.humidity));
            views.setViewVisibility(R.id.humidity, android.view.View.VISIBLE);

            String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
            views.setTextViewText(R.id.lastUpdated, "Updated: " + currentTime);
            views.setViewVisibility(R.id.lastUpdated, android.view.View.VISIBLE);

            Intent intent = new Intent(context, WeatherActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.weather_widget_layout, pendingIntent);

            appWidgetManager.updateAppWidget(id, views);
        }
    }

    private static class WeatherData {
        double temperature;
        String description;
        int humidity;

        WeatherData(double t, String d, int h) {
            this.temperature = t;
            this.description = d;
            this.humidity = h;
        }
    }
}
