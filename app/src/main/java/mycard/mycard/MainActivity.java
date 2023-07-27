package mycard.mycard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;

import org.json.JSONArray;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;

    Button button;
    TextView textView;
    FirebaseUser user;
    FirebaseFirestore db;
    TextView greeting;
    ImageView qrImageView;
    TextView weather;
    WebView cardPreview;

    Button editCard;
    Button editTodos;

    Button menuButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        textView = findViewById(R.id.user_details);
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        qrImageView = findViewById(R.id.qr_image);
        greeting = findViewById(R.id.greeting);
        weather = findViewById(R.id.temperature); // Initialize the weather TextView
        cardPreview = findViewById(R.id.cardPreview);
        editCard = findViewById(R.id.editCard);
        editTodos = findViewById(R.id.manageTodos);
        menuButton = findViewById(R.id.showMenuButton);
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greetingText;
        if (hour >= 0 && hour < 12) {
            greetingText = "Good morning";
        } else if (hour >= 12 && hour < 18) {
            greetingText = "Good afternoon";
        } else if (hour >= 18 && hour < 20) {
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
        TextView clickToShowQrCode = findViewById(R.id.click_to_show_qr_code);
        final ImageView qrImageView = findViewById(R.id.qr_image);

        final boolean[] qrCodeAndUrlVisible = {false}; // Flag to track the visibility state

        clickToShowQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toggle the visibility of the QR code ImageView and URL TextView
                if (!qrCodeAndUrlVisible[0]) {
                    cardPreview.setVisibility(View.VISIBLE);
                    qrImageView.setVisibility(View.GONE); // Show the QR code
                    textView.setVisibility(View.GONE); // Show the URL
                    clickToShowQrCode.setText("Hide Card Preview"); // Update button text
                } else {
                    cardPreview.setVisibility(View.GONE);
                    qrImageView.setVisibility(View.VISIBLE); // Show the QR code
                    textView.setVisibility(View.VISIBLE);// Hide the URL
                    clickToShowQrCode.setText("Show Card Preview"); // Update button text
                }
                qrCodeAndUrlVisible[0] = !qrCodeAndUrlVisible[0]; // Toggle the flag
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

                        Bitmap qrBitmap = convertBase64ToBitmap(qrImageData);
                        qrImageView.setImageBitmap(qrBitmap);
                        qrImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        qrImageView.setAdjustViewBounds(true);
                        qrImageView.setMaxHeight(500);
                        qrImageView.setMaxWidth(500);

                        String city = document.getString("favoriteCity");

                      fetchWeatherData(city);
                        String fullName = document.getString("name");
                        String userJob = document.getString("jobTitle");
                        String userPhone = document.getString("phone");
                        String userEmail = document.getString("email");
                        String userLabel1 = document.getString("social1Label");
                        String userLink1 = document.getString("social1");
                        String userLabel2 = document.getString("social2Label");
                        String userLink2 = document.getString("social2");
                        String userLabel3 = document.getString("social3Label");
                        String userLink3 = document.getString("social3");
                        String userLabel4 = document.getString("social4Label");
                        String userLink4 = document.getString("social4");
                        String userTheme = document.getString("theme");

                        try {
                        String encodedFullName = URLEncoder.encode(fullName, "UTF-8");
                        String encodedUserJob = URLEncoder.encode(userJob, "UTF-8");
                        String encodedUserPhone = URLEncoder.encode(userPhone, "UTF-8");
                        String encodedUserEmail = URLEncoder.encode(userEmail, "UTF-8");
                        String encodedUserLabel1 = URLEncoder.encode(userLabel1, "UTF-8");
                        String encodedUserLink1 = URLEncoder.encode(userLink1, "UTF-8");
                        String encodedUserLabel2 = URLEncoder.encode(userLabel2, "UTF-8");
                        String encodedUserLink2 = URLEncoder.encode(userLink2, "UTF-8");
                        String encodedUserLabel3 = URLEncoder.encode(userLabel3, "UTF-8");
                        String encodedUserLink3 = URLEncoder.encode(userLink3, "UTF-8");
                        String encodedUserLabel4 = URLEncoder.encode(userLabel4, "UTF-8");
                        String encodedUserLink4 = URLEncoder.encode(userLink4, "UTF-8");
                        String encodedUserTheme = URLEncoder.encode(userTheme, "UTF-8");

                            String baseUrl = "https://have-mycard.vercel.app";
                            String dynamicUrl = String.format("%s/%s/?name=%s&job=%s&phone=%s&email=%s&social1Label=%s&social1=%s&social2Label=%s&social2=%s&social3Label=%s&social3=%s&social4Label=%s&social4=%s",
                                    baseUrl, encodedUserTheme, encodedFullName, encodedUserJob, encodedUserPhone, encodedUserEmail, encodedUserLabel1, encodedUserLink1, encodedUserLabel2, encodedUserLink2, encodedUserLabel3, encodedUserLink3, encodedUserLabel4, encodedUserLink4);

                            Log.d("LongURL", dynamicUrl);

                            cardPreview.getSettings().setJavaScriptEnabled(true);
                            cardPreview.setWebViewClient(new WebViewClient());
                            cardPreview.loadUrl(dynamicUrl);

                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                    }
                } else {
                    // Handle any potential errors
                    Exception e = task.getException();
                    if (e != null) {
                        e.printStackTrace();
                    }
                }
            }
        });

        LinearLayout weatherWidget = findViewById(R.id.weatherWidget);
        weatherWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WeatherActivity.class);
                startActivity(intent);
            }
        });

        Button hamburgerButton = findViewById(R.id.hamburger_button);
        hamburgerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                startActivity(intent);
            }
        });
        LinearLayout buttonRow = findViewById(R.id.buttonRow);
        final boolean[] buttonRowVisible = {false};

        menuButton.setOnClickListener(new View.OnClickListener() {

            String myBlue = "#FF039BE5";
            String myRed = "#FF0660" ;

            String myYellow = "#D4E157";

            int myBlueColor = Color.parseColor(myBlue);
            int myRedColor = Color.parseColor(myRed);
            int myYellowColor = Color.parseColor(myYellow);
            @Override
            public void onClick(View v) {
                if (!buttonRowVisible[0]) {
                    // Show the button row and update the text of the menuButton
                    buttonRow.setVisibility(View.VISIBLE);

                    buttonRowVisible[0] = true;
                } else {
                    // Hide the button row and update the text of the menuButton
                    buttonRow.setVisibility(View.GONE);

                    buttonRowVisible[0] = false;
                }
            }
        });

        editTodos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ManageTodosActivity.class);
                startActivity(intent);
            }
        });

        editCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), EditActivity.class);
                startActivity(intent);
            }
        });

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = textView.getText().toString();
                openInBrowser(url);
            }
        });
        registerForContextMenu(qrImageView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.qr_image_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_save) {
            // Handle the "Save Image" option here
            saveImageToGallery();
            return true;
        } else if (itemId == R.id.menu_send) {
            // Handle the "Send Image" option here
            sendImage();
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    private void saveImageToGallery() {
        // Get the current bitmap from qrImageView
        qrImageView.setDrawingCacheEnabled(true);
        Bitmap qrBitmap = Bitmap.createBitmap(qrImageView.getDrawingCache());
        qrImageView.setDrawingCacheEnabled(false);

        // Save the bitmap to the device's gallery
        String displayName = "QR_Code_Image"; // Set a display name for the image
        String mimeType = "image/png"; // Set the image MIME type (change if needed)
        String uriString = MediaStore.Images.Media.insertImage(
                getContentResolver(), qrBitmap, displayName, null
        );

        if (uriString != null) {
            // Image saved successfully
            Uri imageUri = Uri.parse(uriString);
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } else {
            // Failed to save the image
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendImage() {
        // Get the current bitmap from qrImageView
        qrImageView.setDrawingCacheEnabled(true);
        Bitmap qrBitmap = Bitmap.createBitmap(qrImageView.getDrawingCache());
        qrImageView.setDrawingCacheEnabled(false);

        // Get the URL to be included in the message
        String url = textView.getText().toString();

        // Create an intent to send the image
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("image/png"); // Set the image MIME type (change if needed)
        String message = "My Card: " + url; // Include the URL in the message
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.putExtra(Intent.EXTRA_STREAM, getBitmapUri(qrBitmap));

        // Check if there's an app to handle the intent
        if (sendIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(sendIntent, "Send QR Code"));
        } else {
            Toast.makeText(this, "No app found to handle the intent", Toast.LENGTH_SHORT).show();
        }
    }


    // Helper method to get the Uri of the image from the Bitmap
    private Uri getBitmapUri(Bitmap bitmap) {
        File imageFile = new File(getExternalCacheDir(), "qr_code_image.png");
        try {
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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

                        // Get the weather description and humidity
                        JSONArray weatherArray = json.getJSONArray("weather");
                        if (weatherArray.length() > 0) {
                            JSONObject weatherObject = weatherArray.getJSONObject(0);
                            String weatherDescription = weatherObject.getString("description");
                            double humidity = mainObject.getDouble("humidity");

                            // Update the weather description and humidity in the UI
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Update the weather description in the description TextView
                                    TextView descriptionTextView = findViewById(R.id.description);
                                    descriptionTextView.setText(weatherDescription);

                                    TextView favoriteCityTextView = findViewById(R.id.favoriteCity);
                                    favoriteCityTextView.setText(city);

                                    // Update the humidity value in the humidity TextView
                                    TextView humidityTextView = findViewById(R.id.humidity);
                                    humidityTextView.setText(+ Math.round(humidity) + "%");

                                    // Update the temperature in the UI (as you did before)
                                    weather.setText(+ Math.round(temperature) + "°F");
                                }
                            });
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
