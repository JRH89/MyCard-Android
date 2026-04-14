package mycard.mycard;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import mycard.mycard.workers.ToDoUpdateWorker;
import mycard.mycard.workers.CombinedUpdateWorker;

public class ManageTodosActivity extends AppCompatActivity {

    private List<String> todos;
    private ArrayAdapter<String> todoAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_todos);

        todos = new ArrayList<>();

        // Initialize the ListView and ArrayAdapter
        ListView listView = findViewById(R.id.listViewTodos);
        todoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, todos);
        listView.setAdapter(todoAdapter);

        // Handle item click to delete the todo
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String todo = todos.get(position);
                deleteTodoFromFirestore(todo);
            }
        });

        // Handle button click to add a new todo
        MaterialButton addButton = findViewById(R.id.btnAddTodo);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = findViewById(R.id.etNewTodo);
                String newTodo = editText.getText().toString();
                if (!newTodo.isEmpty()) {
                    addTodoToFirestore(newTodo);
                    editText.getText().clear();
                }
            }
        });

        // Fetch todos from Firestore
        fetchTodosFromFirestore();

        setupNavigation();
    }

    private void setupNavigation() {
        View navBar = findViewById(R.id.buttonRow);
        if (navBar == null) return;

        View homeBtn = navBar.findViewById(R.id.home_button);
        if (homeBtn != null) {
            homeBtn.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
            });
        }

        View editBtn = navBar.findViewById(R.id.edit_button);
        if (editBtn != null) {
            editBtn.setOnClickListener(v -> {
                startActivity(new Intent(this, EditActivity.class));
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
                startActivity(new Intent(this, MenuActivity.class));
            });
        }
    }

    private void fetchTodosFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // Check if the user is logged in
        if (currentUser != null) {
            // Get the email of the current user
            String userEmail = currentUser.getEmail();

            // Access Firestore instance and get the document reference for the user's data
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userDocRef = db.collection(userEmail).document("ToDo");

            // Fetch the data from the document
            userDocRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        // Document exists, fetch the todos and update the list
                        Object todosObject = document.get("todos");
                        if (todosObject instanceof List<?>) {
                            todos.clear();
                            todos.addAll((List<String>) todosObject);
                            todoAdapter.notifyDataSetChanged();
                        } else {
                            // todos field is missing or not a list, but document exists
                            todos.clear();
                            todoAdapter.notifyDataSetChanged();
                        }
                    } else {
                        // Document does not exist (no todos yet)
                        todos.clear();
                        todoAdapter.notifyDataSetChanged();
                    }
                } else {
                    // Error fetching document
                    Log.e("ManageTodosActivity", "Error fetching todos", task.getException());
                }
            });
        }
    }

    private void sendWidgetUpdateBroadcast() {
        Intent intent = new Intent(this, ToDoWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), ToDoWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);

        // Trigger an immediate update via WorkManager
        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(ToDoUpdateWorker.class).build();
        WorkManager.getInstance(this).enqueue(updateRequest);

        OneTimeWorkRequest combinedUpdateRequest = new OneTimeWorkRequest.Builder(CombinedUpdateWorker.class).build();
        WorkManager.getInstance(this).enqueue(combinedUpdateRequest);
    }

// Call this method whenever a todo is added or deleted in the activity
// For example, after adding a todo to Firestore, call sendWidgetUpdateBroadcast()
// And after deleting a todo from Firestore, call sendWidgetUpdateBroadcast()



    private void addTodoToFirestore(String newTodo) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userDocRef = db.collection(userEmail).document("ToDo");

            userDocRef.update("todos", com.google.firebase.firestore.FieldValue.arrayUnion(newTodo))
                    .addOnSuccessListener(aVoid -> {
                        fetchTodosFromFirestore();
                        sendWidgetUpdateBroadcast();
                    })
                    .addOnFailureListener(e -> {
                        // If document doesn't exist, create it
                        java.util.Map<String, Object> data = new java.util.HashMap<>();
                        java.util.List<String> newTodos = new java.util.ArrayList<>();
                        newTodos.add(newTodo);
                        data.put("todos", newTodos);
                        userDocRef.set(data).addOnSuccessListener(aVoid2 -> {
                            fetchTodosFromFirestore();
                            sendWidgetUpdateBroadcast();
                        });
                    });
        }
    }

    private void deleteTodoFromFirestore(String todo) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userDocRef = db.collection(userEmail).document("ToDo");

            userDocRef.update("todos", com.google.firebase.firestore.FieldValue.arrayRemove(todo))
                    .addOnSuccessListener(aVoid -> {
                        fetchTodosFromFirestore();
                        sendWidgetUpdateBroadcast();
                    });
        }
    }

}