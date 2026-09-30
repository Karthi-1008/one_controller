package dev.tvdeck.onecontroller.core.remotev2

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

object ProtobufHelper {

    fun writeVarint(out: OutputStream, value: Long) {
        var v = value
        while (true) {
            if ((v and 0x7FL.inv()) == 0L) {
                out.write(v.toInt())
                return
            } else {
                out.write(((v and 0x7F) or 0x80).toInt())
                v = v ushr 7
            }
        }
    }

    fun readVarint(input: InputStream): Long {
        var result = 0L
        var shift = 0
        while (shift < 64) {
            val b = input.read()
            if (b == -1) throw java.io.EOFException("Premature EOF while reading varint")
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        throw IllegalArgumentException("Varint too long")
    }

    fun makeTag(fieldNumber: Int, wireType: Int): Long {
        return ((fieldNumber.toLong() shl 3) or wireType.toLong())
    }

    fun writeIntField(out: ByteArrayOutputStream, fieldNumber: Int, value: Long) {
        writeVarint(out, makeTag(fieldNumber, 0))
        writeVarint(out, value)
    }

    fun writeStringField(out: ByteArrayOutputStream, fieldNumber: Int, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        writeBytesField(out, fieldNumber, bytes)
    }

    fun writeBytesField(out: ByteArrayOutputStream, fieldNumber: Int, bytes: ByteArray) {
        writeVarint(out, makeTag(fieldNumber, 2))
        writeVarint(out, bytes.size.toLong())
        out.write(bytes)
    }

    fun writeMessageField(out: ByteArrayOutputStream, fieldNumber: Int, messageBytes: ByteArray) {
        writeBytesField(out, fieldNumber, messageBytes)
    }
}
