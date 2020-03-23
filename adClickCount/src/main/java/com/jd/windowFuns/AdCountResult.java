package com.jd.windowFuns;

import com.jd.bean.CountByProvince;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.api.windowing.windows.Window;
import org.apache.flink.util.Collector;

import java.sql.Timestamp;

public class AdCountResult implements WindowFunction<Long, CountByProvince, Tuple, TimeWindow> {

    @Override
    public void apply(Tuple tuple, TimeWindow window, Iterable<Long> input, Collector<CountByProvince> out) throws Exception {
        CountByProvince countByProvince = new CountByProvince();

        countByProvince.setWindowEnd(new Timestamp(window.getEnd()).toString());
        countByProvince.setProvince(tuple.getField(0));
        countByProvince.setCount(input.iterator().next());

        out.collect(countByProvince);
    }


}
