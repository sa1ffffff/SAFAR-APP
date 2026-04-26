package com.safar.app.views.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    private static final String SUPPORT_EMAIL = "safar.support.app@gmail.com";

    private HelpStateHolder stateHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateHolder = new HelpStateHolder();
        HelpBridgeKt.setupComposeUI(this, stateHolder);
    }

    public void openSupportEmail() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{SUPPORT_EMAIL});
            intent.putExtra(Intent.EXTRA_SUBJECT, "SAFAR Support Request");
            startActivity(Intent.createChooser(intent, "Contact Support"));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open email app", Toast.LENGTH_SHORT).show();
        }
    }
}
