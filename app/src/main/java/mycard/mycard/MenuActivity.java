package mycard.mycard;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.appcompat.app.AlertDialog;

public class MenuActivity extends AppCompatActivity {

    Button buttonEditCard;
    Button buttonLogout;
    Button buttonCloseMenu;
    FirebaseAuth auth;
    FirebaseUser user;
    Button buttonDelete;

    Button buttonSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        buttonLogout = findViewById(R.id.logout);
        buttonLogout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });

        buttonDelete = findViewById(R.id.deleteAcct);
        buttonDelete.setOnClickListener(view -> {
            showDeleteAccountConfirmationDialog();
        });

        buttonEditCard = findViewById(R.id.editCard);
        buttonEditCard.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), EditActivity.class);
            startActivity(intent);
        });

        buttonCloseMenu = findViewById(R.id.btnCloseMenu);
        buttonCloseMenu.setOnClickListener(view -> onBackPressed());

        buttonSupport = findViewById(R.id.support);
        buttonSupport.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), SupportActivity.class);
            startActivity(intent);
        });

    }

    private void showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Account Deletion");
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteAccount();
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteAccount() {
        if (user != null) {
            user.delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Account deletion successful
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(getApplicationContext(), Login.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Account deletion failed
                            // Display an error message or handle the error as needed
                        }
                    });
        }
    }
}
