package com.aware.plugin.upmc.dash.adapters;

import android.support.v7.widget.RecyclerView;
import android.support.wear.widget.BoxInsetLayout;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aware.plugin.upmc.dash.R;

public class InabilityAdapter extends RecyclerView.Adapter<InabilityAdapter.ViewHolder> {
    private String[] mDataset;

    public InabilityAdapter(String[] myDataset) {
        mDataset = myDataset;
    }

    @Override
    public InabilityAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.inability_text_view, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(mDataset[position]);
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView;

        public ViewHolder(TextView b) {
            super(b);
            textView = b;

        }
    }

}
