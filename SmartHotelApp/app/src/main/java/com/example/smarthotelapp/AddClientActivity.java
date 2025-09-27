package com.example.smarthotelapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AddClientActivity extends AppCompatActivity {
    
    private static final String TAG = "AddClientActivity";
    
    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText phoneEditText;
    private TextInputEditText nationalIdEditText;
    private TextInputEditText emailEditText;
    private MaterialButton saveButton;
    private MaterialButton cancelButton;
    
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_client);
        
        initViews();
        setupToolbar();
        setupListeners();
        
        // Initialize Volley
        requestQueue = Volley.newRequestQueue(this);
    }
    
    private void initViews() {
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        nationalIdEditText = findViewById(R.id.nationalIdEditText);
        emailEditText = findViewById(R.id.emailEditText);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    private void setupListeners() {
        saveButton.setOnClickListener(v -> saveClient());
        cancelButton.setOnClickListener(v -> finish());
    }
    
    private void saveClient() {
        // Validate input
        if (!validateInput()) {
            return;
        }
        
        // Disable save button to prevent multiple submissions
        saveButton.setEnabled(false);
        saveButton.setText("جاري الحفظ...");
        
        // Prepare data
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String nationalId = nationalIdEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        
        String url = NetworkConfig.getAddClientUrl();
        Log.d(TAG, "Saving client to: " + url);
        
        StringRequest request = new StringRequest(
            Request.Method.POST,
            url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");
                        
                        if (success) {
                            Log.d(TAG, "Client saved successfully");
                            Toast.makeText(AddClientActivity.this, "تم حفظ العميل بنجاح", Toast.LENGTH_SHORT).show();
                            finish(); // Close activity and return to previous screen
                        } else {
                            Log.e(TAG, "Failed to save client: " + message);
                            showError("فشل في حفظ العميل: " + message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        showError("خطأ في معالجة الاستجابة");
                    } finally {
                        // Re-enable save button
                        saveButton.setEnabled(true);
                        saveButton.setText("حفظ");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Network error: " + error.getMessage());
                    showError("خطأ في الاتصال بالخادم");
                    
                    // Re-enable save button
                    saveButton.setEnabled(true);
                    saveButton.setText("حفظ");
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("first_name", firstName);
                params.put("last_name", lastName);
                params.put("phone", phone);
                params.put("national_id", nationalId);
                params.put("email", email);
                return params;
            }
        };
        
        requestQueue.add(request);
    }
    
    private boolean validateInput() {
        // Reset errors
        firstNameEditText.setError(null);
        lastNameEditText.setError(null);
        phoneEditText.setError(null);
        
        boolean isValid = true;
        
        // Validate first name
        if (TextUtils.isEmpty(firstNameEditText.getText())) {
            firstNameEditText.setError("الاسم الأول مطلوب");
            isValid = false;
        }
        
        // Validate last name
        if (TextUtils.isEmpty(lastNameEditText.getText())) {
            lastNameEditText.setError("الاسم الأخير مطلوب");
            isValid = false;
        }
        
        // Validate phone
        String phone = phoneEditText.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            phoneEditText.setError("رقم الجوال مطلوب");
            isValid = false;
        } else if (phone.length() < 9) {
            phoneEditText.setError("رقم الجوال غير صحيح");
            isValid = false;
        }
        
        // Validate email if provided
        String email = emailEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("البريد الإلكتروني غير صحيح");
            isValid = false;
        }
        
        return isValid;
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}
