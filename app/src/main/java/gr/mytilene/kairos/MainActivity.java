package gr.mytilene.kairos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Ρυθμίσεις του widget (διεύθυνση Pi, θέμα, ανανέωση) — χωρίς layout XML. */
public class MainActivity extends Activity {

    private TextView label(String s, int sizeSp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sizeSp);
        t.setTextColor(bold ? Color.WHITE : Color.parseColor("#94A3B8"));
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(14), 0, dp(4));
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(24), dp(20), dp(24));
        box.setBackgroundColor(Color.parseColor("#0E1528"));

        box.addView(label("☀️ Καιρός Μυτιλήνη — widget", 22, true));
        box.addView(label("Πρόσθεσε το widget: πάτα παρατεταμένα στην αρχική οθόνη → Widgets → «Καιρός Μυτιλήνη». "
                + "Πάτημα στο widget = ανοίγει η εφαρμογή καιρού · ⟳ = ανανέωση τώρα.", 14, false));

        box.addView(label("Διεύθυνση του Pi", 15, true));
        final EditText url = new EditText(this);
        url.setText(Prefs.url(this));
        url.setTextColor(Color.WHITE);
        url.setSingleLine(true);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        box.addView(url);

        box.addView(label("Θέμα", 15, true));
        final RadioGroup theme = new RadioGroup(this);
        theme.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton dark = new RadioButton(this);
        dark.setText("Σκούρο");
        dark.setTextColor(Color.WHITE);
        dark.setId(1);
        RadioButton light = new RadioButton(this);
        light.setText("Ανοιχτό");
        light.setTextColor(Color.WHITE);
        light.setId(2);
        theme.addView(dark);
        theme.addView(light);
        theme.check("light".equals(Prefs.theme(this)) ? 2 : 1);
        box.addView(theme);

        box.addView(label("Ανανέωση κάθε (λεπτά, τουλάχιστον 15)", 15, true));
        final EditText mins = new EditText(this);
        mins.setText(String.valueOf(Prefs.minutes(this)));
        mins.setTextColor(Color.WHITE);
        mins.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(mins);

        Button save = new Button(this);
        save.setText("Αποθήκευση και ανανέωση widget");
        save.setOnClickListener(v -> {
            int m = 15;
            try {
                m = Math.max(15, Integer.parseInt(mins.getText().toString().trim()));
            } catch (NumberFormatException ignored) {
            }
            Prefs.get(this).edit()
                    .putString("url", url.getText().toString())
                    .putString("theme", theme.getCheckedRadioButtonId() == 2 ? "light" : "dark")
                    .putInt("minutes", m)
                    .apply();
            WeatherWidget.schedule(this);
            sendBroadcast(new Intent(this, WeatherWidget.class).setAction(WeatherWidget.ACTION_REFRESH));
            Toast.makeText(this, "Αποθηκεύτηκε — το widget ανανεώνεται", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(20);
        box.addView(save, lp);

        Button open = new Button(this);
        open.setText("Άνοιγμα εφαρμογής καιρού");
        open.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.url(this)))));
        box.addView(open, new LinearLayout.LayoutParams(lp));

        TextView note = label("Το κινητό πρέπει να βλέπει το Pi (Tailscale ενεργό). "
                + "Η εικόνα φτιάχνεται στο Pi: " + Prefs.url(this) + "/api/widget.png", 12, false);
        note.setGravity(Gravity.START);
        box.addView(note);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor("#0E1528"));
        sv.addView(box);
        setContentView(sv);
    }
}
