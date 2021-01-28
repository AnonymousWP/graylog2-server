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
package org.graylog2.shared.messageq.sqs;


import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Singleton
public class SqsMessageQueueAcknowledger extends AbstractIdleService implements MessageQueueAcknowledger {

    private static final Logger LOG = LoggerFactory.getLogger(SqsMessageQueueAcknowledger.class);
    private static final long BATCH_FLUSH_INTERVAL = Duration.ofSeconds(1).toNanos();

    private final CountDownLatch readyLatch = new CountDownLatch(1);
    private final String queueUrl;
    private final Semaphore deleteSemaphore;
    private final ScheduledExecutorService periodicalExecutorService;
    private volatile long lastBatchSendTime;

    private SqsAsyncClient sqsClient;
    private List<DeleteMessageBatchRequestEntry> currentBatch = new ArrayList<>(10);

    @Inject
    public SqsMessageQueueAcknowledger(BaseConfiguration config) {
        this.queueUrl = config.getSqsQueueUrl().toString();
        this.deleteSemaphore = new Semaphore(config.getSqsMaxInflightOutboundBatches());
        this.periodicalExecutorService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("sqs-delete-message-batch-flush-%d")
                        .build());
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting SQS message queue deletion service");

        final SqsAsyncClientBuilder clientBuilder = SqsAsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder());
        this.sqsClient = clientBuilder.build();

        // Service is ready for writing
        readyLatch.countDown();

        periodicalExecutorService.scheduleWithFixedDelay(this::flushBatch, BATCH_FLUSH_INTERVAL, BATCH_FLUSH_INTERVAL,
                TimeUnit.NANOSECONDS);
    }

    @Override
    protected void shutDown() throws Exception {
        periodicalExecutorService.shutdown();
        if (sqsClient != null) {
            sqsClient.close();
        }
    }

    @Override
    public void acknowledge(Object receiptHandle) {
        acknowledge(Collections.singletonList(receiptHandle));
    }

    @Override
    // TODO: periodically flush batches which have been left hanging
    public void acknowledge(List<Object> messageIds) {
        try {
            readyLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        final Iterator<Object> iterator = messageIds.iterator();

        List<List<DeleteMessageBatchRequestEntry>> batchesToSend = new ArrayList<>();

        synchronized(this) {
            while (iterator.hasNext()) {
                Object receiptHandle = iterator.next();
                if (!(receiptHandle instanceof String)) {
                    LOG.error("Couldn't delete message. Expected <" + receiptHandle + "> to be a String receipt handle");
                    continue;
                }
                final DeleteMessageBatchRequestEntry entry = DeleteMessageBatchRequestEntry.builder()
                        .receiptHandle((String) receiptHandle)
                        .id(String.valueOf(currentBatch.size() + 1))
                        .build();
                currentBatch.add(entry);

                if (currentBatch.size() == 10) {
                    batchesToSend.add(currentBatch);
                    currentBatch = new ArrayList<>(10);
                }
            }
        }

        batchesToSend.forEach(this::deleteBatch);
    }

    private void flushBatch() {
        if (lastBatchSendTime != 0 && BATCH_FLUSH_INTERVAL > System.nanoTime() - lastBatchSendTime) {
            return;
        }

        final List<DeleteMessageBatchRequestEntry> batchToSend;
        synchronized(this) {
            if (currentBatch.isEmpty()) {
                return;
            }
            batchToSend = currentBatch;
            currentBatch = new ArrayList<>(10);
        }
        deleteBatch(batchToSend);
    }

    private void deleteBatch(List<DeleteMessageBatchRequestEntry> entries) {
        lastBatchSendTime = System.nanoTime();
        try {
            deleteSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        sqsClient.deleteMessageBatch(request -> request.queueUrl(queueUrl)
                .entries(entries))
                .whenComplete((response, error) -> {
                    if (error != null) {
                        LOG.error("Couldn't delete message", error);
                    }
                    deleteSemaphore.release();
                });
    }
}
