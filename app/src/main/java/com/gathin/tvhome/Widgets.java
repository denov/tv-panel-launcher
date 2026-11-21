package com.gathin.tvhome;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * widget_provider (config.xml or a theme): a row of chips under the clock (icon, small label, value), published by
 * an app such as TV Panel. Local change, modelled on GameDay: the launcher only reads the app's provider and stays
 * offline; the app does the fetching. Read while Home is on screen, every {@link #TICK_MS} and on each change.
 *
 * Contract: the named package declares {@value #META} (its provider authority) on its application; the provider
 * answers content://authority/widgets with the columns key, label, value, icon (PNG blob) and open (1 when OK should
 * open a page). A row with an empty value is drawn as its icon alone (the label is still its spoken name); one
 * with neither value nor icon is skipped. OK on an open chip starts {@value #ACTION} in that package with the chip's key as "key".
 */
final class Widgets {
    static final String META = "com.gathin.tvhome.widgets";
    static final String ACTION = "com.gathin.tvhome.action.OPEN_WIDGET";
    static final long TICK_MS = 15_000;
    private static final int MAX = 8;
    private static final String[] COLUMNS = {"key", "label", "value", "icon", "open"};

    static final class Chip {
        final String key;
        final String label;
        final String value;
        final Bitmap icon;
        final boolean open;

        Chip(String key, String label, String value, Bitmap icon, boolean open) {
            this.key = key;
            this.label = label;
            this.value = value;
            this.icon = icon;
            this.open = open;
        }
    }

    private final Context ctx;
    private final String pkg;
    private final Uri uri;
    private final Handler main;
    private final Runnable onChange;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ContentObserver observer;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            read();
            main.postDelayed(this, TICK_MS);
        }
    };
    private boolean watching;
    private List<Chip> chips = Collections.emptyList();

    private Widgets(Context ctx, String pkg, Uri uri, Handler main, Runnable onChange) {
        this.ctx = ctx;
        this.pkg = pkg;
        this.uri = uri;
        this.main = main;
        this.onChange = onChange;
        observer = new ContentObserver(main) {
            @Override
            public void onChange(boolean selfChange) {
                read();
            }
        };
    }

    /** Null when the key is empty, the package is missing, or it names no provider of its own. */
    static Widgets create(Context ctx, String pkg, Handler main, Runnable onChange) {
        if (pkg == null || pkg.trim().isEmpty()) return null;
        String p = pkg.trim();
        try {
            PackageManager pm = ctx.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(p, PackageManager.GET_META_DATA);
            String authority = info.metaData == null ? null : info.metaData.getString(META);
            if (authority == null || authority.trim().isEmpty()) return null;
            ProviderInfo provider = pm.resolveContentProvider(authority.trim(), 0);
            if (provider == null || !p.equals(provider.packageName)) return null;
            Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority.trim()).appendPath("widgets").build();
            return new Widgets(ctx, p, uri, main, onChange);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Home is on screen: read now, on each change and every TICK_MS. */
    void start() {
        if (!watching) {
            try {
                ctx.getContentResolver().registerContentObserver(uri, false, observer);
                watching = true;
            } catch (SecurityException e) {
                // The app went away; keep what is on screen.
            }
        }
        main.removeCallbacks(tick);
        tick.run();
    }

    /** Home is off screen: nothing runs. */
    void stop() {
        main.removeCallbacks(tick);
        if (watching) ctx.getContentResolver().unregisterContentObserver(observer);
        watching = false;
    }

    void shutdown() {
        stop();
        io.shutdownNow();
    }

    void read() {
        try {
            io.execute(() -> {
                List<Chip> got = new ArrayList<>();
                try (Cursor c = ctx.getContentResolver().query(uri, COLUMNS, null, null, null)) {
                    while (c != null && c.moveToNext() && got.size() < MAX) {
                        String label = c.getString(1);
                        String value = c.getString(2);
                        byte[] png = c.getBlob(3);
                        Bitmap icon = png == null ? null : BitmapFactory.decodeByteArray(png, 0, png.length);
                        // No value: an icon-only chip (TV Panel's cameras). Neither: nothing to draw.
                        if (value == null) value = "";
                        if (value.isEmpty() && icon == null) continue;
                        got.add(new Chip(c.getString(0), label == null ? "" : label, value, icon, c.getInt(4) == 1));
                    }
                } catch (RuntimeException gone) {
                    return;   // uninstalled or crashed mid-query: keep what is on screen
                }
                main.post(() -> {
                    chips = got;
                    onChange.run();
                });
            });
        } catch (RejectedExecutionException shutDown) {
            // Home is being destroyed.
        }
    }

    List<Chip> chips() {
        return chips;
    }

    /** The page behind a chip, in the app that published it; null when it has none or the app is gone. */
    Intent open(Chip chip) {
        if (!chip.open) return null;
        Intent i = new Intent(ACTION).setPackage(pkg).putExtra("key", chip.key).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return ctx.getPackageManager().resolveActivity(i, 0) == null ? null : i;
    }
}
