package org.zefrenchwan.streams;


import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.test.junit5.MiniClusterExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MiniClusterExtension.class)
public class  AggTest {
    
    @Test 
    public void testCount() throws Exception {
        final List<Event> inputs = Arrays.asList(
            new Event(1L, "blog", 10L, "a"), 
            new Event(2L, "blog", 10L, "b"), 
            new Event(3L, "blog", 5L, "c") 
        );


        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<Event> source = env.fromData(inputs);
        DataStream<Long> result = source
            .assignTimestampsAndWatermarks(
                WatermarkStrategy
                    .<Event>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                    .withTimestampAssigner((event, timestamp) -> event.getTime())
            )
            .map(x -> 1L)
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
        Long finalCount = reduction.stream().reduce(0L, (a,b) -> a+b);
        Assertions.assertEquals(3L, finalCount);
    }

    @Test 
    public void testCountBy() throws Exception {

        final List<Event> inputs = Arrays.asList(
            new Event(1L, "blog", 10L, "a"), 
            new Event(2L, "book", 10L, "b"), 
            new Event(3L, "blog", 5L, "c") 
        );

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<Event> source = env.fromData(inputs);
        DataStream<TreeMap<String,Long>> stream = source
            .assignTimestampsAndWatermarks(
                WatermarkStrategy
                    .<Event>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                    .withTimestampAssigner((event, timestamp) -> event.getTime())
            )
            .map(new MapFunction<Event,Tuple2<String, Long>>() {
                @Override
                public Tuple2<String, Long> map(Event a) throws Exception {
                    return new Tuple2<String, Long>(a.getSource(), 1L);
                }
            })
            .keyBy(new KeySelector<Tuple2<String,Long>,Long>() {
                public Long getKey(Tuple2<String,Long> value) throws Exception {
                    return value.f1;
                }
            })
            .window(TumblingEventTimeWindows.of(Duration.ofSeconds(1)))
            .aggregate(new GroupByAggregation());

        stream.print();
        List<TreeMap<String,Long>> localResult = stream.executeAndCollect(1000);
        Assertions.assertEquals(localResult.size(), 1);
        TreeMap<String,Long> expected = new TreeMap<String, Long>();
        expected.put("blog", 2L);
        expected.put("book", 1L);
        Assertions.assertEquals(expected, localResult.get(0));
        
    }
}
