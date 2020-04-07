package com.vimal.mediaplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class EmptyRecyclerViewAdapter extends RecyclerView.Adapter<EmptyRecyclerViewAdapter.ViewHolder> {

    private String mMessage;

    public EmptyRecyclerViewAdapter(String message){
        this. mMessage = message;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.empty_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        if(mMessage != null){
            viewHolder.mMessageView.setText(mMessage);
        }
        return viewHolder;
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {}

    @Override
    public int getItemCount() {
        return 1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mMessageView;
        public ViewHolder(View view) {
            super(view);
            mView = view;
            mMessageView = view.findViewById(R.id.emptyMessage);
        }
    }
}
