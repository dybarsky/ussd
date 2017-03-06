package com.tooploox.ussd.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tooploox.ussd.R;
import com.tooploox.ussd.utils.SetupHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 06/03/2017 15:09
 */
public class SetupActivity extends AppCompatActivity {

    @BindView(R.id.permissions_status)
    ImageView ivPermissionStatus;

    @BindView(R.id.accessibility_status)
    ImageView ivAccessibilityStatus;

    @BindView(R.id.permissions_description)
    TextView tvPermissionDescription;

    @BindView(R.id.accessibility_description)
    TextView tvAccessibilityDescription;

    @BindView(R.id.next)
    Button bNext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setup);
        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPermissionsAndUpdateUI();
        checkAccessibilityAndUpdateUI();
        checkNextButtonAvailability();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissionsAndUpdateUI();
        checkNextButtonAvailability();
    }

    private void checkPermissionsAndUpdateUI() {
        if (SetupHelper.isPermissionGranted(this)) {
            ivPermissionStatus.setImageResource(R.drawable.ic_enabled);
            tvPermissionDescription.setText(R.string.permission_description_enabled);
            tvPermissionDescription.setOnClickListener(null);
        } else {
            ivPermissionStatus.setImageResource(R.drawable.ic_disabled);
            tvPermissionDescription.setText(R.string.permission_description_disabled);
            tvPermissionDescription.setOnClickListener(v -> SetupHelper.requestPermissions(this));
        }
    }

    private void checkAccessibilityAndUpdateUI() {
        if (SetupHelper.isAccessibilityServiceEnabled(this)) {
            ivAccessibilityStatus.setImageResource(R.drawable.ic_enabled);
            tvAccessibilityDescription.setText(R.string.accessibility_description_enabled);
            tvAccessibilityDescription.setOnClickListener(null);
        } else {
            ivAccessibilityStatus.setImageResource(R.drawable.ic_disabled);
            tvAccessibilityDescription.setText(R.string.accessibility_description_disabled);
            tvAccessibilityDescription.setOnClickListener(v -> SetupHelper.openAccessibilitySettings(this));
        }
    }

    private void checkNextButtonAvailability() {
        if (SetupHelper.isPermissionGranted(this) && SetupHelper.isAccessibilityServiceEnabled(this)) {
            bNext.setVisibility(View.VISIBLE);
            bNext.setOnClickListener(v -> {
                Intent intent = new Intent(this, UssdListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            });
        } else {
            bNext.setVisibility(View.GONE);
        }

    }

}
