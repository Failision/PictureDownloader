package com.example.mercury.picturedownloader;


import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    private Button downloadButton;
    private Button deleteButton;
    private ProgressBar progressBar;
    private TextView textViewLoad;
    private ImageLoader iLoader;
    private ImageView img;
    private Bitmap bitmap;
    private String statusLoad;
    private boolean download_button_flag = false;
    final int PROGRESS_BAR_MAX = 100;
    final String UPDATE_PROGRESS_BAR_ACTION = "update_progress_bar";

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(MyReceiver,
                new IntentFilter(UPDATE_PROGRESS_BAR_ACTION));
        super.onResume();
    }

    private BroadcastReceiver MyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress_count", 0);
            statusLoad = intent.getStringExtra("status_load");
            if(progress > 0) {
                progressBar.setProgress(progress);
            }
            if(progress < 0){
                progressBar.setIndeterminate(true);
            }
            if(getResources().getString(R.string.string_status_downloading) == statusLoad){
                textViewLoad.setText(statusLoad);
                progressBar.setVisibility(View.VISIBLE);
                downloadButton.setEnabled(false);
            }
            if(intent.getBooleanExtra("bar_visible", true) == false){
                progressBar.setVisibility(View.INVISIBLE);
                textViewLoad.setText(statusLoad);
                if(isPictureExist()){setImage();}
                downloadButton.setEnabled(true);
                deleteButton.setEnabled(true);
            }
        }
    };

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(MyReceiver);
        super.onPause();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        downloadButton = (Button) findViewById(R.id.id_download_button);
        progressBar = (ProgressBar) findViewById(R.id.id_progress_bar);
        textViewLoad = (TextView) findViewById(R.id.id_status_download);
        deleteButton = (Button) findViewById(R.id.id_delete_button);
        progressBar.setMax(PROGRESS_BAR_MAX);
        progressBar.setVisibility(View.INVISIBLE);
        statusLoad = getResources().getString(R.string.string_status_idle);
        textViewLoad.setText(statusLoad);
        final String url = (String) getResources().getString(R.string.url);
        final boolean isPictureExist = isPictureExist();

        deleteButton.setEnabled(false);
        if(isPictureExist){
            deleteButton.setEnabled(true);
            setImage();
        }

        if (savedInstanceState != null) {
            download_button_flag = savedInstanceState.getBoolean("load_flag");
            statusLoad = savedInstanceState.getString("status_load");
            textViewLoad.setText(statusLoad);
            downloadButton.setText(savedInstanceState.getString("downloadButton_text"));
            if (statusLoad == getResources().getString(R.string.string_status_downloading)) {
                progressBar.setVisibility(View.VISIBLE);
                downloadButton.setEnabled(false);
            }
        }
        else
            if (isPictureExist) {
                Toast toast = Toast.makeText(MainActivity.this, "Image has already downloaded.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM, 0, 150);
                toast.show();
            }

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (download_button_flag == true) {
                    File file = new File(Environment.getExternalStorageDirectory() + "/dirr/" + "test_pic.png");
                    Intent intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    Uri uri = Uri.parse("file://" + file.getAbsolutePath());
                    intent.setDataAndType(uri, "image/*");
                    List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
                    String activityName = list.get(0).activityInfo.name;
                    String activityPackageName = list.get(0).activityInfo.packageName;
                    intent.setClassName(activityPackageName, activityName);
                    startActivity(intent);
                } else {
                    if (isConnected(MainActivity.this)) {
                        iLoader = new ImageLoader();
                        iLoader.execute(url);
                    } else {
                        Toast.makeText(MainActivity.this, "Network connection error, check your connection", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    File file = new File(Environment.getExternalStorageDirectory() + "/dirr/" + "test_pic.png");
                    file.delete();
                    setImage();
                    download_button_flag = false;
                    statusLoad = getResources().getString(R.string.string_status_idle);
                    textViewLoad.setText(statusLoad);
                    downloadButton.setText("Download");
                    deleteButton.setEnabled(false);
            }
        });
    }

    public class ImageLoader extends AsyncTask<String, Integer, Bitmap> {

        byte[] bytes;
        Bitmap bitmap;
        int progCount = 1;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            statusLoad = (String) getResources().getString(R.string.string_status_downloading);
            textViewLoad.setText(statusLoad);
            downloadButton.setEnabled(false);
        }

        protected Bitmap doInBackground(final String... args) {
            try {
                Intent intent = new Intent(UPDATE_PROGRESS_BAR_ACTION);
                URL url = new URL(args[0]);
                InputStream is = (InputStream) url.getContent();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int length = connection.getContentLength();
                length = (int) (length > 0 ? length / 100 : 1024);
                byte[] buffer = new byte[length];
                int bytesRead;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                while ((bytesRead = is.read(buffer)) != -1) {
                    onProgressUpdate();
                    Thread.sleep(50);
                    output.write(buffer, 0, bytesRead);
                    if (connection.getContentLength() < 0) {
                        intent.putExtra("progress_count", -1);
                    } else {
                        intent.putExtra("progress_count", progCount);
                        progCount++;
                    }
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
                }
                bytes = output.toByteArray();
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                saveImageToInternalStorage(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate();
            statusLoad = getResources().getString(R.string.string_status_downloading);
            Intent intent = new Intent(UPDATE_PROGRESS_BAR_ACTION);
            intent.putExtra("status_load", statusLoad);
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
        }

        protected void onPostExecute(Bitmap image) {
            Intent intent = new Intent(UPDATE_PROGRESS_BAR_ACTION);
            progressBar.setIndeterminate(false);
            if (image != null) {
                setImage();
                downloadButton.setText("Open");
                download_button_flag = true;
                statusLoad = (String) getResources().getString(R.string.string_status_downloaded);
                Toast.makeText(MainActivity.this, "Image saved.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Image Does Not exist or You have some troubles", Toast.LENGTH_SHORT).show();
                statusLoad = (String) getResources().getString(R.string.string_status_idle);
            }
            intent.putExtra("status_load", statusLoad);
            textViewLoad.setText(statusLoad);
            intent.putExtra("bar_visible", false);
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
        }

        public void saveImageToInternalStorage(Bitmap image) {
            try {
                if (image != null) {
                    File file = new File(Environment.getExternalStorageDirectory() + "/dirr");
                    if (!file.isDirectory()) {
                        file.mkdir();
                    }
                    file = new File(Environment.getExternalStorageDirectory() + "/dirr", "test_pic" + ".png");
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    try {
                        image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                    } finally {
                        fileOutputStream.close();
                    }
                }
            } catch (Exception e) {
                Log.e("TAAG", e.getMessage());
            }
        }
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    public boolean isPictureExist() {
        File file = new File(Environment.getExternalStorageDirectory() + "/dirr/" + "test_pic.png");
        return file.exists();
    }

    public void setImage(){
        img = (ImageView) findViewById(R.id.id_download_image);
        download_button_flag = true;
        downloadButton.setText("Open");
        bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/dirr/" + "test_pic.png");
        img.setImageBitmap(bitmap);
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("status_load", statusLoad);
        if (download_button_flag == false) {
            outState.putBoolean("load_flag", false);
            outState.putString("downloadButton_text", "Download");
        } else {
            outState.putBoolean("load_flag", true);
            outState.putString("downloadButton_text", "Open");
        }
    }
}
