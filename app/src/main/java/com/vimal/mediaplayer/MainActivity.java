package com.vimal.mediaplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;
import com.vimal.Config;
import com.vimal.models.MediaModel;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    private static final int seekInterval = 5000;

    private Context context;
    private RecyclerView recyclerMediaList;
    private MediaListAdapter mediaListAdapter;
    private SimpleExoPlayer player;

    private ImageView fullScreenIcon, volumeMuteButton;
    private TextView titleTextView;
    private boolean mExoPlayerFullscreen = false;
    private int PLAYER_VOLUME = 100;
    private boolean isMute = false;
    private PlayerView playerView;
    private AppCompatSeekBar volumeSeekBar;
    private ImageButton forward,rewind;
    private ConstraintLayout mainView;
    private EmptyRecyclerViewAdapter emptyRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;
        recyclerMediaList = findViewById(R.id.recyclerMediaList);
        mainView = findViewById(R.id.mainView);
        playerView = new PlayerView(this);
        playerView = findViewById(R.id.playerView);
        playerView.setVisibility(View.GONE);



        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        } else {
            new MediaFetcherAsync(new FilesHandler(), context).execute();
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (player != null)
                if (player.isPlayingAd()) {
                    pausePlayer();

                    openFullscreen();
                    startPlayer();
                }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (player != null)
                if (player.isPlayingAd()) {
                    pausePlayer();
                    closeFullScreen();
                    startPlayer();

                }
        }
    }

    private void setUpPlayer(MediaModel mediaModel) {
        playerView.setVisibility(View.VISIBLE);
        volumeMuteButton = findViewById(R.id.volumeMuteButton);
        volumeMuteButton.setOnClickListener(this);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        volumeSeekBar.setMax(100);
        fullScreenIcon = findViewById(R.id.exo_fullscreen_icon);
        titleTextView = findViewById(R.id.titleVideo);
        titleTextView.setText(mediaModel.getName());
        fullScreenIcon.setOnClickListener(view -> {
            if (!mExoPlayerFullscreen)
                openFullscreen();
            else
                closeFullScreen();

        });
        forward = findViewById(R.id.forward);
        rewind = findViewById(R.id.rewind);

        forward.setOnClickListener(click-> seekForward());
        rewind.setOnClickListener(click-> seekBackward());


        //  Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, new DefaultTrackSelector());
        playerView.setShutterBackgroundColor(Color.BLACK);
        //Handling audio focus.
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build();
        player.setAudioAttributes(audioAttributes, true);
        // Checking the Audio shared  preference is present on not
        boolean isAudioPreferencePresent = checkAudioPreferenceData();
        if (isAudioPreferencePresent) {
            //Getting stored values from shared preference and setting the player state.
            SharedPreferences audioPreference = context.getSharedPreferences(Config.PLAYER_AUDIO_PREFERENCE, Context.MODE_PRIVATE);
            PLAYER_VOLUME = audioPreference.getInt(Config.PLAYER_VOLUME, 0);
            isMute = audioPreference.getBoolean(Config.MUTE_STATE, false);
            player.setVolume(PLAYER_VOLUME / 100f);
            setMute(false);
            volumeSeekBar.setProgress(PLAYER_VOLUME);
        } else {
            player.setVolume(PLAYER_VOLUME / 100f);
            volumeSeekBar.setProgress(PLAYER_VOLUME);
            //Create Shared preference
            SharedPreferences audioPreference = context.getSharedPreferences(Config.PLAYER_AUDIO_PREFERENCE, Context.MODE_PRIVATE);
            SharedPreferences.Editor preferenceEditor = audioPreference.edit();
            preferenceEditor.putInt(Config.PLAYER_VOLUME, PLAYER_VOLUME);
            preferenceEditor.putBoolean(Config.MUTE_STATE, isMute);
            preferenceEditor.apply();
        }

        // Bind the player to the view.
        playerView.setPlayer(player);
        //building Media source:
        MediaSource mediaSource = null;
        mediaSource = playVideoFromUrl(this, mediaModel.getUrl());
        player.prepare(mediaSource);
        //player plays in potrait screen
        closeFullScreen();

        // player video listener
        player.addVideoListener(new VideoListener() {
            @Override
            public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

            }
        });

        // player event listener
        player.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_READY: {
                        findViewById(R.id.exo_btns_fl).setVisibility(View.VISIBLE);
                        break;
                    }
                    case Player.STATE_BUFFERING: {
                        findViewById(R.id.exo_btns_fl).setVisibility(View.INVISIBLE);
                        break;
                    }
                    case Player.STATE_IDLE:
                        break;
                    case Player.STATE_ENDED: {
                        findViewById(R.id.exo_btns_fl).setVisibility(View.VISIBLE);
                        break;
                    }
                }
            }
        });

        //Player Events listener.
        player.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (!playWhenReady) {
                    pausePlayer();
                } else {
                    startPlayer();
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.e("ExoPlaybackException", error.toString());
            }

            @Override
            public void onPositionDiscontinuity(int reason) {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }

            @Override
            public void onSeekProcessed() {

            }
        });

        //Volume seekBar change listener.
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int volume, boolean b) {
                PLAYER_VOLUME = volume;
                if (PLAYER_VOLUME == 0) {
                    setMute(true);
                } else {
                    setMute(false);
                }
                player.setVolume(volume / 100f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateAudioPreference();
            }
        });
    }


    @Override
    public void onBackPressed() {

        if (mExoPlayerFullscreen)
            closeFullScreen();
        else {
            super.onBackPressed();

            if (player != null) {
                player.stop();
            }
        }

    }



    /**
     * Method for checking the audio shared preference is present or not.
     *
     * @return true-present, false-not present.
     */
    private boolean checkAudioPreferenceData() {
        File prefFile = new File("/data/data/" + getPackageName() + "/shared_prefs/" + Config.PLAYER_AUDIO_PREFERENCE + ".xml");
        return prefFile.exists();
    }


    /**
     * Creating the media source from local file path
     *
     * @param context  Application context
     * @param filePath Local file path
     * @return file uri.
     */
    private MediaSource playVideoFromFile(Context context, String filePath) {
        return playVideoFromUri(context, Uri.fromFile(new File(filePath)));
    }

    /**
     * Creating the media source from url
     *
     * @param context Application context
     * @param url     Url from Server
     * @return file uri.
     */
    private MediaSource playVideoFromUrl(Context context, String url) {
        return playVideoFromUri(context, Uri.parse(url));
    }

    /**
     * Method for creating the media source from the file uri.
     *
     * @param context Application context.
     * @param parse   File uri
     * @return media source.
     */
    private MediaSource playVideoFromUri(Context context, Uri parse) {
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "GoSee Place"));
        return new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(parse);
    }

    /**
     * Method for showing the video player in full screen mode.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private void openFullscreen() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mainView);
        constraintSet.connect(R.id.mainView,ConstraintSet.RIGHT,R.id.mainView,ConstraintSet.LEFT,0);
        constraintSet.connect(R.id.mainView,ConstraintSet.TOP,R.id.mainView,ConstraintSet.BOTTOM,0);
        constraintSet.applyTo(mainView);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        fullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_fullscreen_shrink));
        mExoPlayerFullscreen = true;
    }

    /**
     * Method for showing the video player in portrait mode.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private void closeFullScreen() {
        mExoPlayerFullscreen = false;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        fullScreenIcon.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_fullscreen_expand));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.volumeMuteButton: {
                if (isMute)
                    setMute(false);
                else
                    setMute(true);
                break;
            }
        }
    }

    /**
     * Method for seek forward seekInterval (5 seconds)
     */
    private void seekForward() {
        player.seekTo(player.getCurrentPosition() + seekInterval);
    }

    /**
     * Method for seekbackward seekInterval (5 seconds)
     */
    private void seekBackward() {
        player.seekTo(player.getCurrentPosition() - seekInterval);
    }

    /**
     * Method for mute and un-mute audio.
     *
     * @param toMute true -for mute, false - for un-mute.
     */
    private void setMute(boolean toMute) {
        if (toMute) {
            player.setVolume(0f);
            isMute = true;
            volumeMuteButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_volume_mute));
            updateAudioPreference();
        } else {
            player.setVolume(PLAYER_VOLUME / 100f);
            isMute = false;
            volumeMuteButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_volume_up));
            updateAudioPreference();
        }
    }

    /**
     * Method for updating the audio shared preference.
     */
    private void updateAudioPreference() {
        SharedPreferences audioPreference = context.getSharedPreferences(Config.PLAYER_AUDIO_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor preferenceEditor = audioPreference.edit();
        preferenceEditor.putInt(Config.PLAYER_VOLUME, PLAYER_VOLUME);
        preferenceEditor.putBoolean(Config.MUTE_STATE, isMute);
        preferenceEditor.apply();
    }

    /**
     * Method for pause the player.
     */
    private void pausePlayer()
    {
        player.setPlayWhenReady(false);
        player.getPlaybackState();
    }

    /**
     * Method for start the player.
     */
    private void startPlayer() {
        player.setPlayWhenReady(true);
        player.getPlaybackState();
    }

    private class FilesHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            List<MediaModel> mediaModels;
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
            linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
            recyclerMediaList.setLayoutManager(linearLayoutManager);
            switch (msg.what) {
                case 1:
                    Bundle bundle = msg.getData();
                    mediaModels = (List<MediaModel>) bundle.getSerializable("files");
                    mediaListAdapter = new MediaListAdapter(mediaModels);
                    recyclerMediaList.setAdapter(mediaListAdapter);
                    mediaListAdapter.click(media -> {
                        if(player!=null) {
                            player.stop();
                        }
                        setUpPlayer(media);
                    });
                    break;
                case 2:{
                    emptyRecyclerViewAdapter = new EmptyRecyclerViewAdapter(getString(R.string.emptyMessage));
                    recyclerMediaList.setAdapter(emptyRecyclerViewAdapter);
                    break;
                }

                default:
                    mediaModels = null;
            }

        }
    }

}
