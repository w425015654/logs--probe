package com.travelsky.airline.trp.ops.telassa.probe.util;

/**
 * CAT 相关逻辑
 *
 * @author zengfan
 */
public class CatUtils {
    public static String VERSION = "2.5";
    public static String DOMAIN = null;

    public final static String CHANNEL_NVL = "NVL";                         // 官网渠道
    public final static String CHANNEL_B2C = "B2C";                         // 官网渠道
    public final static String CHANNEL_TRIBE = "Tribe";                     // Tribe 渠道
    public final static String CHANNEL_TASK = "Task";                       // 定时任务 渠道


    // URL 请求前缀，这里分为 URL.Direct 和 URL.Tribe，表示是否是 Tribe 接口
    // URL.Forward 表示是 forward 的 url
    // URL.Redirect 表示是 sendRedirect 的 url
    private final static String TYPE_URL_DIRECT = "URL.Direct";         // 网页直接访问
    private final static String TYPE_URL_TRIBE = "URL.Tribe";
    private final static String TYPE_URL_FORWARD = "URL.Forward";
    private final static String TYPE_URL_REDIRECT = "URL.Redirect";

    private final static String TYPE_WebService = "WebService";
    private final static String TYPE_TableProcessor = "TableProcessor";
    public final static String TYPE_IBEClient = "IBEClient.";
    private final static String TYPE_SPMTranslation = "SPMTranslation";
    private final static String TYPE_SPMClientThread = "SPMClientThread";
    private final static String TYPE_JAVA_CONNECTOR = "JavaConnector";
    public final static String TYPE_TASK = "Task";

    // OTA 接口前缀，后面需要补上 TAOBAO、CUXIAO 等
    public final static String TYPE_OTA_PREFIX = "OTA.";

    // Form 校验时输出事件
    private final static String TYPE_FORM_VALIDATION = "FormValidation";

    // XSL 转换
    public final static String TYPE_XSL_TRANSFORM = "XslTransform";

    // 访问第三方 SOAP 接口，如 user
    public final static String TYPE_SOAP_THREAD = "SOAPThread";

    // 访问第三方 HTTP 接口
    public final static String TYPE_HTTP_THREAD = "HTTPThread";

    // 发送邮件
    public final static String TYPE_SMTP_EMAIL = "SMTPEmail";

    // UserService 入口
    private final static String TYPE_UserService = "UserService";

    // UserService 里的函数调用
    public final static String TYPE_RPC_INVOKE = "RpcInvoke.";

    // CMS 入口
    private final static String TYPE_URL_CMS = "URL.CMS";

    // XSL 事件
    private final static String TYPE_XSL = "XSL";

    // IBE 层查询入口
    public final static String TYPE_SHOPPING_3 = "Shopping.3.";

    // Service 层查询入口
    public final static String TYPE_SHOPPING_2 = "Shopping.2.";

    // JavaConnector 层查询入口
    public final static String TYPE_SHOPPING_1 = "Shopping.1.";

    // 开始一个 IBE Url Transaction, 前面需有参数 cat$queryParams
    // telassa$channel, telassa$rootType, telassa$rootName
    public static final String newIbeEditorUrlTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    String uri$for$cat = ((javax.servlet.http.HttpServletRequest)$1).getRequestURI();\n" +
            "    if(uri$for$cat != null && (uri$for$cat.endsWith(\".do\") || uri$for$cat.endsWith(\".servlet\"))){\n" +
            "        String transaction$Type = \"" + TYPE_URL_DIRECT + "\";\n" +
            "        telassa$channel =  \"" + CHANNEL_B2C + "\";\n" +
            "        String contentType = ((javax.servlet.ServletRequest)$1).getContentType();\n" +
            "        if (contentType != null) {\n" +
            "            contentType = contentType.trim();\n" +
            "            if (contentType.startsWith(com.openjawx.xRez.core.RenderingMode.CONTENT_TYPE_JSON)) {\n" +
            "                transaction$Type = \"" + TYPE_URL_TRIBE + "\";\n" +
            "                telassa$channel =  \"" + CHANNEL_TRIBE + "\";\n" +
            "            }\n" +
            "        }\n" +

            // 如果能取到 telassa-id, 表示是通过 sendRedirect 过来的 URL
            "        if (cat$queryParams.containsKey(\"telassa-id\")){\n" +
            "            transaction$Type = \"" + TYPE_URL_REDIRECT + "\";\n" +
            "                telassa$channel =  \"" + CHANNEL_B2C + "\";\n" +   // Tribe 应没有 sendRedirect 的方式
            "        }\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(transaction$Type, uri$for$cat);\n" +

