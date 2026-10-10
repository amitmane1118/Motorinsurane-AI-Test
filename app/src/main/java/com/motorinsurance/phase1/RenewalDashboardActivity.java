package com.motorinsurance.phase1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Private, on-device renewal overview. No network, call, recording, or customer-data upload. */
public final class RenewalDashboardActivity extends Activity {
    private static final int PICK_CSV = 41;
    private static final int INK = Color.rgb(20, 35, 63);
    private static final int MUTED = Color.rgb(101, 116, 139);
    private static final int BACKGROUND = Color.rgb(244, 247, 251);
    private static final int BLUE = Color.rgb(46, 91, 224);
    private static final int GREEN = Color.rgb(21, 133, 103);
    private static final int ORANGE = Color.rgb(192, 111, 26);
    private static final int RED = Color.rgb(187, 63, 73);
    private final List<Customer> customers = new ArrayList<>();
    private LinearLayout content;
    private TextView countLabel;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        content.setPadding(pad, dp(20), pad, dp(28));
        scroll.addView(content);
        setContentView(scroll);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(22), dp(22), dp(22), dp(23));
        hero.setBackground(round(INK, 24));
        TextView overline = text("MOTOR INSURANCE  /  RENEWALS", 11, Color.rgb(172, 196, 255), true);
        hero.addView(overline);
        TextView title = text("Renewal desk", 29, Color.WHITE, true);
        title.setPadding(0, dp(8), 0, dp(3));
        hero.addView(title);
        hero.addView(text("A clear view of your customers and upcoming policies.", 14, Color.rgb(219, 228, 244), false));
        content.addView(hero, matchWrap());

        Button importButton = new Button(this);
        importButton.setText("IMPORT PRIVATE SHEET CSV");
        importButton.setTextColor(Color.WHITE);
        importButton.setTextSize(13);
        importButton.setAllCaps(false);
        importButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        importButton.setBackground(round(BLUE, 16));
        importButton.setPadding(dp(16), dp(10), dp(16), dp(10));
        importButton.setOnClickListener(v -> chooseFile());
        LinearLayout.LayoutParams action = matchWrap();
        action.topMargin = dp(16);
        content.addView(importButton, action);
        TextView privacy = text("Choose a CSV exported from your private Google Sheet. The app reads it on this device and keeps it in memory only.", 12, MUTED, false);
        privacy.setPadding(dp(2), dp(7), dp(2), dp(10));
        content.addView(privacy, matchWrap());

        countLabel = text(customers.isEmpty() ? "YOUR RENEWAL OVERVIEW" : "YOUR RENEWAL OVERVIEW  ·  " + customers.size() + " UNIQUE NUMBERS", 12, MUTED, true);
        countLabel.setPadding(dp(2), dp(11), 0, dp(10));
        content.addView(countLabel, matchWrap());
        addMetrics();

        TextView listTitle = text("Upcoming renewals", 20, INK, true);
        listTitle.setPadding(dp(2), dp(22), 0, dp(10));
        content.addView(listTitle, matchWrap());
        if (customers.isEmpty()) {
            LinearLayout empty = card();
            empty.addView(text("Your customer list will appear here", 16, INK, true));
            TextView tip = text("Export the private Sheet as CSV, then tap Import. Dates are read as DD-MM-YYYY.", 13, MUTED, false);
            tip.setPadding(0, dp(7), 0, 0);
            empty.addView(tip);
            content.addView(empty, matchWrap());
        } else {
            List<Customer> sorted = new ArrayList<>(customers);
            sorted.sort((a, b) -> a.expiry.compareTo(b.expiry));
            for (Customer c : sorted) addCustomerCard(c);
        }

        LinearLayout note = card();
        note.setPadding(dp(15), dp(14), dp(15), dp(14));
        note.addView(text("CALLING & COVER", 11, BLUE, true));
        TextView disclaimer = text("This version organises renewals only. It does not place AI calls or send Sheet data to a server. An NCB estimate needs insurer and policy-schedule verification. Add-on cover applies only when shown in that customer’s schedule.", 12, MUTED, false);
        disclaimer.setPadding(0, dp(6), 0, 0);
        note.addView(disclaimer);
        LinearLayout.LayoutParams noteParams = matchWrap();
        noteParams.topMargin = dp(16);
        content.addView(note, noteParams);
    }

    private void addMetrics() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int soon = 0, overdue = 0, missing = 0;
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        for (Customer c : customers) {
            if (c.expiry == null) missing++;
            else {
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, c.expiry);
                if (days < 0) overdue++;
                else if (days <= 30) soon++;
            }
        }
        row.addView(metric("DUE IN 30 DAYS", String.valueOf(soon), BLUE), weighted());
        row.addView(metric("OVERDUE", String.valueOf(overdue), RED), weighted());
        row.addView(metric("DATE MISSING", String.valueOf(missing), ORANGE), weighted());
        content.addView(row, matchWrap());
    }

    private View metric(String label, String value, int accent) {
        LinearLayout c = card();
        c.setPadding(dp(12), dp(14), dp(8), dp(13));
        TextView num = text(value, 24, accent, true);
        c.addView(num);
        TextView caption = text(label, 9, MUTED, true);
        caption.setPadding(0, dp(4), 0, 0);
        c.addView(caption);
        return c;
    }

    private void addCustomerCard(Customer c) {
        LinearLayout box = card();
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        TextView person = text(c.owner.isEmpty() ? "Customer" : c.owner, 16, INK, true);
        heading.addView(person, new LinearLayout.LayoutParams(0, -2, 1));
        String status = c.expiry == null ? "DATE NEEDED" : status(c.expiry);
        TextView badge = text(status, 10, statusColor(status), true);
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setPadding(dp(8), dp(5), dp(8), dp(5));
        int tint = statusColor(status);
        badge.setBackground(round(Color.argb(25, Color.red(tint), Color.green(tint), Color.blue(tint)), 20));
        heading.addView(badge, wrap());
        box.addView(heading);

        String car = join(c.reg, join(c.make, c.model));
        box.addView(spaced(text(car.isEmpty() ? "Vehicle details unavailable" : car, 13, MUTED, false)));
        if (!c.phone.isEmpty()) box.addView(spaced(text(c.phone, 14, BLUE, true)));
        box.addView(spaced(text(join(c.insurer, c.expiry == null ? "Renewal date missing" : "Renewal  " + c.expiry.format(DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT))), 12, INK, false)));
        box.addView(spaced(text("Claim last year: " + (c.claim.isEmpty() ? "not recorded" : c.claim) + "     NCB estimate: " + ncb(c.claim), 12, MUTED, false)));
        LinearLayout.LayoutParams lp = matchWrap(); lp.bottomMargin = dp(10);
        content.addView(box, lp);
    }

    private String status(LocalDate date) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(ZoneId.of("Asia/Kolkata")), date);
        return days < 0 ? "OVERDUE" : days == 0 ? "TODAY" : days <= 30 ? "DUE SOON" : "UPCOMING";
    }
    private int statusColor(String s) { return s.equals("OVERDUE") ? RED : s.equals("DUE SOON") || s.equals("TODAY") ? ORANGE : GREEN; }
    private String ncb(String claim) { return claim.equalsIgnoreCase("yes") ? "0%*" : claim.equalsIgnoreCase("no") ? "20%*" : "verify"; }

    private void chooseFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");
        startActivityForResult(i, PICK_CSV);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_CSV || result != RESULT_OK || data == null || data.getData() == null) return;
        try { readCsv(data.getData()); render(); }
        catch (Exception ex) { Toast.makeText(this, "Could not read this CSV. Export the Sheet as CSV and try again.", Toast.LENGTH_LONG).show(); }
    }

    private void readCsv(Uri uri) throws Exception {
        LinkedHashMap<String, Customer> byPhone = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (line == null) throw new IllegalArgumentException("Empty CSV");
            List<String> headers = csv(line);
            Map<String, Integer> ix = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) ix.put(headers.get(i).trim().toLowerCase(Locale.ROOT), i);
            while ((line = br.readLine()) != null) {
                List<String> row = csv(line);
                String phone = value(row, ix, "mobile");
                String key = phone.replaceAll("[^0-9]", "");
                if (key.startsWith("91") && key.length() == 12) key = key.substring(2);
                if (key.startsWith("0") && key.length() == 11) key = key.substring(1);
                if (key.length() != 10 || key.charAt(0) < '6' || key.charAt(0) > '9') continue;
                Customer c = byPhone.get(key);
                if (c == null) {
                    c = new Customer(); c.phone = phone; c.owner = value(row, ix, "owner name");
                    c.insurer = value(row, ix, "insurance company name"); c.reg = value(row, ix, "reg no");
                    c.make = value(row, ix, "vehicle make"); c.model = value(row, ix, "vehicle model variant");
                    c.claim = value(row, ix, "claim reported in preceding policy year");
                    c.expiry = date(value(row, ix, "renewal date")); byPhone.put(key, c);
                }
            }
        }
        customers.clear(); customers.addAll(byPhone.values());
        if (customers.isEmpty()) throw new IllegalArgumentException("No Indian mobile numbers found");
    }

    private LocalDate date(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String value = raw.trim();
        for (String p : new String[]{"dd-MM-uuuu", "dd/MM/uuuu", "uuuu-MM-dd"}) try { return LocalDate.parse(value, DateTimeFormatter.ofPattern(p, Locale.ROOT).withResolverStyle(java.time.format.ResolverStyle.STRICT)); } catch (DateTimeParseException ignored) { }
        return null;
    }
    private String value(List<String> row, Map<String,Integer> ix, String key) {
        Integer i = ix.get(key); return i == null || i >= row.size() ? "" : row.get(i).trim();
    }
    private List<String> csv(String line) {
        List<String> out = new ArrayList<>(); StringBuilder cell = new StringBuilder(); boolean quoted = false;
        for (int i=0;i<line.length();i++) { char ch=line.charAt(i); if (ch=='"') { if (quoted && i+1<line.length() && line.charAt(i+1)=='"') { cell.append('"'); i++; } else quoted=!quoted; } else if (ch==',' && !quoted) { out.add(cell.toString()); cell.setLength(0); } else cell.append(ch); }
        out.add(cell.toString()); return out;
    }

    private LinearLayout card() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(15),dp(15),dp(15),dp(15)); l.setBackground(round(Color.WHITE,18)); return l; }
    private TextView text(String value, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if (bold) t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private View spaced(View v) { v.setPadding(0,dp(7),0,0); return v; }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1,-2); }
    private LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-2,-2); }
    private LinearLayout.LayoutParams weighted() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,-2,1); p.setMargins(0,0,dp(7),0); return p; }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private String join(String a,String b) { return a == null || a.isEmpty() ? b == null ? "" : b : b == null || b.isEmpty() ? a : a + "  ·  " + b; }
    private static final class Customer { String phone="",owner="",insurer="",reg="",make="",model="",claim=""; LocalDate expiry; }
}
