package com.example.myapplicationtest;

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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final int GALLERY_REQUEST_CODE = 21;
    private static final int PERMISSION_REQUEST_CODE = 123;

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

        ImageButton encryptButton = findViewById(R.id.btn_encrypt);
        ImageButton decryptButton = findViewById(R.id.btn_decrypt);
        Button browseButton = findViewById(R.id.btn_browse);

        encryptButton.setEnabled(false);
        decryptButton.setEnabled(false);

        encryptButton.setOnClickListener(v -> {
            if (hasPermissions()) {
                TextView fileName = findViewById(R.id.tvFileName);
                String filename = fileName.getText().toString();

                if (!filename.isEmpty() && !filename.endsWith(".enc") && !filename.equals("File name")) {
                    showToast("Encryption started...");
                    EditText fileKey = findViewById(R.id.edKey);
                    String key = fileKey.getText().toString();
                    new CryptoManager.CryptoTask(key, filename, true, handler).execute();

                } else {
                    showToast("Select a correct file to encrypt.");
                }
            } else {
                requestPermissions();
            }
        });

        decryptButton.setOnClickListener(v -> {
            if (hasPermissions()) {
                TextView fileName = findViewById(R.id.tvFileName);
                String filename = fileName.getText().toString();

                if (!filename.isEmpty() && filename.endsWith(".enc")) {
                    showToast("Decryption started...");

                    EditText fileKey = findViewById(R.id.edKey);
                    String key = fileKey.getText().toString();

                    new CryptoManager.CryptoTask(key, filename, false, handler).execute();

                } else {
                    showToast("Select a correct encrypted file.");
                }
            } else {
                showToast("Permissions not granted. Please grant the necessary permissions.");
            }
        });

        browseButton.setOnClickListener(v -> {
            if (hasPermissions()) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                startActivityForResult(intent, GALLERY_REQUEST_CODE);
            } else {
                showToast("Permissions not granted. Please grant the necessary permissions.");
                checkAndRequestPermissions();
            }
        });

        TextView fileName = findViewById(R.id.tvFileName);
        EditText fileKey = findViewById(R.id.edKey);

        fileName.addTextChangedListener(new TextWatcher() {
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

        fileKey.addTextChangedListener(new TextWatcher() {
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
        TextView fileName = findViewById(R.id.tvFileName);
        EditText fileKey = findViewById(R.id.edKey);

        String filename = fileName.getText().toString();
        String password = fileKey.getText().toString();

        ImageButton encryptButton = findViewById(R.id.btn_encrypt);
        ImageButton decryptButton = findViewById(R.id.btn_decrypt);

        boolean isFileChosen = !filename.isEmpty() && !filename.equals("File name");
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
        TextView fileName = findViewById(R.id.tvFileName);
        if (resultCode == Activity.RESULT_OK) {
            Uri selectedFile = data.getData();
            if (selectedFile != null) {
                RealPathUtil obj = new RealPathUtil();
                String selectedFilePath = obj.getRealPath(this, selectedFile);
                if (selectedFilePath != null) {
                    fileName.setText(selectedFilePath);
                } else {
                    showToast("Failed to get the file path.");
                }
            } else {
                showToast("Selected file is null.");
            }
            updateButtonStates();
        }
    }

    private final Handler handler = new Handler(msg -> {
        int resultCode = msg.arg1;
        TextView fileName = findViewById(R.id.tvFileName);
        EditText fileKey = findViewById(R.id.edKey);
        switch (resultCode){
            case 1 :
                showToast("Encryption successful");
                break;
            case 2:
                showToast("Decryption successful");
                break;
            default:
                showToast("Operation failed");
                break;
        }
        fileName.setText(R.string.file_path);
        fileKey.getText().clear();
        return true;
    });
}
