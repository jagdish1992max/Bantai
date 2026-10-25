package com.bantai.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.List;

@CapacitorPlugin(name = "Bantai")
public class BantaiPlugin extends Plugin {

  private void go(Intent i) {
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    getContext().startActivity(i);
  }

  private int num(PluginCall call, String key, int def) {
    try {
      return (int) Math.round(Double.parseDouble(call.getString(key, String.valueOf(def))));
    } catch (Exception e) {
      return def;
    }
  }

  @PluginMethod
  public void torch(PluginCall call) {
    boolean on = Boolean.parseBoolean(call.getString("on", "true"));
    try {
      CameraManager cm = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
      String target = null;
      for (String id : cm.getCameraIdList()) {
        CameraCharacteristics c = cm.getCameraCharacteristics(id);
        Boolean flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        Integer facing = c.get(CameraCharacteristics.LENS_FACING);
        if (flash != null && flash && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
          target = id;
          break;
        }
      }
      if (target == null) {
        call.reject("flash nahi mila");
        return;
      }
      cm.setTorchMode(target, on);
      call.resolve();
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }

  @PluginMethod
  public void alarm(PluginCall call) {
    try {
      Intent i = new Intent(AlarmClock.ACTION_SET_ALARM)
        .putExtra(AlarmClock.EXTRA_HOUR, num(call, "hour", 7))
        .putExtra(AlarmClock.EXTRA_MINUTES, num(call, "minute", 0))
        .putExtra(AlarmClock.EXTRA_MESSAGE, call.getString("label", "Bantai"))
        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
      go(i);
      call.resolve();
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }

  @PluginMethod
  public void timer(PluginCall call) {
    try {
      Intent i = new Intent(AlarmClock.ACTION_SET_TIMER)
        .putExtra(AlarmClock.EXTRA_LENGTH, num(call, "seconds", 60))
        .putExtra(AlarmClock.EXTRA_MESSAGE, call.getString("label", "Bantai"))
        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
      go(i);
      call.resolve();
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }

  @PluginMethod
  public void openApp(PluginCall call) {
    try {
      String name = call.getString("name", "").toLowerCase().trim();
      if (name.isEmpty()) {
        call.reject("app ka naam do");
        return;
      }
      PackageManager pm = getContext().getPackageManager();
      Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
      List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
      ResolveInfo best = null;
      for (ResolveInfo r : list) {
        String lab = r.loadLabel(pm).toString().toLowerCase();
        if (lab.equals(name)) {
          best = r;
          break;
        }
        if (best == null && lab.contains(name)) best = r;
      }
      if (best == null) {
        call.reject("app nahi mila: " + name);
        return;
      }
      Intent li = pm.getLaunchIntentForPackage(best.activityInfo.packageName);
      if (li == null) {
        call.reject("app khul nahi sakta");
        return;
      }
      go(li);
      JSObject ret = new JSObject();
      ret.put("opened", best.loadLabel(pm).toString());
      call.resolve(ret);
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }

  @PluginMethod
  public void dial(PluginCall call) {
    try {
      go(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(call.getString("number", "")))));
      call.resolve();
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }

  @PluginMethod
  public void sms(PluginCall call) {
    try {
      Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(call.getString("number", ""))));
      i.putExtra("sms_body", call.getString("message", ""));
      go(i);
      call.resolve();
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }

  @PluginMethod
  public void whatsapp(PluginCall call) {
    try {
      String digits = call.getString("number", "").replaceAll("[^0-9]", "");
      if (digits.length() == 10) digits = "91" + digits;
      String url = "https://wa.me/" + digits + "?text=" + Uri.encode(call.getString("message", ""));
      go(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
      call.resolve();
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }

  @PluginMethod
  public void maps(PluginCall call) {
    try {
      go(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(call.getString("query", "")))));
      call.resolve();
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }

  @PluginMethod
  public void volume(PluginCall call) {
    try {
      AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
      int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
      int p = Math.max(0, Math.min(100, num(call, "percent", 50)));
      am.setStreamVolume(AudioManager.STREAM_MUSIC, max * p / 100, 0);
      call.resolve();
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }

  @PluginMethod
  public void camera(PluginCall call) {
    try {
      go(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
      call.resolve();
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }







  @PluginMethod
  public void startWake(PluginCall call) {
    try {
      Context c = getContext();
      if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        getActivity().requestPermissions(new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, 77);
      }
      Intent i = new Intent(c, WakeService.class);
      if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
      else c.startService(i);
      call.resolve();
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }

  @PluginMethod
  public void stopWake(PluginCall call) {
    try {
      getContext().stopService(new Intent(getContext(), WakeService.class));
      call.resolve();
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }

  @PluginMethod
  public void consumeWake(PluginCall call) {
    JSObject r = new JSObject();
    r.put("wake", WakeService.pendingWake);
    WakeService.pendingWake = false;
    call.resolve(r);
  }

  @PluginMethod
  public void overlay(PluginCall call) {
    try {
      boolean ok = Settings.canDrawOverlays(getContext());
      if (!ok && "true".equals(call.getString("request", "false"))) {
        go(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName())));
      }
      JSObject r = new JSObject();
      r.put("granted", ok);
      call.resolve(r);
    } catch (Exception e) {
      call.reject(String.valueOf(e.getMessage()));
    }
  }
}
