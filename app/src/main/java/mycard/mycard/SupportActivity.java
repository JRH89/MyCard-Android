package mycard.mycard;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class SupportActivity extends AppCompatActivity {
    private CloseableHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        httpClient = HttpClients.createDefault();

        Button buttonSend = findViewById(R.id.sendButton);
        buttonSend.setOnClickListener(v -> sendEmail());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Close the httpClient in onDestroy to release resources
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendEmail() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            SendGrid sendGrid = new SendGrid("");
            Email from = new Email("hookerhillstudios@gmail.com");
            String subject = "Test Email";
            Email to = new Email("mycarddigitalbusinesscards@gmail.com");
            Content content = new Content("text/plain", "Hello, this is a test email sent from SendGrid!");
            Mail mail = new Mail(from, subject, to, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            try {
                request.setBody(mail.build());
            } catch (IOException e) {
                e.printStackTrace();
                showResultOnMainThread(false);
                return;
            }

            try {
                Response response = sendGrid.api(request);
                boolean success = response.getStatusCode() >= 200 && response.getStatusCode() < 300;
                showResultOnMainThread(success);
            } catch (IOException e) {
                e.printStackTrace();
                showResultOnMainThread(false);
            }
        });
    }

    private void showResultOnMainThread(final boolean success) {
        runOnUiThread(() -> {
            if (success) {
                // Email sent successfully
                showToast("Email sent successfully! We will process your request as soon as possible.");
            } else {
                // Handle email sending failure
                showToast("Failed to send email. Please try again later.");
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
