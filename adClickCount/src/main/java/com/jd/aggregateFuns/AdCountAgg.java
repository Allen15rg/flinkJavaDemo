package com.jd.aggregateFuns;

import com.jd.bean.AdClickEvent;
import org.apache.flink.api.common.functions.AggregateFunction;

public class AdCountAgg implements AggregateFunction<AdClickEvent, Long, Long> {

    @Override
    public Long createAccumulator() {
        return 0L;
    }

    @Override
    public Long add(AdClickEvent adClickEvent, Long acc) {
        return acc + 1;
    }

    @Override
    public Long getResult(Long acc) {
        return acc;
    }

    @Override
    public Long merge(Long acc1, Long acc2) {
        return acc1 + acc2;
    }
}
