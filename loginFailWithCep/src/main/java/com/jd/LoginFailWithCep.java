package com.jd;

import com.jd.bean.LoginEvent;
import com.jd.bean.LoginFailWarning;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.net.URL;
import java.util.List;
import java.util.Map;

public class LoginFailWithCep {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.setParallelism(1);

        //1、读取事件流
        URL resource = LoginFailWithCep.class.getResource("/LoginLog.csv");
        DataStream<String> dataStream = env.readTextFile(resource.getPath());

        //2、解析事件流到POJO
        SingleOutputStreamOperator<LoginEvent> loginEventStream = dataStream.map(new MapFunction<String, LoginEvent>() {
            @Override
            public LoginEvent map(String s) throws Exception {

                LoginEvent loginEvent = new LoginEvent();
                String[] strArr = s.split(",");
                loginEvent.setUserId(Long.parseLong(strArr[0].trim()));
                loginEvent.setIp(strArr[1].trim());
                loginEvent.setLoginState(strArr[2].trim());
                loginEvent.setTimestamp(Long.parseLong(strArr[3].trim()));

                return loginEvent;
            }
        });

        //3、添加watermark+分流
        KeyedStream<LoginEvent, Tuple> loginEventByUserStream = loginEventStream.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<LoginEvent>(Time.seconds(5)) {
            @Override
            public long extractTimestamp(LoginEvent element) {
                return element.getTimestamp() * 1000L;
            }
        }).keyBy("userId");

        //4、定义patten
        Pattern<LoginEvent, ?> loginEventPattern = Pattern.<LoginEvent>begin("begin").where(new SimpleCondition<LoginEvent>() {
            @Override
            public boolean filter(LoginEvent loginEvent) throws Exception {
                return loginEvent.getLoginState().equals("fail");
            }
        }).next("next").where(new SimpleCondition<LoginEvent>() {
            @Override
            public boolean filter(LoginEvent loginEvent) throws Exception {
                return loginEvent.getLoginState().equals("fail");
            }
        }).within(Time.seconds(2));

        //5、事件流中应用所定义的patten
        SingleOutputStreamOperator<LoginFailWarning> loginFailDataStream = CEP.pattern(loginEventByUserStream, loginEventPattern).select(new PatternSelectFunction<LoginEvent, LoginFailWarning>() {
            @Override
            public LoginFailWarning select(Map<String, List<LoginEvent>> map) throws Exception {

                LoginEvent firstLoginEvent = map.get("begin").iterator().next();
                LoginEvent lastLoginEvent = map.get("next").iterator().next();
                LoginFailWarning loginFailWarning = new LoginFailWarning();
                loginFailWarning.setUserId(firstLoginEvent.getUserId());
                loginFailWarning.setFirstLoginTime(firstLoginEvent.getTimestamp());
                loginFailWarning.setLastLoginTime(lastLoginEvent.getTimestamp());
                loginFailWarning.setWarning("the user " + firstLoginEvent.getUserId() + " has failed to login more than twice in two seconds！！");

                return loginFailWarning;
            }
        });

        loginFailDataStream.print();

        env.execute("login fail detection with cep");
    }
}