            // 设置 Telassa 上下文变量
            "        telassa$rootId = com.dianping.cat.Cat.getCurrentMessageId();\n" +
            "        telassa$rootType = transaction$Type;\n" +
            "        telassa$rootName = uri$for$cat;\n" +
            "    }\n\n";


    // 开始一个 IBE Url Forward Transaction
    public static final String newIbeForwardTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    String uri$for$cat = $3;\n" +
            "    if(uri$for$cat != null && (uri$for$cat.endsWith(\".do\") || uri$for$cat.endsWith(\".servlet\"))){\n" +
            "        String transaction$Type = \"" + TYPE_URL_FORWARD + "\";\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(transaction$Type, uri$for$cat);\n" +
            "    }\n\n";

    // 事务成功
    public static final String successTransaction = "" +
            "    if(cat$Transaction != null) {\n" +
            // 如果当前 Transaction 已经被内层代码置为 false，则外层循环不再修改此结果
            "        if (\"0\".equals(cat$Transaction.getStatus()) || cat$Transaction.getStatus() == null) {\n" +
            "            cat$Transaction.setSuccessStatus();\n" +
            "        }\n" +
            "    }\n";


    // 根据 Document 里的 error 节点，设置 statusCode 和 errorDesc
    // 前面需由变量 org.w3c.dom.Document cat$document、String cat$statusCode、String cat$errorDesc
    public static final String setStatusCodeByDocument = "" +
            "        if (cat$document == null) {\n" +
            "            cat$statusCode = \"ERROR\";\n" +
            "            cat$errorDesc = \"response.getDocument() is NULL.\";\n" +
            "        } else {\n" +
            "            org.w3c.dom.NodeList errors = cat$document.getElementsByTagName(\"Error\");\n" +
            "            if (errors != null && errors.getLength() != 0) {\n" +
            "                cat$statusCode = \"ERROR\";\n" +
            "                cat$errorDesc = com.travelsky.airline.trp.ops.telassa.probe.util.DomUtils.nodeListToString(errors);\n" +
            "            }\n" +
            "        }\n";

    // 事务失败, 需有个 String cat$statusCode 标记错误类型
    public static final String statusTransaction = "" +
            "    if(cat$Transaction != null) {\n" +
            "        if(cat$statusCode == null) {\n" +
            "            cat$Transaction.setSuccessStatus();\n" +
            "        } else {\n" +
            "            cat$Transaction.setStatus(cat$statusCode);\n" +
            "        }\n" +
            "    }\n";


    // 添加一个错误描述
    // 需有个 String cat$errorDesc 标记错误信息
    public static final String addErrorDescTransaction = "" +
            "    if(cat$Transaction != null && cat$errorDesc != null && cat$errorDesc.length() != 0){\n" +
            "        cat$Transaction.addData(\"errorDesc=\".concat(cat$errorDesc));\n" +
            "    }\n";


    // 添加一个错误描述, 需有个 String oj$clientCode 标记 clientCode
    public static final String addClientCodeTransaction = "" +
            "    if(cat$Transaction != null && oj$clientCode != null && oj$clientCode.length() != 0){\n" +
            "        cat$Transaction.addData(\"client=\".concat(oj$clientCode));\n" +
            "    }\n";


    // 添加一个 response Source 描述, 需有个 String cat$responseSource
    public static final String addResponseSourceTransaction = "" +
            "    if(cat$Transaction != null && cat$responseSource != null && cat$responseSource.length() != 0){\n" +
            "        cat$Transaction.addData(\"responseSource=\".concat(cat$responseSource));\n" +
            "    }\n";


    // 捕获一个异常，Cat 记录后原样抛出
    public static final String catchException = "" +
            "        if(cat$Transaction != null) {\n" +
            "            cat$Transaction.setStatus(e);\n" +
            "            com.dianping.cat.Cat.logError(e);\n" +
            "        }\n";

    // 结束一个 cat
    public static final String completeTransaction = "" +
            "    if (cat$Transaction != null) {\n" +
            "        cat$Transaction.complete();\n" +
            "    }\n";


    // 开始一个 Service WebService Transaction
    // 只考虑 url 中不带 . 的 path，即不记录各种 .js，.css，.jsp 等等访问
    public static final String newWebServiceTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    String uri$for$cat = ((javax.servlet.http.HttpServletRequest)$1).getRequestURI();\n" +
            "    if(uri$for$cat != null && uri$for$cat.indexOf('.') == -1){\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_WebService + "\", uri$for$cat);\n" +
            "    }\n\n";


