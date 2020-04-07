package com.vimal.mediaplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.vimal.models.MediaModel;

import java.util.List;

public class MediaListAdapter extends RecyclerView.Adapter<MediaListAdapter.viewHolder> {

    private final List<MediaModel> mediaList;
    private MedialistClick medialistClick;

    public MediaListAdapter(List<MediaModel> mediaList) {
        this.mediaList = mediaList;
    }

    @NonNull
    @Override
    public MediaListAdapter.viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_list_item, parent, false);
        return new viewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaListAdapter.viewHolder holder, final int position) {
        holder.name.setText(mediaList.get(position).getName());
        holder.cardView.setOnClickListener(v -> {medialistClick.onClick(mediaList.get(position));
        });
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    class viewHolder extends RecyclerView.ViewHolder {
       private TextView name,isPlaying;
       private CardView cardView;

        viewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            name = itemView.findViewById(R.id.nameTextView);
            isPlaying = itemView.findViewById(R.id.playingText);
        }
    }

    void click(MedialistClick medialistClick) {
        this.medialistClick = medialistClick;
    }
}
