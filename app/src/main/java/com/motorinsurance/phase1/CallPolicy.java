package com.motorinsurance.phase1;

import java.time.Instant;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Pure rules: India mobile numbers only; equivalent formats share one daily key. */
public final class CallPolicy {
    public static final long GAP_MS = 60_000;
    private CallPolicy() {}
    public static String normalize(String input) {
        if (input == null || !input.matches("[+0-9() \\-]+")) throw new IllegalArgumentException("Use an Indian mobile number; no extensions or dial codes.");
        String n = input.replaceAll("[() \\-]", "");
        if (n.startsWith("+91")) n = n.substring(3);
        else if (n.startsWith("0091") && n.length() == 14) n = n.substring(4);
        else if (n.startsWith("91") && n.length() == 12) n = n.substring(2);
        else if (n.startsWith("0") && n.length() == 11) n = n.substring(1);
        if (!n.matches("[6-9][0-9]{9}")) throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number.");
        return "+91" + n;
    }
    public static long day(long now) {
        return Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate().toEpochDay();
    }
    public static boolean duplicate(long previousDay, long today) { return previousDay >= today; }
    public static long remaining(long endWall, long endElapsed, int endBoot, long nowWall, long nowElapsed, int boot) {
        if (endWall == 0) return 0;
        long since = (endBoot == boot && endElapsed <= nowElapsed) ? nowElapsed - endElapsed : nowWall - endWall;
        return Math.max(0, GAP_MS - Math.max(0, since));
    }
    public static String key(String canonical) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder s = new StringBuilder("number_");
            for (byte b : digest) s.append(String.format("%02x", b & 255));
            return s.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
