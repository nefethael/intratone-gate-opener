package fr.nefethael.intratonegateopener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telecom.TelecomManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.ToggleButton;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private final int REQUEST_INTRATONE_PERMISSIONS = 5;

    public enum PhoneGate {
        PHONE_EXTERNAL_GATE,
        PHONE_INTERNAL_GATE,
    }

    private Handler mainHandler = new Handler();
    private Runnable getInTask = new Runnable() {
        @Override
        public void run() {
            getIn();
            final ToggleButton getInButton = findViewById(R.id.getinbutton);
            getInButton.setChecked(false);
        }
    };
    private Runnable getOutTask = new Runnable() {
        @Override
        public void run() {
            getOut();
            final ToggleButton getOutButton = findViewById(R.id.getoutbutton);
            getOutButton.setChecked(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ProgressBar progressBar = findViewById(R.id.progressBar);
        final ToggleButton getInButton = findViewById(R.id.getinbutton);
        final ToggleButton getOutButton = findViewById(R.id.getoutbutton);

        getInButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    getInButton.setEnabled(false);
                    getOutButton.setEnabled(false);
                    progressBar.setIndeterminate(true);

                    mainHandler.post(getInTask);
                }else{
                    progressBar.setIndeterminate(false);
                    getInButton.setEnabled(true);
                    getOutButton.setEnabled(true);
                }
            }
        });

        getOutButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    getInButton.setEnabled(false);
                    getOutButton.setEnabled(false);
                    progressBar.setIndeterminate(true);

                    mainHandler.post(getOutTask);
                }else{
                    progressBar.setIndeterminate(false);
                    getInButton.setEnabled(true);
                    getOutButton.setEnabled(true);
                }
            }
        });

        // initializing our button.
        Button settingsBtn = findViewById(R.id.idBtnSettings);

        // adding on click listener for our button.
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // opening a new intent to open settings activity.
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            }
        });


        requestIntratonePermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(REQUEST_INTRATONE_PERMISSIONS)
    public void requestIntratonePermissions() {
        String[] perms = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.ANSWER_PHONE_CALLS,
        };
        if(EasyPermissions.hasPermissions(this, perms)) {
            initialiseIntratone();
        } else {
            EasyPermissions.requestPermissions(this, "Please grant the Intratone permissions", REQUEST_INTRATONE_PERMISSIONS, perms);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        initialiseIntratone();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    private void initialiseIntratone(){

    }

    private String getGatePhone(PhoneGate gate){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String gateStg = null;
        switch(gate){
            case PHONE_INTERNAL_GATE:
                boolean innerOk = sharedPreferences.getBoolean("inner_switch", true);
                String i = sharedPreferences.getString("inner_phone_num", "");
                if(innerOk){
                    if (!i.isEmpty()) {
                        gateStg = i;
                    }
                }else{
                    gateStg = "";
                }
                break;
            case PHONE_EXTERNAL_GATE:
                String o = sharedPreferences.getString("outer_phone_num", "");
                if (!o.isEmpty()){
                    gateStg = o;
                }
                break;
        }
        return gateStg;
    }

    private boolean validateSettings(String outerStr, String innerStr){
        if (outerStr == null || innerStr == null){
            DialogFragment newFragment = new EmptyGateDialogFragment();
            newFragment.show(getSupportFragmentManager(), "gate");
            return false;
        }
        return true;
    }

    private void getIn(){
        String outerStr = getGatePhone(PhoneGate.PHONE_EXTERNAL_GATE);
        String innerStr = getGatePhone(PhoneGate.PHONE_INTERNAL_GATE);

        if(!validateSettings(outerStr, innerStr)){
            return;
        }

        // coming from outside open external gate
        openGate(outerStr);

        // wait it opens
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // open internal gate
        openGate(innerStr);
    }

    private void getOut(){
        String outerStr = getGatePhone(PhoneGate.PHONE_EXTERNAL_GATE);
        String innerStr = getGatePhone(PhoneGate.PHONE_INTERNAL_GATE);

        if(!validateSettings(outerStr, innerStr)){
            return;
        }

        // coming from inside open internal gate
        openGate(innerStr);

        // open external gate
        openGate(outerStr);
    }

    private void openGate(String gate){
        if (gate == null || gate.isEmpty()){
            return;
        }

        // call dialer
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + gate));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
        startActivity(intent);

        try {
            Thread.sleep(5000);

            // put application back to front
            Intent intent2 = new Intent(this, MainActivity.class);
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent2);

            Thread.sleep(9000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // end the call
        cutTheCall();
    }

    @SuppressLint("MissingPermission")
    private boolean cutTheCall() {
        boolean callDisconnected = false;
        TelecomManager telecomManager = (TelecomManager) getApplicationContext().getSystemService(TELECOM_SERVICE);
        if (telecomManager.isInCall()) {
            callDisconnected = telecomManager.endCall();
        }
        return callDisconnected;
    }
}