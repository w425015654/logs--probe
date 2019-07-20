package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.*;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.lang.reflect.Modifier;


/**
 * 修改 SPMSupplierThread、SPMIntermediaryThread 的具体实现
 * 1. 在 notify 之前，取出 Main 线程的 MDC 中的 telassa-id 值
 * 2. 在 wait 之后，将 telassa-id 的值传递到 SupplierThread 或者 IntermediaryThread中
 */
public class SPMNodeThread implements Transformlet {
    private static final Logger logger = Logger.getLogger(SPMNodeThread.class);

    @Override
    public boolean needTransform(String className) {
        return ("com.openjaw.serviceProvider.SPMIntermediaryThread".equals(className) ||
                "com.openjaw.serviceProvider.SPMSupplierThread".equals(className));
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        CatForkUtils.addTelassaField(clazz, logger);
        CatForkUtils.addCatField(clazz, logger);

        // 更新 processMessage 函数, 在 notify 之前:
        // 1. 取出 Main 线程的 MDC 中的 telassa-id 值, 并设置到成员变量 TELASSA_ID_FIELD_NAME 里
        // 2. 设置 CAT 信息
        CatForkUtils.doCaptureThreadContext(clazz, "processMessage", "notify", logger);

        // 这两步主要是为了添加 Prometheus 线程池监控，跟 CAT 没有关系
        this.updateProcess(clazz);// 这个要放在前面，先修改了 process 的函数体，下一步再重命名
        this.renameProcess(clazz);


        // 更新 run 函数
        // 1. 在 waitForRequest 之后重放 TELASSA_ID_FIELD_NAME 的值到当前进程的 MDC 中, 并 log trans 开始
        // 2. 在 responseReceived 之后,设置 CAT 结束
        CatForkUtils.doReplayThreadContext(clazz, "run",
                "waitForRequest",
                "{\n$proceed($$);\n" + CatForkUtils.replayThreadContext + "}\n",
                "responseReceived",
                "{\n$_ = $proceed($$);\n" + CatForkUtils.completeForkedTransaction + "}\n",
                logger);
    }


    /**
     * 更新 process 函数, 在 incActiveThreads 之后:
     * 1. 加入 Prometheus 统计信息
     *
     * @param clazz
     * @throws NotFoundException
     */
    private void updateProcess(CtClass clazz) throws NotFoundException {
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod("process");

        String getStats = "    com.openjaw.serviceProvider.SPMNodeStats prometheus$stats = null;\n";
        String getType = "    String prometheus$SPMNodeType = null;\n";
        String getCode = "    String prometheus$SPMNodeCode = null;\n";

        if (method.getLongName().contains("SPMIntermediaryThread")) {
            getStats += "    prometheus$stats = intermediary.getStats();\n\n";

            getType += "    prometheus$SPMNodeType = intermediary.getType();\n\n";
            getCode += "    prometheus$SPMNodeCode = intermediary.getCode();\n\n";
        } else {
            getStats += "    prometheus$stats = supplier.getStats();\n\n";

            getType += "    prometheus$SPMNodeType = supplier.getType();\n\n";
            getCode += "    prometheus$SPMNodeCode = supplier.getCode();\n\n";
        }


        // 在调用 incActiveThreads 之后输出线程池信息
        final String code = "\n" +
                "{\n" +
                "$proceed($$);\n" +
                getStats +
                getType +
                getCode +
                PrometheusUtils.gaugeOJSupplierThread +     // 线程执行时的线程池状态
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


    /**
     * 重命名 process 函数，在函数之后加入 Prometheus 监控信息
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
        oldMethod.setModifiers(oldMethod.getModifiers() & ~java.lang.reflect.Modifier.PROTECTED /* remove protected */ | Modifier.PRIVATE /* add private */);

        // 添加 Prometheus 监控
        String getStats = "    com.openjaw.serviceProvider.SPMNodeStats prometheus$stats = null;\n";
        String getType = "    String prometheus$SPMNodeType = null;\n";
        String getCode = "    String prometheus$SPMNodeCode = null;\n";

        if (clazz.getName().contains("SPMIntermediaryThread")) {
            getStats += "    prometheus$stats = intermediary.getStats();\n\n";

            getType += "    prometheus$SPMNodeType = intermediary.getType();\n\n";
            getCode += "    prometheus$SPMNodeCode = intermediary.getCode();\n\n";
        } else {
            getStats += "    prometheus$stats = supplier.getStats();\n\n";

            getType += "    prometheus$SPMNodeType = supplier.getType();\n\n";
            getCode += "    prometheus$SPMNodeCode = supplier.getCode();\n\n";
        }


        // 在调用 process 之后加入 Prometheus 监控信息
        final String code = "\n{\n" +
                "    try { \n" +
                "        com.openjaw.messaging.ResponseMessageWrapper result = " + oldMethodRename + "($$);\n" +
                "        return result;\n" +
                "    } catch (Exception e) {\n" +
                "        throw e;\n" +
                "    } finally {\n" +
                getStats +
                getType +
                getCode +
                "    " + PrometheusUtils.gaugeOJSupplierThread +    // 线程执行后的线程池状态
                "     }\n" +
                "}\n";

        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(oldMethod) + " of class " + clazz.getName() + ": " + code);

    }

}
