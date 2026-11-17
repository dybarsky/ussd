package com.tooploox.ussd.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.tooploox.ussd.R;
import com.tooploox.ussd.databinding.ActivitySetupBinding;
import com.tooploox.ussd.utils.SetupHelper;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 06/03/2017 15:09
 */
public class SetupActivity extends AppCompatActivity {

    private ActivitySetupBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
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
            binding.permissionsStatus.setImageResource(R.drawable.ic_enabled);
            binding.permissionsDescription.setText(R.string.permission_description_enabled);
            binding.permissionsDescription.setOnClickListener(null);
        } else {
            binding.permissionsStatus.setImageResource(R.drawable.ic_disabled);
            binding.permissionsDescription.setText(R.string.permission_description_disabled);
            binding.permissionsDescription.setOnClickListener(v -> SetupHelper.requestPermissions(this));
        }
    }

    private void checkAccessibilityAndUpdateUI() {
        if (SetupHelper.isAccessibilityServiceEnabled(this)) {
            binding.accessibilityStatus.setImageResource(R.drawable.ic_enabled);
            binding.accessibilityDescription.setText(R.string.accessibility_description_enabled);
            binding.accessibilityDescription.setOnClickListener(null);
        } else {
            binding.accessibilityStatus.setImageResource(R.drawable.ic_disabled);
            binding.accessibilityDescription.setText(R.string.accessibility_description_disabled);
            binding.accessibilityDescription.setOnClickListener(v -> SetupHelper.openAccessibilitySettings(this));
        }
    }

    private void checkNextButtonAvailability() {
        if (SetupHelper.isPermissionGranted(this) && SetupHelper.isAccessibilityServiceEnabled(this)) {
            binding.next.setVisibility(View.VISIBLE);
            binding.next.setOnClickListener(v -> {
                Intent intent = new Intent(this, UssdListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            });
        } else {
            binding.next.setVisibility(View.GONE);
        }

    }

}
