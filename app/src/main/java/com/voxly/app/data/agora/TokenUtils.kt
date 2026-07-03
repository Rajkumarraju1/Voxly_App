package com.voxly.app.data.agora

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

/**
 * A simpler helper to generate Agora Tokens client-side.
 * ADAPTED FROM AGORA OPEN SOURCE SAMPLES.
 * NOTE: This is for TESTING purposes only. In production, tokens must be generated on a server.
 */
object TokenUtils {

    fun generateToken(
        appId: String,
        appCertificate: String,
        channelName: String,
        uid: Int,
        expirationTimeInSeconds: Int = 3600
    ): String {
        if (appId.isBlank() || appCertificate.isBlank()) {
            return ""
        }
        
        // Calculate privilege expiration
        val currentTimestamp = (System.currentTimeMillis() / 1000).toInt()
        val privilegeTs = currentTimestamp + expirationTimeInSeconds
        val salt = (1..99999999).random()

        // Pack the content
        val packable = ByteBuf()
        packable.put(appId.toByteArray())
        packable.put(channelName.toByteArray())
        packable.putInt(uid) // user ID
        packable.putInt(salt)
        packable.putInt(privilegeTs) // timestamp
        packable.putShort(1.toShort()) // privilege count
        packable.putShort(1.toShort()) // kJoinChannel
        packable.putInt(privilegeTs)

        val signature = generateSignature(appCertificate, packable.buffer)
        val crc = CRC32()
        crc.update(packable.buffer)
        val crcValue = crc.value.toInt()
        val crcBuff = ByteBuf().putInt(crcValue).buffer
        
        // Final Message
        val m = ByteBuf()
            .put(signature)
            .put(crcBuff)
            .put(packable.buffer)
            .buffer
            
        // Version string: "006" + appId + base64(m)
        return "006$appId${Base64.encodeToString(m, Base64.NO_WRAP)}"
    }

    private fun generateSignature(appCertificate: String, msg: ByteArray): ByteArray {
        val key = SecretKeySpec(appCertificate.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        return mac.doFinal(msg)
    }

    // Helper class to pack bytes manually (Little Endian)
    class ByteBuf {
        val stream = ByteArrayOutputStream()

        val buffer: ByteArray
            get() = stream.toByteArray()

        fun put(bytes: ByteArray): ByteBuf {
            putShort(bytes.size.toShort())
            stream.write(bytes)
            return this
        }

        fun putInt(value: Int): ByteBuf {
            stream.write(value and 0xFF)
            stream.write((value shr 8) and 0xFF)
            stream.write((value shr 16) and 0xFF)
            stream.write((value shr 24) and 0xFF)
            return this
        }

        fun putShort(value: Short): ByteBuf {
            stream.write(value.toInt() and 0xFF)
            stream.write((value.toInt() shr 8) and 0xFF)
            return this
        }
    }
}
