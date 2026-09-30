package dev.tvdeck.onecontroller.core.remotev2

import android.content.Context
import android.util.Log
import dev.tvdeck.onecontroller.core.crypto.CryptoManager
import dev.tvdeck.onecontroller.core.model.TvState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket

enum class RemoteDirection(val value: Int) {
    START_LONG(1),
    END_LONG(2),
    SHORT(3)
}

class AndroidTvRemoteV2Client(private val context: Context) {
    companion object {
        private const val TAG = "RemoteV2Client"
        const val DEFAULT_PAIRING_PORT = 6467
        const val DEFAULT_REMOTE_PORT = 6466
    }

    private var remoteSocket: SSLSocket? = null
    private var socketScope: CoroutineScope? = null
    private var serverCert: X509Certificate? = null

    private val _tvState = MutableStateFlow(TvState())
    val tvState: StateFlow<TvState> = _tvState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // -------------------------------------------------------------
    // Pairing Flow (Port 6467)
    // -------------------------------------------------------------

    suspend fun startPairing(
        host: String,
        port: Int = DEFAULT_PAIRING_PORT,
        onCodeRequested: suspend () -> String
    ): Boolean = withContext(Dispatchers.IO) {
        var pairingSocket: SSLSocket? = null
        try {
            Log.d(TAG, "Connecting to pairing port $port on $host")
            var capturedServerCert: X509Certificate? = null
            val sslContext = CryptoManager.getSslContext(context) { cert ->
                capturedServerCert = cert
            }

            pairingSocket = sslContext.socketFactory.createSocket() as SSLSocket
            pairingSocket.soTimeout = 30000
            pairingSocket.connect(InetSocketAddress(host, port), 8000)
            pairingSocket.startHandshake()

            val inStream = pairingSocket.inputStream
            val outStream = pairingSocket.outputStream

            // 1. Send PairingRequest (field 10)
            val pairingReqPayload = ByteArrayOutputStream().apply {
                ProtobufHelper.writeStringField(this, 1, "atvremote") // service_name MUST be atvremote
                ProtobufHelper.writeStringField(this, 2, "OneController") // client_name
            }.toByteArray()

            val outerReq = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, 2) // protocol_version = 2 (Android TV Remote v2 standard)
                ProtobufHelper.writeIntField(this, 2, 200) // STATUS_OK
                ProtobufHelper.writeMessageField(this, 10, pairingReqPayload) // pairing_request (field 10)
            }.toByteArray()

            sendPoloFrame(outStream, outerReq)

            // 2. Read PairingRequestAck
            val ackMsg = readPoloFrame(inStream)
            Log.d(TAG, "Received PairingRequestAck (${ackMsg.size} bytes)")

