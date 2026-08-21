package com.six.fortuna;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class AnnounceActivity extends AppCompatActivity {
    private int currentIndex = 0;
    private PrescriptStore store;
    private MediaPlayer mediaPlayer;
    private boolean wasPlayingBeforeStop = false;

    public void addAnnounce(String e, ArrayList<String> announce) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            announce.addFirst(e);
        } else {
            announce.add(0, e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new PrescriptStore(this);
        mediaPlayer = MediaPlayer.create(this, R.raw.lobotomy_2);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(store.loadVolume(), store.loadVolume());
        mediaPlayer.start();
        setContentView(R.layout.announce);

        ArrayList<String> announces = new ArrayList<>();
        addAnnounce(getString(R.string.announce_1), announces);
        addAnnounce(getString(R.string.announce_2), announces);
        addAnnounce(getString(R.string.announce_3), announces);
        addAnnounce(getString(R.string.announce_4), announces);
        addAnnounce(getString(R.string.announce_5), announces);
        addAnnounce(getString(R.string.announce_6), announces);
        addAnnounce(getString(R.string.announce_7), announces);
        addAnnounce(getString(R.string.announce_8), announces);
        addAnnounce(getString(R.string.announce_9), announces);

        TextView tvAnnounce = findViewById(R.id.Announcement);
        refreshUI(tvAnnounce, announces, currentIndex);

        findViewById(R.id.done).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingActivity.class));
        });

        findViewById(R.id.toNext).setOnClickListener(v -> {
            if (currentIndex <= 0) {
                Toast.makeText(this, getString(R.string.toast_no_newer), Toast.LENGTH_SHORT).show();
            } else {
                currentIndex--;
                refreshUI(tvAnnounce, announces, currentIndex);
            }
        });

        findViewById(R.id.toPrevious).setOnClickListener(v -> {
            if (currentIndex >= announces.size() - 1) {
                Toast.makeText(this, getString(R.string.toast_no_older), Toast.LENGTH_SHORT).show();
            } else {
                currentIndex++;
                refreshUI(tvAnnounce, announces, currentIndex);
            }
        });
    }

    public void refreshUI(TextView area, ArrayList<String> announces, int index) {
        area.setText(announces.get(index));
    }

    public void changeMusic(int musicResId) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        mediaPlayer = MediaPlayer.create(this, musicResId);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            wasPlayingBeforeStop = true;
            mediaPlayer.pause();
        } else {
            wasPlayingBeforeStop = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (wasPlayingBeforeStop && mediaPlayer != null) {
            mediaPlayer.start();
        }
    }
}