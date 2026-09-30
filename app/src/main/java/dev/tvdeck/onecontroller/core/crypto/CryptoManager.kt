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

        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (ks.containsAlias(KEYSTORE_ALIAS)) {
                val privKey = ks.getKey(KEYSTORE_ALIAS, null) as? PrivateKey
                val cert = ks.getCertificate(KEYSTORE_ALIAS) as? X509Certificate
                if (privKey != null && cert != null) {
                    val pubKey = cert.publicKey
                    val kp = KeyPair(pubKey, privKey)
                    cachedKeyPair = kp
                    cachedClientCertificate = cert
                    return Pair(kp, cert)
                }
            }

            // Generate new RSA 2048 keypair with self-signed certificate in AndroidKeyStore
            val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(2048)
                .setCertificateSubject(X500Principal("CN=OneController, O=TVDeck, C=US"))
                .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
                .setCertificateNotBefore(Date(System.currentTimeMillis() - 86400000L))
                .setCertificateNotAfter(Date(System.currentTimeMillis() + 20L * 365 * 86400000L))
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .build()

            kpg.initialize(spec)
            val kp = kpg.generateKeyPair()
            val cert = ks.getCertificate(KEYSTORE_ALIAS) as X509Certificate

            cachedKeyPair = kp
            cachedClientCertificate = cert
            return Pair(kp, cert)
        } catch (e: Exception) {
            Log.e(TAG, "AndroidKeyStore init failed, falling back to software key", e)
            return getOrCreateSoftwareKeyAndCert(context)
        }
    }

    private fun getOrCreateSoftwareKeyAndCert(context: Context): Pair<KeyPair, X509Certificate> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val privB64 = prefs.getString("soft_tls_priv", null)
        val pubB64 = prefs.getString("soft_tls_pub", null)

        val kf = KeyFactory.getInstance("RSA")
        if (privB64 != null && pubB64 != null) {
            val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privB64, Base64.DEFAULT)))
            val pubKey = kf.generatePublic(X509EncodedKeySpec(Base64.decode(pubB64, Base64.DEFAULT)))
            val kp = KeyPair(pubKey, privKey)
            val cert = generateSelfSignedCertificate(kp)
            cachedKeyPair = kp
            cachedClientCertificate = cert
            return Pair(kp, cert)
        }

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()
        prefs.edit()
            .putString("soft_tls_priv", Base64.encodeToString(kp.private.encoded, Base64.DEFAULT))
            .putString("soft_tls_pub", Base64.encodeToString(kp.public.encoded, Base64.DEFAULT))
            .apply()

        val cert = generateSelfSignedCertificate(kp)
        cachedKeyPair = kp
        cachedClientCertificate = cert
        return Pair(kp, cert)
    }

    private fun generateSelfSignedCertificate(keyPair: KeyPair): X509Certificate {
        // Minimal valid dummy X509 cert representation for fallback
        val ks = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        val cert = ks.getCertificate("dummy") as? X509Certificate
        if (cert != null) return cert

        // Return a mock wrapper or generated X509
        return SimpleX509Certificate(keyPair.public as RSAPublicKey)
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

        val codeHexRemaining = pairingCode.substring(2)
        md.update(hexStringToByteArray(codeHexRemaining))

        val digest = md.digest()

        val expectedFirstByte = pairingCode.substring(0, 2).toInt(16).toByte()
        if (digest[0] != expectedFirstByte) {
            Log.w(TAG, "Pairing secret first byte check mismatch: got ${digest[0]} vs expected $expectedFirstByte")
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

    // Minimal self-signed certificate wrapper fallback
    private class SimpleX509Certificate(private val rsaPublicKey: RSAPublicKey) : X509Certificate() {
        override fun getPublicKey(): PublicKey = rsaPublicKey
        override fun toString(): String = "SimpleX509Certificate(OneController)"
        override fun hasUnsupportedCriticalExtension(): Boolean = false
        override fun getCriticalExtensionOIDs(): MutableSet<String>? = null
        override fun getNonCriticalExtensionOIDs(): MutableSet<String>? = null
        override fun getExtensionValue(oid: String?): ByteArray? = null
        override fun checkValidity() {}
        override fun checkValidity(date: Date?) {}
        override fun getVersion(): Int = 3
        override fun getSerialNumber(): BigInteger = BigInteger.ONE
        override fun getIssuerDN(): Principal = X500Principal("CN=OneController")
        override fun getSubjectDN(): Principal = X500Principal("CN=OneController")
        override fun getNotBefore(): Date = Date(0)
        override fun getNotAfter(): Date = Date(System.currentTimeMillis() + 315360000000L)
        override fun getTBSCertificate(): ByteArray = ByteArray(0)
        override fun getSignature(): ByteArray = ByteArray(0)
        override fun getSigAlgName(): String = "SHA256withRSA"
        override fun getSigAlgOID(): String = "1.2.840.113549.1.1.11"
        override fun getSigAlgParams(): ByteArray? = null
        override fun getIssuerUniqueID(): BooleanArray? = null
        override fun getSubjectUniqueID(): BooleanArray? = null
        override fun getKeyUsage(): BooleanArray? = null
        override fun getBasicConstraints(): Int = -1
        override fun getEncoded(): ByteArray = ByteArray(0)
        override fun verify(key: PublicKey?) {}
        override fun verify(key: PublicKey?, sigProvider: String?) {}
    }
}
