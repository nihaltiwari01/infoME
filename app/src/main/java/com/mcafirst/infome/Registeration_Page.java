package com.mcafirst.infome;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Registeration_Page extends AppCompatActivity {

    public static final String EXTRA_ROLL_NUMBER = "ROLL_NUMBER";
    TextInputEditText enterPassword;
    TextInputEditText confirmPassword;
    MaterialButton submitButton;
    private String receivedEmailID; // This will hold the email (rollNumber + "nitm.ac.in")
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registeration_page);

        submitButton = findViewById(R.id.registrationButton);
        enterPassword = findViewById(R.id.registrationEnterPassword);
        confirmPassword = findViewById(R.id.registrationConfirmPassword);
        progressBar = findViewById(R.id.registrationProgressBar);

        mAuth = FirebaseAuth.getInstance();

        Intent intent = getIntent();
        receivedEmailID = null;
        if (intent != null && intent.hasExtra(EXTRA_ROLL_NUMBER)) {
            receivedEmailID = intent.getStringExtra(EXTRA_ROLL_NUMBER);
        } else {
            Toast.makeText(this, "Error: Could not get user details " , Toast.LENGTH_SHORT).show();
        }

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

    });
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // Handle back arrow click in toolbar
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
//                onBackPressed();
                // Call dispatcher (modern replacement of onBackPressed)
//                getOnBackPressedDispatcher().onBackPressed();
                finish();
            }

        });
    }

    private void registerUser() {
        if (receivedEmailID == null || receivedEmailID.isEmpty()) {
            Toast.makeText(this, "Error: Email ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        String password = enterPassword.getText().toString().trim();
        String confirmPass = confirmPassword.getText().toString().trim();
        // --- Validations ---
        if (TextUtils.isEmpty(password)) {
            enterPassword.setError("Password is required.");
            enterPassword.requestFocus();
            return;
        }

        if (password.length() < 6) { // Firebase default minimum password length
            enterPassword.setError("Password must be at least 6 characters.");
            enterPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPass)) {
            confirmPassword.setError("Confirm password is required.");
            confirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPass)) {
            confirmPassword.setError("Passwords do not match.");
            confirmPassword.requestFocus();
            // Clear the confirm password field for convenience
            confirmPassword.setText("");
            return;
        }
        submitButton.setEnabled(false); // Prevent multiple clicks
        mAuth.createUserWithEmailAndPassword(receivedEmailID, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                submitButton.setEnabled(true);
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    Toast.makeText(Registeration_Page.this, "Registration successful.",
                            Toast.LENGTH_SHORT).show();
                    String userName = "";
//                    upload user data
                    uploadUserData(receivedEmailID, userName);

                    if (user != null) {
                        user.sendEmailVerification()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "Verification email sent.");
                                            Toast.makeText(Registeration_Page.this,
                                                    "Verification email sent to " + user.getEmail(),
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            Log.e(TAG, "sendEmailVerification", task.getException());
                                            Toast.makeText(Registeration_Page.this,
                                                    "Failed to send verification email.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                    // Navigate to the next activity (e.g., Main Activity or Login Page)
                     Intent nextActivityIntent = new Intent(Registeration_Page.this, Login_Page.class);
                     startActivity(nextActivityIntent);
                     finish(); // Close this registration activity
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.getException());
                    Toast.makeText(Registeration_Page.this, "Authentication failed: " +
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error") , Toast.LENGTH_LONG).show();
                }
            }
        });


    }

    private void uploadUserData(String receivedEmailID, String userName) {

    }
}