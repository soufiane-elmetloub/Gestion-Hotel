package com.example.smarthotelapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private RelativeLayout loginButton;
    private ImageView togglePassword;

    private SessionManager sessionManager;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure window to handle transparent status bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_login);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            startMainActivity();
            finish();
            return;
        }

        // Initialize views
        usernameEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        togglePassword = findViewById(R.id.togglePassword);


        // Set click listeners
        togglePassword.setOnClickListener(v -> {
            // Check the current input type
            if (passwordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // If password is hidden, show it
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                // If password is shown, hide it
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye);
            }
            // Move cursor to the end
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }
            
            performLogin();
        });
        

    }

    private void performLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        loginButton.setEnabled(false);

        String url = NetworkConfig.getLoginUrl();

        // Create JSON object for request
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("username", username);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            Log.e(TAG, "JSON Exception: " + e.getMessage());
            return;
        }

        // Create request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    loginButton.setEnabled(true);
                    try {
                        boolean success = response.getBoolean("success");
                        String message = response.getString("message");

                        if (success) {
                            // Parse user data
                            JSONObject userData = response.getJSONObject("user");
                            String userId = userData.getString("id");
                            String userName = userData.getString("username");
                            String firstName = userData.getString("first_name");
                            String lastName = userData.getString("last_name");
                            String fullName = firstName + " " + lastName;
                            String assignedSection = userData.optString("assigned_section", "غير محدد");
                            String phoneNumber = userData.optString("phone_number", "N/A");

                            // Save session
                            sessionManager.createLoginSession(userId, userName, fullName, phoneNumber, assignedSection);

                            // Navigate to main activity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        Toast.makeText(LoginActivity.this, "Response parsing error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loginButton.setEnabled(true);
                    Log.e(TAG, "Login error: " + error.toString());
                    
                    String errorMessage = "Connection error";
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        errorMessage = "Server error (code: " + statusCode + ")";
                    } else if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                        if (error.getMessage().contains("timeout")) {
                            errorMessage = "Connection timeout - check your network";
                        } else if (error.getMessage().contains("Unable to resolve host")) {
                            errorMessage = "Cannot reach server - check IP address";
                        } else {
                            errorMessage = "Network error: " + error.getMessage();
                        }
                    }
                    
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });

        // Add to request queue
        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }



    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
    }
}
