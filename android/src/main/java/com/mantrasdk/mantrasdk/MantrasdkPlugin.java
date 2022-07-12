package com.mantrasdk.mantrasdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;
/** MantrasdkPlugin */
public class MantrasdkPlugin implements FlutterPlugin, MethodCallHandler, MFS100Event {
  /// The MethodChannel that will the communication between Flutter and native
  /// Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine
  /// and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;

  int timeout = 10000;

  private enum ScannerAction {
    Capture, Verify
  }

  ScannerAction scannerAction = ScannerAction.Capture;

  private boolean isCaptureRunning = false;

  MFS100 mfs100 = null;

  Context context;
  private FingerData lastCapFingerData = null;

  int count;
  Result response;

  private Handler handler;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "mantrasdk");
    channel.setMethodCallHandler(this);

    handler = new Handler(Looper.getMainLooper());

    context = flutterPluginBinding.getApplicationContext();

    handler = new Handler(Looper.getMainLooper());

    if (mfs100 == null) {
      SetTextOnUIThread("mfs null");

      mfs100 = new MFS100(this);
      mfs100.SetApplicationContext(context);
    } else {
      SetTextOnUIThread("init here");

      InitScanner();
    }

  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    response = result;
    if (call.method.equals("getPlatformVersion")) {
      result.success("Ansdroid " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("invokeCapture")) {
      capture();
    }
    else if(call.method.equals("getFinger"))
    {
      getFinger();
    }
    else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void OnDeviceAttached(int vid, int pid, boolean hasPermission) {

    int ret;
    if (!hasPermission) {
      SetTextOnUIThread("Permission denied");
      return;
    }
    if (vid == 1204 || vid == 11279) {
      if (pid == 34323) {
        ret = mfs100.LoadFirmware();
        if (ret != 0) {
          SetTextOnUIThread(mfs100.GetErrorMsg(ret));
        } else {
          SetTextOnUIThread("Load firmware success");
        }
      } else if (pid == 4101) {
        String key = "Without Key";
        ret = mfs100.Init();
        if (ret == 0) {
          showSuccessLog(key);
        } else {
          SetTextOnUIThread(mfs100.GetErrorMsg(ret));
        }

      }
    }

  }

  @Override
  public void OnDeviceDetached() {

    UnInitScanner();
    SetTextOnUIThread("Device removed");

  }

  @Override
  public void OnHostCheckFailed(String s) {

    try {
      SetTextOnUIThread(s);

    } catch (Exception ignored) {
    }

  }

  void getFinger()
  {
    if (!isCaptureRunning) {
      StartSyncCapture();
    }
  }

  void capture() {

    scannerAction = ScannerAction.Capture;


  }

  private void SetTextOnUIThread(final String str) {

    handler.post(
            new Runnable() {
              @Override
              public void run() {
                Toast.makeText(context, str, Toast.LENGTH_LONG).show();
              }
            });

  }

  private void showSuccessLog(String key) {
    SetTextOnUIThread("Init success");
    String info = "\nKey: " + key + "\nSerial: "
            + mfs100.GetDeviceInfo().SerialNo() + " Make: "
            + mfs100.GetDeviceInfo().Make() + " Model: "
            + mfs100.GetDeviceInfo().Model()
            + "\nCertificate: " + mfs100.GetCertification();
    SetTextOnUIThread(info);
  }

  private void InitScanner() {
    try {
      int ret = mfs100.Init();
      if (ret != 0) {
        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
      } else {
        SetTextOnUIThread("Init success");
        String info = "Serial: " + mfs100.GetDeviceInfo().SerialNo()
                + " Make: " + mfs100.GetDeviceInfo().Make()
                + " Model: " + mfs100.GetDeviceInfo().Model()
                + "\nCertificate: " + mfs100.GetCertification();
        SetTextOnUIThread(info);
      }
    } catch (Exception ex) {
      Toast.makeText(context, "Init failed, unhandled exception",
              Toast.LENGTH_LONG).show();
      SetTextOnUIThread("Init failed, unhandled exception");
    }
  }

  private void StartSyncCapture() {

    new Thread(new Runnable() {

      @Override
      public void run() {
        SetTextOnUIThread("Starting here");
        isCaptureRunning = true;
        try {
          FingerData fingerData = new FingerData();
          int ret = mfs100.AutoCapture(fingerData, timeout, true);
          Log.e("StartSyncCapture.RET", "" + ret);
          if (ret != 0) {
            SetTextOnUIThread("Inside error "+mfs100.GetErrorMsg(ret));
          } else {
            lastCapFingerData = fingerData;
            final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0,
                    fingerData.FingerImage().length);

            extractWSQImage();

            SetTextOnUIThread("Capture Success");
            String log = "\nQuality: " + fingerData.Quality()
                    + "\nNFIQ: " + fingerData.Nfiq()
                    + "\nWSQ Compress Ratio: "
                    + fingerData.WSQCompressRatio()
                    + "\nImage Dimensions (inch): "
                    + fingerData.InWidth() + "\" X "
                    + fingerData.InHeight() + "\""
                    + "\nImage Area (inch): " + fingerData.InArea()
                    + "\"" + "\nResolution (dpi/ppi): "
                    + fingerData.Resolution() + "\nGray Scale: "
                    + fingerData.GrayScale() + "\nBits Per Pixal: "
                    + fingerData.Bpp() + "\nWSQ Info: "
                    + fingerData.WSQInfo();
            SetTextOnUIThread(log);

          }
        } catch (Exception ex) {
          SetTextOnUIThread("Error" + ex.getMessage());
        } finally {
          isCaptureRunning = false;
        }
      }
    }).start();
  }

  private void UnInitScanner() {
    try {
      int ret = mfs100.UnInit();
      if (ret != 0) {
        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
      } else {
        SetTextOnUIThread("Uninit Success");
        SetTextOnUIThread("Uninit Success");
        lastCapFingerData = null;
      }
    } catch (Exception e) {
      Log.e("UnInitScanner.EX", e.toString());
    }
  }

  private void extractWSQImage() {

    try {

      if (lastCapFingerData == null) {
        SetTextOnUIThread("Finger not capture");
        return;
      }
      byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height()) + 1078];
      byte[] wsqImage;
      int dataLen = mfs100.ExtractWSQImage(lastCapFingerData.RawData(), tempData);
      if (dataLen <= 0) {
        if (dataLen == 0) {
          SetTextOnUIThread("Failed to extract WSQ Image");
        } else {
          SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
        }
      } else {
        wsqImage = new byte[dataLen];
        System.arraycopy(tempData, 0, wsqImage, 0, dataLen);
        // WriteFile("WSQ.wsq", wsqImage);

        SetTextOnUIThread("Extract WSQ Image Success");

        final String base64FingerString = Base64.encodeToString(wsqImage, Base64.DEFAULT);

        Log.e("base64FingerString", base64FingerString);
        response.success(base64FingerString);
        handler.post(
                new Runnable() {
                  @Override
                  public void run() {
                    channel.invokeMethod("onCapture", base64FingerString);
                  }
                });

      }
    } catch (Exception e) {

      Log.e("Error", "Extract WSQ Image Error", e);
    }
  }
}
