package com.travelsky.airline.trp.ops.telassa.probe.util;

/**
 * 维护 telassaId 相关静态代码
 *
 * @author zengfan
 */
public class TelassaUtils {
    // 开始远程调用 Service 之前，传递 TelassaId
    public static final String remoteCallServiceClient = "" +
            "    String telassa$Id = (String)org.apache.log4j.MDC.get(\"telassa-id\");\n" +
            "    if (telassa$Id != null) {\n" +
            "        String telassa$header = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.encodeHeader(telassa$Id);\n" +
            "        $0.setHTTPHeader(\"telassa-id\", telassa$header);\n" +
            "    }\n" +

            // channel、rootId、rootType、rootName 不会有中文，不需要 encode 编码
            "    String telassa$channel = (String)org.apache.log4j.MDC.get(\"telassa-channel\");\n" +
            "    if (telassa$channel != null) {\n" +
            "       $0.setHTTPHeader(\"telassa-channel\", telassa$channel);\n" +
            "    }\n" +

            "    String telassa$rootId = (String)org.apache.log4j.MDC.get(\"telassa-rootId\");\n" +
            "    if (telassa$rootId != null) {\n" +
            "        $0.setHTTPHeader(\"telassa-rootId\", telassa$rootId);\n" +
            "    }\n" +

            "    String telassa$rootType = (String)org.apache.log4j.MDC.get(\"telassa-rootType\");\n" +
            "    if (telassa$rootType != null) {\n" +
            "        $0.setHTTPHeader(\"telassa-rootType\", telassa$rootType);\n" +
            "    }\n" +

            "    String telassa$rootName = (String)org.apache.log4j.MDC.get(\"telassa-rootName\");\n" +
            "    if (telassa$rootName != null) {\n" +
            "      $0.setHTTPHeader(\"telassa-rootName\", telassa$rootName);\n" +
            "    }\n";


    // 开始远程调用 UserServiceService 之前，传递 TelassaId
    public static final String remoteCallUserServiceClient = "" +
            "    String telassa$Id = (String)org.apache.log4j.MDC.get(\"telassa-id\");\n" +
            "    if (telassa$Id != null) {\n" +
            "        String telassa$header = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.encodeHeader(telassa$Id);\n" +
            "        ((java.util.Map)($0.headers)).put(\"telassa-id\", telassa$header);\n" +
            "    }\n" +

            // channel、rootType、rootName 不会有中文，不需要 encode 编码
            "    String telassa$channel = (String)org.apache.log4j.MDC.get(\"telassa-channel\");\n" +
            "    if (telassa$channel != null) {\n" +
            "        ((java.util.Map)($0.headers)).put(\"telassa-channel\", telassa$channel);\n" +
            "    }\n" +

            "    String telassa$rootId = (String)org.apache.log4j.MDC.get(\"telassa-rootId\");\n" +
            "    if (telassa$rootId != null) {\n" +
            "        ((java.util.Map)($0.headers)).put(\"telassa-rootId\", telassa$rootId);\n" +
            "    }\n" +

            "    String telassa$rootType = (String)org.apache.log4j.MDC.get(\"telassa-rootType\");\n" +
            "    if (telassa$rootType != null) {\n" +
            "        ((java.util.Map)($0.headers)).put(\"telassa-rootType\", telassa$rootType);\n" +
            "    }\n" +

            "    String telassa$rootName = (String)org.apache.log4j.MDC.get(\"telassa-rootName\");\n" +
            "    if (telassa$rootName != null) {\n" +
            "        ((java.util.Map)($0.headers)).put(\"telassa-rootName\", telassa$rootName);\n" +
            "    }\n";


    // 开始 redirect 之前，传递 TelassaId
    public static final String remoteRedirectClient = "" +
            "    String telassa$Id = (String)org.apache.log4j.MDC.get(\"telassa-id\");\n" +
            "    if (telassa$Id != null) {\n" +
            "        String telassa$attr = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.encodeHeader(telassa$Id);\n" +
            "        $3 = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.addQueryString($3, \"telassa-id\", telassa$attr);\n" +
            "    }\n";

    // 初始化 IBE TelassaId，前面需初始化完成变量 telassa$userName, session$Id, cat$Transaction, uri$for$cat
    public static final String initIbeTelassaId = "" +
            "    String cat$MessageId = com.dianping.cat.Cat.getCurrentMessageId();\n" +
            "    telassa$Id = com.travelsky.airline.trp.ops.telassa.probe.util.IdUtils.getIbeTelassaId(telassa$userName, session$Id, cat$MessageId);\n" +
            "    if (telassa$Id != null && telassa$Id.length() != 0) {\n" +
            "        org.apache.log4j.MDC.put(\"telassa-id\", telassa$Id);\n" +
            "        org.jboss.logging.MDC.put(\"telassa-id\", telassa$Id);\n" +
            "    }\n" +

