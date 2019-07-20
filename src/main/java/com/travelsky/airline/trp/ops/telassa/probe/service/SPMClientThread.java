package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.lang.reflect.Modifier;

/**
 * 修改 SPMClientThread 的具体实现
 * 这里先添加 CAT 日志，后续添加 Telassa
 *
 * @author zengfan
 */
public class SPMClientThread implements Transformlet {
    private static final Logger logger = Logger.getLogger(SPMClientThread.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjaw.serviceProvider.SPMClientThread".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.modifyProcess(clazz);  // 这个要放在前面，先修改了 process 的函数体，下一步再重命名
        this.renameProcess(clazz);
    }

    /**
     * 更新 com.openjaw.serviceProvider.SPMClientThread#process 函数
     *
     * @param clazz
     */
    private void renameProcess(CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 process 函数
        final String query = "process";
        final CtMethod oldMethod = clazz.getDeclaredMethod(query);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, query, clazz, null);

        // 将原来的 process 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$process$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~Modifier.PROTECTED /* remove protected */ | Modifier.PRIVATE /* add private */);

        // 添加 Prometheus 监控
        String prometheusVariable = "    com.openjaw.serviceProvider.SPMNodeStats prometheus$stats = ((com.openjaw.serviceProvider.SPMClient)($0.getNode())).getStats();\n" +
                "    String prometheus$SPMNodeType = $0.getNode().getType();\n" +
                "    String prometheus$SPMNodeCode = $0.getNode().getCode();\n";

        // 在调用 process 前后添加 CAT 控制
        final String code = "\n{\n" +
                "    String cat$TagName = $2.getTagName();\n" +
                "    " + CatUtils.newSPMClientThreadTransaction +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中
                "    String telassa$Id = (String)org.apache.log4j.MDC.get(\"telassa-id\");\n" +
                "    String oj$sourceName = null;\n" +
                "    " + TelassaUtils.initServiceTelassaId +
                "    " + prometheusVariable +
                "    try { \n" +
                "    " + PrometheusUtils.gaugeOJClientThread +  // 线程执行之前的线程池状态
                "        com.openjaw.messaging.ResponseMessageWrapper result = " + oldMethodRename + "($$);\n" +

                // 通过判断是否有 Error 节点来判断 RQ 是否执行成功
                "        String cat$statusCode = com.dianping.cat.message.Message.SUCCESS;\n" +
                "        String cat$errorDesc = null;\n" +
                "        org.w3c.dom.Document cat$document = null;\n" +
                "        String cat$responseSource = null;\n" +

                "        if (result != null) {" +
                "            cat$document = result.getDocument();\n" +
                "            cat$responseSource = result.getSource();\n" +
                "        }\n" +

                "        String oj$clientCode = null;\n" +
                "        if ($1 != null) {\n" +
                "            oj$clientCode = $1.getCode();\n" +
                "        }" +

                "    " + CatUtils.setStatusCodeByDocument +
                "    " + CatUtils.addResponseSourceTransaction +
                "    " + CatUtils.addClientCodeTransaction +
                "    " + CatUtils.addErrorDescTransaction +

                // 发生错误时，如果不是 OTA_AirLowFareSearchRQ, 则输出 RQ 信息
                "    if(cat$Transaction != null && cat$errorDesc != null && cat$errorDesc.length() != 0 && $2 != null){\n" +
                "        org.w3c.dom.Document cat$RQDocument = ((com.openjaw.messaging.MessageWrapper)$2).getDocument();\n" +
                "        String cat$RQ = \"\\n\\r\" + com.travelsky.airline.trp.ops.telassa.probe.util.DomUtils.nodeToString(cat$RQDocument, cat$TagName);\n" +
                "        cat$Transaction.addData(\"RQ=\".concat(cat$RQ));\n" +
                "    }\n" +

                "    " + CatUtils.statusTransaction +
                "        return result;\n" +
                "    } catch (Exception e) {\n" +
                "    " + CatUtils.catchException +
                "        throw e;\n" +
                "    } finally {\n" +
                "    " + CatUtils.completeTransaction +
                "    " + PrometheusUtils.gaugeOJClientThread + // 线程执行之后的线程池状态
                "     }\n" +
                "}\n";

        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(oldMethod) + " of class " + clazz.getName() + ": " + code);

    }

    /**
     * 更新 process 函数, 在 incActiveThreads 之后:
     * 1. 加入 Prometheus 统计信息
     *
     * @param clazz
     * @throws NotFoundException
     */
    private void modifyProcess(CtClass clazz) throws NotFoundException {
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod("process");

        // 添加 Prometheus 监控
        String prometheusVariable = "    com.openjaw.serviceProvider.SPMNodeStats prometheus$stats = ((com.openjaw.serviceProvider.SPMClient)($0.getNode())).getStats();\n" +
                "    String prometheus$SPMNodeType = $0.getNode().getType();\n" +
                "    String prometheus$SPMNodeCode = $0.getNode().getCode();\n";

        // 在调用 send 前后输出线程池信息
        final String code = "\n" +
                "{\n" +
                "$proceed($$);\n" +
                prometheusVariable +
                PrometheusUtils.gaugeOJClientThread +   // 线程执行时的线程池状态
                "}\n";

        try {
            // 替换代码
            method.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equalsIgnoreCase("incActiveThreads")) {
                        m.replace(code);

                        logger.info("Instrument lines after incActiveThreads() in " + method.getName() + " of class "
                                + method.getDeclaringClass().getName() + ": " + code);
                    }
                }
            });

        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }

}
