package mycard.mycard;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Fetch the current user from Firebase Auth
        schedulePeriodicUpdates(context);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Check if the user is logged in
        if (currentUser != null) {
            // Get the email of the current user
            String userEmail = currentUser.getEmail();

            // Access Firestore instance and get the document reference for the user's data
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userDocRef = db.collection(userEmail).document("userInfo");

            // Fetch the data from the document
            userDocRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Document exists, fetch the favorite city
                        String favoriteCity = document.getString("favoriteCity");

                        // Fetch weather data using the AsyncTask
                        if (favoriteCity != null) {
                            FetchWeatherDataTask weatherDataTask = new FetchWeatherDataTask(context, appWidgetManager, appWidgetIds, favoriteCity);
                            weatherDataTask.execute();
                        } else {
                            // Handle the case when favoriteCity is null
                            Log.e("WeatherWidgetProvider", "Favorite city is null");
                        }
                    } else {
                        // Document does not exist
                        Log.e("WeatherWidgetProvider", "Document does not exist");
                    }
                } else {
                    // Error fetching document
                    FirebaseFirestoreException exception = (FirebaseFirestoreException) task.getException();
                    if (exception != null) {
                        Log.e("WeatherWidgetProvider", "Error fetching document: " + exception.getMessage());
                    }
                }
            });
        }
    }


    // Method to fetch weather data for the widget using AsyncTask
    // Method to fetch weather data for the widget using AsyncTask
    private static class FetchWeatherDataTask extends AsyncTask<Void, Void, Double> {
        private Context context;
        private AppWidgetManager appWidgetManager;
        private int[] appWidgetIds;
        private String city;

        public FetchWeatherDataTask(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String city) {
            this.context = context;
            this.appWidgetManager = appWidgetManager;
            this.appWidgetIds = appWidgetIds;
            this.city = city;
        }

        @Override
        protected Double doInBackground(Void... params) {
            String apiKey = "bdeec3fe00b9a10009325e073c8ec400";
            String units = "imperial";
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey + "&units=" + units;

            try {
                URL apiUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    bufferedReader.close();
                    inputStream.close();

                    String responseData = responseBuilder.toString();
                    JSONObject json = new JSONObject(responseData);

                    // Check if the API response contains "cod" key with value 200, indicating a successful response
                    int responseCode = json.getInt("cod");
                    if (responseCode == 200) {
                        JSONObject mainObject = json.getJSONObject("main");
                        double temperature = mainObject.getDouble("temp");
                        return temperature;
                    } else {
                        // Handle the case when the API response is not successful
                        Log.e("WeatherWidgetProvider", "API response is not successful, cod: " + responseCode);
                    }
                } else {
                    Log.e("Weather API", "Error response: " + connection.getResponseCode()); // Log error response code
                }

                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Double temperature) {
            if (temperature != null) {
                // Update the widget UI
                for (int appWidgetId : appWidgetIds) {
                    // Construct the RemoteViews object for the widget
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
                    views.setTextViewText(R.id.temperature, Math.round(temperature) + "°F");

                    // Instruct the widget manager to update the widget
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            } else {
                // Handle the case when there was an error fetching data
                Log.e("WeatherWidgetProvider", "Error fetching weather data");
            }
        }
    }

    private void schedulePeriodicUpdates(Context context) {
        // Create an intent to be sent when the alarm triggers
        Intent updateIntent = new Intent(context, WeatherWidget.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WeatherWidget.class));
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

        // Create a PendingIntent to be fired when the alarm triggers
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); // Add the FLAG_IMMUTABLE flag here

        // Set up a repeating alarm using AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long intervalMillis = AlarmManager.INTERVAL_HOUR; // Update every half hour
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), intervalMillis, pendingIntent);
    }

}
