package com.example.prsmusic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView noMusicTextView;
    SearchView searchView;
    ArrayList<AudioModel> songsList = new ArrayList<>();
    ArrayList<AudioModel> filteredSongsList = new ArrayList<>();
    private static final int PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Ensure correct layout is used

        recyclerView = findViewById(R.id.recycler_view);
        noMusicTextView = findViewById(R.id.no_songs_text); // Make sure this ID matches in XML
        searchView = findViewById(R.id.search_view);

        // Check for permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission()) {
                requestPermission();
            } else {
                loadSongs();
            }
        } else {
            loadSongs(); // No need for runtime permission on Android versions below API 23
        }

        // Set up search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // Optional: handle search submission
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText);
                return true;
            }
        });
    }

    // Check if the permission is granted
    boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    // Request the permission
    void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 (API level 33) and above, request the READ_MEDIA_AUDIO permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, PERMISSION_CODE);
        } else {
            // For lower Android versions, request the READ_EXTERNAL_STORAGE permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
        }
    }

    // Handle the permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs();
            } else {
                Toast.makeText(this, "Permission Denied. Please enable it in Settings.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Load the songs if permission is granted
    private void loadSongs() {
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Cursor cursor = null;

        try {
            cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    String duration = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                    AudioModel songData = new AudioModel(path, title, duration);
                    if (new File(songData.getPath()).exists()) {
                        songsList.add(songData);
                    }
                } while (cursor.moveToNext());
            }

            if (songsList.isEmpty()) {
                noMusicTextView.setVisibility(View.VISIBLE);
            } else {
                noMusicTextView.setVisibility(View.GONE);
                filteredSongsList.addAll(songsList); // Initialize filtered list with all songs
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(new MusicLister(filteredSongsList, getApplicationContext()));
            }
        } finally {
            if (cursor != null) {
                cursor.close(); // Close the cursor to prevent memory leaks
            }
        }
    }

    // Filter songs based on search query
    private void filterSongs(String query) {
        filteredSongsList.clear();
        if (query.isEmpty()) {
            filteredSongsList.addAll(songsList);
        } else {
            for (AudioModel song : songsList) {
                if (song.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filteredSongsList.add(song);
                }
            }
        }
        recyclerView.getAdapter().notifyDataSetChanged(); // Refresh the RecyclerView
    }
}
