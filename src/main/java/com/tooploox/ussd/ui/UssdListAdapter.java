package com.tooploox.ussd.ui;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.tooploox.ussd.BR;
import com.tooploox.ussd.R;
import com.tooploox.ussd.domain.Ussd;

import java.util.ArrayList;
import java.util.List;

/**
 * Maksym Dybarskyi | maksym.dybarskyi@tooploox.com
 * 01/03/2017 16:35
 */
class UssdListAdapter extends RecyclerView.Adapter<UssdListAdapter.Holder> {

    class Holder extends RecyclerView.ViewHolder {

        private ViewDataBinding binding;

        Holder(ViewDataBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private List<Ussd> data = new ArrayList<>();

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewDataBinding binding = DataBindingUtil.inflate(inflater, R.layout.item, parent, false);
//        binding.getRoot().setOnClickListener(v -> listener.onClicked(av.rv.getChildAdapterPosition(v)));
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

    public void setDataAndInvalidate(List<Ussd> data) {
        this.data = data;
        this.notifyDataSetChanged();
    }
}
