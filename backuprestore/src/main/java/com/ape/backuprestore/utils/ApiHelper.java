/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ape.backuprestore.utils;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.hardware.Camera;
import android.os.Build;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ApiHelper {
    private static final String TAG = "CAM_ApiHelper";

    public static interface VERSION_CODES {
        // These value are copied from Build.VERSION_CODES
        public static final int GINGERBREAD_MR1 = 10;
        public static final int HONEYCOMB = 11;
        public static final int HONEYCOMB_MR1 = 12;
        public static final int HONEYCOMB_MR2 = 13;
        public static final int ICE_CREAM_SANDWICH = 14;
        public static final int ICE_CREAM_SANDWICH_MR1 = 15;
        public static final int JELLY_BEAN = 16;
        public static final int JELLY_BEAN_MR1 = 17;
        public static final int JELLY_BEAN_MR2 = 18;
    }

    public static final String DATACB = "android.hardware.Camera.CameraDataCallback";

    public static final String CAMERA_CLASSNAME = "android.hardware.Camera";
    public static final String CAMERA_PARAMETERS_CLASSNAME = "android.hardware.Camera$Parameters";
    // ////////
    public static final String TINNO_FEATURE = "com.tinno.android.feature.tinnoFeature";
    public static final String FEATURE_CONFIG = "com.myos.camera.setting.FeatureConfig";

    public static final String FEATURE_STRING = "com.tinno.android.feature.FeatureString";

    public static final String SYSTEM_MANAGER = "com.tinno.android.server.systeminterface.SystemManager";

    public static final String FEATURE_CONFIG_BOOL_VALUE = "getBooleanValue";

    public static final String FEATURE_CONFIG_INT_VALUE = "getIntValue";

    public static final String SECURE_SUPPORT = "enable_secure";
    public static final String Telephony_Properties = "com.android.internal.telephony.TelephonyProperties";

    public static final String Media_Recorder = "android.media.MediaRecorder";
    public static final String Media_RecorderEX_MTK = "com.mediatek.media.MediaRecorderEx";

    public static final String NVCAMERA_CLASSNAME = "com.nvidia.NvCamera";

    public static final String NVCAMERA_NVPARAMETERS_CLASSNAME = "com.nvidia.NvCamera$NvParameters";
    public static final String QCCAMERA_CLASSNAME = "android.hardware.Camera";
    public static final String QCCAMERA_PARAMETERS_CLASSNAME = "android.hardware.Camera$Parameters";
    public static final String IMAGE_EFFECT_SERVICE_CLASSNAME = "android.media.ImageEffectService";

    public static final String IMAGE_EFFECT_PROXY_CLASSNAME = "android.media.ImageEffectProxy";

    public static final String SYSTEMPROPERTIES = "android.os.SystemProperties";
    // public static final String CRYPTO_CLASSNAME =
    // "com.tinno.android.crypto.CryptoManagerClient";

    // public static final String CRYPTO_PRIVATEFILE =
    // "com.tinno.android.crypto.PrivateFile";

    public static final String CRYPTO_NOTIFY_SECURESERVER = "notifySecureServer";
    public static final String TN_PRIVATE_IMAGE_NUMBER = "image_number";

    public static final String TN_PRIVATE_AUDIO_IMAGE = "isAudioJpeg";
    public static final boolean HAS_FINE_RESOLUTION_QUALITY_LEVELS = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    // /
    public static final boolean AT_LEAST_16 = Build.VERSION.SDK_INT >= 16;

    public static final boolean USE_888_PIXEL_FORMAT = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;

    public static final boolean ENABLE_PHOTO_EDITOR = Build.VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH;

    public static final boolean HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE = hasField(
            View.class, "SYSTEM_UI_FLAG_LAYOUT_STABLE");

    public static final boolean HAS_VIEW_SYSTEM_UI_FLAG_HIDE_NAVIGATION = hasField(
            View.class, "SYSTEM_UI_FLAG_HIDE_NAVIGATION");

    public static final boolean HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT = hasField(
            MediaColumns.class, "WIDTH");

    public static final boolean HAS_REUSING_BITMAP_IN_BITMAP_REGION_DECODER = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;

    public static final boolean HAS_REUSING_BITMAP_IN_BITMAP_FACTORY = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_SET_BEAM_PUSH_URIS = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;

    public static final boolean HAS_SET_DEFALT_BUFFER_SIZE = hasMethod(
            "android.graphics.SurfaceTexture", "setDefaultBufferSize",
            int.class, int.class);

    public static final boolean HAS_RELEASE_SURFACE_TEXTURE = hasMethod(
            "android.graphics.SurfaceTexture", "release");

    public static final boolean HAS_SURFACE_TEXTURE = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_MTP = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1;

    public static final boolean HAS_AUTO_FOCUS_MOVE_CALLBACK = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;

    public static final boolean HAS_REMOTE_VIEWS_SERVICE = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_INTENT_EXTRA_LOCAL_ONLY = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_SET_SYSTEM_UI_VISIBILITY = hasMethod(
            View.class, "setSystemUiVisibility", int.class);

    public static final boolean HAS_FACE_DETECTION;

    static {
        boolean hasFaceDetection = false;
        try {
            Class<?> listenerClass = Class
                    .forName("android.hardware.Camera$FaceDetectionListener");
            hasFaceDetection = hasMethod(Camera.class,
                    "setFaceDetectionListener", listenerClass)
                    && hasMethod(Camera.class, "startFaceDetection")
                    && hasMethod(Camera.class, "stopFaceDetection")
                    && hasMethod(Camera.Parameters.class,
                    "getMaxNumDetectedFaces");
        } catch (Throwable t) {
        }
        HAS_FACE_DETECTION = hasFaceDetection;
    }

    public static final boolean mHasSmileshot;

    static {
        boolean hasSmileshot = false;
        try {
            Class<?> listenerClass = Class
                    .forName("android.hardware.Camera$SmileCallback");
            hasSmileshot = hasMethod(Camera.class, "setSmileCallback",
                    listenerClass);

        } catch (Throwable t) {
        }
        mHasSmileshot = hasSmileshot;
    }

    public static final boolean mMTKHasCshot;

    static {
        boolean hasContinuousshot = false;
        try {
            Class<?> listenerClass = Class
                    .forName("android.hardware.Camera$ContinuousShotCallback");
            hasContinuousshot = hasMethod(Camera.class,
                    "setContinuousShotCallback", listenerClass);

        } catch (Throwable t) {
        }
        mMTKHasCshot = hasContinuousshot;
    }

    public static final boolean HAS_GET_CAMERA_DISABLED = hasMethod(
            DevicePolicyManager.class, "getCameraDisabled", ComponentName.class);

    public static final boolean HAS_MEDIA_ACTION_SOUND = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;

    public static final boolean HAS_TIME_LAPSE_RECORDING = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_ZOOM_WHEN_RECORDING = Build.VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH;

    public static final boolean HAS_CAMERA_FOCUS_AREA = Build.VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH;

    public static final boolean HAS_CAMERA_METERING_AREA = Build.VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH;

    public static final boolean HAS_MOTION_EVENT_TRANSFORM = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_EFFECTS_RECORDING = false;

    // "Background" filter does not have "context" input port in jelly bean.
    public static final boolean HAS_EFFECTS_RECORDING_CONTEXT_INPUT = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1;

    public static final boolean HAS_GET_SUPPORTED_VIDEO_SIZE = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_SET_ICON_ATTRIBUTE = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_MEDIA_PROVIDER_FILES_TABLE = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_SURFACE_TEXTURE_RECORDING = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;

    public static final boolean HAS_ACTION_BAR = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    // Ex: View.setTranslationX.
    public static final boolean HAS_VIEW_TRANSFORM_PROPERTIES = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_CAMERA_HDR = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1;

    public static final boolean HAS_OPTIONS_IN_MUTABLE = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean CAN_START_PREVIEW_IN_JPEG_CALLBACK = Build.VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH;

    public static final boolean HAS_VIEW_PROPERTY_ANIMATOR = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1;

    public static final boolean HAS_POST_ON_ANIMATION = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;

    public static final boolean HAS_ANNOUNCE_FOR_ACCESSIBILITY = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;

    public static final boolean HAS_OBJECT_ANIMATION = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_GLES20_REQUIRED = Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB;

    public static final boolean HAS_ROTATION_ANIMATION = hasField(
            WindowManager.LayoutParams.class, "rotationAnimation");

    public static final boolean HAS_ORIENTATION_LOCK = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2;

    public static final boolean HAS_CANCELLATION_SIGNAL = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;

    public static final boolean HAS_MEDIA_MUXER = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2;

    public static final boolean HAS_DISPLAY_LISTENER = Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1;

    public static int getIntFieldIfExists(Class<?> klass, String fieldName,
                                          Class<?> obj, int defaultVal) {
        try {
            Field f = klass.getDeclaredField(fieldName);
            return f.getInt(obj);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static boolean hasField(Class<?> klass, String fieldName) {
        try {
            klass.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private static boolean hasMethod(String className, String methodName,
                                     Class<?>... parameterTypes) {
        try {
            Class<?> klass = Class.forName(className);
            klass.getDeclaredMethod(methodName, parameterTypes);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    private static boolean hasMethod(Class<?> klass, String methodName,
                                     Class<?>... paramTypes) {
        try {
            klass.getDeclaredMethod(methodName, paramTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public static Class<?> getClass(String className) {
        Class<?> class1 = null;
        try {
            class1 = Class.forName(className);
        } catch (ClassNotFoundException e) {
            // e.printStackTrace();
            Log.e("ApiHelper", "----ClassNotFoundException---" + className);
        }

        return class1;
    }

    public static Constructor<?> getConstructor(Class<?> class1,
                                                Class<?>... parameterTypes) {
        Constructor<?> constructor = null;
        if (class1 != null) {
            try {
                constructor = class1.getConstructor(parameterTypes);
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return constructor;
    }

    public static Constructor<?> getConstructor(String className,
                                                Class<?>... parameterTypes) {

        Class<?> clazz = null;
        Constructor<?> constructor = null;
        try {
            clazz = Class.forName(className);
            constructor = clazz.getConstructor(parameterTypes);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return constructor;
    }

    public static Object getInstance(Constructor<?> constructor,
                                     Object... parameters) {

        if (null == constructor)
            return null;

        Object instance = null;
        // try {
        if (null != constructor) {
            if (null != parameters)
                try {
                    instance = constructor.newInstance(parameters);
                } catch (InstantiationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            else
                try {
                    instance = constructor.newInstance();
                } catch (InstantiationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
        // } catch (Exception ex) {
        // throw new RuntimeException(ex);
        // //ex.printStackTrace();
        // }

        return instance;
    }

    public static Method getMethod(String className, String methodName,
                                   Class<?>... parameterTypes) {
        Class<?> clazz = null;
        Method method = null;
        try {
            clazz = Class.forName(className);
            method = clazz.getMethod(methodName, parameterTypes);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return method;
    }

    public static Object getField(String className, String fieldName) {
        Class<?> clazz = getClass(className);
        Object result = null;

        if (clazz != null) {
            Field field = null;
            try {
                field = clazz.getField(fieldName);
                // field = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // e.printStackTrace();
                Log.e(TAG, "getField:" + fieldName + " failed");
            }
            if (field != null) {
                try {
                    result = field.get(null);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static Object getField(String className, String fieldName, Object o) {
        Class<?> clazz = getClass(className);
        Object result = null;

        if (clazz != null) {
            Field field = null;
            try {
                field = clazz.getField(fieldName);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "getField:" + fieldName + " failed");
            }
            if (field != null) {
                try {
                    result = field.get(o);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static Object invokeMethod(Method method, Object obj,
                                      Object... parameters) {

        Object retObj = null;
        if (null == method /* || null == obj */)
            return null;

        try {
            retObj = method.invoke(obj, parameters);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            System.out.print(e.getTargetException());
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retObj;
    }
}
