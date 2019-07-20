package com.travelsky.airline.trp.ops.telassa.probe.util;

import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * CAT 跨线程时的相关逻辑
 *
 * @author zengfan
 */
public class CatForkUtils {
    /**
     * 跨线程时需要的相关参数
     */
    // 添加新字段 telassaId、telassaChannel、telassaRootType、telassaRootName
    public static final String TELASSA_ID_FIELD_NAME = "telassaId$field$add$by$telassa";
    public static final String TELASSA_CHANNEL_FIELD_NAME = "telassaChannel$field$add$by$telassa";
    public static final String TELASSA_ROOT_ID_FIELD_NAME = "telassaRootId$field$add$by$telassa";
    public static final String TELASSA_ROOT_TYPE_FIELD_NAME = "telassaRootType$field$add$by$telassa";
    public static final String TELASSA_ROOT_NAME_FIELD_NAME = "telassaRootName$field$add$by$telassa";

    // 添加 cat 相关字段
    public static final String CAT_FORKABLE_TRANSACTION = "_cat$forkableTransaction";
    public static final String CAT_FORKED_TRANSACTION = "_cat$forkedTransaction";


    // Client 端开始远程访问 Service
    public static final String remoteCallServiceClient = "" +
            "    com.dianping.cat.Cat.Context cat$Context = new com.travelsky.airline.trp.ops.telassa.probe.cat.CatContext();\n" +
            "    com.dianping.cat.Cat.logRemoteCallClient(cat$Context, \"" + CatUtils.DOMAIN + "\");\n" +
            "    $0.setHTTPHeader(com.dianping.cat.Cat.Context.ROOT, cat$Context.getProperty(com.dianping.cat.Cat.Context.ROOT));\n" +
            "    $0.setHTTPHeader(com.dianping.cat.Cat.Context.PARENT, cat$Context.getProperty(com.dianping.cat.Cat.Context.PARENT));\n" +
            "    $0.setHTTPHeader(com.dianping.cat.Cat.Context.CHILD, cat$Context.getProperty(com.dianping.cat.Cat.Context.CHILD));\n\n";


    // Client 端开始远程访问 UserServiceService
    public static final String remoteCallUserServiceClient = "" +
            "    com.dianping.cat.Cat.Context cat$Context = new com.travelsky.airline.trp.ops.telassa.probe.cat.CatContext();\n" +
            "    com.dianping.cat.Cat.logRemoteCallClient(cat$Context, \"" + CatUtils.DOMAIN + "\");\n" +
            "    ((java.util.Map)($0.headers)).put(com.dianping.cat.Cat.Context.ROOT, cat$Context.getProperty(com.dianping.cat.Cat.Context.ROOT));\n" +
            "    ((java.util.Map)($0.headers)).put(com.dianping.cat.Cat.Context.PARENT, cat$Context.getProperty(com.dianping.cat.Cat.Context.PARENT));\n" +
            "    ((java.util.Map)($0.headers)).put(com.dianping.cat.Cat.Context.CHILD, cat$Context.getProperty(com.dianping.cat.Cat.Context.CHILD));\n\n";


    // Server 端开始远程访问 Service
    public static final String remoteCallServer = "" +
            "    String cat$Root = ((javax.servlet.http.HttpServletRequest)$1).getHeader(com.dianping.cat.Cat.Context.ROOT);\n" +
            "    String cat$Parent = ((javax.servlet.http.HttpServletRequest)$1).getHeader(com.dianping.cat.Cat.Context.PARENT);\n" +
            "    String cat$Child = ((javax.servlet.http.HttpServletRequest)$1).getHeader(com.dianping.cat.Cat.Context.CHILD);\n" +
            "    if (cat$Root != null && cat$Root.length() != 0) {\n" +
            "        com.dianping.cat.Cat.Context cat$Context = new com.travelsky.airline.trp.ops.telassa.probe.cat.CatContext();\n" +
            "        cat$Context.addProperty(com.dianping.cat.Cat.Context.ROOT, cat$Root);\n" +
            "        cat$Context.addProperty(com.dianping.cat.Cat.Context.PARENT, cat$Parent);\n" +
            "        cat$Context.addProperty(com.dianping.cat.Cat.Context.CHILD, cat$Child);\n" +
            "        com.dianping.cat.Cat.logRemoteCallServer(cat$Context);\n" +
            "    }\n";


