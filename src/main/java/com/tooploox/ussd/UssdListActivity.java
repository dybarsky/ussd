package com.tooploox.ussd;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.Inflater;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

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

    private class AddUssdDialog extends AlertDialog {

        protected AddUssdDialog(Context context) {
            super(context);
        }
    }

    private Adapter adapter = new Adapter();

    @BindView(R.id.recycler_view)
    RecyclerView rv;

    @OnClick(R.id.button_add)
    public void onAddButtonClicked(View button) {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity);
        ButterKnife.bind(this);

        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

}