    // 开始一个 TableProcessor Transaction
    // 上层需要有一个变量 cat$Name 标记当前处理的方法
    public static final String newTableProcessorTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    if(cat$Name != null){\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_TableProcessor + "\", cat$Name);\n" +
            "    }\n\n";


    // 开始一个 IBEClient Transaction
    // 上层需要有一个变量 query$Name 标记当前 query 的参数
    // 还要有一个变量 cat$type 标记当前 type
    public static final String newIBEClientTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    cat$Transaction = com.dianping.cat.Cat.newTransaction(cat$type, query$Name);\n\n";


    // 开始一个 OJRQ Transaction
    // 上层需要有一个变量 cat$TagName 标记当前要处理的 RQ
    public static final String newSPMTranslationTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    if(cat$TagName != null){\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_SPMTranslation + "\", cat$TagName);\n" +
            "    }\n\n";


    // 开始一个 OJRQ Transaction
    // 上层需要有一个变量 cat$TagName 标记当前要处理的 RQ
    public static final String newSPMClientThreadTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    if(cat$TagName != null){\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_SPMClientThread + "\", cat$TagName);\n" +
            "    }\n\n";


    // 开始一个 JavaConnector Transaction
    public static final String newJavaConnectorTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    org.w3c.dom.Node cat$FirstChild = ((org.w3c.dom.Document)$1).getFirstChild();\n" +
            "    if (cat$FirstChild != null) {\n" +
            "        String cat$LocalName = cat$FirstChild.getLocalName();\n" +
            "        if (cat$LocalName != null && cat$LocalName.length() != 0){\n" +
            "            cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_JAVA_CONNECTOR + "\", cat$LocalName);\n" +
            "        }\n" +
            "    }\n\n";


    // 开始一个 OTA Transaction
    // 前面需要有 cat$channel 标记当前渠道，cat$serviceType 表示当前操作
    public static final String newOTATransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    if (cat$channel == null || cat$channel.length() == 0) {\n" +
            "        cat$channel = \"NVL\";\n" +
            "    }\n" +
            "    if (cat$serviceType == null || cat$serviceType.length() == 0) {\n" +
            "        cat$serviceType = \"NVL\";\n" +
            "    }\n" +
            "    cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_OTA_PREFIX + "\".concat(cat$channel), cat$serviceType);\n\n";


    // 开始一个 Task Transaction
    // 上层需要有一个变量 task$name 标记当前处理的方法
    public static final String newTaskTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    if(task$name != null && task$name.length() != 0){\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_TASK + "\", task$name);\n" +
            "    }\n\n";

    // 设置一个 HTTPPost 的 CAT Transaction 为 Error
    // 前面需有个变量，String cat$errorType, 标记错误类型
    // 前面需有个变量，String cat$errorDesc，标记错误描述
    public static final String setHttpPostTransactionError = "" +
            "com.dianping.cat.message.spi.MessageManager message$Manager= com.dianping.cat.message.internal.DefaultMessageManager.getInstance();\n" +
            "com.dianping.cat.message.Transaction cat$Transaction = message$Manager.getPeekTransaction();\n" +
            "if (cat$Transaction != null) {\n" +
            "    cat$Transaction.setStatus(cat$errorType);\n" +
            "    cat$Transaction.addData(\"errorDesc=\".concat(cat$errorDesc));\n" +
            "}\n";

    // 记录一个异常事件
    public static final String logFormValidationEvent = "" +
            "java.lang.StackTraceElement[] cat$stackTraceElements = java.lang.Thread.currentThread().getStackTrace();\n" +
            "java.lang.StringBuilder cat$stringBuilder = new java.lang.StringBuilder();\n" +
            "for (int i=1; i< cat$stackTraceElements.length; i++) {\n" +
            // 最多只记录 10 层堆栈，多的无用
            "    if (i >= 10) {\n" +
            "        cat$stringBuilder.append(\"...\");\n" +
            "        break;\n" +
            "    }\n" +
            "    java.lang.StackTraceElement element = cat$stackTraceElements[i];\n" +
            "    String stackTrace = element.getClassName() + \".\" + element.getMethodName() + \"(\" + element.getFileName() + \":\" + element.getLineNumber() + \") <<< \";\n" +
            "    cat$stringBuilder.append(stackTrace);\n" +
            "}\n" +
            "String cat$stackTrace = cat$stringBuilder.toString();\n" +
            "com.dianping.cat.Cat.logEvent(\"" + TYPE_FORM_VALIDATION + "\", cat$errorType, cat$errorDesc, cat$stackTrace);\n\n";


    // 开始一个 SOAPThread Transaction
    // 上层需要有一个变量 local$name 标记当前要调用的远程方法
    public static final String newSOAPThreadTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    if(local$name != null && local$name.length() != 0){\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_SOAP_THREAD + "\", local$name);\n" +
            "    }\n\n";


    // 开始一个 HTTPThread Transaction
    // 上层需要有一个变量 message$name 标记 HTTP message
    public static final String newHTTPThreadTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    if(message$name != null && message$name.length() != 0){\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_HTTP_THREAD + "\", message$name);\n" +
            "    }\n\n";


    // 开始一个 SMTPEmail Transaction
    // 上层需要有一个变量 subject$name 标记邮件主题
    public static final String newSMTPEmailTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    if(subject$name != null && subject$name.length() != 0){\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_SMTP_EMAIL + "\", subject$name);\n" +
            "    }\n\n";


    // 开始一个 Service WebService Transaction
    // 只考虑 url 中不带 . 的 path，即不记录各种 .js，.css，.jsp 等等访问
    public static final String newUserServiceTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    String uri$for$cat = ((javax.servlet.http.HttpServletRequest)$1).getRequestURI();\n" +
            "    if(uri$for$cat != null && uri$for$cat.indexOf('.') == -1){\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_UserService + "\", uri$for$cat);\n" +
            "    }\n\n";


    // 开始一个 Rpc Invoke Transaction
    // 上层需要 cat$type 标记 type, 有一个变量 method$name 标记函数的具体名字
    public static final String newRpcInvokeTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    if(method$name != null && method$name.length() != 0){\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(cat$type, method$name);\n" +
            "    }\n\n";


    // 设置一个 RPCUtil 的 CAT Transaction 为 Error
    // 前面需有个变量，String cat$errorType, 标记错误类型
    // 前面需有个变量，String cat$errorDesc，标记错误描述
    public static final String setRPCUtilTransactionError = "" +
            "com.dianping.cat.message.spi.MessageManager message$Manager= com.dianping.cat.message.internal.DefaultMessageManager.getInstance();\n" +
            "com.dianping.cat.message.Transaction cat$Transaction = message$Manager.getPeekTransaction();\n" +
            "if (cat$Transaction != null) {\n" +
            "    cat$Transaction.setStatus(\"ERROR\");\n" +
            "    cat$Transaction.addData(\"errorType=\".concat(cat$errorType));\n" +
            "    cat$Transaction.addData(\"errorDesc=\".concat(cat$errorDesc));\n" +
            "}\n";

    /**
     * 记录远程 IP 地址，第一个变量 $1 必须是 javax.servlet.http.HttpServletRequest 类型
     * 为了避免引入
     * <dependency>
     * <groupId>javax.servlet</groupId>
     * <artifactId>servlet-api</artifactId>
     * <version>2.5</version>
     * <scope>provided</scope>
     * </dependency>
     * 这个包，这里直接把代码写死，而不是在 Util 里写一个通用的  getRemoteHosts 函数
     * http://www.51gjie.com/javaweb/960.html
     */
    public static String addRemoteAddress = "" +
            "    String cat$ip = null;\n" +
            "    if (!($1 instanceof javax.servlet.http.HttpServletRequest)) {\n" +
            "        cat$ip = $1.getRemoteHost();\n" +
            "    }\n" +
            "" +
            "    javax.servlet.http.HttpServletRequest cat$ServletRequest = (javax.servlet.http.HttpServletRequest) $1;\n" +
            "    cat$ip = cat$ServletRequest.getHeader(\"x-forwarded-for\");\n" +
            "    if (cat$ip == null || cat$ip.length() == 0 || \"unknown\".equalsIgnoreCase(cat$ip)) {\n" +
            "        cat$ip = cat$ServletRequest.getHeader(\"Proxy-Client-IP\");\n" +
            "    }\n" +
            "" +
            "    if (cat$ip == null || cat$ip.length() == 0 || \"unknown\".equalsIgnoreCase(cat$ip)) {\n" +
            "        cat$ip = cat$ServletRequest.getHeader(\"WL-Proxy-Client-IP\");\n" +
            "    }\n" +
            "    if (cat$ip == null || cat$ip.length() == 0 || \"unknown\".equalsIgnoreCase(cat$ip)) {\n" +
            "        cat$ip = cat$ServletRequest.getRemoteAddr();\n" +
            "    }\n" +
            "    if (cat$ip == null || cat$ip.equals(\"0:0:0:0:0:0:0:1\")) {\n" +
            "        cat$ip = \"127.0.0.1\";\n" +
            "    }\n" +
            "    if (cat$Transaction != null) {\n" +
            "        cat$Transaction.addData(\"remoteIp=\".concat(cat$ip));\n" +
            "        cat$Transaction.addData(\"version=\".concat(\"" + VERSION + "\"));\n" +
            "    }\n";

    // 获取 Session
    // 获取 xRezWebSessionBean
    // 获取 ibeForm

    // 开始一个 CMS Transaction
    // 只考虑 url 中不带 . 的 path，即不记录各种 .js，.css，.jsp 等等访问
    public static final String newCMSTransaction = "" +
            "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
            "    String uri$for$cat = ((javax.servlet.http.HttpServletRequest)$1).getRequestURI();\n" +
            "    if(uri$for$cat != null){\n" +
            "        cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + TYPE_URL_CMS + "\", uri$for$cat);\n" +
            "    }\n\n";


    // 记录一个 XslError Event，前面需有 cat$name, cat$errorCode, cat$eventDesc
    public static final String logXslExceptEvent = "" +
            "    com.dianping.cat.Cat.logEvent(\"" + TYPE_XSL + "\", cat$name, cat$errorCode, cat$eventDesc);\n\n";


    // 基于 OTA_AirLowFareSearchRQ 重设其最近的 Transaction，不新建
    // 前面需有 cat$node，cat$newType
    public static final String renameSearchRQTransaction = "\n" +
            "            String trans$name = com.travelsky.airline.trp.ops.telassa.probe.util.DomUtils.getSearchRQCatName(cat$node);\n" +
            "            if (trans$name != null) {\n" +
            // 这里不新建 Transaction，否则整体时间由上级 Transaction 控制时间，没法指定时间，所以干脆直接修改上级 Transaction
            "                com.dianping.cat.message.spi.MessageManager message$Manager= com.dianping.cat.message.internal.DefaultMessageManager.getInstance();\n" +
            "                cat$peekTransaction = (com.dianping.cat.message.internal.DefaultTransaction)(message$Manager.getPeekTransaction());\n" +
            "                String cat$originType = cat$peekTransaction.getType();\n" +
            "                String cat$originName = cat$peekTransaction.getName();\n" +
            // 只有上游节点是  URL 或者 WebService 时才进行重命名操作
            "                if (cat$originType != null && (cat$originType.startsWith(\"URL.\") || cat$originType.startsWith(\"WebService\") || cat$originType.startsWith(\"JavaConnector\"))){ \n" +
            "                    cat$peekTransaction.setType(cat$newType);\n" +
            "                    cat$peekTransaction.setName(trans$name);\n" +
            // 记录原始的 catType、catName
            "                    cat$peekTransaction.addData(\"OriginType=\".concat(cat$originType));\n" +
            "                    cat$peekTransaction.addData(\"OriginName=\".concat(cat$originName));\n" +
            // 记录行程信息
            "                    String cat$itinerary = com.travelsky.airline.trp.ops.telassa.probe.util.DomUtils.getRQItineraryDetail(cat$node);\n" +
            "                    cat$peekTransaction.addData(\"itinerary=\".concat(cat$itinerary));\n" +
            // 记录 IATA 信息
            "                    String cat$iata = com.travelsky.airline.trp.ops.telassa.probe.util.DomUtils.getRQIATA(cat$node);\n" +
            "                    cat$peekTransaction.pushData(\"IATACode=\".concat(cat$iata));\n" +
            "                }\n" +
            "            }\n";

    // 基于 cat$shoppingLevel 和 MDC 中的 telassa-channel 设置 Shopping 的 Type
    public static final String initSearchRQType = "\n" +
            "            String cat$newType = cat$shoppingLevel + \"" + CatUtils.CHANNEL_NVL + "\";\n" +
            "            String telassa$channel = (String)org.apache.log4j.MDC.get(\"telassa-channel\");\n" +
            "            if (telassa$channel != null && telassa$channel.length() != 0) {\n" +
            "                cat$newType = cat$shoppingLevel + telassa$channel;\n" +
            "            }\n";
}
