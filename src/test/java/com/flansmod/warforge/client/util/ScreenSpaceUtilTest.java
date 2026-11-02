package com.flansmod.warforge.client.util;

import com.flansmod.warforge.client.util.ScreenSpaceUtil.ScreenPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenSpaceUtilTest {
    @Test
    void topAnchorsStackDownward() {
        ScreenSpaceUtil.resetOffsets(400, 300);
        assertEquals(0, ScreenSpaceUtil.getY(ScreenPos.TOP_LEFT, 10));
        assertEquals(10, ScreenSpaceUtil.getY(ScreenPos.TOP_LEFT, 5));
        ScreenSpaceUtil.incrementY(ScreenPos.TOP_LEFT, 3);
        assertEquals(18, ScreenSpaceUtil.currentY(ScreenPos.TOP_LEFT));
    }

    @Test
    void bottomAnchorsStackUpward() {
        ScreenSpaceUtil.resetOffsets(400, 300);
        assertEquals(290, ScreenSpaceUtil.getY(ScreenPos.BOTTOM_RIGHT, 10));
        ScreenSpaceUtil.incrementY(ScreenPos.BOTTOM_RIGHT, 4);
        assertEquals(286, ScreenSpaceUtil.currentY(ScreenPos.BOTTOM_RIGHT));
    }

    @Test
    void horizontalPlacement() {
        ScreenSpaceUtil.resetOffsets(400, 300);
        assertEquals(0, ScreenSpaceUtil.getX(ScreenPos.TOP_LEFT, 50));
        assertEquals(350, ScreenSpaceUtil.getX(ScreenPos.TOP_RIGHT, 50));
        assertEquals(175, ScreenSpaceUtil.getX(ScreenPos.TOP, 50));
    }
}
