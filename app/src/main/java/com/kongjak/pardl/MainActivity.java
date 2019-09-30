package com.kongjak.pardl;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

public class MainActivity extends AppCompatActivity {

    String model, last_firmware_ver, current_firmware_ver;

    public static void initializeSSLContext(Context mContext) {
        try {
            SSLContext.getInstance("TLSv1.2");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            ProviderInstaller.installIfNeeded(mContext.getApplicationContext());
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        check_device();
        checkNet();
        initializeSSLContext(getBaseContext());
        initView();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check_is_last(v);
            }
        });
    }

    public void checkNet() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Toast.makeText(getApplicationContext(), getString(R.string.network_alert), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public void check_device() {
        switch (Build.MANUFACTURER) {
            case "PANTECH":
                model = Build.MODEL;
                break;
            default:
                Toast.makeText(getApplicationContext(), getString(R.string.not_pantech), Toast.LENGTH_SHORT).show();
                finish();
                break;
        }
    }

    public void initView() {
        TextView device_firmware_txt = findViewById(R.id.device_firmware);
        TextView last_firmware_txt = findViewById(R.id.last_firmware);
        TextView last_available_txt = findViewById(R.id.last_available);

        device_firmware_txt.setText(getString(R.string.current_ver_is, now_firmware()));
        last_firmware_txt.setText(getString(R.string.last_ver_is, last_firmware()));
        if (!is_last()) {
            last_available_txt.setVisibility(View.VISIBLE);
            last_available_txt.setText(getString(R.string.last_exist));
        }
    }

    public String now_firmware() {

        /*
          If current_firmware_ver is null, then get current firmware version.
          or If current_firmware_ver is exits, don't get current firmware version.
         */
        if (current_firmware_ver == null) {
            StringBuilder output = new StringBuilder();

            Process p;
            try {
                p = Runtime.getRuntime().exec("getprop ro.product.software_ver");
                p.waitFor();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
                current_firmware_ver = output.toString();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return current_firmware_ver;
    }


    public String last_firmware() {
        /*
          If last_firmware_ver is null, then get last firmware version.
          or If last_firmware_ver is exits, don't get last firmware version.
         */
        if (last_firmware_ver == null) {
            Thread th = new Thread(new Runnable() {
                StringBuffer sb = new StringBuffer();

                @Override
                public void run() {
                    try {
                        URL url = new URL(getString(R.string.last_version_url, model));

                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(url.openStream()));

                        String str;
                        while ((str = reader.readLine()) != null) {
                            sb.append(str);
                        }
                        last_firmware_ver = sb.toString();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            th.start();
            try {
                th.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return last_firmware_ver;
    }

    public boolean is_last() {
        return now_firmware().equals(last_firmware());
    }

    public void check_is_last(View v) {
        if (!now_firmware().equals(last_firmware())) {
            dlDialog();
        } else {
            Snackbar.make(v, R.string.already_last, Snackbar.LENGTH_LONG)
                    .setAction(R.string.yes, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            get_last_Update_zip();
                        }
                    }).show();
        }
    }

    public void dlDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(getString(R.string.update_dialog_msg, last_firmware()))
                .setPositiveButton(R.string.download_update_zip, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        get_last_Update_zip();
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }

    public void get_last_Update_zip() {
        Log.d("PAR", getString(R.string.download_url, model, last_firmware()));
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(getString(R.string.download_url, model, last_firmware())));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_web) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(this, Uri.parse(getString(R.string.default_url)));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