            // 3. Send Options (field 20)
            val encPayload = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, 3) // ENCODING_TYPE_HEXADECIMAL = 3
                ProtobufHelper.writeIntField(this, 2, 6) // symbol_length = 6
            }.toByteArray()

            val optionsPayload = ByteArrayOutputStream().apply {
                ProtobufHelper.writeMessageField(this, 1, encPayload) // input_encodings (field 1)
                ProtobufHelper.writeIntField(this, 3, 1) // preferred_role = ROLE_TYPE_INPUT (field 3)
            }.toByteArray()

            val outerOptions = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, 2) // protocol_version = 2
                ProtobufHelper.writeIntField(this, 2, 200) // STATUS_OK
                ProtobufHelper.writeMessageField(this, 20, optionsPayload) // options (field 20)
            }.toByteArray()

            sendPoloFrame(outStream, outerOptions)

            // 4. Read Options from TV
            val tvOptionsMsg = readPoloFrame(inStream)
            Log.d(TAG, "Received Options from TV (${tvOptionsMsg.size} bytes)")

            // 5. Send Configuration (field 30)
            val configEnc = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, 3) // ENCODING_TYPE_HEXADECIMAL = 3
                ProtobufHelper.writeIntField(this, 2, 6) // symbol_length = 6
            }.toByteArray()

            val configPayload = ByteArrayOutputStream().apply {
                ProtobufHelper.writeMessageField(this, 1, configEnc) // encoding (field 1)
                ProtobufHelper.writeIntField(this, 2, 1) // client_role = ROLE_TYPE_INPUT (field 2)
            }.toByteArray()

            val outerConfig = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, 2) // protocol_version = 2
                ProtobufHelper.writeIntField(this, 2, 200) // STATUS_OK
                ProtobufHelper.writeMessageField(this, 30, configPayload) // configuration (field 30)
            }.toByteArray()

            sendPoloFrame(outStream, outerConfig)

            // 6. Read ConfigurationAck - TV now shows code on screen!
            val confAck = readPoloFrame(inStream)
            Log.d(TAG, "ConfigurationAck received (${confAck.size} bytes). TV is now displaying 6-character code on screen.")

            // 7. Request pairing code from user
            val rawCode = onCodeRequested().trim().uppercase().replace(" ", "").removePrefix("0X")
            if (rawCode.length != 6) {
                Log.e(TAG, "Invalid pairing code: $rawCode (expected 6 hex characters)")
                return@withContext false
            }

            // 8. Compute Secret Hash
            val (_, clientCert) = CryptoManager.getOrCreateRemoteV2KeyAndCert(context)
            val sCert = capturedServerCert ?: (pairingSocket.session.peerCertificates[0] as X509Certificate)
            val secretBytes = CryptoManager.computePoloSecret(clientCert, sCert, rawCode)

            // 9. Send Secret (field 40)
            val secretPayload = ByteArrayOutputStream().apply {
                ProtobufHelper.writeBytesField(this, 1, secretBytes)
            }.toByteArray()

            val outerSecret = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, 2) // protocol_version = 2
                ProtobufHelper.writeIntField(this, 2, 200) // STATUS_OK
                ProtobufHelper.writeMessageField(this, 40, secretPayload) // secret (field 40)
            }.toByteArray()

            sendPoloFrame(outStream, outerSecret)

            // 10. Read SecretAck
            val secretAck = readPoloFrame(inStream)
            Log.d(TAG, "Pairing completed successfully! SecretAck received (${secretAck.size} bytes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Pairing failed: ${e.message}", e)
            false
        } finally {
            try {
                pairingSocket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun sendPoloFrame(out: OutputStream, payload: ByteArray) {
        ProtobufHelper.writeVarint(out, payload.size.toLong())
        out.write(payload)
        out.flush()
    }

    private fun readPoloFrame(input: InputStream): ByteArray {
        val len = ProtobufHelper.readVarint(input).toInt()
        val buf = ByteArray(len)
        var offset = 0
        while (offset < len) {
            val count = input.read(buf, offset, len - offset)
            if (count == -1) throw java.io.EOFException("Socket closed during read")
            offset += count
        }
        return buf
    }

    // -------------------------------------------------------------
    // Remote Control Flow (Port 6466)
    // -------------------------------------------------------------

    suspend fun connect(host: String, port: Int = DEFAULT_REMOTE_PORT): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        try {
            Log.d(TAG, "Connecting to Remote v2 service at $host:$port")
            val sslContext = CryptoManager.getSslContext(context) { cert ->
                serverCert = cert
            }

            val socket = sslContext.socketFactory.createSocket() as SSLSocket
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.connect(InetSocketAddress(host, port), 6000)
            socket.startHandshake()

            remoteSocket = socket
            _isConnected.value = true

            val outStream = socket.outputStream
            val inStream = socket.inputStream

            // Send RemoteConfigure
            val devInfo = ByteArrayOutputStream().apply {
                ProtobufHelper.writeStringField(this, 1, "OneController")
                ProtobufHelper.writeStringField(this, 2, "TVDeck")
                ProtobufHelper.writeIntField(this, 3, 1)
                ProtobufHelper.writeStringField(this, 4, "1")
                ProtobufHelper.writeStringField(this, 5, "dev.tvdeck.onecontroller")
                ProtobufHelper.writeStringField(this, 6, "1.0.0")
            }.toByteArray()

            val configMsg = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, 622)
                ProtobufHelper.writeMessageField(this, 2, devInfo)
            }.toByteArray()

            val outerConfig = ByteArrayOutputStream().apply {
                ProtobufHelper.writeMessageField(this, 1, configMsg) // remote_configure
            }.toByteArray()

            sendRemoteFrame(outStream, outerConfig)

            // Send RemoteSetActive
            val setActiveMsg = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, 622)
            }.toByteArray()

            val outerActive = ByteArrayOutputStream().apply {
                ProtobufHelper.writeMessageField(this, 2, setActiveMsg) // remote_set_active
            }.toByteArray()

            sendRemoteFrame(outStream, outerActive)

            // Start listening loop
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            socketScope = scope
            scope.launch {
                listenIncomingMessages(inStream, outStream)
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Remote v2: ${e.message}", e)
            _isConnected.value = false
            false
        }
    }

    private suspend fun listenIncomingMessages(inStream: InputStream, outStream: OutputStream) {
        try {
            while (currentCoroutineContext().isActive && remoteSocket?.isConnected == true) {
                val frame = readRemoteFrame(inStream)
                handleIncomingRemoteFrame(frame, outStream)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Remote v2 listen loop ended: ${e.message}")
        } finally {
            _isConnected.value = false
        }
    }

    private fun handleIncomingRemoteFrame(frame: ByteArray, outStream: OutputStream) {
        try {
            val bais = ByteArrayInputStream(frame)
            while (bais.available() > 0) {
                val tag = ProtobufHelper.readVarint(bais)
                val fieldNum = (tag ushr 3).toInt()
                val wireType = (tag and 0x7).toInt()

                when (fieldNum) {
                    8 -> { // remote_ping_request
                        val len = ProtobufHelper.readVarint(bais).toInt()
                        val pingData = ByteArray(len).apply { bais.read(this) }
                        val pingStream = ByteArrayInputStream(pingData)
                        var val1 = 0L
                        if (pingStream.available() > 0) {
                            val ptag = ProtobufHelper.readVarint(pingStream)
                            if ((ptag ushr 3) == 1L) {
                                val1 = ProtobufHelper.readVarint(pingStream)
                            }
                        }
                        // Send ping response
                        sendPingResponse(outStream, val1)
                    }
                    50 -> { // remote_set_volume_level
                        val len = ProtobufHelper.readVarint(bais).toInt()
                        val volBytes = ByteArray(len).apply { bais.read(this) }
                        parseVolumeLevel(volBytes)
                    }
                    else -> {
                        // Skip unhandled wire types
                        when (wireType) {
                            0 -> ProtobufHelper.readVarint(bais)
                            2 -> {
                                val l = ProtobufHelper.readVarint(bais).toInt()
                                bais.skip(l.toLong())
                            }
                            else -> bais.skip(1)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing incoming frame", e)
        }
    }

    private fun parseVolumeLevel(bytes: ByteArray) {
        try {
            val stream = ByteArrayInputStream(bytes)
            var currentVol = _tvState.value.volumeLevel
            var maxVol = _tvState.value.maxVolume
            var isMuted = _tvState.value.isMuted

            while (stream.available() > 0) {
                val tag = ProtobufHelper.readVarint(stream)
                val field = (tag ushr 3).toInt()
                val wire = (tag and 7).toInt()
                when (field) {
                    6 -> maxVol = ProtobufHelper.readVarint(stream).toInt()
                    7 -> currentVol = ProtobufHelper.readVarint(stream).toInt()
                    8 -> isMuted = ProtobufHelper.readVarint(stream) != 0L
                    else -> {
                        if (wire == 0) ProtobufHelper.readVarint(stream)
                        else if (wire == 2) stream.skip(ProtobufHelper.readVarint(stream))
                    }
                }
            }
            _tvState.value = _tvState.value.copy(
                volumeLevel = currentVol,
                maxVolume = maxVol,
                isMuted = isMuted
            )
        } catch (_: Exception) {}
    }

    private fun sendPingResponse(outStream: OutputStream, val1: Long) {
        try {
            val pingResp = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, val1)
            }.toByteArray()

            val outer = ByteArrayOutputStream().apply {
                ProtobufHelper.writeMessageField(this, 9, pingResp) // remote_ping_response
            }.toByteArray()

            sendRemoteFrame(outStream, outer)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send ping response", e)
        }
    }

    private fun sendRemoteFrame(out: OutputStream, payload: ByteArray) {
        synchronized(out) {
            ProtobufHelper.writeVarint(out, payload.size.toLong())
            out.write(payload)
            out.flush()
        }
    }

    private fun readRemoteFrame(input: InputStream): ByteArray {
        val len = ProtobufHelper.readVarint(input).toInt()
        val buf = ByteArray(len)
        var offset = 0
        while (offset < len) {
            val c = input.read(buf, offset, len - offset)
            if (c == -1) throw java.io.EOFException("Socket closed")
            offset += c
        }
        return buf
    }

    // -------------------------------------------------------------
    // Public Commands
    // -------------------------------------------------------------

    fun sendKey(keyCode: Int, direction: RemoteDirection = RemoteDirection.SHORT): Boolean {
        val socket = remoteSocket ?: return false
        if (!socket.isConnected) return false

        return try {
            val keyInject = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, keyCode.toLong())
                ProtobufHelper.writeIntField(this, 2, direction.value.toLong())
            }.toByteArray()

            val outer = ByteArrayOutputStream().apply {
                ProtobufHelper.writeMessageField(this, 10, keyInject) // remote_key_inject
            }.toByteArray()

            sendRemoteFrame(socket.outputStream, outer)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending key $keyCode: ${e.message}")
            false
        }
    }

    fun sendText(text: String): Boolean {
        val socket = remoteSocket ?: return false
        if (!socket.isConnected) return false

        return try {
            val imeObject = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, 0)
                ProtobufHelper.writeIntField(this, 2, 0)
                ProtobufHelper.writeStringField(this, 3, text)
            }.toByteArray()

            val editInfo = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, 1) // insert
                ProtobufHelper.writeMessageField(this, 2, imeObject)
            }.toByteArray()

            val batchEdit = ByteArrayOutputStream().apply {
                ProtobufHelper.writeIntField(this, 1, 0) // counter
                ProtobufHelper.writeIntField(this, 2, 0)
                ProtobufHelper.writeMessageField(this, 3, editInfo)
            }.toByteArray()

            val outer = ByteArrayOutputStream().apply {
                ProtobufHelper.writeMessageField(this, 21, batchEdit) // remote_ime_batch_edit
            }.toByteArray()

            sendRemoteFrame(socket.outputStream, outer)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text: ${e.message}")
            false
        }
    }

    fun launchAppLink(url: String) {
        val socket = remoteSocket ?: return
        if (!socket.isConnected) return

        try {
            val appLink = ByteArrayOutputStream().apply {
                ProtobufHelper.writeStringField(this, 1, url)
            }.toByteArray()

            val outer = ByteArrayOutputStream().apply {
                ProtobufHelper.writeMessageField(this, 90, appLink) // remote_app_link_launch_request
            }.toByteArray()

            sendRemoteFrame(socket.outputStream, outer)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app link: ${e.message}")
        }
    }

    fun disconnect() {
        socketScope?.cancel()
        socketScope = null
        try {
            remoteSocket?.close()
        } catch (_: Exception) {}
        remoteSocket = null
        _isConnected.value = false
    }
}
