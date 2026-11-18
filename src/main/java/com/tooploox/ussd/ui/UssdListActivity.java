package com.tooploox.ussd.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.tooploox.ussd.App;
import com.tooploox.ussd.R;
import com.tooploox.ussd.databinding.ActivityUssdListBinding;
import com.tooploox.ussd.databinding.DialogAddUssdBinding;
import com.tooploox.ussd.domain.Ussd;
import com.tooploox.ussd.utils.EventBus;
import com.tooploox.ussd.utils.Predicate;
import com.tooploox.ussd.utils.SetupHelper;
import com.tooploox.ussd.utils.Strings;

public class UssdListActivity extends AppCompatActivity {

    public class UiEventsReactor {

        public void onAddUssdButtonClicked(View button) {
            showAddUssdDialog();
        }

        public void onDialogDoneButtonClicked(DialogInterface dialog, int which) {
            if (!isInputValid())
                return;

            presenter.addUssd(dialogBinding.dialogEtUssdQuery.getText().toString());
            dialog.dismiss();
        }

        public void onUssdItemClicked(Ussd ussd) {
            presenter.runUssd(ussd);
        }
    }

    private Predicate<EditText> notEmptyPredicate =
            et -> et.getText() != null && !Strings.isNullOrEmpty(et.getText().toString());

    private ActivityUssdListBinding binding;
    private DialogAddUssdBinding dialogBinding;
    private UiEventsReactor eventsReactor = new UiEventsReactor();
    private UssdListAdapter adapter = new UssdListAdapter(view -> binding.recyclerView.getChildAdapterPosition(view), eventsReactor);
    private UssdListPresenter presenter = new UssdListPresenter(App.INSTANCE.ussdStorage, adapter, App.INSTANCE.ussdExecutor);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        binding = ActivityUssdListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        binding.buttonAdd.setOnClickListener(eventsReactor::onAddUssdButtonClicked);
    }

    private void applyWindowInsets() {
        // The FAB's margin from the layout (@dimen/fab_margin) is the base; the bottom inset is added on top of it.
        ViewGroup.MarginLayoutParams fabParams = (ViewGroup.MarginLayoutParams) binding.buttonAdd.getLayoutParams();
        int fabBottom = fabParams.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (root, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            // Top and sides are applied to the whole screen so the banner sits below the status bar.
            root.setPadding(bars.left, bars.top, bars.right, 0);
            // The list scrolls above the navigation bar (clipToPadding=false) but its last row can be reached.
            binding.recyclerView.setPadding(0, 0, 0, bars.bottom);
            fabParams.setMargins(fabParams.leftMargin, fabParams.topMargin, fabParams.rightMargin, fabBottom + bars.bottom);
            binding.buttonAdd.setLayoutParams(fabParams);
            // All children are handled above and none needs the insets, so consume them.
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!checkOnboarding()) {
            updateBanner();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.loadUssdList();
        App.INSTANCE.eventBus.subscribe(EventBus.Event.USSD_RESULT_RECEIVED, event -> presenter.loadUssdList());
    }

    @Override
    protected void onPause() {
        super.onPause();
        App.INSTANCE.eventBus.unsubscribe(EventBus.Event.USSD_RESULT_RECEIVED);
    }

    public void showAddUssdDialog() {
        dialogBinding = DialogAddUssdBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, eventsReactor::onDialogDoneButtonClicked)
                .create();
        dialog.show();
    }

    private boolean isInputValid() {
        return notEmptyPredicate.apply(dialogBinding.dialogEtUssdQuery);
    }

    /**
     * Opens the setup wizard the first time the app runs. Users upgrading from a version without the wizard
     * already hold the phone permission, so they only need to see the accessibility disclosure.
     *
     * @return true if the wizard was launched
     */
    private boolean checkOnboarding() {
        if (App.INSTANCE.consentStore.isOnboardingDone()) {
            return false;
        }
        Intent intent = new Intent(this, SetupActivity.class);
        intent.putExtra(SetupActivity.EXTRA_STEP,
                SetupHelper.isPermissionGranted(this) ? SetupActivity.STEP_ACCESSIBILITY : SetupActivity.STEP_INTRO);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        return true;
    }

    /**
     * Declining (or later revoking) a permission must not trap the user in the wizard; instead the list shows a
     * banner that reopens the wizard at the missing step. This is also how the disclosure can be shown again.
     */
    private void updateBanner() {
        boolean phoneGranted = SetupHelper.isPermissionGranted(this);
        boolean repliesOn = App.INSTANCE.consentStore.isAccessibilityAccepted()
                && SetupHelper.isAccessibilityServiceEnabled(this);
        if (phoneGranted && repliesOn) {
            binding.banner.setVisibility(View.GONE);
            return;
        }
        binding.bannerText.setText(phoneGranted ? R.string.banner_replies_off : R.string.banner_phone_off);
        binding.bannerAction.setOnClickListener(v -> {
            Intent intent = new Intent(this, SetupActivity.class);
            intent.putExtra(SetupActivity.EXTRA_STEP,
                    phoneGranted ? SetupActivity.STEP_ACCESSIBILITY : SetupActivity.STEP_PHONE);
            startActivity(intent);
        });
        binding.banner.setVisibility(View.VISIBLE);
    }
}
