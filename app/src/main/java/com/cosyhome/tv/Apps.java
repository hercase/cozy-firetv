package com.cosyhome.tv;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The TV apps to show as tiles: every LEANBACK_LAUNCHER activity, one per package, dock apps
 * first in their configured order, then the pinned ones, then the rest alphabetically.
 * Banner loading follows cleanyourtv's AppAdapter (MIT, https://github.com/Gathin23/cleanyourtv).
 */
final class Apps {

    static final class App {
        final String label;
        final String pkg;
        final ComponentName component;
        final Drawable banner;
        final Drawable icon;

        App(String label, ComponentName component, Drawable banner, Drawable icon) {
            this.label = label;
            this.pkg = component.getPackageName();
            this.component = component;
            this.banner = banner;
            this.icon = icon;
        }
    }

    /** The IP camera viewer gets its own name and tile art instead of its phone-sized icon. */
    private static final String CAMERAS_PACKAGE = "com.rcreations.ipcamviewerFree";

    private Apps() {
    }

    static List<App> load(Context ctx, List<String> dock, List<String> pinned, Set<String> hidden) {
        PackageManager pm = ctx.getPackageManager();
        Intent probe = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<App> apps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // Apps made for phones (IP Cam Viewer and the like) only declare LAUNCHER. Pick up the ones
        // the user installed, plus any system one named in dock or pinned, which is the only way to
        // reach the Appstore; the rest of the system's LAUNCHER activities (Settings...) stay out.
        List<ResolveInfo> found = new ArrayList<>(pm.queryIntentActivities(probe, 0));
        Intent phoneProbe = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(phoneProbe, 0)) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            boolean system = (ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (!system || dock.contains(pkg) || pinned.contains(pkg)) found.add(ri);
        }
        for (ResolveInfo ri : found) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(ctx.getPackageName()) || hidden.contains(pkg) || !seen.add(pkg)) continue;
            ComponentName component = new ComponentName(pkg, ri.activityInfo.name);
            String label = String.valueOf(ri.loadLabel(pm)).trim();
            Drawable banner = loadBanner(ctx, pm, ri);
            if (pkg.equals(CAMERAS_PACKAGE)) {
                label = ctx.getString(R.string.cameras);
                banner = shrink(ctx, ctx.getDrawable(R.drawable.banner_cameras));
            }
            apps.add(new App(label, component, banner, ri.loadIcon(pm)));
        }
        Collections.sort(apps, (a, b) -> {
            int ra = rank(a.pkg, dock, pinned), rb = rank(b.pkg, dock, pinned);
            if (ra != rb) return Integer.compare(ra, rb);
            return a.label.compareToIgnoreCase(b.label);
        });
        return apps;
    }

    private static int rank(String pkg, List<String> dock, List<String> pinned) {
        int d = dock.indexOf(pkg);
        if (d >= 0) return d;
        int p = pinned.indexOf(pkg);
        return p >= 0 ? 100 + p : 1000;
    }

    private static Drawable loadBanner(Context ctx, PackageManager pm, ResolveInfo ri) {
        Drawable d = null;
        int id = ri.activityInfo.banner;
        if (id == 0) id = ri.activityInfo.applicationInfo.banner;
        if (id != 0) {
            try {
                Resources res = pm.getResourcesForApplication(ri.activityInfo.applicationInfo);
                // At least xhdpi, so the artwork stays sharp through the focus zoom.
                int density = Math.max(DisplayMetrics.DENSITY_XHIGH, ctx.getResources().getDisplayMetrics().densityDpi);
                d = res.getDrawableForDensity(id, density, null);
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException ignored) {
            }
        }
        if (d == null) d = ri.activityInfo.loadBanner(pm);
        return d == null ? null : shrink(ctx, d);
    }

    /**
     * Redraws a banner at tile size. Some apps ship 1080p banners: kept as they are, twenty of them
     * cost about 40 MB of bitmaps plus the same again as GPU textures, for tiles 240 px wide.
     */
    private static Drawable shrink(Context ctx, Drawable d) {
        float density = ctx.getResources().getDisplayMetrics().density;
        // The largest tile (the grid's, 120x68dp) at its 1.1x focus zoom.
        int w = Math.round(120 * density * Tile.FOCUS_SCALE), h = Math.round(68 * density * Tile.FOCUS_SCALE);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        int iw = d.getIntrinsicWidth(), ih = d.getIntrinsicHeight();
        if (iw > 0 && ih > 0) {
            // Centre-crop, as the tile's ImageView would.
            float scale = Math.max(w / (float) iw, h / (float) ih);
            int sw = Math.round(iw * scale), sh = Math.round(ih * scale);
            d.setBounds((w - sw) / 2, (h - sh) / 2, (w - sw) / 2 + sw, (h - sh) / 2 + sh);
        } else {
            d.setBounds(0, 0, w, h);
        }
        d.draw(c);
        return new BitmapDrawable(ctx.getResources(), bmp);
    }
}
