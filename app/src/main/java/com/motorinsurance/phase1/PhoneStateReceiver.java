package com.motorinsurance.phase1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/** Receives system call-state events even while the dialer is foreground. Never initiates calls. */
public final class PhoneStateReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) return;
        try {
            CallStore store = new CallStore(context);
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                // Aggregate state prevents a second SIM's IDLE event ending another active call.
                store.observe(store.inCall());
            } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                store.observe(true);
            }
        } catch (RuntimeException ignored) {
            // Do not log numbers. Pending remains blocked; activity reconciles on return.
        }
    }
}
