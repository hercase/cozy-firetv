package com.cosyhome.tv;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fire OS protects its launcher, so it can't be disabled and stays the system
 * home. This service makes Cosy Home the effective home screen instead:
 *  - the remote's Home button is consumed and opens Cosy Home;
 *  - whenever the Amazon launcher comes to the front anyway (after boot, or when
 *    an app exits), Cosy Home is brought straight back on top.
 * Event-driven only: it receives window changes from the Amazon launcher's
 * package and key events, and does nothing in between.
 */
public class HomeButtonService extends AccessibilityService {

    private static final String AMAZON_LAUNCHER = "com.amazon.tv.launcher";
    private long lastLaunch;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static volatile long pausedUntil;

    /** Lets an Amazon screen opened from Cosy Home (Settings) appear without being redirected away. */
    static void pauseRedirect() {
        pauseRedirect(1200);
    }

    /**
     * The same, for a screen the user stays on. The Appstore has no UI of its own on Fire OS: it
     * hands straight over to the Amazon launcher, so browsing it means the launcher is on screen
     * the whole time. Pause for as long as that lasts; Home still works, and landing back on Cosy
     * Home ends the pause.
     */
    static void pauseRedirect(long ms) {
        pausedUntil = SystemClock.uptimeMillis() + ms;
    }

    /** Back on Cosy Home, so whatever we were letting through is done. */
    static void resumeRedirect() {
        pausedUntil = 0;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) openCosy(300);
        return true; // swallow both down and up so Fire OS never sees Home
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && AMAZON_LAUNCHER.contentEquals(event.getPackageName())
                && event.getClassName() != null
                && event.getClassName().toString().contains("HomeActivity")
                && SystemClock.uptimeMillis() > pausedUntil) {
            openCosy(0); // never drop this one: Amazon's home is on screen right now
        }
    }

    private void openCosy(long minGapMs) {
        long now = SystemClock.uptimeMillis();
        if (now - lastLaunch < minGapMs) return; // collapse key repeats
        lastLaunch = now;
        launchCosy();
        // Re-launching Amazon's home while it is already on top raises no window event, so check
        // that the redirect landed and retry if it didn't.
        for (long delay : new long[] {700, 1500}) handler.postDelayed(this::ensureCosy, delay);
    }

    private void launchCosy() {
        startActivity(new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    private void ensureCosy() {
        if (!HomeActivity.resumed && SystemClock.uptimeMillis() > pausedUntil) launchCosy();
    }

    @Override public void onInterrupt() {}
}
