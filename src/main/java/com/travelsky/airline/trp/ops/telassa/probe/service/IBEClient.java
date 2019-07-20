package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

import java.lang.reflect.Modifier;


/**
 * 修改 IBEClient 的具体实现
 * 1. 在 query 之前，输出 log 开始，在 query 之后，输出 log 结束
 */
public class IBEClient implements Transformlet {
    private static final Logger logger = Logger.getLogger(IBEClient.class);

    @Override
    public boolean needTransform(String className) {
        return "com.travelsky.ibe.client.IBEClient".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.updateQuery(clazz);
    }


    /**
     * 更新 query 函数
     *
     * @param clazz
     * @throws NotFoundException
     */
    private void updateQuery(final CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 query 函数
        final String query = "query";
        final CtMethod oldMethod = clazz.getDeclaredMethod(query);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, query, clazz, null);

        // 将原来的 query 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$query$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~Modifier.PUBLIC /* remove public */ | Modifier.PRIVATE /* add private */);


        // 在调用 query 前后输出 CAT 调用链信息
        final String code = "\n{\n" +
                "    String arg$Str = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.arrayToString($1);\n" +
                "    String query$Name = null;\n" +
                "    if(arg$Str != null){\n" +
                "        query$Name = arg$Str.substring(0, arg$Str.indexOf(\" \"));\n" +
                "    }\n" +
                "    if (query$Name == null || query$Name.length() == 0) {\n" +
                "       query$Name = \"NVL\";\n" +
                "    }\n" +

                // 根据 channel 区分 IBEClient 的不同接口
                "    String cat$channel = (String)org.apache.log4j.MDC.get(\"telassa-channel\");\n" +
                "    if (cat$channel == null || cat$channel.length() == 0) {\n" +
                "       cat$channel = $0.getApplicationChannel();\n" +
                "    }\n" +
                "    if (cat$channel == null || cat$channel.length() == 0) {\n" +
                "       cat$channel = \"NVL\";\n" +
                "    }\n" +
                "    String cat$type = \"" + CatUtils.TYPE_IBEClient + "\".concat(cat$channel);\n" +
                "    " + CatUtils.newIBEClientTransaction +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中
//                "    " + PrometheusUtils.startIBEClientTimer +

                "    long cat$startTime = System.currentTimeMillis();\n" +
                "    String cat$errorDesc = null;\n" +
                "    try { \n" +
                "        String result = " + oldMethodRename + "($$);\n" +
                "    " + CatUtils.successTransaction +
//                "    " + PrometheusUtils.successIBEClient +
                "        return result;\n" +
                "     } catch (Exception e) {\n" +
                "    " + CatUtils.catchException +
                "        cat$errorDesc = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.parseIBEClientException(e);\n" +
//                "    " + PrometheusUtils.exceptionIBEClient +
                "        throw e;\n" +
                "     } finally {\n" +
                "         if (cat$Transaction != null) {\n" +
                "             String cat$tid = $0.getTxnTraceKey();\n" +
                "             cat$Transaction.addData(\"TID=\".concat(cat$tid));\n" +

                // 记录 socket 连接耗时
                "             long cat$socketConnectCost = $0.socketConnectedTime - cat$startTime;\n" +
                "             String cat$socketConnectMillis = String.valueOf(cat$socketConnectCost);\n" +
                "             cat$Transaction.addData(\"socketConnectMillis=\".concat(cat$socketConnectMillis));\n" +

                // 记录 handshake 耗时
                "             long cat$handShakingCost = $0.handShakingSentTime - $0.handShakingPrepareTime;\n" +
                "             String cat$handShakingMillis = String.valueOf(cat$handShakingCost);\n" +
                "             cat$Transaction.addData(\"handShakingMillis=\".concat(cat$handShakingMillis));\n" +

                // 记录 errorDesc，errorDesc 必须记录在最后面
                "             if (cat$errorDesc != null && cat$errorDesc.length() != 0) {\n" +
                "                 cat$Transaction.addData(\"errorDesc=\".concat(cat$errorDesc));\n" +
                "             }\n" +

                "         }\n" +
                "    " + CatUtils.completeTransaction +
//                "    " + PrometheusUtils.completeTimer +
                "     }\n" +
                "}\n";

        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(oldMethod) + " of class " + clazz.getName() + ": " + code);
    }
}
