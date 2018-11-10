package com.vam.vam;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CameraFragment extends Fragment {
    public static final String TAG = CameraFragment.class.getSimpleName();
    public static final int REQUEST_PERMISION_CODE = 1;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_90, 180);
    }
    HandlerThread bgThread;
    Handler bgThreadHandler;
    TextureView textureView;
    CameraDevice cameraDevice;
    CaptureRequest.Builder captureRequestBuilder;
    CameraCaptureSession cameraCaptureSession;
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            System.out.println("Surface Available");
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            System.out.println("Camera opened");
            cameraDevice = camera;
            createPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };
    Button captureButton;

    public static CameraFragment newInstance() {
        CameraFragment cameraFragment = new CameraFragment();

        return cameraFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera,container,false);
        textureView = view.findViewById(R.id.texture_view_camera_frament);
        textureView.setSurfaceTextureListener(textureListener);
        captureButton = view.findViewById(R.id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startBgThread();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopBgThread();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    void openCamera() {
        CameraManager cameraManager = (CameraManager)getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraDeviceId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDeviceId);
            StreamConfigurationMap configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if(ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(),"Please Grant Permissions to use Camera",Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(getActivity(),
                        new String[] {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISION_CODE);
                return;
            }

            cameraManager.openCamera(cameraDeviceId,stateCallback,bgThreadHandler);
        }catch(CameraAccessException caE) {
            caE.printStackTrace();
        }
    }

    void createPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        Surface surface = new Surface(surfaceTexture);
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if(cameraDevice == null) {
                        Toast.makeText(getContext(), "Camera is Already closed", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    cameraCaptureSession = session;
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO);
                    try {
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, bgThreadHandler);
                    }catch(CameraAccessException caE) {
                        caE.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            },bgThreadHandler);


        } catch(CameraAccessException caE) {
            caE.printStackTrace();
        }

    }

    void takePicture() {
        if(cameraDevice == null) {
            Toast.makeText(getActivity(),"Camera device null",Toast.LENGTH_SHORT).show();
            return;
        }
        CameraManager cameraManager = ActivityCompat.getSystemService(getActivity(),CameraManager.class);
        try {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegsizes = null;
            if(cameraCharacteristics != null) {
                jpegsizes = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if(jpegsizes!=null) {
                width = jpegsizes[0].getWidth();
                height = jpegsizes[0].getHeight();
            }

            ImageReader imageReader = ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
            final List<Surface> surfaces = new ArrayList<>();
            surfaces.add(imageReader.getSurface());
            surfaces.add(new Surface(textureView.getSurfaceTexture()));
            System.out.println("surfaces : "+surfaces.size());
            final CaptureRequest.Builder captRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captRequestBuilder.addTarget(imageReader.getSurface());
            captRequestBuilder.set(CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO);

            /*int deviceOrientation = getActivity().getWindowManager().getDefaultDisplay().getOrientation();
            captRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(deviceOrientation));*/

            imageReader.setOnImageAvailableListener(reader -> {
                try (Image image = reader.acquireLatestImage()) {
                   // predictInput(image,rotationCompensation);

                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    //write to the file
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    System.out.println(bitmap);
                    System.out.println("Written to bitmap");
                    uploadFile(saveBitmap(bitmap));

                }
            },bgThreadHandler);

            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                                           TotalCaptureResult result) {
                                super.onCaptureCompleted(session, request, result);
                                CameraFragment.this.createPreview();
                            }
                        },bgThreadHandler);
                    } catch(CameraAccessException caE) {
                        caE.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            },bgThreadHandler);
        } catch(CameraAccessException caE) {
            caE.printStackTrace();
        }
    }

    private void startBgThread() {
        bgThread = new HandlerThread("Background Thread");
        bgThread.start();
        bgThreadHandler = new Handler(bgThread.getLooper());
    }

    private void stopBgThread() {
        bgThread.quitSafely();
        try {
            bgThread.join();
            bgThread = null;
            bgThreadHandler = null;
        } catch(InterruptedException iE) {
            iE.printStackTrace();
        }
    }

    public File saveBitmap(Bitmap bitmap) {
        // Get the directory for the user's public pictures directory.
        if(!isExternalStorageWritable()) {
            Log.e(TAG, "External Storage not writable");
            return null;
        }
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "vamimages");
        Log.i(TAG,"directory pictures exists : "+dir.exists());
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Directory not created");
            return null;
        }
        String fileName  = "vam"+String.valueOf(Calendar.getInstance().getTimeInMillis())+".png";
        File file = new File(dir,fileName);
        try(FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,fos);
            Toast.makeText(getContext(),file.getAbsolutePath(),Toast.LENGTH_LONG).show();
            return file;
        } catch(IOException ioE) {
            ioE.printStackTrace();
            Log.e(TAG,"Unable to write to the file");
            return null;
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public void uploadFile(File file) {
        Log.i(TAG, "Uploading file");
        final StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("vamimages").child(file.getName());
        UploadTask uploadTask = storageRef.putFile(Uri.fromFile(file));
        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Upload Task failed");
                    throw task.getException();
                }
                Log.i(TAG, "Upload Task Successful. File successfully uploaded to Firebase storage");
                return storageRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if(task.isSuccessful()) {
                    Uri uri = task.getResult();
                    FirebaseDatabase.getInstance().getReference()
                            .child("vamimages")
                            .push()
                            .setValue(uri.toString());
                    Log.i(TAG,"Download URL Task successful");
                } else {
                    Log.e(TAG,"Download URL Task failed");
                    Log.e(TAG,task.getException().getMessage());
                }
            }
        });
    }
}
