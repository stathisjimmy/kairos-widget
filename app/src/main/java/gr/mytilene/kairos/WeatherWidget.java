package gr.mytilene.kairos;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Widget αρχικής οθόνης: κατεβάζει την εικόνα  <Pi>/api/widget.png  (την φτιάχνει ο server με τα εφέ θερμοκρασίας)
 * και την δείχνει. Ανανέωση κάθε 15′ (ρυθμίζεται), με το ⟳ ή όταν αλλάζει μέγεθος. Πάτημα → ανοίγει η εφαρμογή καιρού.
 */
public class WeatherWidget extends AppWidgetProvider {
    static final String ACTION_REFRESH = "gr.mytilene.kairos.REFRESH";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        schedule(ctx);
        refreshAsync(ctx, goAsync());
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr, int id, Bundle opts) {
        refreshAsync(ctx, goAsync());
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            refreshAsync(ctx, goAsync());
        }
    }

    @Override
    public void onDisabled(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(refreshIntent(ctx, 2));
    }

    static PendingIntent refreshIntent(Context ctx, int code) {
        Intent i = new Intent(ctx, WeatherWidget.class).setAction(ACTION_REFRESH);
        return PendingIntent.getBroadcast(ctx, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Περιοδική ανανέωση (μη ακριβής, φιλική στη μπαταρία). */
    static void schedule(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long every = Prefs.minutes(ctx) * 60_000L;
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + every, every,
                refreshIntent(ctx, 2));
    }

    static void refreshAsync(final Context ctx, final PendingResult pr) {
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                update(app);
            } catch (Throwable ignored) {
            } finally {
                if (pr != null) pr.finish();
            }
        }).start();
    }

    /** Κατεβάζει την εικόνα και ενημερώνει όλα τα widget. */
    static void update(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, WeatherWidget.class));
        if (ids == null || ids.length == 0) return;
        String base = Prefs.url(ctx);
        File file = new File(ctx.getFilesDir(), "widget.png");
        String err = null;
        try {
            URL u = new URL(base + "/api/widget.png?w=1000&h=500&theme=" + Prefs.theme(ctx));
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(10_000);
            c.setReadTimeout(25_000);
            c.setUseCaches(false);
            int code = c.getResponseCode();
            if (code != 200) throw new Exception("HTTP " + code);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] b = new byte[16384];
                int n;
                while ((n = in.read(b)) > 0) buf.write(b, 0, n);
            }
            c.disconnect();
            byte[] png = buf.toByteArray();
            if (png.length < 100) throw new Exception("κενή εικόνα");
            File tmp = new File(ctx.getFilesDir(), "widget.tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(png);
            }
            if (!tmp.renameTo(file)) throw new Exception("αποθήκευση");
        } catch (Exception e) {
            err = "Χωρίς σύνδεση με το Pi"
                    + (file.exists() ? " · εικόνα " + new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(file.lastModified())) : "");
        }
        Uri img = Uri.parse("content://" + ImageProvider.AUTHORITY + "/widget.png?t=" + file.lastModified());
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(base));
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget);
            if (file.exists()) {
                v.setImageViewUri(R.id.img, Uri.EMPTY);   // αναγκάζει τον launcher να ξαναδιαβάσει την εικόνα
                v.setImageViewUri(R.id.img, img);
            }
            if (err != null) {
                v.setTextViewText(R.id.msg, err);
                v.setViewVisibility(R.id.msg, View.VISIBLE);
            } else {
                v.setViewVisibility(R.id.msg, View.GONE);
            }
            v.setOnClickPendingIntent(R.id.img, openPi);
            v.setOnClickPendingIntent(R.id.refresh, refreshIntent(ctx, 1));
            mgr.updateAppWidget(id, v);
        }
    }
}
