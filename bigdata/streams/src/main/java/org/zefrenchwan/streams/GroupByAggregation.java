package org.zefrenchwan.streams;

import java.util.TreeMap;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;

public class GroupByAggregation implements AggregateFunction<Tuple2<String,Long>, MapAccumulator, TreeMap<String,Long>> {

    @Override
    public MapAccumulator createAccumulator() {
        return new MapAccumulator();
    }

    @Override
    public MapAccumulator add(Tuple2<String, Long> value, MapAccumulator accumulator) {
        accumulator.add(value);
        return accumulator;
    }

    @Override
    public TreeMap<String, Long> getResult(MapAccumulator accumulator) {
        return accumulator.getLocalValue();
    }

    @Override
    public MapAccumulator merge(MapAccumulator a, MapAccumulator b) {
        MapAccumulator acc = a.copy();
        acc.merge(b);
        return acc;
    }
}
