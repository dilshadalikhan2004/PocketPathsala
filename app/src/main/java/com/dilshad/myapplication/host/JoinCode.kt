package com.dilshad.myapplication.host

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

data class JoinCode(val url: String, val matrix: BitMatrix)

fun joinUrl(hostAddress: String, port: Int): String =
    "http://${hostAddress.trim().removePrefix("http://").removeSuffix("/")}:$port/"

fun makeJoinCode(url: String): JoinCode {
    require(url.startsWith("http://")) { "Join URL must be local HTTP." }
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1)
    return JoinCode(url, MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 256, 256, hints))
}
