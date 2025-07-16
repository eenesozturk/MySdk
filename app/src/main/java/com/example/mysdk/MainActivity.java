package com.example.mysdk;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.securelibrary.SdkUtils;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String welcome = SdkUtils.getWelcomeMessage(this);
        String date = SdkUtils.getCurrentDate(this);
        String version = SdkUtils.getAppVersion(this);
        String message = welcome + "\n\nTarih: " + date + "\nVersiyon: " + version;

        TextView textView = findViewById(R.id.sdkMessageText);
        textView.setText(message);
    }
}
