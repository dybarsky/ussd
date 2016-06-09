package com.tooploox.ussd;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UssdListActivity extends AppCompatActivity {


    private class Holder extends RecyclerView.ViewHolder {

        private ViewDataBinding binding;

        public Holder(ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }


    private class Adapter extends RecyclerView.Adapter<Holder> {

        private List<Ussd> data = new ArrayList<>();

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.item, parent, false);
            binding.getRoot().setOnClickListener(v -> onListItemClicked(av.rv.getChildAdapterPosition(v)));
            return new Holder(binding);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            holder.binding.setVariable(BR.ussd, data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }


    class ActivityViews {
        @BindView(R.id.recycler_view)
        RecyclerView rv;

        @BindView(R.id.button_add)
        FloatingActionButton fab;
    }


    class DialogVies {

    }


    private Adapter adapter = new Adapter();
    private ActivityViews av = new ActivityViews();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity);

        ButterKnife.bind(av, this);

        av.fab.setOnClickListener(this::onAddButtonClicked);

        av.rv.setHasFixedSize(true);
        av.rv.setLayoutManager(new LinearLayoutManager(this));
        av.rv.setAdapter(adapter);
    }

    public void onAddButtonClicked(View button) {
        DialogVies dv = new DialogVies();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(R.layout.dialog)
                .setCancelable(true)
                .setTitle(R.string.add_ussd)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    d.dismiss();
                    onDialogDoneButtonClicked(dv);
                })
                .create();
        dialog.show();

        ButterKnife.bind(dv, dialog);
    }

    private String match(Ussd ussd) {
        Matcher matcher = Pattern
                .compile(ussd.getRegex())
                .matcher(ussd.getResponse());
        return matcher.matches()
                ? matcher.group()
                : "-";
    }

    public void onDialogDoneButtonClicked(DialogVies dv) {
        Ussd ussd = new Ussd();
        ussd.setCode("*101#");
        ussd.setRegex("\\d+.\\d+");
        ussd.setResponse("Stan Twojego konta wynosi 24.56");
        ussd.setResult(match(ussd));

        adapter.data.add(ussd);
        adapter.notifyDataSetChanged();
    }


    public void onListItemClicked(int adapterPosition) {

    }
}