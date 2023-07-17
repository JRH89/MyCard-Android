package mycard.mycard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    Button button;
    TextView textView;
    FirebaseUser user;
    FirebaseFirestore db;

    TextView greeting;
    ImageView qrImageView;
    TextView weather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        button = findViewById(R.id.logout);
        textView = findViewById(R.id.user_details);
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        qrImageView = findViewById(R.id.qr_image);
        greeting = findViewById(R.id.greeting);
        weather = findViewById(R.id.weather);

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greetingText;
        if (hour >= 0 && hour < 12) {
            greetingText = "Good morning";
        } else if (hour >= 12 && hour < 16) {
            greetingText = "Good afternoon";
        } else if (hour >= 16 && hour < 18) {
            greetingText = "Good evening";
        } else {
            greetingText = "Good night";
        }

        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            textView.setText(user.getEmail());
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        DocumentReference docRef = db.collection(user.getEmail()).document("userInfo");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String name = document.getString("name");
                        String[] names = name.split(" ");
                        String firstName = names[0];

                        // Capitalize the first letter of the first name
                        String capitalizedFirstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1);

                        greeting.setText(greetingText + ", " + capitalizedFirstName);

                        String url = document.getString("short");
                        textView.setText(url);
                        Linkify.addLinks(textView, Linkify.WEB_URLS); // Make the URL clickable

                        String qrImageData = document.getString("qrImage");
                        // Convert the base64PNG data URL string to a Bitmap or load it directly into the ImageView
                        // Here's an example of converting the base64 string to a Bitmap
                        Bitmap qrBitmap = convertBase64ToBitmap(qrImageData);
                        qrImageView.setImageBitmap(qrBitmap);
                        qrImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        qrImageView.setAdjustViewBounds(true);
                        qrImageView.setMaxHeight(500);
                        qrImageView.setMaxWidth(500);

                        String city = document.getString("favoriteCity");


                        // Call method to fetch weather data
                        fetchWeatherData(city);

                    }
                } else {
                    // Handle any potential errors here
                    Exception e = task.getException();
                    if (e != null) {
                        e.printStackTrace();
                    }
                }
            }
        });

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = textView.getText().toString();
                openInBrowser(url);
            }
        });
    }

    private Bitmap convertBase64ToBitmap(String base64String) {
        // Remove the data URL prefix (e.g., "data:image/png;base64,")
        String imageData = base64String.substring(base64String.indexOf(",") + 1);

        byte[] decodedBytes = Base64.decode(imageData, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private void openInBrowser(String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void fetchWeatherData(String city) {
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

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                weather.setText(String.format(Locale.getDefault(), "%.1f", temperature) + "°F");
                            }
                        });
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

