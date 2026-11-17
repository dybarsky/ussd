package com.tooploox.ussd.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.View;
import android.widget.EditText;

import com.tooploox.ussd.App;
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
        super.onCreate(savedInstanceState);

        binding = ActivityUssdListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        binding.buttonAdd.setOnClickListener(eventsReactor::onAddUssdButtonClicked);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkSetupConditions();
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

    private void checkSetupConditions() {
        if (!SetupHelper.isPermissionGranted(this) || !SetupHelper.isAccessibilityServiceEnabled(this)) {
            Intent intent = new Intent(this, SetupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}