    // Client 端开始 redirect
    // NOTICE: 这里有一个小 tricky 的地方，就是这个远程事务的 PARENT 取的是 ROOT，而不是 PARENT
    //         因为 clientRedirect 发生在最后一个事务，但是执行的时候已经是最后一步了，把它拼在最内层的事务不怎么合理
    // 设置了，但是没有啥效果。。就先不改吧，确实是最里面事务发起的 clientRedirect
    public static final String remoteRedirectClient = "" +
            "    com.dianping.cat.Cat.Context cat$Context = new com.travelsky.airline.trp.ops.telassa.probe.cat.CatContext();\n" +
            "    com.dianping.cat.Cat.logRemoteCallClient(cat$Context, \"" + CatUtils.DOMAIN + "\");\n" +
            "    $3 = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.addQueryString($3, com.dianping.cat.Cat.Context.ROOT, cat$Context.getProperty(com.dianping.cat.Cat.Context.ROOT));\n" +
            "    $3 = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.addQueryString($3, com.dianping.cat.Cat.Context.PARENT, cat$Context.getProperty(com.dianping.cat.Cat.Context.PARENT));\n" +
            "    $3 = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.addQueryString($3, com.dianping.cat.Cat.Context.CHILD, cat$Context.getProperty(com.dianping.cat.Cat.Context.CHILD));\n\n";


    // Server 端开始 redirect, 前面需初始化好 Map cat$queryParams
    public static final String remoteRedirectServer = "" +
            "    String cat$Root = (String)(cat$queryParams.get(com.dianping.cat.Cat.Context.ROOT));\n" +
            "    String cat$Parent = (String)(cat$queryParams.get(com.dianping.cat.Cat.Context.PARENT));\n" +
            "    String cat$Child = (String)(cat$queryParams.get(com.dianping.cat.Cat.Context.CHILD));\n" +
            "    if (cat$Root != null && cat$Root.length() != 0) {\n" +
            "        com.dianping.cat.Cat.Context cat$Context = new com.travelsky.airline.trp.ops.telassa.probe.cat.CatContext();\n" +
            "        cat$Context.addProperty(com.dianping.cat.Cat.Context.ROOT, cat$Root);\n" +
            "        cat$Context.addProperty(com.dianping.cat.Cat.Context.PARENT, cat$Parent);\n" +
            "        cat$Context.addProperty(com.dianping.cat.Cat.Context.CHILD, cat$Child);\n" +
            "        com.dianping.cat.Cat.logRemoteCallServer(cat$Context);\n" +
            "    }\n";


    /**
     * 添加 telassaId 字段
     *
     * @param clazz
     */
    public static void addTelassaField(final CtClass clazz, Logger logger) throws CannotCompileException {
        ProbeUtils.addFieldUnassigned(clazz, "java.lang.String", CatForkUtils.TELASSA_ID_FIELD_NAME, logger);
        ProbeUtils.addFieldUnassigned(clazz, "java.lang.String", CatForkUtils.TELASSA_CHANNEL_FIELD_NAME, logger);
        ProbeUtils.addFieldUnassigned(clazz, "java.lang.String", CatForkUtils.TELASSA_ROOT_ID_FIELD_NAME, logger);
        ProbeUtils.addFieldUnassigned(clazz, "java.lang.String", CatForkUtils.TELASSA_ROOT_TYPE_FIELD_NAME, logger);
        ProbeUtils.addFieldUnassigned(clazz, "java.lang.String", CatForkUtils.TELASSA_ROOT_NAME_FIELD_NAME, logger);
    }


