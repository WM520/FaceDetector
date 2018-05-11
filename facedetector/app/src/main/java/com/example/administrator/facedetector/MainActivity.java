package com.example.administrator.facedetector;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity  {
    private static final int PICK_IMAGE_ALBUM = 0x12;
    private static final int REQUEST_PERMISSION = 0x1000;
    private ImageView iv;
    private Button takePic;
    private static final String TAG = "MainActivity";
    private Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            }
        }
        mContext = this;
    }


    private void initViews() {
        findViewById(R.id.btn_de_face_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ");
                openAlbum();
            }
        });
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: 11111111111");
                FaceDetectorActivity.start(mContext);
            }
        });
        iv = ((ImageView) findViewById(R.id.iv));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        switch (requestCode) {
            case PICK_IMAGE_ALBUM: {
                String[] cloum = {MediaStore.Images.Media.DATA};
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri, cloum, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(cloum[0]);
                    String imagePath = cursor.getString(columnIndex);
                    Log.e("tag", "【MainActivity】类的方法：【onActivityResult】: " + imagePath);
                    Bitmap bitmap = new FaceSDK().DetectionBitmap(BitmapFactory.decodeFile(imagePath));
                    iv.setImageBitmap(bitmap);
                    cursor.close();
                }
            }
            break;
        }
    }



    private void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_ALBUM);
    }

}
