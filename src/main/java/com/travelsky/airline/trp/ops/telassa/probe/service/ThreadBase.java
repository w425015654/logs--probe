package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatForkUtils;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;


/**
 * 修改 com.openjaw.connection.ThreadBase 的具体实现
 * 1. 在 notify 之前，取出 Main 线程的 MDC 中的 telassa-id 值
 * 2. 在 process 之前，将 telassa-id 的值传递到子线程中
 */
public class ThreadBase implements Transformlet {
    private static final Logger logger = Logger.getLogger(ThreadBase.class);

    @Override
    public boolean needTransform(String className) {
        return ("com.openjaw.connection.ThreadBase".equals(className));
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        CatForkUtils.addTelassaField(clazz, logger);
        CatForkUtils.addCatField(clazz, logger);

        // 更新 wakeup 函数, 在 notify 之前:
        // 1. 取出 Main 线程的 MDC 中的 telassa-id 值, 并设置到成员变量 TELASSA_ID_FIELD_NAME 里
        // 2. 设置 CAT 信息
        CatForkUtils.doCaptureThreadContext(clazz, "wakeup", "notify", logger);

        // 更新 run 函数
        // 1. 在 process 之前重放 TELASSA_ID_FIELD_NAME 的值到当前进程的 MDC 中, 这个跟 SPMNodeThread 里不一样，SPMNodeThread 是在 waitForRequest 之后
        // 2. 在 receivedResponse 之前, 设置 CAT 结束，这个跟 SPMNodeThread 里不一样，SPMNodeThread 是在 responseReceived 之后
        CatForkUtils.doReplayThreadContext(clazz, "run",
                "process", "{\n" + CatForkUtils.replayThreadContext + "\n$_ = $proceed($$);\n}\n",
                "receivedResponse", "{\n" + CatForkUtils.completeForkedTransaction + "\n$proceed($$);\n}\n",
                logger);
    }
}
