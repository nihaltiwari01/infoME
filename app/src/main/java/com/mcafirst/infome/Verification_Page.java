package com.mcafirst.infome;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Firebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class Verification_Page extends AppCompatActivity {

    TextInputEditText verificationRollNo;
    MaterialButton verificationBtn;
    DatabaseReference databaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verification);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("MasterData");

        verificationRollNo = findViewById(R.id.verificationRollNo);
        verificationBtn = findViewById(R.id.verificationSubmitBtn);


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
        // Handle system back button with dispatcher
//        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback() {
//            @Override
//            public void handleOnBackPressed() {
//
//            }
//        });

        verificationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkRollNumber();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkRollNumber() {
        String rollNumber = verificationRollNo.getText().toString().trim() ;
//        if (rollNumber.isEmpty()) {
//            Toast.makeText(Verification_Page.this, "Please enter roll number", Toast.LENGTH_SHORT).show();
//            return;
//        }

        if (TextUtils.isEmpty(rollNumber)) {
            verificationRollNo.setError("Enter Roll Number!");
            return;
        }

        Query query = databaseReference.child("unregisteredStudent").orderByValue().equalTo(rollNumber);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    System.out.println("Roll number: " + rollNumber + " found");
                    Toast.makeText(Verification_Page.this, "Roll number : " + rollNumber + " verified" , Toast.LENGTH_SHORT ).show();
                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                        System.out.println("ID " + snapshot1.getKey() + " Value " + snapshot1.getValue(String.class));
                        Intent intent = new Intent(Verification_Page.this, Registeration_Page.class);
                    intent.putExtra("userID", rollNumber + "@nitm.ac.in");
                        intent.putExtra("role", "student");
                    startActivity(intent);
                    }
                } else {
                    Query query1 = databaseReference.child("unRegisteredAdmin");
                    query1.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                                    Log.d("ERROR_REPORT", "ID " + snapshot1.getKey() + " Value " + snapshot1.getValue(String.class));
                                    Intent intent = new Intent(Verification_Page.this, Registeration_Page.class);
                                    intent.putExtra("userID", snapshot1.getValue(String.class));
                                    intent.putExtra("role", "teacher");
                                    startActivity(intent);
                                }
                            } else {
                                Toast.makeText(Verification_Page.this, "Your roll number is not registered" , Toast.LENGTH_SHORT ).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(Verification_Page.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
Toast.makeText(Verification_Page.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

//        databaseReference.child("unRegisteredStudent").orderByValue().equalTo(rollNumber).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if(snapshot.exists()) {
//                    Log.d("IMP", snapshot.toString().trim());
//                    Toast.makeText(Verification_Page.this, "VERIFIED : " + rollNumber + " " + snapshot.toString(), Toast.LENGTH_SHORT ).show();
//
//                    Intent intent = new Intent(Verification_Page.this, Registeration_Page.class);
//                    intent.putExtra("rollNumber", rollNumber);
//                    startActivity(intent);
//                } else {
//                    Toast.makeText(Verification_Page.this, "Enter Valid Roll Number" , Toast.LENGTH_SHORT ).show();
//
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//Toast.makeText(Verification_Page.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });


    }

}