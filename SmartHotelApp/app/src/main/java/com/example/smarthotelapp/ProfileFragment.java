package com.example.smarthotelapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    // Views
    private CircleImageView profileImage;
    private ImageView editProfileIcon;
    private TextView userNameTextView;
    private LinearLayout optionsContainer;
    private LinearLayout employeeInfoContainer;
    private RelativeLayout viewProfileButton;
    private RelativeLayout contactSupportButton;
    private RelativeLayout settingsButton;
    private RelativeLayout logoutButton;
    private ImageView backButton;

    // Info TextViews
    private TextView infoUserId, infoUsername, infoFullName, infoAssignedSection, infoPhoneNumber;

    // Session
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize SessionManager
        sessionManager = new SessionManager(requireContext());

        // Initialize Views
        profileImage = view.findViewById(R.id.profile_image);
        editProfileIcon = view.findViewById(R.id.edit_profile_icon);
        userNameTextView = view.findViewById(R.id.user_name);
        optionsContainer = view.findViewById(R.id.options_container);
        employeeInfoContainer = view.findViewById(R.id.employee_info_container);
        viewProfileButton = view.findViewById(R.id.option_edit_profile); // This is now the "View Profile" button
        contactSupportButton = view.findViewById(R.id.option_contact_support);
        settingsButton = view.findViewById(R.id.option_settings);
        logoutButton = view.findViewById(R.id.option_logout);
        backButton = view.findViewById(R.id.back_button);

        // Initialize Info TextViews
        infoUserId = view.findViewById(R.id.info_user_id);
        infoUsername = view.findViewById(R.id.info_username);
        infoFullName = view.findViewById(R.id.info_full_name);
        infoAssignedSection = view.findViewById(R.id.info_assigned_section);
        infoPhoneNumber = view.findViewById(R.id.info_phone_number);

        // Set user name from session
        userNameTextView.setText(sessionManager.getUserName());

        // Set click listeners
        editProfileIcon.setOnClickListener(v -> openGallery());
        viewProfileButton.setOnClickListener(v -> showEmployeeInfo());
        backButton.setOnClickListener(v -> showOptions());
        contactSupportButton.setOnClickListener(v -> showContactSupportDialog());
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SettingsActivity.class);
            startActivity(intent);
        });
        logoutButton.setOnClickListener(v -> showLogoutConfirm());
    }

    private void showEmployeeInfo() {
        // Hide options and show info
        optionsContainer.setVisibility(View.GONE);
        employeeInfoContainer.setVisibility(View.VISIBLE);

        // Populate data
        HashMap<String, String> userDetails = sessionManager.getUserDetails();
        infoUserId.setVisibility(View.GONE); // Hide the User ID
        infoUsername.setText("Username: " + userDetails.get("username"));
        infoFullName.setText("Full Name: " + userDetails.get("full_name"));
        infoAssignedSection.setText("Assigned Section: " + userDetails.get("assigned_section"));
        // Assuming phone number is stored in session. We will add this functionality next.
        infoPhoneNumber.setText("Phone: " + userDetails.get("phone_number"));
    }

    private void showOptions() {
        // Hide info and show options
        employeeInfoContainer.setVisibility(View.GONE);
        optionsContainer.setVisibility(View.VISIBLE);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void showContactSupportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_contact_support, null);
        builder.setView(dialogView);

        // Get views from dialog layout
        TextView employeeNameText = dialogView.findViewById(R.id.employee_name_text);
        TextView employeeSectionText = dialogView.findViewById(R.id.employee_section_text);
        EditText subjectEditText = dialogView.findViewById(R.id.subject_edit_text);
        EditText messageEditText = dialogView.findViewById(R.id.message_edit_text);
        Button sendButton = dialogView.findViewById(R.id.send_button);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);

        // Get user details from session and populate the fields
        HashMap<String, String> userDetails = sessionManager.getUserDetails();
        String fullName = userDetails.get("full_name");
        String assignedSection = userDetails.get("assigned_section");

        employeeNameText.setText(fullName != null ? fullName : "N/A");
        employeeSectionText.setText(assignedSection != null ? assignedSection : "N/A");

        AlertDialog dialog = builder.create();

        sendButton.setOnClickListener(v -> {
            String subject = subjectEditText.getText().toString().trim();
            String message = messageEditText.getText().toString().trim();

            if (subject.isEmpty()) {
                subjectEditText.setError("Subject is required");
                return;
            }
            if (message.isEmpty()) {
                messageEditText.setError("Message is required");
                return;
            }

            String employeeId = sessionManager.getUserId();
            sendSupportRequest(employeeId, fullName, assignedSection, subject, message, dialog);
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
    }

    private void sendSupportRequest(final String employeeId, final String employeeName, final String employeeSection, final String subject, final String message, final AlertDialog dialog) {
        String url = NetworkConfig.getSubmitSupportRequestUrl();
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.getString("status");
                        String responseMessage = jsonResponse.getString("message");

                        if ("success".equals(status)) {
                            Toast.makeText(getContext(), responseMessage, Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Error: " + responseMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getContext(), "Error parsing server response.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getContext(), "Network Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("employee_id", employeeId);
                params.put("employee_name", employeeName);
                params.put("employee_section", employeeSection);
                params.put("subject", subject);
                params.put("message", message);
                return params;
            }
        };

        queue.add(stringRequest);
    }

    private void showLogoutConfirm() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (d, which) -> {
                    sessionManager.logoutUser();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .show();
    }
}
