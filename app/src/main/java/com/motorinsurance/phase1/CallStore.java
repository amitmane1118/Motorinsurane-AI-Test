package com.motorinsurance.phase1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;
import android.telecom.TelecomManager;

/** Only hashed number/day markers and call timing are stored, privately on this device. */
final class CallStore {
    private final Context context;
    private final SharedPreferences p;
    CallStore(Context c) { context = c.getApplicationContext(); p = context.getSharedPreferences("call_guard", Context.MODE_PRIVATE); }
    private int boot() { return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1); }
    boolean pending() { return p.getBoolean("pending", false); }
    boolean inCall() {
        if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Phone permission is required to verify call state.");
        TelecomManager telecom = context.getSystemService(TelecomManager.class);
        if (telecom == null) throw new IllegalStateException("Calling unavailable on this device.");
        return telecom.isInCall();
    }
    boolean attempted(String n) { return p.contains(CallPolicy.key(n)); }
    boolean duplicate(String n) { return CallPolicy.duplicate(p.getLong(CallPolicy.key(n), Long.MIN_VALUE), CallPolicy.day(System.currentTimeMillis())); }
    void save(SharedPreferences.Editor e) { if (!e.commit()) throw new IllegalStateException("Cannot save call protection. Call blocked."); }
    long remaining() {
        if (p.getLong("endWall",0) != 0 && p.getInt("endBoot",-2) != boot()) ended();
        return CallPolicy.remaining(p.getLong("endWall",0), p.getLong("endElapsed",0), p.getInt("endBoot",-2),
                System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot());
    }
    void ended() {
        save(p.edit().putBoolean("pending",false).putBoolean("active",false)
            .putLong("endWall",System.currentTimeMillis()).putLong("endElapsed",SystemClock.elapsedRealtime()).putInt("endBoot",boot()));
    }
    void observe(boolean active) {
        if (active) {
            if (!p.getBoolean("active",false)) save(p.edit().putBoolean("active",true));
        } else if (pending() || p.getBoolean("active",false)) ended();
    }
    void reserve(String n, boolean retry) {
        long now = System.currentTimeMillis();
        if (now < p.getLong("lastWall",0)) throw new IllegalStateException("Device clock moved backwards. Restore automatic date/time.");
        if (pending() || inCall()) throw new IllegalStateException("A call is still active or awaiting its end.");
        if (remaining() > 0) throw new IllegalStateException("Wait for the 60-second cooldown.");
        if (retry && !attempted(n)) throw new IllegalStateException("Use Start queue for the first attempt.");
        if (!retry && duplicate(n)) throw new IllegalStateException("Duplicate blocked for today.");
        long day = Math.max(p.getLong(CallPolicy.key(n),Long.MIN_VALUE),CallPolicy.day(now));
        save(p.edit().putLong(CallPolicy.key(n),day).putLong("lastWall",now).putBoolean("pending",true));
    }
}
