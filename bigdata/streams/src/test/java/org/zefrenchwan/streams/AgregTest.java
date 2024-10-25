package org.zefrenchwan.streams;

import java.time.Duration;
import java.util.Arrays;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import  org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.test.junit5.MiniClusterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MiniClusterExtension.class)
public class AgregTest {

    @Test
    public void testAgregProcessingTime() throws Exception {
        var events = Arrays.asList(
            new Event(1L, "blog", 10L, "About wine..."),
            new Event(2L, "book", 10L, "About good wine..."),
            new Event(3L, "blog", 17L, "About cheese...")
        );

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setAutoWatermarkInterval(Duration.ofMillis(100).toMillis());
        DataStreamSource<Event> stream = env.fromData(events);
        stream
            .assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forBoundedOutOfOrderness(Duration.ofMillis(100)).withTimestampAssigner((event, timestamp) -> event.getTime()))
            .keyBy(Event::getPersonId)
            .window(TumblingProcessingTimeWindows.of(Duration.ofSeconds(1)))
            .aggregate(...)

    }
}