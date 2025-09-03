package com.mcafirst.infome;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login_Page extends AppCompatActivity {
    private static final String TAG = "LoginPage";

    // UI Elements for Login
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar; // Optional

    // Other UI Elements
    private Button registerBtn;
    private TextView forgetPassBtn;

    // Firebase
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        // Initialize Login UI elements
        emailEditText = findViewById(R.id.login_email_EditText); // Replace with your Email EditText ID
        passwordEditText = findViewById(R.id.login_password_EditText); // Replace with your Password EditText ID
        loginButton = findViewById(R.id.login_loginBtn); // Replace with your Login Button ID
        progressBar = findViewById(R.id.login_progressBar); // Optional: Replace with your ProgressBar ID

        // Initialize other UI elements
        registerBtn = findViewById(R.id.login_registerBtn);
        forgetPassBtn = findViewById(R.id.login_forget_text);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
        registerBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Login_Page.this, Verification_Page.class);
                    startActivity(intent);
                }
            });
        forgetPassBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent =  new Intent(Login_Page.this, Forgot_Password_Page.class);
                    startActivity(intent);
                }
            });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, navigate to main activity

            if (currentUser.isEmailVerified()) {
                Toast.makeText(Login_Page.this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                // Unlock features or navigate
                navigateToMainActivity(); // or remove verification prompts
            } else {
                userVerification(currentUser);
                Toast.makeText(Login_Page.this, "Email still not verified. Please check your email or resend.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(Login_Page.this, MainActivity.class); // Replace MainActivity with your target activity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
        finish(); // Close LoginActivity
    }

    @OptIn(markerClass = UnstableApi.class)
    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // --- Validations ---
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required.");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address.");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required.");
            passwordEditText.requestFocus();
            return;
        }
        // --- End Validations ---

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false); // Prevent multiple clicks

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Sign in success
//                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(Login_Page.this, "Login Successful.",
                                Toast.LENGTH_SHORT).show();

                        // Optional: Check if email is verified if you implemented email verification
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                // Email is verified, proceed to main app
                                Toast.makeText(Login_Page.this, "Login Successful.", Toast.LENGTH_SHORT).show();
                                navigateToMainActivity();
                            } else {
                                // Email is NOT verified
                                Toast.makeText(Login_Page.this, "Please verify your email address to continue.", Toast.LENGTH_LONG).show();
                                Log.w(TAG, "Login attempt with unverified email: " + user.getEmail());
                                mAuth.signOut();
                                userVerification(user);


                                // Option B: Sign the user out to force them to verify before trying again
                                // Uncomment if you want to sign them out immediately

                                // Keep them on the login page or a dedicated "please verify" page.
                                // Do NOT call navigateToMainActivity() here.
                                // You might want to update the UI to show a "Resend Email" button.
                            }
                        } else {
                            // This case should ideally not happen if task.isSuccessful() is true
                            Toast.makeText(Login_Page.this, "Login failed: User data not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Login failed (wrong password, user not found, etc.)
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(Login_Page.this, "Authentication failed: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();

// Reset progress bar and button state
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);
                    }
                });
    }

//    private void userVerification(FirebaseUser user) {
//        // Option A: Send verification email again (in case they lost the first one)
//        user.sendEmailVerification()
//                .addOnCompleteListener(task1 -> {
//                    if (task1.isSuccessful()) {
//                        Toast.makeText(Login_Page.this,
//                                "Verification email sent to " + user.getEmail() + ". Please check your inbox and spam folder.",
//                                Toast.LENGTH_LONG).show();
//
//                        try {
//                            Intent intent = new Intent(Intent.ACTION_MAIN);
//                            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
//                            // Optionally, try to narrow down to Gmail if specifically desired,
//                            // but this is not guaranteed to work on all devices or open the inbox directly.
//
//                            // A more generic approach is better:
//                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Ensures it opens in a new task
//
//                            if (intent.resolveActivity(getPackageManager()) != null) {
//                                startActivity(intent);
//                            } else {
//                                Toast.makeText(Login_Page.this, "No email app found.", Toast.LENGTH_SHORT).show();
//                            }
//                        } catch (android.content.ActivityNotFoundException ex) {
//                            Toast.makeText(Login_Page.this, "No email app found.", Toast.LENGTH_SHORT).show();
//                        }
//                    } else {
////                        Log.e(TAG, "sendEmailVerification failed", task1.getException());
//                        Toast.makeText(Login_Page.this,
//                                "Failed to send verification email. Try logging in again later.",
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }

private void userVerification(FirebaseUser user) {
    // Send verification email again
    user.sendEmailVerification()
            .addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()) {
                    Toast.makeText(Login_Page.this,
                            "Verification email sent to " + user.getEmail() + ". Please check your Gmail or email app.",
                            Toast.LENGTH_LONG).show();

                    try {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        intent.setComponent(new android.content.ComponentName(
                                "com.google.android.gm",
                                "com.google.android.gm.ConversationListActivityGmail")); // Gmail inbox activity

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                    } catch (Exception e) {
                        // Fallback if Gmail is not installed
                        try {
                            Intent emailIntent = new Intent(Intent.ACTION_MAIN);
                            emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
                            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(Intent.createChooser(emailIntent, "Open email app"));
                        } catch (Exception ex) {
                            Toast.makeText(Login_Page.this, "No email app found.", Toast.LENGTH_SHORT).show();
                        }
                    }


                } else {
                    Toast.makeText(Login_Page.this,
                            "Failed to send verification email. Try logging in again later.",
                            Toast.LENGTH_SHORT).show();
                }
            });
}



}