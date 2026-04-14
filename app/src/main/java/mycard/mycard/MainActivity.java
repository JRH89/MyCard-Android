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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import com.google.android.material.button.MaterialButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.google.android.material.card.MaterialCardView;
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

import org.json.JSONArray;
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

    Button editCard;





    private String cardUrl = "";
    private String userEmailStr = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        // textView = findViewById(R.id.user_details);
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        qrImageView = findViewById(R.id.qr_image);
        greeting = findViewById(R.id.greeting);
        weather = findViewById(R.id.temperature); // Initialize the weather TextView

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
            // textView.setText(user.getEmail());
        }
        final ImageView qrImageView = findViewById(R.id.qr_image);

        DocumentReference docRef = db.collection(user.getEmail()).document("userInfo");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String name = document.getString("name");

                        // Check if 'name' field exists and is not null
                        if (name != null && !name.isEmpty()) {
                            String[] names = name.split(" ");
                            String firstName = names[0];

                            // Capitalize the first letter of the first name
                            String capitalizedFirstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1);

                            greeting.setText(greetingText + ", " + capitalizedFirstName);
                        } else {
                            // Handle the case where 'name' field is missing or null
                            greeting.setText(greetingText); // Use the default greeting
                        }


                        String url = document.getString("short");
                        cardUrl = url;
                        // textView.setText(url);
                        // Linkify.addLinks(textView, Linkify.WEB_URLS); // Make the URL clickable

                        String qrImageData = document.getString("qrImage");
                        if (qrImageData != null) {
                            Bitmap qrBitmap = convertBase64ToBitmap(qrImageData);
                            qrImageView.setImageBitmap(qrBitmap);
                            // Rest of the code...
                        } else {
                            // Handle the case where 'qrImage' field is missing or null
                            // You can set a default image or hide the ImageView
                            qrImageView.setVisibility(View.GONE);
                        }
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
                        userEmailStr = userEmail;
                        String userLabel1 = document.getString("social1Label");
                        String userLink1 = document.getString("social1");
                        String userLabel2 = document.getString("social2Label");
                        String userLink2 = document.getString("social2");
                        String userLabel3 = document.getString("social3Label");
                        String userLink3 = document.getString("social3");
                        String userLabel4 = document.getString("social4Label");
                        String userLink4 = document.getString("social4");
                        String userTheme = document.getString("theme");

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

        MaterialCardView weatherWidget = findViewById(R.id.weatherWidget);
        weatherWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WeatherActivity.class);
                startActivity(intent);
            }
        });



        // MaterialButton editCardBtn = findViewById(R.id.editCard);
        // editCardBtn.setOnClickListener(new View.OnClickListener() {
        //     @Override
        //     public void onClick(View v) {
        //         Intent intent = new Intent(getApplicationContext(), EditActivity.class);
        //         startActivity(intent);
        //     }
        // });

        setupNavigation();
    }

    private void setupNavigation() {
        View navBar = findViewById(R.id.buttonRow);
        if (navBar == null) return;

        View homeBtn = navBar.findViewById(R.id.home_button);
        if (homeBtn != null) {
            homeBtn.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "You are at Home", Toast.LENGTH_SHORT).show();
            });
        }

        View editBtn = navBar.findViewById(R.id.edit_button);
        if (editBtn != null) {
            editBtn.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, EditActivity.class));
                } catch (Exception e) {
                    Toast.makeText(this, "Error starting EditActivity", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            });
        }

        View shareBtn = navBar.findViewById(R.id.share_button);
        if (shareBtn != null) {
            shareBtn.setOnClickListener(v -> {
                showShareDialog();
            });
        }

        View menuBtn = navBar.findViewById(R.id.menu_button);
        if (menuBtn != null) {
            menuBtn.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, MenuActivity.class));
                } catch (Exception e) {
                    Toast.makeText(this, "Error starting MenuActivity", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            });
        }
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
        String url = cardUrl; // textView.getText().toString();

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

    private void showShareDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_share_options, null);
        builder.setView(dialogView);

        final android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton shareLinkBtn = dialogView.findViewById(R.id.shareLink);
        MaterialButton shareQRCodeBtn = dialogView.findViewById(R.id.shareQRCode);
        MaterialButton shareEmailBtn = dialogView.findViewById(R.id.shareEmail);
        MaterialButton btnCancelShare = dialogView.findViewById(R.id.btnCancelShare);

        shareLinkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out my digital business card: " + cardUrl);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share via"));
                dialog.dismiss();
            }
        });

        shareQRCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendImage();
                dialog.dismiss();
            }
        });

        shareEmailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
                intent.putExtra(Intent.EXTRA_SUBJECT, "My Digital Business Card");
                intent.putExtra(Intent.EXTRA_TEXT, "Hi,\n\nYou can view my digital business card here: " + cardUrl);
                startActivity(Intent.createChooser(intent, "Send Email"));
                dialog.dismiss();
            }
        });

        btnCancelShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void fetchWeatherData(String city) {
        String apiKey = BuildConfig.OPEN_WEATHER_API_KEY;
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
