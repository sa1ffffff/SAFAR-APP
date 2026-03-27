package com.safar.app.views.activities;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * SAFAR ABOUT ACTIVITY
 * 
 * Displays app information, developer details, and links.
 */
public class AboutActivity extends AppCompatActivity {

    private static final String GITHUB_URL = "https://github.com/sa1ffffff";
    private static final String DEVELOPER_EMAIL = "saifullahwasim1@gmail.com";
    private static final String PRIVACY_URL = "https://safar.pk/privacy-policy";
    private static final String TERMS_URL = "https://safar.pk/terms-of-service";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AboutBridgeKt.setupComposeUI(this);
    }

    /**
     * Get app version from package info.
     */
    public String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }

    /**
     * Open developer's GitHub profile.
     */
    public void openGitHub() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL));
            startActivity(intent);
        } catch (Exception e) {
            // Handle error silently
        }
    }

    /**
     * Open email client to contact developer.
     */
    public void openEmail() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + DEVELOPER_EMAIL));
            intent.putExtra(Intent.EXTRA_SUBJECT, "SAFAR App Feedback");
            startActivity(Intent.createChooser(intent, "Send Email"));
        } catch (Exception e) {
            // Handle error silently
        }
    }

    /**
     * Open privacy policy in browser.
     */
    public void openPrivacyPolicy() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL));
            startActivity(intent);
        } catch (Exception e) {
            // Handle error silently
        }
    }

    /**
     * Open terms of service in browser.
     */
    public void openTermsOfService() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL));
            startActivity(intent);
        } catch (Exception e) {
            // Handle error silently
        }
    }
}