package com.captaindroid.secureapi;


import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import androidx.security.crypto.MasterKeys;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity {
    private String TAG = "Main";

    private FirebaseAuth mAuth;

    private DatabaseReference mDatabase;
    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("key");

        mAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    mDatabase.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Log.e("key", snapshot.getValue().toString());
                            storeKeyInSharedPreference(snapshot.getValue().toString());
                            readFromSharedPreference();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }

                    });
                } else {

                }
            }
        });

    }

    private void storeKeyInSharedPreference(String key) {
        String sharedPrefsFile = "myKeys";

        String mainKeyAlias = null;
        try {
             sharedPreferences = EncryptedSharedPreferences.create(
                    this,
                    sharedPrefsFile,
                    getMasterKey(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
            sharedPrefsEditor.putString("key", key);
            sharedPrefsEditor.commit();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void readFromSharedPreference() {
        Log.e("after store", sharedPreferences.getString("key", "error"));
    }

    private MasterKey getMasterKey() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build();

            try {
                return new MasterKey.Builder(this)
                        .setKeyGenParameterSpec(spec)
                        .build();
            } catch (Exception e) {
                Log.e("M", e.getMessage());
            }

        } else {
            try {
                return new MasterKey.Builder(this)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            } catch (Exception e) {
            }
        }

        return null;
    }

}