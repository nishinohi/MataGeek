package com.example.matageek.customexception

class MessagePacketSizeException(messageName: String, size: Int) : Exception(
    "$messageName packet must larger than $size"
) {
}