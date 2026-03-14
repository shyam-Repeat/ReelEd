package com.reeled.quizoverlay.util

import java.security.MessageDigest

object PinHasher {
    fun hash(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun verify(pin: String, hash: String): Boolean {
        return hash(pin) == hash
    }
}