            "    if (telassa$Id != null && telassa$Id.length() != 0 && cat$Transaction != null) {\n" +
            "        cat$Transaction.addData(\"telassaId=\".concat(telassa$Id));\n" +
            "    }\n";


    // 初始化 Service TelassaId，前面需定义 telassa$Id，oj$sourceName, 初始化完成 cat$Transaction
    public static final String initServiceTelassaId = "" +
            "    if (telassa$Id == null || telassa$Id.length() == 0) {\n" +
            "        String cat$MessageId = com.dianping.cat.Cat.getCurrentMessageId();\n" +
            "        String oj$sessionId = (String)org.apache.log4j.MDC.get(\"sessionid\");\n" +
            "        telassa$Id = com.travelsky.airline.trp.ops.telassa.probe.util.IdUtils.getServiceTelassaId(oj$sourceName, oj$sessionId, cat$MessageId);\n" +

            "        org.apache.log4j.MDC.put(\"telassa-id\", telassa$Id);\n" +
            "        org.jboss.logging.MDC.put(\"telassa-id\", telassa$Id);\n" +

            "        if (telassa$Id != null && telassa$Id.length() != 0 && cat$Transaction != null) {\n" +
            "            cat$Transaction.addData(\"telassaId=\".concat(telassa$Id));\n" +
            "        }\n" +
            "    }\n";

    // 上一步必须先给变量 telassaId 赋值
    public static final String mdcTelassaId = "" +
            "    if (telassa$Id != null && telassa$Id.length() != 0) {\n" +
            "        org.apache.log4j.MDC.put(\"telassa-id\", telassa$Id);\n" +
            "        org.jboss.logging.MDC.put(\"telassa-id\", telassa$Id);\n" +
            "    }\n\n";


    //  手动清除 telassa 上下文信息
    public static final String clearTelassaContext = "" +
            "    org.apache.log4j.MDC.remove(\"telassa-id\");\n" +
            "    org.jboss.logging.MDC.remove(\"telassa-id\");\n" +

            "    org.apache.log4j.MDC.remove(\"telassa-channel\");\n" +
            "    org.jboss.logging.MDC.remove(\"telassa-channel\");\n" +

            "    org.apache.log4j.MDC.remove(\"telassa-rootId\");\n" +
            "    org.jboss.logging.MDC.remove(\"telassa-rootId\");\n" +

            "    org.apache.log4j.MDC.remove(\"telassa-rootType\");\n" +
            "    org.jboss.logging.MDC.remove(\"telassa-rootType\");\n" +

            "    org.apache.log4j.MDC.remove(\"telassa-rootName\");\n" +
            "    org.jboss.logging.MDC.remove(\"telassa-rootName\");\n";


    // 初始化 Editor TelassaId，前面需初始化完成变量 telassa$userName, session$Id, cat$Transaction, uri$for$cat
    public static final String initEditorTelassaId = "" +
            "    String cat$MessageId = com.dianping.cat.Cat.getCurrentMessageId();\n" +
            "    telassa$Id = com.travelsky.airline.trp.ops.telassa.probe.util.IdUtils.getEditorTelassaId(telassa$userName, session$Id, cat$MessageId);\n" +
            "    if (telassa$Id != null && telassa$Id.length() != 0) {\n" +
            "        org.apache.log4j.MDC.put(\"telassa-id\", telassa$Id);\n" +
            "        org.jboss.logging.MDC.put(\"telassa-id\", telassa$Id);\n" +
            "    }\n" +

            "    if (telassa$Id != null && telassa$Id.length() != 0 && cat$Transaction != null) {\n" +
            "        cat$Transaction.addData(\"telassaId=\".concat(telassa$Id));\n" +
            "    }\n";


    // 初始化 UserServiceService TelassaId，前面需定义 telassa$Id，oj$sourceName, 初始化完成 cat$Transaction
    public static final String initUserServiceTelassaId = "" +
            "    if (telassa$Id == null || telassa$Id.length() == 0) {\n" +
            "        String cat$MessageId = com.dianping.cat.Cat.getCurrentMessageId();\n" +
            "        String oj$sessionId = (String)org.apache.log4j.MDC.get(\"sessionid\");\n" +
            "        telassa$Id = com.travelsky.airline.trp.ops.telassa.probe.util.IdUtils.getUserServiceTelassaId(oj$sourceName, oj$sessionId, cat$MessageId);\n" +

            "        org.apache.log4j.MDC.put(\"telassa-id\", telassa$Id);\n" +
            "        org.jboss.logging.MDC.put(\"telassa-id\", telassa$Id);\n" +

            "        if (telassa$Id != null && telassa$Id.length() != 0 && cat$Transaction != null) {\n" +
            "            cat$Transaction.addData(\"telassaId=\".concat(telassa$Id));\n" +
            "        }\n" +
            "    }\n";


