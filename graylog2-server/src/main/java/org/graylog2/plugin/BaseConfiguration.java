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
package org.graylog2.plugin;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.github.joschi.jadconfig.validators.URIAbsoluteValidator;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.graylog2.configuration.PathConfiguration;
import org.graylog2.shared.messageq.MessageJournalMode;
import org.graylog2.shared.messageq.MessageJournalModeConverter;
import org.graylog2.shared.messageq.pulsar.PulsarServiceUrlValidator;
import org.graylog2.utilities.ProxyHostsPattern;
import org.graylog2.utilities.ProxyHostsPatternConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@SuppressWarnings("FieldMayBeFinal")
public abstract class BaseConfiguration extends PathConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(BaseConfiguration.class);

    private static final String SETTING_SQS_QUEUE_URL = "sqs_queue_url";

    @Parameter(value = "shutdown_timeout", validator = PositiveIntegerValidator.class)
    protected int shutdownTimeout = 30000;

    @Parameter(value = "processbuffer_processors", required = true, validator = PositiveIntegerValidator.class)
    private int processBufferProcessors = 5;

    @Parameter(value = "processor_wait_strategy", required = true)
    private String processorWaitStrategy = "blocking";

    @Parameter(value = "ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int ringSize = 65536;

    @Parameter(value = "inputbuffer_ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int inputBufferRingSize = 65536;

    @Parameter(value = "inputbuffer_wait_strategy", required = true)
    private String inputBufferWaitStrategy = "blocking";

    @Parameter(value = "async_eventbus_processors")
    private int asyncEventbusProcessors = 2;

    @Parameter(value = "udp_recvbuffer_sizes", required = true, validator = PositiveIntegerValidator.class)
    private int udpRecvBufferSizes = 1048576;

    @Parameter("message_journal_enabled")
    private boolean messageJournalEnabled = true;

    @Parameter(value = "message_journal_mode", converter = MessageJournalModeConverter.class)
    private MessageJournalMode messageJournalMode = MessageJournalMode.DISK;

    @Parameter(value = "pulsar_service_url", validators = PulsarServiceUrlValidator.class)
    private String pulsarServiceUrl = "pulsar://localhost:6650";

    // TODO: move sqs settings into separate config class
    @Parameter(value = SETTING_SQS_QUEUE_URL, validators = URIAbsoluteValidator.class)
    private URI sqsQueueUrl;

    @Parameter(value = "sqs_max_inflight_outbound_batches", validators = PositiveIntegerValidator.class)
    private int sqsMaxInflightOutboundBatches = 5;

    @Parameter(value = "sqs_max_inflight_receive_batches", validators = PositiveIntegerValidator.class)
    private int sqsMaxInflightReceiveBatches = 10;

    @Parameter(value = "sqs_max_done_receive_batches", validators = PositiveIntegerValidator.class)
    private int sqsMaxDoneReceiveBatches = 10;

    @Parameter("inputbuffer_processors")
    private int inputbufferProcessors = 2;

    @Parameter("message_recordings_enable")
    private boolean messageRecordingsEnable = false;

    @Parameter("disable_native_system_stats_collector")
    private boolean disableNativeSystemStatsCollector = false;

    @Parameter(value = "http_proxy_uri")
    private URI httpProxyUri;

    @Parameter(value = "http_non_proxy_hosts", converter = ProxyHostsPatternConverter.class)
    private ProxyHostsPattern httpNonProxyHostsPattern;

    @Parameter(value = "http_connect_timeout", validator = PositiveDurationValidator.class)
    private Duration httpConnectTimeout = Duration.seconds(5L);

    @Parameter(value = "http_write_timeout", validator = PositiveDurationValidator.class)
    private Duration httpWriteTimeout = Duration.seconds(10L);

    @Parameter(value = "http_read_timeout", validator = PositiveDurationValidator.class)
    private Duration httpReadTimeout = Duration.seconds(10L);

    @Parameter(value = "installation_source", validator = StringNotBlankValidator.class)
    private String installationSource = "unknown";

    @Parameter(value = "proxied_requests_thread_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int proxiedRequestsThreadPoolSize = 32;

    public int getProcessBufferProcessors() {
        return processBufferProcessors;
    }

    private WaitStrategy getWaitStrategy(String waitStrategyName, String configOptionName) {
        switch (waitStrategyName) {
            case "sleeping":
                return new SleepingWaitStrategy();
            case "yielding":
                return new YieldingWaitStrategy();
            case "blocking":
                return new BlockingWaitStrategy();
            case "busy_spinning":
                return new BusySpinWaitStrategy();
            default:
                LOG.warn("Invalid setting for [{}]:"
                        + " Falling back to default: BlockingWaitStrategy.", configOptionName);
                return new BlockingWaitStrategy();
        }
    }

    public WaitStrategy getProcessorWaitStrategy() {
        return getWaitStrategy(processorWaitStrategy, "processbuffer_wait_strategy");
    }

    public int getRingSize() {
        return ringSize;
    }

    public int getInputBufferRingSize() {
        return inputBufferRingSize;
    }

    public WaitStrategy getInputBufferWaitStrategy() {
        return getWaitStrategy(inputBufferWaitStrategy, "inputbuffer_wait_strategy");
    }

    public int getAsyncEventbusProcessors() {
        return asyncEventbusProcessors;
    }

    public abstract String getNodeIdFile();

    public boolean isMessageJournalEnabled() {
        return messageJournalEnabled;
    }

    public MessageJournalMode getMessageJournalMode() {
        return messageJournalMode;
    }

    public String getPulsarServiceUrl() {
        return pulsarServiceUrl;
    }

    public URI getSqsQueueUrl() {
        return sqsQueueUrl;
    }

    public int getSqsMaxInflightOutboundBatches() {
        return sqsMaxInflightOutboundBatches;
    }

    public int getSqsMaxInflightReceiveBatches() {
        return sqsMaxInflightReceiveBatches;
    }

    public int getSqsMaxDoneReceiveBatches() {
        return sqsMaxDoneReceiveBatches;
    }

    public void setMessageJournalEnabled(boolean messageJournalEnabled) {
        this.messageJournalEnabled = messageJournalEnabled;
    }

    public int getInputbufferProcessors() {
        return inputbufferProcessors;
    }

    public int getShutdownTimeout() {
        return shutdownTimeout;
    }

    public int getUdpRecvBufferSizes() {
        return udpRecvBufferSizes;
    }

    public boolean isMessageRecordingsEnabled() {
        return messageRecordingsEnable;
    }

    public boolean isDisableNativeSystemStatsCollector() {
        return disableNativeSystemStatsCollector;
    }

    public URI getHttpProxyUri() {
        return httpProxyUri;
    }

    public ProxyHostsPattern getHttpNonProxyHostsPattern() {
        return httpNonProxyHostsPattern;
    }

    public Duration getHttpConnectTimeout() {
        return httpConnectTimeout;
    }

    public Duration getHttpWriteTimeout() {
        return httpWriteTimeout;
    }

    public Duration getHttpReadTimeout() {
        return httpReadTimeout;
    }

    public String getInstallationSource() {
        return installationSource;
    }

    @ValidatorMethod
    public void validateSqsConfiguration() throws ValidationException {
        if (!isMessageJournalEnabled()) {
            return;
        }
        if (getMessageJournalMode() == MessageJournalMode.SQS && getSqsQueueUrl() == null) {
            throw new ValidationException(
                    "Running with journal mode \"" + MessageJournalMode.SQS + "\" but configuration parameter \"" +
                            SETTING_SQS_QUEUE_URL + "\" is missing. Please fix your configuration.");
        }
    }
}
