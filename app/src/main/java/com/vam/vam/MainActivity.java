package com.vam.vam;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.ar.sceneform.ux.ArFragment;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button switchButton;
    boolean arSessioninProgress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchButton = findViewById(R.id.button_switch);
        switchButton.setOnClickListener(this);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main_activity_frame_layout,CameraFragment.newInstance(),"AR fragment");
        ft.addToBackStack("AR Fragment starting");
        ft.commit();
    }

    @Override
    public void onClick(View v) {
        startActivity(new Intent(this,ARSessionActivity.class));
    }
}
