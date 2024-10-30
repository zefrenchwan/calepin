package org.zefrenchwan.streams;


import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.WindowAssigner;
import org.apache.flink.test.junit5.MiniClusterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MiniClusterExtension.class)
public class  AggTest {
    
    private final List<Event> inputs = Arrays.asList(
        new Event(1L, "blog", 10L, "a"), 
        new Event(2L, "blog", 10L, "b"), 
        new Event(3L, "blog", 5L, "c") 
    );

    @Test 
    public void testCount() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<Event> source = env.fromData(inputs);
        DataStream<Long> result = source
            .assignTimestampsAndWatermarks(
                WatermarkStrategy
                    .<Event>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                    .withTimestampAssigner((event, timestamp) -> event.getTime())
            )
            .map(x -> 1L)
            .name("get person id")
            .keyBy(x -> x)
            .window(TumblingEventTimeWindows.of(Duration.ofSeconds(1)))
            .reduce(new ReduceFunction<Long>() {
                @Override
                public Long reduce(Long a, Long b) throws Exception {
                    return a + b;
                }
            });


        result.print();
        List<Long> reduction = result.executeAndCollect(1000);
        System.out.println(reduction);
    }
}
