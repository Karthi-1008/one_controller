package dev.tvdeck.onecontroller.core.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.*
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.net.ssl.*
import javax.security.auth.x500.X500Principal

object CryptoManager {
    private const val TAG = "CryptoManager"
    private const val KEYSTORE_ALIAS = "OneControllerRemoteKey"
    private const val PREFS_NAME = "onecontroller_crypto_prefs"
    private const val PREF_ADB_PRIV_KEY = "adb_private_key"
    private const val PREF_ADB_PUB_KEY = "adb_public_key"

    private var cachedSslContext: SSLContext? = null
    private var cachedClientCertificate: X509Certificate? = null
    private var cachedKeyPair: KeyPair? = null

    // -------------------------------------------------------------
    // TLS / Android TV Remote v2 Keys & SSLContext
    // -------------------------------------------------------------

    @Synchronized
    fun getOrCreateRemoteV2KeyAndCert(context: Context): Pair<KeyPair, X509Certificate> {
        if (cachedKeyPair != null && cachedClientCertificate != null) {
            return Pair(cachedKeyPair!!, cachedClientCertificate!!)
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val privB64 = prefs.getString("soft_tls_priv", null)
        val pubB64 = prefs.getString("soft_tls_pub", null)
        val certB64 = prefs.getString("soft_tls_cert_der", null)

        val kf = KeyFactory.getInstance("RSA")
        val cf = java.security.cert.CertificateFactory.getInstance("X.509")

        if (privB64 != null && pubB64 != null && certB64 != null) {
            try {
                val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privB64, Base64.DEFAULT)))
                val pubKey = kf.generatePublic(X509EncodedKeySpec(Base64.decode(pubB64, Base64.DEFAULT)))
                val kp = KeyPair(pubKey, privKey)
                val certBytes = Base64.decode(certB64, Base64.DEFAULT)
                val cert = cf.generateCertificate(java.io.ByteArrayInputStream(certBytes)) as X509Certificate

                cachedKeyPair = kp
                cachedClientCertificate = cert
                Log.d(TAG, "Loaded existing Remote v2 keypair and valid X.509 certificate")
                return Pair(kp, cert)
            } catch (e: Exception) {
                Log.w(TAG, "Failed loading cached Remote v2 certificate, regenerating...", e)
            }
        }

        // Generate new RSA 2048 keypair
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()

        // Generate compliant RFC 5280 self-signed X.509 certificate (CN=atvremote, 20-year validity)
        val cert = generateCompliantX509Certificate(kp)

        prefs.edit()
            .putString("soft_tls_priv", Base64.encodeToString(kp.private.encoded, Base64.DEFAULT))
            .putString("soft_tls_pub", Base64.encodeToString(kp.public.encoded, Base64.DEFAULT))
            .putString("soft_tls_cert_der", Base64.encodeToString(cert.encoded, Base64.DEFAULT))
            .apply()

        cachedKeyPair = kp
        cachedClientCertificate = cert
        Log.d(TAG, "Generated and saved new RFC 5280 X.509 certificate (CN=atvremote, ${cert.encoded.size} bytes)")
        return Pair(kp, cert)
    }

    private fun derWrap(tag: Int, content: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write(tag)
        val len = content.size
        if (len < 128) {
            out.write(len)
        } else if (len < 256) {
            out.write(0x81)
            out.write(len)
        } else {
            out.write(0x82)
            out.write((len ushr 8) and 0xFF)
            out.write(len and 0xFF)
        }
        out.write(content, 0, len)
        return out.toByteArray()
    }

    private fun derSequence(vararg parts: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        for (p in parts) {
            out.write(p, 0, p.size)
        }
        return derWrap(0x30, out.toByteArray())
    }

    private fun generateCompliantX509Certificate(kp: KeyPair): X509Certificate {
        // 1. version [0] EXPLICIT INTEGER 2 (v3)
        val v3Int = derWrap(0x02, byteArrayOf(0x02))
        val version = derWrap(0xA0, v3Int)

        // 2. serialNumber INTEGER (positive)
        val serial = derWrap(0x02, BigInteger.valueOf(System.currentTimeMillis()).abs().toByteArray())

        // 3. signature AlgorithmIdentifier: sha256WithRSAEncryption (1.2.840.113549.1.1.11) with NULL param
        val sigAlg = byteArrayOf(
            0x30.toByte(), 0x0d.toByte(),
            0x06.toByte(), 0x09.toByte(), 0x2a.toByte(), 0x86.toByte(), 0x48.toByte(), 0x86.toByte(),
            0xf7.toByte(), 0x0d.toByte(), 0x01.toByte(), 0x01.toByte(), 0x0b.toByte(),
            0x05.toByte(), 0x00.toByte()
        )

        // 4. issuer: SEQUENCE { SET { SEQUENCE { OID 2.5.4.3 (commonName), UTF8String "atvremote" } } }
        val cnOid = byteArrayOf(0x06, 0x03, 0x55, 0x04, 0x03)
        val cnVal = derWrap(0x0c, "atvremote".toByteArray(Charsets.UTF_8))
        val atvSeq = derWrap(0x30, cnOid + cnVal)
        val atvSet = derWrap(0x31, atvSeq)
        val issuer = derWrap(0x30, atvSet)

        // 5. validity: SEQUENCE { UTCTime "240101000000Z", UTCTime "440101000000Z" } (2024 to 2044)
        val notBefore = derWrap(0x17, "240101000000Z".toByteArray(Charsets.US_ASCII))
        val notAfter = derWrap(0x17, "440101000000Z".toByteArray(Charsets.US_ASCII))
        val validity = derWrap(0x30, notBefore + notAfter)

        // 6. subject: same as issuer (CN=atvremote)
        val subject = issuer

        // 7. subjectPublicKeyInfo: standard DER X.509 format
        val spki = kp.public.encoded

        // TBSCertificate SEQUENCE
        val tbs = derSequence(version, serial, sigAlg, issuer, validity, subject, spki)

        // Sign with private key using SHA256withRSA
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(kp.private)
        signer.update(tbs)
        val rawSig = signer.sign()

        // signatureValue BIT STRING (0x00 unused bits prefix)
        val sigBitString = ByteArray(rawSig.size + 1)
        sigBitString[0] = 0x00
        System.arraycopy(rawSig, 0, sigBitString, 1, rawSig.size)
        val signature = derWrap(0x03, sigBitString)

        // Final Certificate SEQUENCE
        val certDer = derSequence(tbs, sigAlg, signature)

        val cf = java.security.cert.CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(java.io.ByteArrayInputStream(certDer)) as X509Certificate
    }

    fun getSslContext(context: Context, onServerCertReceived: ((X509Certificate) -> Unit)? = null): SSLContext {
        val (kp, cert) = getOrCreateRemoteV2KeyAndCert(context)

        val km = object : javax.net.ssl.X509ExtendedKeyManager() {
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> = arrayOf("client")
            override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: java.net.Socket?): String = "client"
            override fun chooseEngineClientAlias(keyTypes: Array<out String>?, issuers: Array<out Principal>?, engine: javax.net.ssl.SSLEngine?): String = "client"
            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null
            override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: java.net.Socket?): String? = null
            override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: javax.net.ssl.SSLEngine?): String? = null
            override fun getCertificateChain(alias: String?): Array<X509Certificate> = arrayOf(cert)
            override fun getPrivateKey(alias: String?): PrivateKey = kp.private
        }

        val tm = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (!chain.isNullOrEmpty()) {
                    onServerCertReceived?.invoke(chain[0])
                }
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(arrayOf(km), arrayOf(tm), SecureRandom())
        return sslContext
    }

    // -------------------------------------------------------------
    // Polo Pairing Crypto
    // -------------------------------------------------------------

    fun computePoloSecret(
        clientCert: X509Certificate,
        serverCert: X509Certificate,
        pairingCode: String
    ): ByteArray {
        val clientPub = clientCert.publicKey as RSAPublicKey
        val serverPub = serverCert.publicKey as RSAPublicKey

        val clientModulus = clientPub.modulus
        val clientExponent = clientPub.publicExponent
        val serverModulus = serverPub.modulus
        val serverExponent = serverPub.publicExponent

        val md = MessageDigest.getInstance("SHA-256")
        md.update(toUnsignedBigEndian(clientModulus))
        md.update(toUnsignedBigEndian(clientExponent))
        md.update(toUnsignedBigEndian(serverModulus))
        md.update(toUnsignedBigEndian(serverExponent))

        val cleanCode = pairingCode.trim().replace(" ", "").removePrefix("0x").removePrefix("0X").uppercase()
        val codeHexRemaining = if (cleanCode.length > 2) cleanCode.substring(2) else cleanCode
        md.update(hexStringToByteArray(codeHexRemaining))

        val digest = md.digest()

        if (cleanCode.length >= 2) {
            try {
                val expectedFirstByte = cleanCode.substring(0, 2).toInt(16).toByte()
                if (digest[0] != expectedFirstByte) {
                    Log.w(TAG, "Pairing secret first byte check mismatch: got ${digest[0]} vs expected $expectedFirstByte")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing first 2 hex digits of code: $cleanCode")
            }
        }
        return digest
    }

    private fun toUnsignedBigEndian(bi: BigInteger): ByteArray {
        val bytes = bi.toByteArray()
        return if (bytes.isNotEmpty() && bytes[0] == 0.toByte()) {
            bytes.copyOfRange(1, bytes.size)
        } else {
            bytes
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    // -------------------------------------------------------------
    // ADB Keys & Authentication
    // -------------------------------------------------------------

    @Synchronized
    fun getOrCreateAdbKeyPair(context: Context): KeyPair {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val privStr = prefs.getString(PREF_ADB_PRIV_KEY, null)
        val pubStr = prefs.getString(PREF_ADB_PUB_KEY, null)

        val kf = KeyFactory.getInstance("RSA")
        if (privStr != null && pubStr != null) {
            try {
                val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privStr, Base64.DEFAULT)))
                val pubKey = kf.generatePublic(X509EncodedKeySpec(Base64.decode(pubStr, Base64.DEFAULT)))
                return KeyPair(pubKey, privKey)
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading ADB keypair, regenerating...", e)
            }
        }

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()

        prefs.edit()
            .putString(PREF_ADB_PRIV_KEY, Base64.encodeToString(kp.private.encoded, Base64.DEFAULT))
            .putString(PREF_ADB_PUB_KEY, Base64.encodeToString(kp.public.encoded, Base64.DEFAULT))
            .apply()

        return kp
    }

    fun signAdbToken(keyPair: KeyPair, token: ByteArray): ByteArray {
        val cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keyPair.private)
        return cipher.doFinal(token)
    }

    fun getAdbPublicKeyPayload(keyPair: KeyPair): String {
        val pub = keyPair.public as RSAPublicKey
        val modulus = pub.modulus
        val exponent = pub.publicExponent

        // Encode AOSP struct RSAPublicKey (524 bytes)
        val buffer = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(64) // len in 32-bit words (2048 / 32)

        val r32 = BigInteger.valueOf(2).pow(32)
        val n0 = modulus.remainder(r32)
        val n0inv = n0.modInverse(r32).negate().remainder(r32).add(r32).remainder(r32).toLong()
        buffer.putInt(n0inv.toInt())

        // Modulus words (64 words, little endian)
        var tempN = modulus
        for (i in 0 until 64) {
            val word = tempN.remainder(r32).toLong()
            buffer.putInt(word.toInt())
            tempN = tempN.shiftRight(32)
        }

        // rr = (2^2048)^2 mod n
        val r = BigInteger.valueOf(2).pow(2048)
        var rr = r.multiply(r).remainder(modulus)
        for (i in 0 until 64) {
            val word = rr.remainder(r32).toLong()
            buffer.putInt(word.toInt())
            rr = rr.shiftRight(32)
        }

        buffer.putInt(exponent.toInt())

        val b64 = Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
        return "$b64 onecontroller@tvdeck\u0000"
    }
}
