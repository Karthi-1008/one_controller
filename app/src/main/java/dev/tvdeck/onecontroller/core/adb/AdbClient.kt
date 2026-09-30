package dev.tvdeck.onecontroller.core.adb

import android.content.Context
import android.util.Log
import dev.tvdeck.onecontroller.core.crypto.CryptoManager
import dev.tvdeck.onecontroller.core.model.AdbResult
import dev.tvdeck.onecontroller.core.model.TvFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

class AdbClient(private val context: Context) {
    companion object {
        private const val TAG = "AdbClient"
        const val DEFAULT_ADB_PORT = 5555

        const val A_SYNC = 0x434e5953
        const val A_CNXN = 0x4e584e43
        const val A_OPEN = 0x4e45504f
        const val A_OKAY = 0x59414b4f
        const val A_CLSE = 0x45534c43
        const val A_WRTE = 0x45545257
        const val A_AUTH = 0x48545541

        const val A_AUTH_TOKEN = 1
        const val A_AUTH_SIGNATURE = 2
        const val A_AUTH_RSAPUBLICKEY = 3

        const val A_VERSION = 0x01000000
        const val MAX_PAYLOAD = 1048576
    }

    private var socket: Socket? = null
    private var inStream: InputStream? = null
    private var outStream: OutputStream? = null
    private val localIdGen = AtomicInteger(1)
    private var isConnected = false

    data class AdbMessage(
        val command: Int,
        val arg0: Int,
        val arg1: Int,
        val dataLength: Int,
        val dataChecksum: Int,
        val magic: Int,
        val data: ByteArray = ByteArray(0)
    )

