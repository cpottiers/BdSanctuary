package com.android.bdsanctuary;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.android.bdsanctuary.datas.Global;
import com.cyrilpottiers.androlib.Log;
import com.cyrilpottiers.androlib.dialog.SplashScreenDialog;

public class LoadActivity extends Activity {
    public final static int    SPLASH_SCREEN_ACTIVITY = 0;

    private final static int   SPLASH_SHOW_FIXED_TIME = 1000;
    private final static int   CLOSE_SPLASH           = 0;

    private boolean            isCancelled            = false;
    private SplashScreenDialog dialog                 = null;

    private Handler            handler                = new Handler() {
                                                          @Override
                                                          public void handleMessage(
                                                                  Message msg) {
                                                              switch (msg.what) {
                                                                  case CLOSE_SPLASH: {
                                                                      Intent intent = new Intent(LoadActivity.this, MainActivity.class);
                                                                      startActivity(intent);
                                                                      if (dialog.isShowing()) {
                                                                          dialog.finalize();
                                                                      }
                                                                  }
                                                                      break;
                                                              }
                                                              super.handleMessage(msg);
                                                          }
                                                      };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Global.initialize(getApplicationContext());

        ImageView splash = new ImageView(getApplicationContext());
        splash.setBackgroundResource(R.drawable.splash);
        splash.setScaleType(ScaleType.FIT_XY);

        setContentView(splash, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        dialog = new SplashScreenDialog(this, R.drawable.splash, getResources().getColor(R.color.bgColor), false);
        dialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                Log.d(Global.getLogTag(LoadActivity.class), "splash nok");
                isCancelled = true;
                finish();
            }
        });
        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface arg0) {
                if (!isCancelled) Log.d(Global.getLogTag(LoadActivity.class), "splash ok");
                finish();
            }
        });

        // Show waiting dialog
        handler.sendEmptyMessageDelayed(CLOSE_SPLASH, SPLASH_SHOW_FIXED_TIME);
    }

    @Override
    protected void onStart() {
        super.onStart();
        dialog.show();
    }
}
