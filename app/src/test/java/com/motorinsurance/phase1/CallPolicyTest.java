package com.motorinsurance.phase1;
import org.junit.Test;
import static org.junit.Assert.*;

public class CallPolicyTest {
    // Constructed fixtures; never dialed or used as customer data.
    private String local() { return "6" + "0".repeat(9); }
    @Test public void equivalentFormatsDeduplicate() {
        String c=CallPolicy.normalize(local());
        assertEquals(c,CallPolicy.normalize("+91 "+local()));
        assertEquals(c,CallPolicy.normalize("0"+local()));
        assertEquals(c,CallPolicy.normalize("0091"+local()));
        assertEquals(CallPolicy.key(c),CallPolicy.key(CallPolicy.normalize("91"+local())));
    }
    @Test public void rejectDialCodesAndExtensions() {
        for (String n : new String[]{"112","*"+local()+"#","+1"+local(),local()+";123",local()+" ext 4",""}) {
            try { CallPolicy.normalize(n); fail("Unsafe input accepted"); } catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void dailyLimitAndRollback() {
        assertTrue(CallPolicy.duplicate(100,100)); assertTrue(CallPolicy.duplicate(100,99)); assertFalse(CallPolicy.duplicate(100,101));
    }
    @Test public void cooldownUsesMonotonicClock() {
        assertEquals(60_000,CallPolicy.remaining(1000,2000,1,1000,2000,1));
        assertEquals(1,CallPolicy.remaining(1000,2000,1,999_999,61_999,1));
        assertEquals(0,CallPolicy.remaining(1000,2000,1,1001,62_000,1));
        assertEquals(60_000,CallPolicy.remaining(1000,2000,1,500,500,2));
    }
    @Test public void indiaMidnightBoundary() {
        long before=java.time.Instant.parse("2026-10-10T18:29:59Z").toEpochMilli();
        assertEquals(CallPolicy.day(before)+1,CallPolicy.day(before+1000));
    }
}
