package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Application;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class User {

    private String email;
    private String role;

    public User(String email, String role) throws ExecutionException, InterruptedException {
        this.email = email;
        this.role = role;

        DocumentReference docRef = Application.db.collection("users").document(email.toString());
        Map<String, Object> data = new HashMap<>();
        data.put("Email", email);
        data.put("Role", role);
        //asynchronously write data
        ApiFuture<WriteResult> result = Application.db.collection("users").document(email.toString()).set(data);
        result.get();
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isManager() {
        return this.role != null && this.role.equals("manager");
    }
}
