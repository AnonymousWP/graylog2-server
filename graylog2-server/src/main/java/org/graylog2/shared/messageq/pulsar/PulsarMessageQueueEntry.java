/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.messageq.pulsar;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.graylog2.shared.messageq.MessageQueue;

import javax.annotation.Nullable;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class PulsarMessageQueueEntry implements MessageQueue.Entry {
    private final byte[] id;
    private final byte[] key;
    private final byte[] value;
    private final long timestamp;
    private final MessageId commitId;

    PulsarMessageQueueEntry(Message<byte[]> message) {
        this.commitId = requireNonNull(message.getMessageId(), "messageId cannot be null");
        this.id = commitId.toByteArray();
        this.key = message.getKey().getBytes(UTF_8);
        this.value = requireNonNull(message.getData(), "value cannot be null");
        this.timestamp = message.getEventTime();
    }

    public static PulsarMessageQueueEntry fromMessage(Message<byte[]> message) {
        return new PulsarMessageQueueEntry(message);
    }

    @Nullable
    @Override
    public MessageId commitId() {
        return commitId;
    }

    @Override
    public byte[] id() {
        return id;
    }

    @Nullable
    @Override
    public byte[] key() {
        return key;
    }

    @Override
    public byte[] value() {
        return value;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id.len", id.length)
                .add("key.len", key != null ? key.length : key)
                .add("value.len", value.length)
                .add("timestamp", timestamp)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PulsarMessageQueueEntry that = (PulsarMessageQueueEntry) o;
        return timestamp == that.timestamp &&
                Arrays.equals(id, that.id) &&
                Arrays.equals(key, that.key) &&
                Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Arrays.hashCode(id), Arrays.hashCode(key), Arrays.hashCode(value), timestamp);
    }
}
