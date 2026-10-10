package com.motorinsurance.phase1;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;

public final class MainActivity extends Activity {
    private EditText numbers, retryNumber;
    private TextView status;
    private CallStore store;
    private final ArrayDeque<String> queue = new ArrayDeque<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running, resumed, launched;
    private long launchElapsed;
    private final Runnable ticker = new Runnable() {
        @Override public void run() { if (resumed) { tick(); handler.postDelayed(this,1000); } }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);
        store = new CallStore(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        body.setPadding(pad,pad,pad,pad);
        scroll.addView(body);
        if (android.os.Build.VERSION.SDK_INT >= 30) scroll.setOnApplyWindowInsetsListener((v,insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
        });
        label(body,"Motor Insurance • Phase 1 SIM Call Test",24);
        label(body,"This test starts ordinary SIM calls. You speak yourself. No AI voice, recording, or customer database.",16);
        label(body,"Indian mobile numbers only. Paste one per line. One automatic attempt per number per India calendar day, including failed/unanswered attempts.",16);
        numbers = input(body,"Test numbers — one per line",true);
        button(body,"Grant call permissions",() -> requestPermissions(new String[]{Manifest.permission.CALL_PHONE,Manifest.permission.READ_PHONE_STATE},10));
        button(body,"Start queue",this::startQueue);
        button(body,"Stop queue",() -> { running=false; queue.clear(); status.setText("Queue stopped. An existing SIM call must be ended in the phone app."); });
        status = label(body,"Ready. Use only numbers you are authorized to test.",16);
        label(body,"Manual retry: Call Again → Confirm. The 60-second cooldown still applies.",16);
        retryNumber = input(body,"Number to call again",false);
        button(body,"Call Again",this::confirmRetry);
        label(body,"Keep this app visible between calls. Return here after each call. No calls start while this app is in the background. Restarting the app stops the queue.",14);
        setContentView(scroll);
    }
    private TextView label(LinearLayout b,String text,int size) {
        TextView t = new TextView(this); t.setText(text); t.setTextSize(size); t.setPadding(0,12,0,12); b.addView(t); return t;
    }
    private EditText input(LinearLayout b,String hint,boolean multiline) {
        EditText e = new EditText(this); e.setHint(hint);
        e.setInputType(multiline ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE : InputType.TYPE_CLASS_PHONE);
        e.setSaveEnabled(false); e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        if (multiline) e.setMinLines(3); b.addView(e); return e;
    }
    private void button(LinearLayout b,String title,Runnable action) {
        Button btn = new Button(this); btn.setText(title); btn.setOnClickListener(v -> action.run()); b.addView(btn);
    }
    private boolean permissions() {
        return checkSelfPermission(Manifest.permission.CALL_PHONE)==PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED;
    }
    private void requirePermissions() {
        if (!permissions()) throw new IllegalStateException("Grant Call and Phone permissions first, then press Start queue.");
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) throw new IllegalStateException("A voice-capable SIM device is required.");
    }
    private void startQueue() {
        try {
            requirePermissions();
            if (store.pending() || store.inCall()) throw new IllegalStateException("Wait for the current call to end.");
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (String line : numbers.getText().toString().split("\\R")) if (!line.trim().isEmpty()) unique.add(CallPolicy.normalize(line.trim()));
            if (unique.isEmpty()) throw new IllegalArgumentException("Enter test numbers first.");
            queue.clear(); int skipped=0;
            for (String n : unique) { if (store.duplicate(n)) skipped++; else queue.add(n); }
            numbers.setText("");
            running = !queue.isEmpty();
            status.setText("Queue ready: "+queue.size()+". Previously attempted today skipped: "+skipped+".");
            // First call waits briefly so Stop is available before dispatch.
        } catch (RuntimeException e) { status.setText(e.getMessage()); }
    }
    private void confirmRetry() {
        running=false; queue.clear();
        try {
            requirePermissions();
            final String canonical=CallPolicy.normalize(retryNumber.getText().toString().trim());
            if (!store.attempted(canonical)) throw new IllegalStateException("This number has no previous attempt. Use Start queue.");
            new AlertDialog.Builder(this).setTitle("Confirm Call Again")
                .setMessage("Place another SIM call to "+canonical+"? This is an explicit manual retry and still requires the cooldown.")
                .setNegativeButton("Cancel",(d,w) -> status.setText("Retry cancelled."))
                .setPositiveButton("Confirm",(d,w) -> {
                    if (!resumed) return;
                    try { requirePermissions(); dial(canonical,true); retryNumber.setText(""); }
                    catch (RuntimeException e) { status.setText(e.getMessage()); }
                }).show();
        } catch (RuntimeException e) { status.setText(e.getMessage()); }
    }
    private void dial(String n,boolean retry) {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Call permission is required.");
        store.reserve(n,retry); // Durable reservation BEFORE any external dialer handoff.
        launched=true; launchElapsed=android.os.SystemClock.elapsedRealtime();
        try {
            startActivity(new Intent(Intent.ACTION_CALL,Uri.fromParts("tel",n,null)));
            status.setText("SIM call requested. End it in the phone app, then return here.");
        } catch (RuntimeException e) {
            running=false; queue.clear(); launched=false; store.ended();
            throw new IllegalStateException("Call could not start. Attempt retained; retry only with Call Again → Confirm.");
        }
    }
    private void tick() {
        if (!permissions()) { running=false; queue.clear(); return; }
        try {
            boolean active=store.inCall();
            if (active) { store.observe(true); status.setText("Call active. Queue paused."); return; }
            // Do not mistake the brief idle period before dialer handoff for call completion.
            if (launched && android.os.SystemClock.elapsedRealtime()-launchElapsed < 5000) return;
            store.observe(false); launched=false;
            long wait=store.remaining();
            if (wait>0) { status.setText("Cooldown: "+((wait+999)/1000)+" seconds. Queue remaining: "+queue.size()+"."); return; }
            if (!running) return;
            while (!queue.isEmpty() && store.duplicate(queue.peek())) queue.remove();
            if (queue.isEmpty()) { running=false; status.setText("Queue complete. No automatic retries."); return; }
            requirePermissions(); dial(queue.remove(),false);
        } catch (RuntimeException e) { running=false; queue.clear(); status.setText(e.getMessage()); }
    }
    @Override protected void onResume() {
        super.onResume(); resumed=true; handler.postDelayed(ticker,1000);
        // If events were missed while stopped/killed, conservatively begin a fresh gap on return.
        if (permissions()) try { store.observe(store.inCall()); launched=false; } catch (RuntimeException e) { status.setText("Call state unavailable. Queue blocked."); running=false; }
    }
    @Override protected void onPause() { resumed=false; handler.removeCallbacks(ticker); super.onPause(); }
}
