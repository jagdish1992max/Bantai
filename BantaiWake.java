package com.bantai.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class BantaiWake extends Service {
  static volatile boolean running = false;
  static volatile boolean paused = false;
  static volatile boolean pendingWake = false;
  static BantaiWake inst = null;

  private static final Pattern RX = Pattern.compile("bant(a|aa|ai|ay|aay)i?|\\u092c\\u0902\\u091f\\u093e\\u0908|\\u092c\\u0902\\u091f\\u093e\\u092f|\\u092c\\u0928\\u0924\\u093e\\u0908|\\u092c\\u093e\\u0902\\u091f\\u093e\\u0908|\\u092c\\u0902\\u091f\\u093e\\u0907", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  private final Handler h = new Handler(Looper.getMainLooper());
  private SpeechRecognizer sr = null;
  private Intent recIntent = null;
  private long lastWake = 0;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  static void setPaused(final boolean p) {
    paused = p;
    final BantaiWake s = inst;
    if (s == null) return;
    s.h.post(new Runnable() {
      @Override
      public void run() {
        if (p) s.stopListening();
        else s.startListening();
      }
    });
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel ch = new NotificationChannel("bantai_wake", "Bantai wake word", NotificationManager.IMPORTANCE_LOW);
      ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    }
    PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "bantai_wake") : new Notification.Builder(this);
    Notification n = b.setContentTitle("Bantai sun raha hai")
      .setContentText("\"Hey bantai\" bol")
      .setSmallIcon(android.R.drawable.ic_btn_speak_now)
      .setContentIntent(pi)
      .setOngoing(true)
      .build();
    if (Build.VERSION.SDK_INT >= 30) {
      startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
    } else {
      startForeground(1, n);
    }
    running = true;
    inst = this;
    h.removeCallbacksAndMessages(null);
    h.post(new Runnable() {
      @Override
      public void run() {
        startListening();
      }
    });
    return START_STICKY;
  }

  private void restart(long ms) {
    h.removeCallbacksAndMessages(null);
    h.postDelayed(new Runnable() {
      @Override
      public void run() {
        startListening();
      }
    }, ms);
  }

  private void stopListening() {
    h.removeCallbacksAndMessages(null);
    if (sr != null) {
      try {
        sr.cancel();
      } catch (Exception e) {
      }
    }
  }

  private void startListening() {
    if (!running || paused) return;
    try {
      if (sr == null) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new RecognitionListener() {
          @Override
          public void onReadyForSpeech(Bundle params) {}

          @Override
          public void onBeginningOfSpeech() {}

          @Override
          public void onRmsChanged(float rmsdB) {}

          @Override
          public void onBufferReceived(byte[] buffer) {}

          @Override
          public void onEndOfSpeech() {}

          @Override
          public void onEvent(int eventType, Bundle params) {}

          @Override
          public void onError(int error) {
            restart(error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1500 : 500);
          }

          @Override
          public void onResults(Bundle results) {
            check(results);
            restart(300);
          }

          @Override
          public void onPartialResults(Bundle partial) {
            check(partial);
          }
        });
        recIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
          .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
          .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
      }
      sr.cancel();
      sr.startListening(recIntent);
    } catch (Exception e) {
      restart(2000);
    }
  }

  private void check(Bundle b) {
    if (b == null || paused) return;
    ArrayList<String> m = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    if (m == null) return;
    StringBuilder sb = new StringBuilder();
    for (String s : m) sb.append(s).append(' ');
    if (RX.matcher(sb.toString()).find()) {
      long now = System.currentTimeMillis();
      if (now - lastWake < 5000) return;
      lastWake = now;
      pendingWake = true;
      try {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.putExtra("wake", true);
        startActivity(i);
      } catch (Exception e) {
      }
    }
  }

  @Override
  public void onDestroy() {
    running = false;
    inst = null;
    h.removeCallbacksAndMessages(null);
    if (sr != null) {
      try {
        sr.destroy();
      } catch (Exception e) {
      }
      sr = null;
    }
    super.onDestroy();
  }
}
