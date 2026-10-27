package com.soma369.laimory.core.collection.collector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturedPhotoPolicyTest {
    @Test
    fun `촬영 정보가 있으면 어느 폴더에 있든 찍은 사진이다`() {
        assertTrue(CapturedPhotoPolicy.isCaptured("20261018_120000.jpg", "DCIM/Camera/", hasCaptureInfo = true))
        // 갤러리에서 앨범으로 옮긴 카메라 사진.
        assertTrue(CapturedPhotoPolicy.isCaptured("20261018_120000.jpg", "Pictures/여행/", hasCaptureInfo = true))
    }

    @Test
    fun `촬영 정보가 없으면 DCIM 아래여도 찍은 사진이 아니다`() {
        assertFalse(CapturedPhotoPolicy.isCaptured("image.jpg", "DCIM/여행/", hasCaptureInfo = false))
        assertFalse(CapturedPhotoPolicy.isCaptured("download.png", "Download/", hasCaptureInfo = false))
    }

    @Test
    fun `캡처 파일명이면 촬영 정보가 있어도 뺀다`() {
        assertFalse(CapturedPhotoPolicy.isCaptured("Screenshot_20261018-120000_Chrome.jpg", "DCIM/여행/", hasCaptureInfo = true))
        assertFalse(CapturedPhotoPolicy.isCaptured("screenshot.png", "Pictures/", hasCaptureInfo = true))
    }

    @Test
    fun `캡처 폴더면 촬영 정보가 있어도 뺀다`() {
        assertFalse(CapturedPhotoPolicy.isCaptured("a.jpg", "DCIM/Screenshots/", hasCaptureInfo = true))
        assertFalse(CapturedPhotoPolicy.isCaptured("a.jpg", "Pictures/Screenshots/", hasCaptureInfo = true))
        assertFalse(CapturedPhotoPolicy.isCaptured("a.jpg", "/storage/emulated/0/DCIM/Screenshots/a.jpg", hasCaptureInfo = null))
    }

    @Test
    fun `촬영 정보 컬럼이 없는 버전은 DCIM 아래인지로 본다`() {
        assertTrue(CapturedPhotoPolicy.isCaptured("a.jpg", "DCIM/Camera/", hasCaptureInfo = null))
        assertTrue(CapturedPhotoPolicy.isCaptured("a.jpg", "/storage/emulated/0/DCIM/Camera/a.jpg", hasCaptureInfo = null))
        assertFalse(CapturedPhotoPolicy.isCaptured("a.jpg", "Pictures/KakaoTalk/", hasCaptureInfo = null))
        assertFalse(CapturedPhotoPolicy.isCaptured("a.jpg", null, hasCaptureInfo = null))
    }
}
