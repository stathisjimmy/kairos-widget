package gr.mytilene.kairos;

import android.content.Context;
import android.content.SharedPreferences;

/** Ρυθμίσεις: διεύθυνση του Pi, θέμα, συχνότητα ανανέωσης. */
final class Prefs {
    static final String DEFAULT_URL = "http://100.72.105.75:8080";

    private Prefs() {}

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("kairos", Context.MODE_PRIVATE);
    }

    static String url(Context c) {
        String u = get(c).getString("url", DEFAULT_URL).trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://" + u;
        return u;
    }

    static String theme(Context c) {
        return get(c).getString("theme", "dark");
    }

    static int minutes(Context c) {
        return Math.max(15, get(c).getInt("minutes", 15));
    }
}
