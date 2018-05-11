package com.example.administrator.facedetector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class FaceDetectorActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "FaceDetectorActivity";
    private static final int REQUEST_CAMERA_CODE = 0x100;
    private SurfaceView surfaceView;
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private DrawFacesView facesView;
    private ImageButton video;
    private ImageButton switchImage;
    private int cameraPosition;
    private int tep;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);//日期格式
    private static Date date = new Date();
    private SensorManager sensorManager;
    private SensorEventListener listener;
    private Boolean bIsFocusing;
    private Boolean bIsFocus;

    public static void start(Context context) {
        Intent intent = new Intent(context, FaceDetectorActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tep = 0;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_face_detector);
        cameraPosition = 1;
        initViews();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
                }
                return;
            }
            openSurfaceView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (facesView != null) {
            facesView.removeRect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (facesView != null) {
            facesView.removeRect();
        }
    }

    private void initViews() {
//        surfaceView = new SurfaceView(this);
        surfaceView = (SurfaceView) findViewById(R.id.surface);
        facesView = (DrawFacesView) findViewById(R.id.drawface);
        video = (ImageButton) findViewById(R.id.video);
        switchImage = (ImageButton) findViewById(R.id.facing);
        bIsFocusing = false;
        video.setOnClickListener(this);
        switchImage.setOnClickListener(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (mCamera != null && SesorUtil.isStart() && SesorUtil.isOverRange(event)&&!bIsFocusing) {
                    // 调用自动聚焦回调
                    bIsFocus = false;
                    bIsFocusing = true;
//                    finder_view.bFocused = false;
////            watermark.setVisibility(View.VISIBLE);
//                    finder_view.invalidate();
                    //Log.i(TAG, "==================================onSensorChanged bIsFocus = false");

                    mCamera.cancelAutoFocus();
                    mCamera.autoFocus(autoFocusCB);
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        SesorUtil.startSensor(sensorManager, listener);
//        facesView = new DrawFacesView(this);
//        addContentView(surfaceView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT));
//        addContentView(facesView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT));
    }

    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            bIsFocusing = false;
            if (success) {
                bIsFocus = true;
            } else {
                bIsFocus = false;
            }
        }
    };



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            }
        }
    }


    private void openSurfaceView() {
        mHolder = surfaceView.getHolder();
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mCamera == null) {
                    mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                    mCamera.setFaceDetectionListener(new FaceDetectorListener());
                    try {

                        mCamera.setPreviewDisplay(holder);
                        mCamera.startPreview();
                        // next after startPreview
                        startFaceDetection();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (mHolder.getSurface() == null) {
                    // preview surface does not exist
                    Log.e(TAG, "mHolder.getSurface() == null");
                    return;
                }

                try {
                    mCamera.stopPreview();
                } catch (Exception e) {
                    // ignore: tried to stop a non-existent preview
                    Log.e(TAG, "Error stopping camera preview: " + e.getMessage());
                }

                try {
                    mCamera.setPreviewDisplay(mHolder);
                    int measuredWidth = surfaceView.getMeasuredWidth();
                    int measuredHeight = surfaceView.getMeasuredHeight();
                    setCameraParms(mCamera, measuredWidth, measuredHeight);
                    mCamera.startPreview();
                    // next after startPreview
                    startFaceDetection(); // re-start face detection feature

                } catch (Exception e) {
                    // ignore: tried to stop a non-existent preview
                    Log.d(TAG, "Error starting camera preview: " + e.getMessage());
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCamera.stopPreview();
                // mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
                holder = null;
            }
        });
    }

    public void startFaceDetection() {
        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();
        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0) {
            // mCamera supports face detection, so can start it:
            mCamera.startFaceDetection();
        } else {
            Log.e("tag", "【FaceDetectorActivity】类的方法：【startFaceDetection】: " + "不支持");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.facing:
                Log.d(TAG, "onClick: " + "facing");
                change(v);
                break;
            case R.id.video:
                Log.d(TAG, "onClick: " + "video");
//                Timer nwe = new Timer();
//                nwe.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
                try {
                    takePhoto();
                } catch (IOException e) {
                    e.printStackTrace();
                }


//                Camera.Parameters parameters = mCamera.getParameters();
//                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
//                mCamera.setParameters(parameters);
//                mCamera.takePicture(null, null, pictureCallback);
//                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
//                mCamera.setParameters(parameters);
//                mCamera.takePicture(null, null, pictureCallback);
//                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
//                mCamera.setParameters(parameters);
//                mCamera.takePicture(null, null, pictureCallback);
//                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_SPORTS);
//                mCamera.setParameters(parameters);
//                mCamera.takePicture(null, null, pictureCallback);
//                    }
//                }, 200);
                break;
            default:
                Log.d(TAG, "onClick: ");
                break;
        }
    }

    public void takePhoto() throws IOException {

        if (tep==1) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            mCamera.setParameters(parameters);
        } else if (tep==2){
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_HDR)) {
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
            }
            mCamera.setParameters(parameters);
        } else if (tep==3) {
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_BEACH)) {
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_BEACH);
            }
            mCamera.setParameters(parameters);
        } else if (tep==4) {
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_ACTION)) {
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_ACTION);
            }
            mCamera.setParameters(parameters);
        } else if (tep==5){
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_SPORTS)) {
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_SPORTS);
            }
            mCamera.setParameters(parameters);
        } else if (tep==6){
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_LANDSCAPE)) {
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_LANDSCAPE);
            }
            mCamera.setParameters(parameters);
        } else if (tep==7){
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_PARTY)) {
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_PARTY);
            }
            mCamera.setParameters(parameters);
        } else if (tep==8){
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_BARCODE)) {
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
            }
            mCamera.setParameters(parameters);
        } else if (tep==9){
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_SUNSET)) {
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_SUNSET);
            }
            mCamera.setParameters(parameters);
        } else if (tep==0){

        } else if (tep==10){
//            long spendTime = TimeUtill.stop(0);
//            String spend = new String(String.valueOf(spendTime));
//            writeToFile("takephoto", spend);
        }
        mCamera.takePicture(null, null, pictureCallback);
    }

    //切换前后摄像头
    public void change(View v) {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数

        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
            if (cameraPosition == 1) {
                //现在是后置，变更为前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    mCamera.setFaceDetectionListener(new FaceDetectorListener());
                    facesView.removeRect();
                    try {
                        mCamera.setPreviewDisplay(mHolder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    int measuredWidth = surfaceView.getMeasuredWidth();
                    int measuredHeight = surfaceView.getMeasuredHeight();
                    setCameraParms(mCamera, measuredWidth, measuredHeight);
                    mCamera.startPreview();//开始预览
                    startFaceDetection();
                    cameraPosition = 0;
                    break;
                }
            } else {
                //现在是前置， 变更为后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    mCamera.setFaceDetectionListener(new FaceDetectorListener());
                    facesView.removeRect();
                    try {
                        mCamera.setPreviewDisplay(mHolder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    int measuredWidth = surfaceView.getMeasuredWidth();
                    int measuredHeight = surfaceView.getMeasuredHeight();
                    setCameraParms(mCamera, measuredWidth, measuredHeight);
                    mCamera.startPreview();//开始预览
                    startFaceDetection();
                    cameraPosition = 1;
                    break;
                }
            }

        }
    }


    /**
     * 脸部检测接口
     */
    private class FaceDetectorListener implements Camera.FaceDetectionListener {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0) {
                Camera.Face face = faces[0];
                Rect rect = face.rect;

                Log.d("FaceDetection", "可信度：" + face.score + "face detected: " + faces.length +
                        " Face 1 Location X: " + rect.centerX() +
                        "Y: " + rect.centerY() + "   " + rect.left + " " + rect.top + " " + rect.right + " " + rect.bottom);
                Log.e("tag", "【FaceDetectorListener】类的方法：【onFaceDetection】: ");
                Matrix matrix = updateFaceRect();
                facesView.updateFaces(matrix, faces);
            } else {
                // 只会执行一次
                Log.e("tag", "【FaceDetectorListener】类的方法：【onFaceDetection】: " + "没有脸部");
                facesView.removeRect();
            }
        }
    }


    private Matrix updateFaceRect() {
        Matrix matrix = new Matrix();
        Camera.CameraInfo info = new Camera.CameraInfo();
        // Need mirror for front camera.
        boolean mirror = (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK);
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(90);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(surfaceView.getWidth() / 2000f, surfaceView.getHeight() / 2000f);
        matrix.postTranslate(surfaceView.getWidth() / 2f, surfaceView.getHeight() / 2f);
        return matrix;
    }

    private void setCameraParms(Camera camera, int width, int height) {
        // 获取摄像头支持的pictureSize列表
        Camera.Parameters parameters = camera.getParameters();
        /*List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        // 从列表中选择合适的分辨率
        Camera.Size pictureSize = getProperSize(pictureSizeList, (float) height / width);
        if (null == pictureSize) {
            pictureSize = parameters.getPictureSize();
        }
        // 根据选出的PictureSize重新设置SurfaceView大小
        float w = pictureSize.width;
        float h = pictureSize.height;
        parameters.setPictureSize(pictureSize.width, pictureSize.height);

        surfaceView.setLayoutParams(new FrameLayout.LayoutParams((int) (height * (h / w)), height));

        // 获取摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();
        Camera.Size preSize = getProperSize(previewSizeList, (float) height / width);
        if (null != preSize) {
            parameters.setPreviewSize(preSize.width, preSize.height);
        }
*/
        parameters.setJpegQuality(100);
//        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
//            // 连续对焦
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//        }
//        camera.cancelAutoFocus();
        camera.setDisplayOrientation(90);
        camera.setParameters(parameters);
    }

    private Camera.Size getProperSize(List<Camera.Size> pictureSizes, float screenRatio) {
        Camera.Size result = null;
        for (Camera.Size size : pictureSizes) {
            float currenRatio = ((float) size.width) / size.height;
            if (currenRatio - screenRatio == 0) {
                result = size;
                break;
            }
        }
        if (null == result) {
            for (Camera.Size size : pictureSizes) {
                float curRatio = ((float) size.width) / size.height;
                if (curRatio == 4f / 3) {
                    result = size;
                    break;
                }
            }
        }
        return result;
    }


    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @SuppressLint("WrongConstant")
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            new SavePictureTask().execute(data);
            mCamera.startPreview();
            startFaceDetection();
            if (tep==0) {
                TimeUtill.start();
            }

            if (tep==9) {
                long spendTime = TimeUtill.stop(0);
                String spend = new String(String.valueOf(spendTime));
                try {
                    writeToFile("takephoto", spend);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (++tep < 10) {
                try {
                    takePhoto();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                tep = 0;
            }
        }
    };


    class SavePictureTask extends AsyncTask<byte[], String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(byte[]... params) {
            Log.d("=======", "baackground");
            String filepath = getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
            long now = System.currentTimeMillis();

            try {
                FileOutputStream fos = new FileOutputStream(filepath + "/" + now + ".jpg");
                fos.write(params[0]);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void writeToFile(String type, String msg) throws IOException {

        String filepath =  getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        String logPath = filepath + "/log";
        File file = new File(logPath);
        if (!file.exists()) {
            file.mkdirs();//创建父ew路径
        }
        String fileName = logPath +"/log_" + dateFormat.format(new Date()) + ".log";
        String log = dateFormat.format(date) + " " + type + " " + msg + "\n";
        FileOutputStream fos = null;
        BufferedWriter bw = null;


        fos = new FileOutputStream(fileName, true);
        bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write(log);
        bw.close();
    }

}
