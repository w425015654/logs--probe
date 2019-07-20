package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusUtils;
import javassist.*;

/**
 * 修改 com.openjaw.serviceProvider.SPMSupplierBase 的具体实现
 * 在 send 前后输出线程池信息
 * 注意不应该仅仅在 com.openjaw.serviceProvider.SPMClientThread#process 或者 com.openjaw.serviceProvider.SPMIntermediaryThread#process 里输出，这已经是真正正在执行的过程了，在里面取，当线程都卡住时，就取不到值了
 *
 * @author zengfan
 */
public class SPMSupplierBase implements Transformlet {
    private static final Logger logger = Logger.getLogger(SPMSupplierBase.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjaw.serviceProvider.SPMIntermediary".equals(className) ||
                "com.openjaw.serviceProvider.SPMSupplier".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws CannotCompileException {
        this.makeSend(clazz);
    }

    /**
     * 新增 com.openjaw.serviceProvider.SPMIntermediary#send 和 com.openjaw.serviceProvider.SPMSupplier#send 函数
     * 因为 com.openjaw.serviceProvider.SPMSupplierBase#send 函数是子类的函数，修改起来会有问题
     *
     * @param clazz
     */
    private void makeSend(CtClass clazz) throws CannotCompileException {

        String prometheusVariable = "    com.openjaw.serviceProvider.SPMNodeStats prometheus$stats = $0.stats;\n" +
                "    String prometheus$SPMNodeType = $0.getType();\n" +
                "    String prometheus$SPMNodeCode = $0.getCode();\n\n";

        // 在调用 send 之前输出线程池信息
        final String code = "\n" +
                "public boolean send(com.openjaw.serviceProvider.SPMProcessingState state, com.openjaw.messaging.ServiceProviderRequest request, com.openjaw.debugger.DebugSwitching switching) {\n" +
                prometheusVariable +
                "    try { \n" +
                PrometheusUtils.gaugeOJSupplierThread +     // 线程执行前的线程池状态，这个必须在取 Supplier、Intermediary 之前获取
                "        boolean result = super.send($$);\n" +
                "        return result;\n" +
                "    } catch (Exception e) {\n" +
                "        throw e;\n" +
                "    } finally {\n" +
                //  send 之后线程会立即返回，supplier 或者 Intermediary 线程并未执行完，所以这里取线程池信息也无意义
//                PrometheusUtils.gaugeOJSupplierThread +
                "     }\n" +
                "}\n";
        CtMethod m = CtNewMethod.make(code, clazz);
        clazz.addMethod(m);
        logger.info("add new method send of class " + clazz.getName() + ": " + code);
    }
}
