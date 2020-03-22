package com.jd;

import com.jd.aggregateFuns.AdCountAgg;
import com.jd.aggregateFuns.FilterBlackListUser;
import com.jd.bean.AdClickEvent;
import com.jd.bean.CountByProvince;
import com.jd.windowFuns.AdCountResult;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.net.URL;

public class AdClickStatistics {


    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.setParallelism(1);

        //1、读取数据源
        URL resource = AdClickStatistics.class.getResource("/AdClickLog.csv");
        DataStream<String> adClickStream = env.readTextFile(resource.getPath());

        //2、解析消息到java对象
        SingleOutputStreamOperator<AdClickEvent> adClickEventStream = adClickStream.map(new MapFunction<String, AdClickEvent>() {
            private static final long serialVersionUID = -3852054033416950664L;

            @Override
            public AdClickEvent map(String s) throws Exception {
                AdClickEvent adClickEvent = new AdClickEvent();
                String[] strArr = s.split(",");
                adClickEvent.setUserId(Long.parseLong(strArr[0]));
                adClickEvent.setAdId(Long.parseLong(strArr[1]));
                adClickEvent.setProvince(strArr[2]);
                adClickEvent.setCity(strArr[3]);
                adClickEvent.setTimeStamp(Long.parseLong(strArr[4]));

                return adClickEvent;
            }
        });

        //3、添加watermark
        SingleOutputStreamOperator<AdClickEvent> adClickEventWatermarkStream = adClickEventStream.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<AdClickEvent>(Time.seconds(5)) {
            @Override
            public long extractTimestamp(AdClickEvent element) {
                return element.getTimeStamp() * 1000;
            }
        });

        //4、添加黑名单过滤功能(自定义processFunction，过滤掉的数据输出到侧输出流)
        SingleOutputStreamOperator<AdClickEvent> filterBlackListStream;
        filterBlackListStream = adClickEventWatermarkStream
                .keyBy(new KeySelector<AdClickEvent, Tuple2<Long, Long>>() {
                    @Override
                    public Tuple2<Long, Long> getKey(AdClickEvent adClickEvent) throws Exception {
                        return Tuple2.of(adClickEvent.getUserId(), adClickEvent.getAdId());
                    }
                })
                .process(new FilterBlackListUser(30L));
        //开窗聚合
        SingleOutputStreamOperator<CountByProvince> adCountStream = filterBlackListStream
                .keyBy("province")
                .timeWindow(Time.hours(1), Time.minutes(10))
                .aggregate(new AdCountAgg(), new AdCountResult());

        adCountStream.print();
        filterBlackListStream.getSideOutput(FilterBlackListUser.getBlackListWarningOutputTag()).print("blackList");
        env.execute("adClickEventStream");
    }
}

