package com.firetv.viki;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String HOME = "https://www.viki.com/";

    private FrameLayout root;
    private WebView web;
    private CursorView cursor;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private float cx, cy;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        web = new WebView(this);
        cursor = new CursorView(this);
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(cursor, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        setupWebView();

        root.post(new Runnable() {
            @Override
            public void run() {
                cx = root.getWidth() / 2f;
                cy = root.getHeight() / 2f;
                cursor.setPos(cx, cy);
            }
        });

        web.loadUrl(HOME);
    }

    private void setupWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);

        String ua = s.getUserAgentString();
        Matcher m = Pattern.compile("Chrome/([\\d.]+)").matcher(ua);
        String ver = m.find() ? m.group(1) : "100.0.0.0";
        s.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/" + ver + " Safari/537.36");

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !isWeb(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !isWeb(Uri.parse(url));
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customCallback = callback;
                root.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                web.setVisibility(View.GONE);
                cursor.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                hideCustom();
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (String r : request.getResources()) {
                            if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(r)) {
                                request.grant(new String[]{r});
                                return;
                            }
                        }
                        request.deny();
                    }
                });
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }
        });
    }

    private static boolean isWeb(Uri u) {
        String sc = u.getScheme();
        return "http".equals(sc) || "https".equals(sc);
    }

    private void hideCustom() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        if (customCallback != null) customCallback.onCustomViewHidden();
        customCallback = null;
        web.setVisibility(View.VISIBLE);
        cursor.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;

        if (customView != null) {
            if (code == KeyEvent.KEYCODE_BACK) {
                if (down) hideCustom();
                return true;
            }
            if (down) {
                switch (code) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        video("v.currentTime=Math.max(0,v.currentTime-10);");
                        return true;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        video("v.currentTime+=10;");
                        return true;
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        video("if(v.paused)v.play();else v.pause();");
                        return true;
                }
            }
        }

        if (down) {
            switch (code) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    video("if(v.paused)v.play();else v.pause();");
                    return true;
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    video("v.currentTime+=10;");
                    return true;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    video("v.currentTime=Math.max(0,v.currentTime-10);");
                    return true;
            }
        }

        if (customView != null) return super.dispatchKeyEvent(e);

        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (down) move(0, -1, e.getRepeatCount());
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (down) move(0, 1, e.getRepeatCount());
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (down) move(-1, 0, e.getRepeatCount());
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (down) move(1, 0, e.getRepeatCount());
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (down && e.getRepeatCount() == 0) click();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (down) {
                    if (web.canGoBack()) web.goBack();
                    else finish();
                }
                return true;
            case KeyEvent.KEYCODE_MENU:
                if (down) {
                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
                    web.loadUrl(HOME);
                }
                return true;
        }
        return super.dispatchKeyEvent(e);
    }

    private void video(String body) {
        web.evaluateJavascript(
                "(function(){var v=document.querySelector('video');if(!v)return;" + body + "})()",
                null);
    }

    private void move(int dx, int dy, int repeat) {
        float step = 14 + Math.min(repeat, 20) * 3;
        int w = root.getWidth(), h = root.getHeight();
        cx = Math.max(0, Math.min(w - 1, cx + dx * step));
        cy = Math.max(0, Math.min(h - 1, cy + dy * step));

        final int edge = 70;
        if (dy > 0 && cy >= h - edge) web.scrollBy(0, (int) (step * 2));
        if (dy < 0 && cy <= edge) web.scrollBy(0, (int) (-step * 2));

        cursor.setPos(cx, cy);
    }

    private void click() {
        long t = SystemClock.uptimeMillis();
        web.requestFocus();
        MotionEvent d = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, cx, cy, 0);
        d.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        web.dispatchTouchEvent(d);
        d.recycle();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long t2 = SystemClock.uptimeMillis();
                MotionEvent u = MotionEvent.obtain(t2, t2, MotionEvent.ACTION_UP, cx, cy, 0);
                u.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                web.dispatchTouchEvent(u);
                u.recycle();
            }
        }, 60);
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onPause() {
        web.onPause();
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        root.removeAllViews();
        web.destroy();
        super.onDestroy();
    }

    private static class CursorView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float x, y;

        CursorView(Context c) {
            super(c);
            setFocusable(false);
            setClickable(false);
            fill.setColor(Color.argb(170, 255, 255, 255));
            stroke.setColor(Color.BLACK);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(4);
        }

        void setPos(float px, float py) {
            x = px;
            y = py;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawCircle(x, y, 16, fill);
            canvas.drawCircle(x, y, 16, stroke);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }
    }
}
