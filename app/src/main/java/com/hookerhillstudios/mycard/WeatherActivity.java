package com.hookerhillstudios.mycard;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.google.android.material.button.MaterialButton;
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
import android.content.Intent;
import android.view.inputmethod.InputMethodManager;


public class WeatherActivity extends AppCompatActivity {

    private EditText editTextCity;
    private MaterialButton buttonSearch;
    private TextView textViewTemperature;
    private TextView textViewHumidity;
    private TextView currentCity;

    private TextView weatherDesc;
    private TextView textViewWind;
    private TextView textViewFeelsLike;
    private ForecastAdapter forecastAdapter; // Declare the forecastAdapter as a class-level member
    private RecyclerView recyclerViewForecast; // Declare the recyclerViewForecast as a class-level member

    private static final String API_KEY = BuildConfig.OPEN_WEATHER_API_KEY; 
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
                    editTextCity.setText("");
                } else {
                    Toast.makeText(WeatherActivity.this, R.string.enter_city_error, Toast.LENGTH_SHORT).show();
                }
            }
        });


        // Inside the onCreate method
// ...

        MaterialButton buttonSetFavoriteCity = findViewById(R.id.buttonSetFavoriteCity);
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
                                        Toast.makeText(WeatherActivity.this, R.string.favorite_city_updated, Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(WeatherActivity.this, R.string.favorite_city_update_failed, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                } else {
                    Toast.makeText(WeatherActivity.this, R.string.enter_city_error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        setupNavigation();
    }

    private void setupNavigation() {
        View navBar = findViewById(R.id.buttonRow);
        if (navBar == null) return;

        View homeBtn = navBar.findViewById(R.id.home_button);
        if (homeBtn != null) {
            homeBtn.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }

        View editBtn = navBar.findViewById(R.id.edit_button);
        if (editBtn != null) {
            editBtn.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), EditActivity.class);
                startActivity(intent);
            });
        }

        View shareBtn = navBar.findViewById(R.id.share_button);
        if (shareBtn != null) {
            shareBtn.setOnClickListener(v -> {
                Toast.makeText(this, "Go to Home to share your card", Toast.LENGTH_SHORT).show();
            });
        }

        View menuBtn = navBar.findViewById(R.id.menu_button);
        if (menuBtn != null) {
            menuBtn.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                startActivity(intent);
            });
        }
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
                Toast.makeText(WeatherActivity.this, R.string.fetch_forecast_failed, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(WeatherActivity.this, R.string.fetch_weather_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateCurrentWeatherUI(WeatherData weatherData) {
        // Update the UI with the current weather data
        textViewTemperature.setText(getString(R.string.temp_format, Math.round(weatherData.getTemperature())));
        textViewHumidity.setText(getString(R.string.humidity_format, Math.round(weatherData.getHumidity())));
        textViewWind.setText(getString(R.string.wind_speed_format, Math.round(weatherData.getWindSpeed())));
        textViewFeelsLike.setText(getString(R.string.feels_like_format, Math.round(weatherData.getFeelsLike())));
        weatherDesc.setText(weatherData.getWeatherDescription()); // Set the weather description
    }

    private void updateForecastUI(List<ForecastItem> forecastItems) {
        // Update the UI with the forecast data (use RecyclerView and the ForecastAdapter)
        forecastAdapter.setForecastItems(forecastItems);
    }
}
