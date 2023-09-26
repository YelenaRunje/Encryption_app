package com.example.myapplicationtest;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int GALLERY_REQUEST_CODE = 21;
    private static final int PERMISSION_REQUEST_CODE = 123;
    private byte [] initializatonVector;
    private byte [] aesSalt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if we have permission to access external storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!arePermissionsGranted()) {
                requestPermissions();
                if (Build.VERSION.SDK_INT >= 30){
                    if (!Environment.isExternalStorageManager()){
                        Intent getpermission = new Intent();
                        getpermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivity(getpermission);
                    }
                }
            }
        }

        Button encryptButton = findViewById(R.id.button_encrypt);
        Button decryptButton = findViewById(R.id.button_decrypt);
        Button browseButton = findViewById(R.id.button_browse);

        encryptButton.setEnabled(false);
        decryptButton.setEnabled(false);

        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasPermissions()) {
                    TextView edfile = findViewById(R.id.tvFileName);
                    String filename = edfile.getText().toString();

                    if (!filename.isEmpty() && !filename.endsWith(".enc") && !filename.equals("SELECTED FILE NAME")) {

                        showToast("Encryption started.");
                        EditText edkey = findViewById(R.id.text_key);
                        String textKey = edkey.getText().toString();

                        new CryptoManager.CryptoTask(textKey, filename, true, handler).execute();

                    } else {
                        showToast("Select a correct file to encrypt.");
                    }
                } else {
                    requestPermissions();
                }
            }
        });

        decryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasPermissions()) {
                    TextView edfile = findViewById(R.id.tvFileName);
                    String filename = edfile.getText().toString();

                    if (!filename.isEmpty() && filename.endsWith(".enc")) {
                        showToast("Decryption started...");

                        EditText edkey = findViewById(R.id.text_key);
                        String textKey = edkey.getText().toString();

                        // Use the stored salt and IV for decryption
                        new CryptoManager.CryptoTask(textKey, filename, false, handler).execute();

                    } else {
                        showToast("Select a correct encrypted file.");
                    }
                } else {
                    showToast("Permissions not granted. Please grant the necessary permissions.");
                }
            }
        });

        browseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasPermissions()) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("*/*");
                    startActivityForResult(intent, GALLERY_REQUEST_CODE);
                } else {
                    showToast("Permissions not granted. Please grant the necessary permissions.");
                    checkAndRequestPermissions();
                }
            }
        });

        TextView edfile = findViewById(R.id.tvFileName);
        EditText edkey = findViewById(R.id.text_key);

        edfile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtonStates();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        edkey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtonStates();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateButtonStates() {
        TextView edfile = findViewById(R.id.tvFileName);
        EditText edPassword = findViewById(R.id.text_key);

        String filename = edfile.getText().toString();
        String password = edPassword.getText().toString();

        Button encryptButton = findViewById(R.id.button_encrypt);
        Button decryptButton = findViewById(R.id.button_decrypt);

        boolean isFileChosen = !filename.isEmpty() && !filename.equals("SELECTED FILE NAME");
        boolean isPasswordEntered = !password.isEmpty();

        encryptButton.setEnabled(isFileChosen && isPasswordEntered);
        decryptButton.setEnabled(isFileChosen && isPasswordEntered);
    }

    private boolean hasPermissions() {
        return arePermissionsGranted();
    }

    private boolean arePermissionsGranted() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkAndRequestPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!arePermissionsGranted()) {
                requestPermissions();
            }
        }
    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if both permissions are granted
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, you can proceed with file operations
            } else {
                showToast("Permissions not granted. Please grant the necessary permissions.");
            }
        }
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TextView textFileName = findViewById(R.id.tvFileName);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GALLERY_REQUEST_CODE:
                    Uri selectedImage = data.getData();
                    if (selectedImage != null) {
                        RealPathUtil obj = new RealPathUtil();
                        String selectedFilePath = obj.getRealPath(this, selectedImage);
                        if (selectedFilePath != null) {
                            textFileName.setText(selectedFilePath);
                            textFileName.setVisibility(View.VISIBLE);
                        } else {
                            showToast("Failed to get the file path.");
                        }
                    } else {
                        showToast("Selected file is null.");
                    }
                    break;
            }
            updateButtonStates();
        }
    }


    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int resultCode = msg.arg1;
            TextView edfile = findViewById(R.id.tvFileName);
            EditText edkey = findViewById(R.id.text_key);
            switch (resultCode){
                case 1 : showToast("Encryption successful");
                    break;
                case 2: showToast("Decryption successful");
                    break;
                default: showToast("Operation failed");
                    break;
            }
            edfile.setText(R.string.file_path);
            edkey.getText().clear();
            return true;
        }
    });
}
