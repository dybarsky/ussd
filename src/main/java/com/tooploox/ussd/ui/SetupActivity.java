package com.tooploox.ussd.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.tooploox.ussd.App;
import com.tooploox.ussd.R;
import com.tooploox.ussd.databinding.ActivitySetupBinding;
import com.tooploox.ussd.utils.ConsentStore;
import com.tooploox.ussd.utils.SetupHelper;

/**
 * Three-step onboarding wizard: 1) what the app does, 2) why the phone permission is needed,
 * 3) prominent disclosure and consent for the AccessibilityService (Google Play User Data policy).
 *
 * Consent is recorded only when the user taps "Agree" on step 3. Leaving the screen with Back or Home is not consent.
 */
public class SetupActivity extends AppCompatActivity {

    /** Optional int extra: the step to open first (one of the STEP_* constants). */
    public static final String EXTRA_STEP = "com.tooploox.ussd.extra.STEP";

    public static final int STEP_INTRO = 0;
    public static final int STEP_PHONE = 1;
    public static final int STEP_ACCESSIBILITY = 2;
    private static final int STEP_COUNT = 3;

    private static final String STATE_STEP = "step";
    private static final String STATE_AWAITING_SETTINGS = "awaiting_settings";

    private ActivitySetupBinding binding;
    private ConsentStore consentStore;
    private int step;
    /** True after "Agree and open settings" until the user comes back; used to show a hint if the service is still off. */
    private boolean awaitingSettings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        consentStore = App.INSTANCE.consentStore;

        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            root.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            // The children do not use insets themselves, so consume them here.
            return WindowInsetsCompat.CONSUMED;
        });

        if (savedInstanceState != null) {
            step = savedInstanceState.getInt(STATE_STEP, STEP_INTRO);
            awaitingSettings = savedInstanceState.getBoolean(STATE_AWAITING_SETTINGS, false);
        } else {
            step = getIntent().getIntExtra(EXTRA_STEP, STEP_INTRO);
        }
        step = Math.max(STEP_INTRO, Math.min(STEP_ACCESSIBILITY, step));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (step > STEP_INTRO) {
                    showStep(step - 1);
                } else {
                    finish();
                }
            }
        });

        showStep(step);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP, step);
        outState.putBoolean(STATE_AWAITING_SETTINGS, awaitingSettings);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (step == STEP_ACCESSIBILITY && awaitingSettings && SetupHelper.isAccessibilityServiceEnabled(this)) {
            finishWizard();
            return;
        }
        // The permission may have changed while we were away, and the hint depends on the service state.
        showStep(step);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (step == STEP_PHONE && SetupHelper.isPermissionGranted(this)) {
            showStep(STEP_ACCESSIBILITY);
        } else {
            showStep(step);
        }
    }

    private void showStep(int newStep) {
        step = newStep;

        binding.stepIntro.setVisibility(step == STEP_INTRO ? View.VISIBLE : View.GONE);
        binding.stepPhone.setVisibility(step == STEP_PHONE ? View.VISIBLE : View.GONE);
        binding.stepAccessibility.setVisibility(step == STEP_ACCESSIBILITY ? View.VISIBLE : View.GONE);
        // Show the new step from its top, e.g. when navigating back to a scrolled step.
        binding.stepIntro.scrollTo(0, 0);
        binding.stepPhone.scrollTo(0, 0);
        binding.stepAccessibility.scrollTo(0, 0);

        View[] dots = {binding.dot0, binding.dot1, binding.dot2};
        for (int i = 0; i < STEP_COUNT; i++) {
            dots[i].setBackgroundResource(i == step ? R.drawable.wizard_dot_active : R.drawable.wizard_dot_inactive);
        }

        switch (step) {
            case STEP_INTRO:
                showIntroActions();
                break;
            case STEP_PHONE:
                showPhoneActions();
                break;
            default:
                showAccessibilityActions();
                break;
        }
    }

    private void showIntroActions() {
        binding.btnPrimary.setText(R.string.wizard_next);
        binding.btnPrimary.setOnClickListener(v -> showStep(STEP_PHONE));
        binding.btnSecondary.setVisibility(View.GONE);
    }

    private void showPhoneActions() {
        if (SetupHelper.isPermissionGranted(this)) {
            binding.phoneGranted.setVisibility(View.VISIBLE);
            binding.btnPrimary.setText(R.string.wizard_next);
            binding.btnPrimary.setOnClickListener(v -> showStep(STEP_ACCESSIBILITY));
            binding.btnSecondary.setVisibility(View.GONE);
        } else {
            binding.phoneGranted.setVisibility(View.GONE);
            binding.btnPrimary.setText(R.string.wizard_phone_allow);
            binding.btnPrimary.setOnClickListener(v -> SetupHelper.requestPermissions(this));
            binding.btnSecondary.setText(R.string.wizard_phone_skip);
            binding.btnSecondary.setOnClickListener(v -> showStep(STEP_ACCESSIBILITY));
            binding.btnSecondary.setVisibility(View.VISIBLE);
        }
    }

    private void showAccessibilityActions() {
        // Back from the system settings without enabling the service: explain what is left to do.
        binding.accessibilityHint.setVisibility(awaitingSettings ? View.VISIBLE : View.GONE);

        binding.btnPrimary.setText(R.string.wizard_accessibility_agree);
        binding.btnPrimary.setOnClickListener(v -> onAccessibilityAgreed());
        binding.btnSecondary.setText(R.string.wizard_accessibility_decline);
        binding.btnSecondary.setOnClickListener(v -> onAccessibilityDeclined());
        binding.btnSecondary.setVisibility(View.VISIBLE);
    }

    private void onAccessibilityAgreed() {
        consentStore.setAccessibilityAccepted(true);
        if (SetupHelper.isAccessibilityServiceEnabled(this)) {
            // Existing users who enabled the service before this disclosure existed: nothing to turn on.
            finishWizard();
            return;
        }
        awaitingSettings = true;
        SetupHelper.openAccessibilitySettings(this);
    }

    private void onAccessibilityDeclined() {
        consentStore.setAccessibilityAccepted(false);
        awaitingSettings = false;
        finishWizard();
    }

    private void finishWizard() {
        consentStore.setOnboardingDone(true);
        if (isTaskRoot()) {
            Intent intent = new Intent(this, UssdListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        finish();
    }
}
