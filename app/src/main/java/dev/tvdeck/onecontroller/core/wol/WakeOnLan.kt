package dev.tvdeck.onecontroller.core.wol

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLan {
    private const val TAG = "WakeOnLan"

    suspend fun wake(macStr: String, broadcastIp: String = "255.255.255.255"): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanMac = macStr.replace(":", "").replace("-", "")
            if (cleanMac.length != 12) {
                Log.e(TAG, "Invalid MAC address: $macStr")
                return@withContext false
            }

            val macBytes = ByteArray(6)
            for (i in 0 until 6) {
                macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }

            // Magic packet: 6 bytes 0xFF followed by 16 repetitions of MAC
            val bytes = ByteArray(6 + 16 * 6)
            for (i in 0 until 6) {
                bytes[i] = 0xFF.toByte()
            }
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, bytes, 6 + i * 6, 6)
            }

            val address = InetAddress.getByName(broadcastIp)
            DatagramSocket().use { socket ->
                socket.broadcast = true
                // Send to port 9
                socket.send(DatagramPacket(bytes, bytes.size, address, 9))
                // Send to port 7
                socket.send(DatagramPacket(bytes, bytes.size, address, 7))
            }
            Log.d(TAG, "WoL packet sent successfully to $macStr")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send WoL packet", e)
            false
        }
    }
}
