package com.travelsky.airline.trp.ops.telassa.probe.util;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;

/**
 * DOC
 *
 * @author zengfan
 */
public class PrometheusUtils {
    // IBEClient 接口访问时间
    public static final Summary IBE_CLIENT_LATENCY = Summary.build()
            .name("ibe_client_latency_seconds").help("IBEClient latency in seconds.").labelNames("queryName").register();

    // IBEClient 接口成功/失败次数
    public static final Counter IBE_CLIENT_COUNTER = Counter.build()
            .name("ibe_client_status_total").help("Total IBE Client Status.").labelNames("queryName", "status").register();

    // OJ 线程池，创建的线程数
    public static final Gauge OJ_THREADS_CREATED = Gauge.build()
            .name("oj_threads_crated_total").help("OJ Threads Created Count.").labelNames("type", "code").register();

    // OJ 线程池，活跃线程数
    public static final Gauge OJ_THREADS_ACTIVE = Gauge.build()
            .name("oj_threads_active_total").help("OJ Threads Active Count.").labelNames("type", "code").register();

//    // OJ 线程池，最大活跃线程数
//    public static final Gauge OJ_THREADS_MAX_ACTIVE = Gauge.build()
//            .name("oj_threads_max_active_total").help("OJ Threads Max Active Count.").labelNames("type", "code").register();

    // OJ 线程池，等待线程数
    public static final Gauge OJ_THREADS_PENDING = Gauge.build()
            .name("oj_threads_pending_total").help("OJ Threads Pending Count.").labelNames("type", "code").register();

    // OJ 线程池，最大等待线程数
    public static final Gauge OJ_THREADS_MAX_PENDING = Gauge.build()
            .name("oj_threads_max_pending_total").help("OJ Threads Max Pending Count.").labelNames("type", "code").register();

    // OJ 线程池，丢弃的请求数
    public static final Gauge OJ_THREADS_DROPED = Gauge.build()
            .name("oj_threads_drop_total").help("OJ Threads Dropped Count.").labelNames("type", "code").register();


    // 开始一个 IBEClient Latency
    // 上层需要有一个变量 query$Name 标记当前 query 的参数
    public static final String startIBEClientTimer = "" +
            "    com.travelsky.airline.trp.ops.telassa.probe.shaded.io.prometheus.client.Summary.Timer prometheus$timer = null;\n" +
            "    String[] prometheus$labels = {query$Name}; \n" +
            "    prometheus$timer = ((com.travelsky.airline.trp.ops.telassa.probe.shaded.io.prometheus.client.Summary.Child)(com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusUtils.IBE_CLIENT_LATENCY.labels(prometheus$labels))).startTimer(); \n\n";

    // 执行一个 IBEClient 失败，前面需有一个变量 query$Name 标记当前 query
    public static final String successIBEClient = "" +
            "    String[] prometheus$labels = {query$Name, \"success\"};\n" +
            "    ((com.travelsky.airline.trp.ops.telassa.probe.shaded.io.prometheus.client.Counter.Child)(com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusUtils.IBE_CLIENT_COUNTER.labels(prometheus$labels))).inc();\n\n";

    // 执行一个 IBEClient 失败，前面需有一个变量 query$Name 标记当前 query
    public static final String exceptionIBEClient = "" +
            "    String[] prometheus$labels = {query$Name, \"error\"};\n" +
            "    ((com.travelsky.airline.trp.ops.telassa.probe.shaded.io.prometheus.client.Counter.Child)(com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusUtils.IBE_CLIENT_COUNTER.labels(prometheus$labels))).inc();\n\n";

    // 结束一个 timer
    public static final String completeTimer = "" +
            "    if (prometheus$timer != null) {\n" +
            "        prometheus$timer.observeDuration();\n" +
            "    }\n";


