package mycard.mycard.workers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import mycard.mycard.BuildConfig;
import mycard.mycard.CombinedWidget;
import mycard.mycard.MainActivity;
import mycard.mycard.ManageTodosActivity;
import mycard.mycard.MenuActivity;
import mycard.mycard.R;
import mycard.mycard.WeatherActivity;

public class CombinedUpdateWorker extends Worker {
    private static final String TAG = "CombinedUpdateWorker";

    public CombinedUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return Result.failure();
        }

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            
            // Fetch User Info (City and QR)
            DocumentSnapshot userDoc = Tasks.await(db.collection(user.getEmail()).document("userInfo").get());
            String favoriteCity = userDoc.getString("favoriteCity");
            String qrImageData = userDoc.getString("qrImage");

            // Fetch ToDos
            DocumentSnapshot todoDoc = Tasks.await(db.collection(user.getEmail()).document("ToDo").get());
            List<String> todos = (List<String>) todoDoc.get("todos");

            // Fetch Weather
            String tempText = "--°F";
            if (favoriteCity != null) {
                WeatherData weather = fetchWeatherData(favoriteCity);
                if (weather != null) {
                    tempText = Math.round(weather.temperature) + "°F";
                }
            }

            updateWidget(context, tempText, qrImageData, todos);
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error in combined update", e);
            return Result.retry();
        }
    }

    private WeatherData fetchWeatherData(String city) {
        String apiKey = BuildConfig.OPEN_WEATHER_API_KEY;
        String urlString = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey + "&units=imperial";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONObject main = json.getJSONObject("main");
                return new WeatherData(main.getDouble("temp"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Weather fetch failed", e);
        }
        return null;
    }

    private void updateWidget(Context context, String temp, String qrData, List<String> todos) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, CombinedWidget.class));

        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.combined_widget);
            
            // Update Weather
            views.setTextViewText(R.id.temperature, temp);

            // Update QR Code
            if (qrData != null && !qrData.isEmpty()) {
                Bitmap qrBitmap = convertBase64ToBitmap(qrData);
                views.setImageViewBitmap(R.id.qr_code_image_widget, qrBitmap);
            }

            // Update ToDos
            StringBuilder todoText = new StringBuilder();
            if (todos != null && !todos.isEmpty()) {
                for (int i = 0; i < Math.min(todos.size(), 3); i++) {
                    todoText.append("• ").append(todos.get(i)).append("\n");
                }
            } else {
                todoText.append(context.getString(R.string.no_todos));
            }
            views.setTextViewText(R.id.todoListTextView, todoText.toString().trim());

            // Click Intent for Weather
            Intent weatherIntent = new Intent(context, WeatherActivity.class);
            PendingIntent weatherPI = PendingIntent.getActivity(context, 1, weatherIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.weather_section, weatherPI);

            // Click Intent for QR Code (to Main)
            Intent mainIntent = new Intent(context, MainActivity.class);
            PendingIntent mainPI = PendingIntent.getActivity(context, 2, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.qr_code_image_widget, mainPI);

            // Click Intent for ToDo
            Intent todoIntent = new Intent(context, ManageTodosActivity.class);
            PendingIntent todoPI = PendingIntent.getActivity(context, 3, todoIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.todo_section, todoPI);

            appWidgetManager.updateAppWidget(id, views);
        }
    }

    private Bitmap convertBase64ToBitmap(String base64String) {
        try {
            String imageData = base64String.substring(base64String.indexOf(",") + 1);
            byte[] decodedBytes = Base64.decode(imageData, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private static class WeatherData {
        double temperature;
        WeatherData(double t) { this.temperature = t; }
    }
}