    // 初始化 CMS TelassaId，前面需定义 telassa$Id，oj$sourceName, 初始化完成 cat$Transaction
    public static final String initCMSTelassaId = "" +
            "    if (telassa$Id == null || telassa$Id.length() == 0) {\n" +
            "        String cat$MessageId = com.dianping.cat.Cat.getCurrentMessageId();\n" +
            "        telassa$Id = com.travelsky.airline.trp.ops.telassa.probe.util.IdUtils.getCMSTelassaId(oj$sourceName, null, cat$MessageId);\n" +

            "        if (telassa$Id != null && telassa$Id.length() != 0 && cat$Transaction != null) {\n" +
            "            cat$Transaction.addData(\"telassaId=\".concat(telassa$Id));\n" +
            "        }\n" +
            "    }\n";


    // 上一步必须先给变量 telassa$channel, telassa$rootId，telassa$rootType, telassa$rootName 赋值
    public static final String mdcTelassaContext = "" +
            "    if (telassa$rootName != null && telassa$rootName.length() != 0) {\n" +
            "        org.apache.log4j.MDC.put(\"telassa-rootName\", telassa$rootName);\n" +
            "        org.jboss.logging.MDC.put(\"telassa-rootName\", telassa$rootName);\n" +
            "    }\n" +

            "    if (telassa$rootType != null && telassa$rootType.length() != 0) {\n" +
            "        org.apache.log4j.MDC.put(\"telassa-rootType\", telassa$rootType);\n" +
            "        org.jboss.logging.MDC.put(\"telassa-rootType\", telassa$rootType);\n" +
            "    }\n" +

            "    if (telassa$rootId != null && telassa$rootId.length() != 0) {\n" +
            "        org.apache.log4j.MDC.put(\"telassa-rootId\", telassa$rootId);\n" +
            "        org.jboss.logging.MDC.put(\"telassa-rootId\", telassa$rootId);\n" +
            "    }\n" +

            "    if (telassa$channel != null && telassa$channel.length() != 0) {\n" +
            "        org.apache.log4j.MDC.put(\"telassa-channel\", telassa$channel);\n" +
            "        org.jboss.logging.MDC.put(\"telassa-channel\", telassa$channel);\n" +
            "    }\n";


    // 将 telassa 上下文添加到 Transaction 的最前面，然后用 |t|l|a|s|s|a| 分隔，在此之前的需要用
    // 上一步必须先给变量 cat$Transaction 赋值
    public static final String addTelassaContextData = "" +
            "    if (cat$Transaction != null) {\n" +
            // org.apache.log4j.MDC 取不到时就从 org.jboss.logging.MDC 中取
            "        String telassa$channel = (String)org.apache.log4j.MDC.get(\"telassa-channel\");\n" +
            "        if (telassa$channel == null || telassa$channel.length() == 0) {\n" +
            "            telassa$channel = (String)org.jboss.logging.MDC.get(\"telassa-channel\");\n" +
            "        }\n" +

            "        String telassa$rootId = (String)org.apache.log4j.MDC.get(\"telassa-rootId\");\n" +
            "        if (telassa$rootId == null || telassa$rootId.length() == 0) {\n" +
            "            telassa$rootId = (String)org.jboss.logging.MDC.get(\"telassa-rootId\");\n" +
            "        }\n" +

            "        String telassa$rootType = (String)org.apache.log4j.MDC.get(\"telassa-rootType\");\n" +
            "        if (telassa$rootType == null || telassa$rootType.length() == 0) {\n" +
            "            telassa$rootType = (String)org.jboss.logging.MDC.get(\"telassa-rootType\");\n" +
            "        }\n" +

            "        String telassa$rootName = (String)org.apache.log4j.MDC.get(\"telassa-rootName\");\n" +
            "        if (telassa$rootName == null || telassa$rootName.length() == 0) {\n" +
            "            telassa$rootName = (String)org.jboss.logging.MDC.get(\"telassa-rootName\");\n" +
            "        }\n" +

            // 最前面添加分隔符
            "        cat$Transaction.addData(\"|t|e|l|a|s|s|a|\".concat(\"\"));\n" +

            // 添加到 Transaction 中
            "        if (telassa$rootName != null && telassa$rootName.length() != 0) {\n" +
            "            cat$Transaction.pushData(\"rootName=\".concat(telassa$rootName));\n" +
            "        }\n" +

            "        if (telassa$rootType != null && telassa$rootType.length() != 0) {\n" +
            "            cat$Transaction.pushData(\"rootType=\".concat(telassa$rootType));\n" +
            "        }\n" +

            "        if (telassa$rootId != null && telassa$rootId.length() != 0) {\n" +
            "            cat$Transaction.pushData(\"rootId=\".concat(telassa$rootId));\n" +
            "        }\n" +

            "        if (telassa$channel != null && telassa$channel.length() != 0) {\n" +
            "            cat$Transaction.pushData(\"channel=\".concat(telassa$channel));\n" +
            "        }\n" +
            "    }\n\n";
}