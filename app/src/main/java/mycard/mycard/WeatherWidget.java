package mycard.mycard;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
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

/**
 * Implementation of App Widget functionality.
 */
public class WeatherWidget extends AppWidgetProvider {



    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Fetch the current user from Firebase Auth
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

                        // Fetch weather data using the method from MainActivity
                        fetchWeatherDataForWidget(context, appWidgetManager, appWidgetIds, favoriteCity);
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

    // Method to fetch weather data for the widget
    private void fetchWeatherDataForWidget(Context context, AppWidgetManager appWidgetManager,
                                           int[] appWidgetIds, String city) {
        String apiKey = "bdeec3fe00b9a10009325e073c8ec400";
        String units = "imperial";
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey + "&units=" + units;

        new Thread(new Runnable() {
            @Override
            public void run() {
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
                        Log.d("Weather Response", responseData); // Log the response data for debugging

                        JSONObject json = new JSONObject(responseData);
                        JSONObject mainObject = json.getJSONObject("main");
                        double temperature = mainObject.getDouble("temp");
                        Log.d("Weather Temperature", String.valueOf(temperature)); // Log the temperature for debugging

                        // Get the weather description and humidity
                        JSONArray weatherArray = json.getJSONArray("weather");
                        if (weatherArray.length() > 0) {
                            JSONObject weatherObject = weatherArray.getJSONObject(0);
                            String weatherDescription = weatherObject.getString("description");
                            double humidity = mainObject.getDouble("humidity");

                            // Update the widget UI
                            for (int appWidgetId : appWidgetIds) {
                                // Construct the RemoteViews object for the widget
                                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
                                views.setTextViewText(R.id.favoriteCity, city);
                                views.setTextViewText(R.id.temperature, String.valueOf(temperature) + "°F");
                                views.setTextViewText(R.id.description, weatherDescription);
                                views.setTextViewText(R.id.humidity, Math.round(humidity) + "%");

                                // Instruct the widget manager to update the widget
                                appWidgetManager.updateAppWidget(appWidgetId, views);
                            }
                        }
                    } else {
                        Log.e("Weather API", "Error response: " + connection.getResponseCode()); // Log error response code
                    }

                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}