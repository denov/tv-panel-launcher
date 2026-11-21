package com.gathin.tvhome;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** The widget provider's own tile (TV Panel's) leaves Home: its chips are the way in, and Hold OK on one opens it. */
public class WidgetProviderTileTest {
    @Test public void theProvidersTileIsHidden() {
        List<String> hidden = new ArrayList<>(Collections.singletonList("com.sony.dtv.tvx"));
        Config.hideWidgetProviderTile(hidden, "com.fullstacksystems.tvpanel", false);
        assertEquals(Arrays.asList("com.sony.dtv.tvx", "com.fullstacksystems.tvpanel"), hidden);
        // Once only.
        Config.hideWidgetProviderTile(hidden, "com.fullstacksystems.tvpanel", false);
        assertEquals(2, hidden.size());
    }

    @Test public void unlessTheTileIsWantedOrThereIsNoRow() {
        List<String> hidden = new ArrayList<>();
        Config.hideWidgetProviderTile(hidden, "com.fullstacksystems.tvpanel", true);
        Config.hideWidgetProviderTile(hidden, "", false);
        assertEquals(Collections.emptyList(), hidden);
    }
}
