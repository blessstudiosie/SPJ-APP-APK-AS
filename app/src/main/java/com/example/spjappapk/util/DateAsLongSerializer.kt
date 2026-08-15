package com.example.spjappapk.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DateAsLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DateAsLongSerializer", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val dateStr = decoder.decodeString()
        return DateTimeUtil.parseIso(dateStr)?.time ?: 0L
    }

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeString(DateTimeUtil.getIsoFormat().format(java.util.Date(value)))
    }
}

object OptionalDateAsLongSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("OptionalDateAsLongSerializer", PrimitiveKind.LONG)

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Long? {
        if (decoder.decodeNotNullMark()) {
            val dateStr = decoder.decodeString()
            return DateTimeUtil.parseIso(dateStr)?.time
        }
        return decoder.decodeNull()
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(DateTimeUtil.getIsoFormat().format(java.util.Date(value)))
        }
    }
}
