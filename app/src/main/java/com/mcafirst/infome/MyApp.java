package com.mcafirst.infome;


import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Enable offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
