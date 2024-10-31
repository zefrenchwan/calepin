package org.zefrenchwan.streams;

import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.flink.api.common.accumulators.Accumulator;
import org.apache.flink.api.java.tuple.Tuple2;

public class MapAccumulator implements Accumulator<Tuple2<String, Long>, TreeMap<String,Long>> {

    private final TreeMap<String, Long> values = new TreeMap<>();

    @Override
    public void add(Tuple2<String, Long> value) {
        if (value != null && value.f0 != null) {
            final String key = value.f0;
            final long previous = this.values.getOrDefault(key, 0L);
            final long result = previous + 1;
            values.put(key,result);
        }
    }

    @Override
    public TreeMap<String, Long> getLocalValue() {
        return this.values;
    }

    @Override
    public void resetLocal() {
        this.values.clear();
    }

    @Override
    public void merge(Accumulator<Tuple2<String, Long>, TreeMap<String, Long>> other) {
        for (Entry<String, Long> entry: other.getLocalValue().entrySet()) {
            final String key = entry.getKey();
            final long value = entry.getValue();
            final long existing = this.values.getOrDefault(key, 0L);
            this.values.put(key, value + existing);
        }
    }

    @Override 
    public Accumulator<Tuple2<String, Long>, TreeMap<String,Long>> clone() {
        MapAccumulator result = new MapAccumulator();
        result.values.putAll(this.values);
        return result;
    }   

    public MapAccumulator copy() {
        MapAccumulator other = new MapAccumulator();
        other.values.putAll(this.values);
        return other;
    }
}