    /**
     * 添加 Cat 相关字段 字段
     *
     * @param clazz
     */
    public static void addCatField(final CtClass clazz, Logger logger) throws CannotCompileException {
        ProbeUtils.addFieldUnassigned(clazz, "com.dianping.cat.message.ForkableTransaction", CatForkUtils.CAT_FORKABLE_TRANSACTION, logger);
        ProbeUtils.addFieldUnassigned(clazz, "com.dianping.cat.message.ForkedTransaction", CatForkUtils.CAT_FORKED_TRANSACTION, logger);
    }


    // Client 端开始跨线程调用
    // 注意 getPeekTransaction() 会返回 null，导致 NPE
    public static final String newForkableTransaction = "" +
            "if(com.dianping.cat.Cat.getManager() != null) {\n" +
            "    com.dianping.cat.message.Transaction cat$PeekTransaction = com.dianping.cat.Cat.getManager().getPeekTransaction();\n" +
            "    if (cat$PeekTransaction != null) {\n" +
            "        " + CatForkUtils.CAT_FORKABLE_TRANSACTION + " = cat$PeekTransaction.forFork();\n" +
            "    }\n" +
            "}";

    // Server 端开始跨线程调用
    public static final String newForkedTransaction = "" +
            "    if (" + CatForkUtils.CAT_FORKABLE_TRANSACTION + " != null && \n" +
            "       com.dianping.cat.message.internal.NullMessage.TRANSACTION != " + CatForkUtils.CAT_FORKABLE_TRANSACTION + "){\n" +
            "        " + CatForkUtils.CAT_FORKED_TRANSACTION + " = " + CatForkUtils.CAT_FORKABLE_TRANSACTION + ".doFork();\n" +
            "};\n";


    // 跨线程事务结束
    public static final String completeFordedTransaction = "" +
            "    if(" + CatForkUtils.CAT_FORKED_TRANSACTION + " != null) {\n" +
            "        " + CatForkUtils.CAT_FORKED_TRANSACTION + ".complete();\n" +
            "        " + CatForkUtils.CAT_FORKED_TRANSACTION + " = null;\n" +
            "    }\n" +
            "    if(" + CatForkUtils.CAT_FORKABLE_TRANSACTION + " != null) {\n" +
            "        " + CatForkUtils.CAT_FORKABLE_TRANSACTION + ".complete();\n" +
            "        " + CatForkUtils.CAT_FORKABLE_TRANSACTION + " = null;\n" +
            "    }\n\n";


    // 缓存线程上下文的 Telassa、CAT 信息
    public static final String captureThreadContext = "\n" +
            "String telassa$Id = (String)org.apache.log4j.MDC.get(\"telassa-id\");\n" +
            "if (telassa$Id != null) {\n" +
            "    " + TELASSA_ID_FIELD_NAME + " = telassa$Id;\n" +
            "}\n\n" +

            "String telassa$channel = (String)org.apache.log4j.MDC.get(\"telassa-channel\");\n" +
            "if (telassa$channel != null) {\n" +
            "    " + TELASSA_CHANNEL_FIELD_NAME + " = telassa$channel;\n" +
            "}\n" +


            "String telassa$rootId = (String)org.apache.log4j.MDC.get(\"telassa-rootId\");\n" +
            "if (telassa$rootId != null) {\n" +
            "    " + TELASSA_ROOT_ID_FIELD_NAME + " = telassa$rootId;\n" +
            "}\n" +


            "String telassa$rootType = (String)org.apache.log4j.MDC.get(\"telassa-rootType\");\n" +
            "if (telassa$rootType != null) {\n" +
            "    " + TELASSA_ROOT_TYPE_FIELD_NAME + " = telassa$rootType;\n" +
            "}\n" +

            "String telassa$rootName = (String)org.apache.log4j.MDC.get(\"telassa-rootName\");\n" +
            "if (telassa$rootName != null) {\n" +
            "    " + TELASSA_ROOT_NAME_FIELD_NAME + " = telassa$rootName;\n" +
            "}\n" +

            CatForkUtils.newForkableTransaction;


