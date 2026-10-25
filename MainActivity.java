package com.bantai.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    registerPlugin(BantaiPlugin.class);
    super.onCreate(savedInstanceState);
    if (Build.VERSION.SDK_INT >= 27) {
      setShowWhenLocked(true);
      setTurnScreenOn(true);
    }
    mark(getIntent());
  }

  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    mark(intent);
  }

  private void mark(Intent i) {
    if (i != null && i.getBooleanExtra("wake", false)) {
      WakeService.pendingWake = true;
      i.removeExtra("wake");
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    WakeService.setPaused(true);
  }

  @Override
  public void onPause() {
    super.onPause();
    WakeService.setPaused(false);
  }
}
