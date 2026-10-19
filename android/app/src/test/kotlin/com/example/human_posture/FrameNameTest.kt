package com.example.human_posture

import org.junit.Assert.*
import org.junit.Test

class FrameNameTest {
    @Test fun handlesUnderscoresAndRepeatedDownloadSuffixes() {
        val p = FrameName.parse("Glide_shot_put_19_cam_4_2_4(1)(2).jpg")!!
        assertEquals("Glide_shot_put_19_cam_4",p.clip)
        assertEquals(8,p.index)
        assertEquals("framed_Glide_shot_put_19_cam_4_h36m.npy",p.gtName)
        assertTrue(p.duplicate)
    }
    @Test fun rejectsOverlayAndOutOfRangeNames() {
        listOf("pose_Axel_1_cam_1_1_1.jpg","X_0_1.jpg","X_21_1.jpg","X_1_6.jpg","X.jpg").forEach { assertNull(FrameName.parse(it)) }
    }
    @Test fun firstAndLastFrameUseZeroBasedIndex() {
        assertEquals(0,FrameName.parse("X_1_1.jpg")!!.index)
        assertEquals(99,FrameName.parse("X_20_5.jpg")!!.index)
    }
    @Test fun normalizesGroundTruthDownloadSuffixes() {
        assertEquals("framed_Axel_9_cam_11_h36m.npy",FrameName.canonicalGtName("framed_Axel_9_cam_11_h36m(1)(2).npy"))
    }
}