    // 记录当前 OJ 线程池的 gauge 状态, 前面需初始化完成 com.openjaw.serviceProvider.SPMSupplierBaseStats prometheus$stats
    // 也需要有 prometheus$SPMNodeType 和 prometheus$SPMNodeCode
    public static final String gaugeOJSupplierThread = "" +
            "    String[] prometheus$labels = {prometheus$SPMNodeType, prometheus$SPMNodeCode};\n" +
            "    double prometheus$threadsCreated =((com.openjaw.statistics.StatisticsEntry)prometheus$stats.getStatisticsComponent().getEntriesMap().get(\"CreatedThreads\")).getValue();\n" +
            "    ((com.travelsky.airline.trp.ops.telassa.probe.shaded.io.prometheus.client.Gauge.Child)(com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusUtils.OJ_THREADS_CREATED.labels(prometheus$labels))).set(prometheus$threadsCreated);\n" +

            "    double prometheus$threadsActive = ((com.openjaw.statistics.StatisticsEntry)prometheus$stats.getStatisticsComponent().getEntriesMap().get(\"ActiveThreads\")).getValue();\n" +
            "    ((com.travelsky.airline.trp.ops.telassa.probe.shaded.io.prometheus.client.Gauge.Child)(com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusUtils.OJ_THREADS_ACTIVE.labels(prometheus$labels))).set(prometheus$threadsActive);\n" +

//            "    double prometheus$threadsMaxActive = ((com.openjaw.statistics.StatisticsEntry)prometheus$stats.getStatisticsComponent().getEntriesMap().get(\"MaxActiveThreads\")).getValue();\n" +
//            "    ((com.travelsky.airline.trp.ops.telassa.probe.shaded.io.prometheus.client.Gauge.Child)(com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusUtils.OJ_THREADS_MAX_ACTIVE.labels(prometheus$labels))).set(prometheus$threadsMaxActive);\n" +

            "    double prometheus$threadsPending = ((com.openjaw.statistics.StatisticsEntry)prometheus$stats.getStatisticsComponent().getEntriesMap().get(\"PendingRequests\")).getValue();\n" +
            "    ((com.travelsky.airline.trp.ops.telassa.probe.shaded.io.prometheus.client.Gauge.Child)(com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusUtils.OJ_THREADS_PENDING.labels(prometheus$labels))).set(prometheus$threadsPending);\n" +

            "    double prometheus$threadsDropped = ((com.openjaw.statistics.StatisticsEntry)prometheus$stats.getStatisticsComponent().getEntriesMap().get(\"DroppedRequests\")).getValue();\n" +
            "    ((com.travelsky.airline.trp.ops.telassa.probe.shaded.io.prometheus.client.Gauge.Child)(com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusUtils.OJ_THREADS_DROPED.labels(prometheus$labels))).set(prometheus$threadsDropped);\n";
//            "    System.out.println(prometheus$SPMNodeType + \" \" + prometheus$SPMNodeCode + \" Created: \" + prometheus$threadsCreated + \" Active: \" + prometheus$threadsActive + \" Pending: \" + prometheus$threadsPending  + \" Dropped: \" + prometheus$threadsDropped);\n";



    // 记录当前 OJ 线程池的 gauge 状态, 前面需初始化完成 com.openjaw.serviceProvider.SPMSupplierBaseStats prometheus$stats
    // 也需要有 prometheus$SPMNodeType 和 prometheus$SPMNodeCode
    public static final String gaugeOJClientThread = "" +
            "    String[] prometheus$labels = {prometheus$SPMNodeType, prometheus$SPMNodeCode};\n" +
            "    double prometheus$threadsActive = ((com.openjaw.statistics.StatisticsEntry)prometheus$stats.getStatisticsComponent().getEntriesMap().get(\"ActiveThreads\")).getValue();\n" +
            "    ((com.travelsky.airline.trp.ops.telassa.probe.shaded.io.prometheus.client.Gauge.Child)(com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusUtils.OJ_THREADS_ACTIVE.labels(prometheus$labels))).set(prometheus$threadsActive);\n";
//            "    System.out.println(prometheus$SPMNodeType + \" \" + prometheus$SPMNodeCode + \" Active: \" + prometheus$threadsActive);\n";

}
