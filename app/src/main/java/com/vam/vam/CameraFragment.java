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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraFragment extends Fragment {
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

            int deviceOrientation = getActivity().getWindowManager().getDefaultDisplay().getOrientation();
            captRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(deviceOrientation));
            //roatation compensation for firebase vision
           // int sensorOrientaion = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            /*final int rotationCompensation = ROTATION_COMPENSATIONS
                    .get((ORIENTATIONS.get(deviceOrientation)+sensorOrientaion+270)%360);*/
            final File file = new File(Environment.getDownloadCacheDirectory(),"/capture.jpg");
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    try (Image image = reader.acquireLatestImage()) {
                       // predictInput(image,rotationCompensation);
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        //write to the file
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        System.out.println(bitmap);
                        System.out.println("Written to bitmap");
                    }
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
                                Toast.makeText(getActivity(),"saved to "+file,Toast.LENGTH_LONG).show();
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
}
