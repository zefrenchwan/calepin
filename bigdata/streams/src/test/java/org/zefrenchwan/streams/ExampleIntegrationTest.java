package org.zefrenchwan.streams;


import java.util.Arrays;
import java.util.List;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.junit5.MiniClusterExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MiniClusterExtension.class)
public class ExampleIntegrationTest {

    @Test
    public void testBasicFilter() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<Integer> source = env.fromData(Arrays.asList(1,2,3));
        List<Integer> values = source.filter(x -> x != 2).executeAndCollect(31);
        Assertions.assertEquals(Arrays.asList(1,3), values);
    }

    @Test
    public void testBasicMap() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<Integer> source = env.fromData(Arrays.asList(1,2,3));
        List<Integer> values = source.map(x -> x * 2).executeAndCollect(31);
        Assertions.assertEquals(Arrays.asList(2,4,6), values);
    }

}