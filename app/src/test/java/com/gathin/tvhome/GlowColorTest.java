package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** ambient_glow_color from config.xml or a theme: one colour for the glow, or 0 to follow the focused app. */
public class GlowColorTest {

    @Test
    public void aColourByHex() {
        assertEquals(0xFF228B22, Config.glowColor("#228B22"));
        assertEquals(0x80228B22, Config.glowColor(" #80228b22 "));
    }

    @Test
    public void emptyOrAnythingElseFollowsTheApp() {
        assertEquals(0, Config.glowColor(""));
        assertEquals(0, Config.glowColor(null));
        assertEquals(0, Config.glowColor("green"));
        assertEquals(0, Config.glowColor("#12345"));
        assertEquals(0, Config.glowColor("228B22"));
    }
}
