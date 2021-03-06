/**
 * Copyright (C) 2010-2013 Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.rocketmq.client;

import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;

import com.alibaba.rocketmq.client.consumer.DefaultMQPullConsumer;
import com.alibaba.rocketmq.client.log.ClientLogger;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;


/**
 * @author shijia.wxr<vintage.wang@gmail.com>
 * @since 2013-11-13
 */
public class MQHelper {
    /**
     * 根据时间戳来重置一个订阅组的消费进度
     * 
     * @param messageModel
     *            广播消费还是集群消费
     * @param consumerGroup
     *            订阅组
     * @param topic
     *            topic
     * @param timestamp
     *            时间戳
     * @throws Exception
     */
    public static void resetOffsetByTimestamp(//
            final MessageModel messageModel,//
            final String consumerGroup, //
            final String topic, //
            final long timestamp) throws Exception {
        final Logger log = ClientLogger.getLog();

        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(consumerGroup);
        consumer.setMessageModel(messageModel);
        consumer.start();

        Set<MessageQueue> mqs = null;
        try {
            mqs = consumer.fetchSubscribeMessageQueues(topic);
            if (mqs != null && !mqs.isEmpty()) {
                TreeSet<MessageQueue> mqsNew = new TreeSet<MessageQueue>(mqs);
                for (MessageQueue mq : mqsNew) {
                    long offset = consumer.searchOffset(mq, timestamp);
                    if (offset >= 0) {
                        consumer.updateConsumeOffset(mq, offset);
                        log.info("resetOffsetByTimestamp updateConsumeOffset success, {} {} {}",
                            consumerGroup, offset, mq);
                    }
                }
            }
        }
        catch (Exception e) {
            log.warn("resetOffsetByTimestamp Exception", e);
            throw e;
        }
        finally {
            consumer.shutdown();
            if (mqs != null) {
                consumer.getDefaultMQPullConsumerImpl().getOffsetStore().persistAll(mqs);
            }
        }
    }
}
