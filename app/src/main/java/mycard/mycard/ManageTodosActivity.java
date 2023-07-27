package mycard.mycard;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
        Button addButton = findViewById(R.id.btnAddTodo);
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
                    if (document.exists()) {
                        // Document exists, fetch the todos and update the list
                        Object todosObject = document.get("todos");
                        if (todosObject instanceof List<?>) {
                            todos.clear();
                            todos.addAll((List<String>) todosObject);
                            todoAdapter.notifyDataSetChanged();
                        }
                    } else {
                        // Document does not exist
                    }
                } else {
                    // Error fetching document
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
    }

// Call this method whenever a todo is added or deleted in the activity
// For example, after adding a todo to Firestore, call sendWidgetUpdateBroadcast()
// And after deleting a todo from Firestore, call sendWidgetUpdateBroadcast()



    private void addTodoToFirestore(String newTodo) {
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
                    if (document.exists()) {
                        // Document exists, fetch the todos and update the list
                        Object todosObject = document.get("todos");
                        if (todosObject instanceof List<?>) {
                            List<String> todos = (List<String>) todosObject;
                            todos.add(newTodo);
                            userDocRef.update("todos", todos)
                                    .addOnSuccessListener(aVoid -> {
                                        // Update successful
                                        fetchTodosFromFirestore();
                                        sendWidgetUpdateBroadcast();// Refresh the list to show the added todo
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle the error here
                                    });
                        }
                    } else {
                        // Document does not exist, create a new document and add the todo
                        List<String> newTodos = new ArrayList<>();
                        newTodos.add(newTodo);
                        userDocRef.set(newTodos)
                                .addOnSuccessListener(aVoid -> {
                                    // Todo added successfully
                                    fetchTodosFromFirestore();
                                    sendWidgetUpdateBroadcast();// Refresh the list to show the added todo
                                })
                                .addOnFailureListener(e -> {
                                    // Handle the error here
                                });
                    }
                } else {
                    // Error fetching document
                }
            });
        }

    }

    private void deleteTodoFromFirestore(String todo) {
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
                    if (document.exists()) {
                        // Document exists, fetch the todos and update the list
                        Object todosObject = document.get("todos");
                        if (todosObject instanceof List<?>) {
                            List<String> todos = (List<String>) todosObject;
                            todos.remove(todo);
                            userDocRef.update("todos", todos)
                                    .addOnSuccessListener(aVoid -> {
                                        // Update successful
                                        fetchTodosFromFirestore();
                                        sendWidgetUpdateBroadcast();// Refresh the list to show the updated todos
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle the error here
                                    });
                        }
                    } else {
                        // Document does not exist, nothing to delete
                    }
                } else {
                    // Error fetching document
                }
            });
        }

    }

}