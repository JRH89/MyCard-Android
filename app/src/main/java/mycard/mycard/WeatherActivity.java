package mycard.mycard;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;


public class WeatherActivity extends AppCompatActivity {

    private EditText editTextCity;
    private Button buttonSearch;
    private TextView textViewTemperature;
    private TextView textViewHumidity;
    private TextView currentCity;

    private TextView weatherDesc;
    private TextView textViewWind;
    private TextView textViewFeelsLike;
    private ForecastAdapter forecastAdapter; // Declare the forecastAdapter as a class-level member
    private RecyclerView recyclerViewForecast; // Declare the recyclerViewForecast as a class-level member

    private static final String API_KEY = "35913733e7f076a1cac136c1de270b7d"; // Replace with your OpenWeatherMap API Key
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    private static class WeatherData {
        private double temperature;
        private int humidity;
        private double windSpeed;
        private double feelsLike;
        private String weatherDescription; // Add the weather description field

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getHumidity() {
            return humidity;
        }

        public void setHumidity(int humidity) {
            this.humidity = humidity;
        }

        public double getWindSpeed() {
            return windSpeed;
        }

        public void setWindSpeed(double windSpeed) {
            this.windSpeed = windSpeed;
        }

        public double getFeelsLike() {
            return feelsLike;
        }

        public void setFeelsLike(double feelsLike) {
            this.feelsLike = feelsLike;
        }

        public String getWeatherDescription() {
            return weatherDescription;
        }

        public void setWeatherDescription(String weatherDescription) {
            this.weatherDescription = weatherDescription;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        editTextCity = findViewById(R.id.editTextCity);
        buttonSearch = findViewById(R.id.buttonSearch);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewWind = findViewById(R.id.textViewWind);
        textViewFeelsLike = findViewById(R.id.textViewFeelsLike);
        forecastAdapter = new ForecastAdapter(new ArrayList<>(), true); // Use Imperial units by default
        recyclerViewForecast = findViewById(R.id.recyclerViewForecast);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewForecast.setLayoutManager(layoutManager);
        recyclerViewForecast.setAdapter(forecastAdapter);
        currentCity = findViewById(R.id.currentCity);
        weatherDesc = findViewById(R.id.weatherDesc);

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
                        new WeatherDataAsyncTask(favoriteCity).execute();
                        currentCity.setText(favoriteCity);
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

        // Fetch weather data for the city entered in the search bar and update the UI accordingly
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = editTextCity.getText().toString().trim();
                if (!city.isEmpty()) {
                    new WeatherDataAsyncTask(city).execute();
                    currentCity.setText(city);

                    // Hide the keyboard
                    hideKeyboard();

                    // Clear focus from the EditText
                    editTextCity.clearFocus();
                } else {
                    Toast.makeText(WeatherActivity.this, "Please enter a city name.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        // Inside the onCreate method
// ...

        Button buttonSetFavoriteCity = findViewById(R.id.buttonSetFavoriteCity);
        buttonSetFavoriteCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String favoriteCity = currentCity.getText().toString();
                if (!favoriteCity.isEmpty()) {
                    // Save the favorite city to Firestore for the current user
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        String userEmail = currentUser.getEmail();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        DocumentReference userDocRef = db.collection(userEmail).document("userInfo");
                        userDocRef.update("favoriteCity", favoriteCity)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(WeatherActivity.this, "Favorite city updated.", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(WeatherActivity.this, "Failed to update favorite city.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                } else {
                    Toast.makeText(WeatherActivity.this, "Please enter a city name.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editTextCity.getWindowToken(), 0);
        }
    }

    private class WeatherForecastDataAsyncTask extends AsyncTask<String, Void, List<ForecastItem>> {

        private String city;

        public WeatherForecastDataAsyncTask(String city) {
            this.city = city;
        }

        @Override
        protected List<ForecastItem> doInBackground(String... params) {
            String urlStr = BASE_URL + "/forecast?q=" + city + "&appid=" + API_KEY + "&units=imperial"; // Add "&units=imperial" to get temperature in Fahrenheit.
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        response.append(line);
                    }
                    return parseForecastData(response.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private List<ForecastItem> parseForecastData(String jsonData) {
            try {
                JSONObject jsonObject = new JSONObject(jsonData);
                JSONArray forecastList = jsonObject.getJSONArray("list");
                List<ForecastItem> forecastItems = new ArrayList<>();

                for (int i = 0; i < forecastList.length(); i++) {
                    JSONObject forecastObject = forecastList.getJSONObject(i);
                    String dateTime = forecastObject.getString("dt_txt");
                    JSONObject main = forecastObject.getJSONObject("main");
                    double temperatureCelsius = main.getDouble("temp");
                    double temperatureFahrenheit = convertCelsiusToFahrenheit(temperatureCelsius);
                    JSONArray weatherArray = forecastObject.getJSONArray("weather");
                    String description = weatherArray.getJSONObject(0).getString("description");

                    // Create a new ForecastItem object and add it to the list
                    ForecastItem forecastItem = new ForecastItem(dateTime, temperatureCelsius, temperatureFahrenheit, description);
                    forecastItems.add(forecastItem);
                }

                return forecastItems;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        private double convertCelsiusToFahrenheit(double temperatureCelsius) {
            return (temperatureCelsius * 9 / 5) + 32;
        }

        @Override
        protected void onPostExecute(List<ForecastItem> forecastItems) {
            if (forecastItems != null) {
                // Update the UI with the forecast data
                updateForecastUI(forecastItems);
            } else {
                Toast.makeText(WeatherActivity.this, "Failed to fetch forecast data.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class WeatherDataAsyncTask extends AsyncTask<Void, Void, WeatherData> {

        private String city;

        public WeatherDataAsyncTask(String city) {
            this.city = city;
        }

        @Override
        protected WeatherData doInBackground(Void... params) {
            String urlStr = BASE_URL + "?q=" + city + "&appid=" + API_KEY + "&units=imperial"; // Add "&units=imperial" to get temperature in Fahrenheit.
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        response.append(line);
                    }
                    return parseWeatherData(response.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private WeatherData parseWeatherData(String jsonData) {
            try {
                JSONObject jsonObject = new JSONObject(jsonData);
                JSONObject main = jsonObject.getJSONObject("main");
                double temperature = main.getDouble("temp");
                int humidity = main.getInt("humidity");

                JSONObject wind = jsonObject.getJSONObject("wind");
                double windSpeed = wind.getDouble("speed");

                double feelsLike = jsonObject.getJSONObject("main").getDouble("feels_like");

                JSONArray weatherArray = jsonObject.getJSONArray("weather");
                String weatherDescription = weatherArray.getJSONObject(0).getString("description");

                // Create a new WeatherData object and set the values
                WeatherData weatherData = new WeatherData();
                weatherData.setTemperature(temperature);
                weatherData.setHumidity(humidity);
                weatherData.setWindSpeed(windSpeed);
                weatherData.setFeelsLike(feelsLike);
                weatherData.setWeatherDescription(weatherDescription); // Set the weather description

                return weatherData;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(WeatherData weatherData) {
            if (weatherData != null) {
                // Update the UI with the weather data
                updateCurrentWeatherUI(weatherData);
            } else {
                Toast.makeText(WeatherActivity.this, "Failed to fetch weather data.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateCurrentWeatherUI(WeatherData weatherData) {
        // Update the UI with the current weather data
        textViewTemperature.setText(Math.round(weatherData.getTemperature()) + " °F");
        textViewHumidity.setText("Humidity: " + Math.round(weatherData.getHumidity()) + "%");
        textViewWind.setText("Wind Speed: " + Math.round(weatherData.getWindSpeed()) + " mph");
        textViewFeelsLike.setText("Feels Like: " + Math.round(weatherData.getFeelsLike()) + " °F");
        weatherDesc.setText(weatherData.getWeatherDescription()); // Set the weather description
    }

    private void updateForecastUI(List<ForecastItem> forecastItems) {
        // Update the UI with the forecast data (use RecyclerView and the ForecastAdapter)
        forecastAdapter.setForecastItems(forecastItems);
    }
}