    suspend fun connect(host: String, port: Int = DEFAULT_ADB_PORT): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        try {
            Log.d(TAG, "Connecting to ADB on $host:$port")
            val s = Socket()
            s.tcpNoDelay = true
            s.keepAlive = true
            s.connect(InetSocketAddress(host, port), 5000)
            socket = s
            inStream = BufferedInputStream(s.getInputStream(), 65536)
            outStream = BufferedOutputStream(s.getOutputStream(), 65536)

            val keyPair = CryptoManager.getOrCreateAdbKeyPair(context)

            // Send CNXN
            val cnxnData = "host::OneController\u0000".toByteArray(Charsets.UTF_8)
            sendMessage(A_CNXN, A_VERSION, MAX_PAYLOAD, cnxnData)

            var signatureSent = false
            while (true) {
                val msg = readMessage()
                when (msg.command) {
                    A_CNXN -> {
                        val banner = String(msg.data, Charsets.UTF_8)
                        Log.d(TAG, "Connected to ADB device: $banner")
                        isConnected = true
                        return@withContext true
                    }
                    A_AUTH -> {
                        if (msg.arg0 == A_AUTH_TOKEN) {
                            if (!signatureSent) {
                                // Sign token
                                val sig = CryptoManager.signAdbToken(keyPair, msg.data)
                                sendMessage(A_AUTH, A_AUTH_SIGNATURE, 0, sig)
                                signatureSent = true
                            } else {
                                // Signature not accepted, send public key
                                val pubKey = CryptoManager.getAdbPublicKeyPayload(keyPair)
                                sendMessage(A_AUTH, A_AUTH_RSAPUBLICKEY, 0, pubKey.toByteArray(Charsets.UTF_8))
                            }
                        } else {
                            Log.w(TAG, "Unexpected AUTH arg0: ${msg.arg0}")
                        }
                    }
                    else -> {
                        Log.w(TAG, "Unexpected command during handshake: ${Integer.toHexString(msg.command)}")
                        return@withContext false
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "ADB Connection failed: ${e.message}", e)
            disconnect()
            return@withContext false
        }
    }

    suspend fun executeShell(command: String): AdbResult = withContext(Dispatchers.IO) {
        if (!isConnected || socket == null) {
            return@withContext AdbResult(false, "Not connected to ADB")
        }

        val localId = localIdGen.incrementAndGet()
        val dest = "shell:$command\u0000".toByteArray(Charsets.UTF_8)

        return@withContext try {
            sendMessage(A_OPEN, localId, 0, dest)

            var remoteId = 0
            val outputBuffer = ByteArrayOutputStream()

            while (true) {
                val msg = readMessage()
                if (msg.arg1 != localId) continue

                when (msg.command) {
                    A_OKAY -> {
                        remoteId = msg.arg0
                    }
                    A_WRTE -> {
                        remoteId = msg.arg0
                        outputBuffer.write(msg.data)
                        // Acknowledge receipt
                        sendMessage(A_OKAY, localId, remoteId, ByteArray(0))
                    }
                    A_CLSE -> {
                        // Remote closed channel
                        sendMessage(A_CLSE, localId, remoteId, ByteArray(0))
                        val resultStr = outputBuffer.toString("UTF-8")
                        return@withContext AdbResult(true, resultStr, 0)
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            AdbResult(false, "Channel closed prematurely")
        } catch (e: Exception) {
            Log.e(TAG, "Shell execution failed: ${e.message}", e)
            AdbResult(false, "Error: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // File Management via ADB Shell
    // -------------------------------------------------------------

    suspend fun listFiles(path: String): List<TvFileInfo> = withContext(Dispatchers.IO) {
        val cmd = "ls -la \"$path\""
        val res = executeShell(cmd)
        if (!res.success) return@withContext emptyList()

        val files = mutableListOf<TvFileInfo>()
        for (line in res.output.lines()) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 8) {
                val perms = parts[0]
                val isDir = perms.startsWith("d")
                val size = parts[4].toLongOrNull() ?: 0L
                val name = parts.subList(7, parts.size).joinToString(" ")
                if (name != "." && name != "..") {
                    val fullPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
                    files.add(
                        TvFileInfo(
                            name = name,
                            path = fullPath,
                            isDirectory = isDir,
                            sizeBytes = size,
                            permissions = perms,
                            lastModified = "${parts[5]} ${parts[6]}"
                        )
                    )
                }
            }
        }
        files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val res = executeShell("rm -rf \"$path\"")
        res.success
    }

    suspend fun makeDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        val res = executeShell("mkdir -p \"$path\"")
        res.success
    }

    suspend fun pushFile(localFile: File, remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Read local file bytes, base64 chunked upload or direct shell base64 write
            val bytes = localFile.readBytes()
            val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val chunkSize = 40000
            val totalChunks = (b64.length + chunkSize - 1) / chunkSize

            executeShell("rm -f \"$remotePath.tmp\"")
            for (i in 0 until totalChunks) {
                val start = i * chunkSize
                val end = minOf(start + chunkSize, b64.length)
                val chunk = b64.substring(start, end)
                val appendOp = if (i == 0) ">" else ">>"
                executeShell("echo -n \"$chunk\" $appendOp \"$remotePath.b64\"")
            }
            executeShell("base64 -d \"$remotePath.b64\" > \"$remotePath\" && rm -f \"$remotePath.b64\"")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Push file failed", e)
            false
        }
    }

    suspend fun installApk(localApk: File): AdbResult = withContext(Dispatchers.IO) {
        val tempPath = "/data/local/tmp/app_upload_${System.currentTimeMillis()}.apk"
        val pushed = pushFile(localApk, tempPath)
        if (!pushed) return@withContext AdbResult(false, "Failed to upload APK to device")

        val res = executeShell("pm install -r -d -g -t \"$tempPath\"")
        executeShell("rm -f \"$tempPath\"")
        res
    }

    // -------------------------------------------------------------
    // Low-level ADB Packet I/O
    // -------------------------------------------------------------

    private fun sendMessage(command: Int, arg0: Int, arg1: Int, data: ByteArray) {
        val out = outStream ?: return
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(command)
        header.putInt(arg0)
        header.putInt(arg1)
        header.putInt(data.size)
        header.putInt(calcChecksum(data))
        header.putInt(command xor -1)

        synchronized(out) {
            out.write(header.array())
            if (data.isNotEmpty()) {
                out.write(data)
            }
            out.flush()
        }
    }

    private fun readMessage(): AdbMessage {
        val inS = inStream ?: throw IOException("Not connected")
        val header = ByteArray(24)
        var readBytes = 0
        while (readBytes < 24) {
            val c = inS.read(header, readBytes, 24 - readBytes)
            if (c == -1) throw EOFException("Socket closed while reading header")
            readBytes += c
        }

        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val command = buf.int
        val arg0 = buf.int
        val arg1 = buf.int
        val dataLength = buf.int
        val checksum = buf.int
        val magic = buf.int

        val data = if (dataLength > 0) {
            val d = ByteArray(dataLength)
            var dRead = 0
            while (dRead < dataLength) {
                val c = inS.read(d, dRead, dataLength - dRead)
                if (c == -1) throw EOFException("Socket closed while reading data")
                dRead += c
            }
            d
        } else {
            ByteArray(0)
        }

        return AdbMessage(command, arg0, arg1, dataLength, checksum, magic, data)
    }

    private fun calcChecksum(data: ByteArray): Int {
        var sum = 0
        for (b in data) {
            sum += (b.toInt() and 0xFF)
        }
        return sum
    }

    fun isConnected(): Boolean = isConnected && socket?.isConnected == true

    fun disconnect() {
        isConnected = false
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        inStream = null
        outStream = null
    }
}
