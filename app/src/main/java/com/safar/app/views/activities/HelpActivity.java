package com.safar.app.views.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    private HelpStateHolder stateHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        stateHolder = new HelpStateHolder();
        HelpBridgeKt.setupComposeUI(this, stateHolder);
    }
}
