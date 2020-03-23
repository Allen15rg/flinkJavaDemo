package com.jd.aggregateFuns;

import com.jd.bean.AdClickEvent;
import com.jd.bean.BlackListWarning;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public class FilterBlackListUser extends KeyedProcessFunction<Tuple2<Long, Long>, AdClickEvent, AdClickEvent> {

    //OutputTag标示一个侧输出流
    private static final OutputTag<BlackListWarning> blackListWarningOutputTag = new OutputTag<>("blackList", TypeInformation.of(new TypeHint<BlackListWarning>() {
    }) );
    private Long maxCount;

    //定义状态，保存当前用户对当前广告的点击量
    private ValueState<Long> countState;
    //保存是否发送过黑名单的状态
    private ValueState<Boolean> isSentState;
    //保存定时器触发的时间戳
    private ValueState<Long> resetTimer;

    public FilterBlackListUser(Long maxCount) {
        this.maxCount = maxCount;
    }

    public static OutputTag<BlackListWarning> getBlackListWarningOutputTag() {
        return blackListWarningOutputTag;
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<AdClickEvent> out) throws Exception {
        //定时器触发时，清空状态
        if (timestamp >= resetTimer.value()) {
            countState.clear();
            isSentState.clear();
            resetTimer.clear();
        }

    }

    @Override
    public void processElement(AdClickEvent value, Context ctx, Collector<AdClickEvent> out) throws Exception {

        //取出当前状态
        Long curCount = countState.value();

        if (curCount == null){
            countState.update(0L);
            return;
        }
        //判断是否是第一次处理，如果是，则注册定时器,每天00：00触发
        if (curCount == 0 ) {
            long ts = (ctx.timerService().currentProcessingTime() / (1000 * 60 * 60 * 24) + 1) * (1000 * 60 * 60 * 24);
            resetTimer.update(ts);
            isSentState.update(false);
            //注册定时器
            ctx.timerService().registerProcessingTimeTimer(ts);
        }

        //判断点击计数是否达到上限，如果是，则加入到黑名单
        if (curCount >= this.maxCount) {
            //判断是否发送过黑名单，只发送一次
            if (!isSentState.value()) {
                isSentState.update(true);
                //输出到侧输出流
                BlackListWarning blackListWarning = new BlackListWarning();
                blackListWarning.setUserId(value.getUserId());
                blackListWarning.setAdId(value.getAdId());
                blackListWarning.setMsg("The user " + value.getUserId() + " have clicked the " + value.getAdId() + " more than " + this.maxCount + " today!");

                ctx.output(blackListWarningOutputTag, blackListWarning);
            }
            //如果超过最大点击次数限制，则该条点击消息不加入到主流输出
            return;
        }

        //如果没有超过最大点击次数限制，则输出到主流中,且计数加1
        countState.update(curCount + 1);
        out.collect(value);
    }

    @Override
    public void open(Configuration parameters) throws Exception {

        ValueStateDescriptor<Long> countDescriptor = new ValueStateDescriptor<>("countState", TypeInformation.of(new TypeHint<Long>() {
        }));

        ValueStateDescriptor<Boolean> isSentDescriptor = new ValueStateDescriptor<>("isSentState", TypeInformation.of(new TypeHint<Boolean>() {
        }));

        ValueStateDescriptor<Long> resetTimerDescriptor = new ValueStateDescriptor<>("resetTimerState", TypeInformation.of(new TypeHint<Long>() {
        }));

        countState = getRuntimeContext().getState(countDescriptor);
        isSentState = getRuntimeContext().getState(isSentDescriptor);
        resetTimer = getRuntimeContext().getState(resetTimerDescriptor);
    }
}