    // 将上一个线程 put 好的 Telassa、CAT 在这个线程重放
    public static final String replayThreadContext = "\n" +
            "    if (" + TELASSA_ID_FIELD_NAME + " != null && " + TELASSA_ID_FIELD_NAME + ".length() != 0) {\n" +
            "        String telassa$Id = " + TELASSA_ID_FIELD_NAME + ";\n" +
            "    " + TelassaUtils.mdcTelassaId +
            "    }\n\n" +

            "    String telassa$channel = " + TELASSA_CHANNEL_FIELD_NAME + ";\n" +
            "    String telassa$rootId = " + TELASSA_ROOT_ID_FIELD_NAME + ";\n" +
            "    String telassa$rootType = " + TELASSA_ROOT_TYPE_FIELD_NAME + ";\n" +
            "    String telassa$rootName = " + TELASSA_ROOT_NAME_FIELD_NAME + ";\n" +
            "    " + TelassaUtils.mdcTelassaContext +

            "    " + CatForkUtils.newForkedTransaction;

    // supplier 结束
    public static final String completeForkedTransaction = "\n{\n" +
            "    " + TELASSA_ID_FIELD_NAME + " = null;\n" +
            "    " + TELASSA_CHANNEL_FIELD_NAME + " = null;\n" +
            "    " + TELASSA_ROOT_ID_FIELD_NAME + " = null;\n" +
            "    " + TELASSA_ROOT_TYPE_FIELD_NAME + " = null;\n" +
            "    " + TELASSA_ROOT_NAME_FIELD_NAME + " = null;\n" +
            "    " + TelassaUtils.clearTelassaContext +
            "    " + CatForkUtils.completeFordedTransaction +
            "}\n";


    /**
     * 设置上下文，在 declareMethod 函数之中，callMethod 之前
     *
     * @param clazz
     * @throws NotFoundException
     */
    public static void doCaptureThreadContext(final CtClass clazz, final String declareMethod, final String callMethod, final Logger logger) throws NotFoundException {
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod(declareMethod);

        try {
            // 替换代码
            method.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equalsIgnoreCase(callMethod)) {
                        m.replace("{\n" +
                                CatForkUtils.captureThreadContext +
                                "$proceed($$);\n" +
                                "}\n");

                        logger.info("Instrument lines before " + callMethod + "() in " + method.getName() + " of class "
                                + method.getDeclaringClass().getName() + ": " + CatForkUtils.captureThreadContext);
                    }
                }
            });

        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }


    /**
     * 更新 run 函数
     * 1. 新增变量 telassaTransId , 用来记录 log 事务
     * 2. 在 waitForRequest 之后重放 TELASSA_ID_FIELD_NAME 的值到当前进程的 MDC 中, 并 log trans 开始
     * 3. 在 responseReceived 之后, log trans 结束
     *
     * @param clazz
     */
    public static void doReplayThreadContext(CtClass clazz, final String declareMethod,
                                             final String startCallMethod, final String startCallStatement,
                                             final String completeCallMethod, final String completeCallStatement,
                                             final Logger logger) throws NotFoundException, CannotCompileException {
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod(declareMethod);

        try {
            // 在 startCallMethod 之后重放 线程上下文 的值到当前进程的 MDC 中
            method.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equalsIgnoreCase(startCallMethod)) {
                        m.replace(startCallStatement);

                        logger.info("Instrument lines after " + startCallMethod + "() in " + method.getName() + " of class "
                                + method.getDeclaringClass().getName() + ": " + CatForkUtils.replayThreadContext);
                    }
                }
            });

            // 在 completeCallMethod 之后, 设置 CAT 结束
            method.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equalsIgnoreCase(completeCallMethod)) {
                        m.replace(completeCallStatement);

                        logger.info("Instrument lines after " + completeCallMethod + "() in " + method.getName() + " of class "
                                + method.getDeclaringClass().getName() + ": " + CatForkUtils.completeForkedTransaction);
                    }
                }
            });

        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }


}
