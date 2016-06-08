package com.tooploox.ussd;

import android.content.DialogInterface;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity);

        ActivityViews av = new ActivityViews();
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
                    onDoneButtonClicked(dv);
                })
                .create();
        dialog.show();

        ButterKnife.bind(dv, dialog);
    }

    public void onDoneButtonClicked(DialogVies dv) {
        Toast.makeText(this, "OK", Toast.LENGTH_LONG).show();
//        adapter.data.add();
    }

}