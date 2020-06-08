package com.cnam.greta.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.cnam.greta.R;
import com.cnam.greta.ui.fragment.CameraFragment;

/**
 * Activité de réalité augmentée.
 */
public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraFragment.newInstance())
                    .commit();
        }
    }

}