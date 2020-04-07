package com.vimal.mediaplayer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;

import com.vimal.models.MediaModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class MediaFetcherAsync extends AsyncTask<Void, Void, List<MediaModel>> {
 private Context context;
 private Handler handler;

    public MediaFetcherAsync(Handler handler,Context context) {
        this.context=context;
        this.handler=handler;
    }

    @Override
    protected List<MediaModel> doInBackground(Void... voids) {
        List<MediaModel> mediaModels= new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Video.VideoColumns.DATA, MediaStore.Video.Media.DISPLAY_NAME};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                MediaModel mediaModel = new MediaModel();
                mediaModel.setName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)));
                mediaModel.setUrl(cursor.getString(0));
                mediaModels.add(mediaModel);
            }
            cursor.close();
        }
        return mediaModels;
    }

    @Override
    protected void onPostExecute(List<MediaModel> mediaFilesList) {
        super.onPostExecute(mediaFilesList);
        Message message = Message.obtain();
        message.setTarget(handler);
            if (mediaFilesList != null && mediaFilesList.size()>0)
            {
                message.what = 1;
                Bundle bundle = new Bundle();
                bundle.putSerializable("files", (Serializable) mediaFilesList);
                message.setData(bundle);
            }else {
                message.what = 2;
            }
            message.sendToTarget();
    }
}
