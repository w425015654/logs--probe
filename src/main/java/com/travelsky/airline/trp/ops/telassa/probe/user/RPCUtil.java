package com.travelsky.airline.trp.ops.telassa.probe.user;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

/**
 * 修改 org.apache.axis2.rpc.receivers.RPCUtil#invokeServiceClass 的具体实现, 用以输出具体调用的 UserService 函数
 */
public class RPCUtil implements Transformlet {
    private static final Logger logger = Logger.getLogger(RPCUtil.class);

    @Override
    public boolean needTransform(String className) {
        return "org.apache.axis2.rpc.receivers.RPCUtil".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.updateInvokeServiceClass(clazz);
    }


    /**
     * 更新 invokeServiceClass 函数
     *
     * @param clazz
     */
    private void updateInvokeServiceClass(CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 invokeServiceClass 函数
        final String doFilter = "invokeServiceClass";
        final CtMethod oldMethod = clazz.getDeclaredMethod(doFilter);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, doFilter, clazz, null);

        // 将原来的 invokeServiceClass 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$invokeServiceClass$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);

        final String code = "{\n" +
                "    String method$name = ((java.lang.reflect.Method)$2).getName();\n" +
                "    if (method$name == null || method$name.length() == 0) {\n" +
                "        method$name = \"NVL\";\n" +
                "    }\n\n" +

                "    String cat$type = \"" + CatUtils.TYPE_RPC_INVOKE + CatUtils.CHANNEL_NVL + "\";\n" +
                // org.apache.log4j.MDC 取不到时就从 org.jboss.logging.MDC 中取
                "    String telassa$channel = (String)org.apache.log4j.MDC.get(\"telassa-channel\");\n" +
                "    if (telassa$channel == null || telassa$channel.length() == 0) {\n" +
                "        telassa$channel = (String)org.jboss.logging.MDC.get(\"telassa-channel\");\n" +
                "    }\n" +
                "    if (telassa$channel != null && telassa$channel.length() != 0) {\n" +
                "        cat$type = \"" + CatUtils.TYPE_RPC_INVOKE + "\" + telassa$channel;\n" +
                "    }\n" +

                CatUtils.newRpcInvokeTransaction +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中
                "    try{\n" +
                "        java.lang.Object result = " + oldMethodRename + "($$);\n" +
                "        " + CatUtils.successTransaction +
                "        return result;\n" +
                "    } catch (Exception e) {\n" +
                "    " + CatUtils.catchException +
                "        throw e;\n" +
                "    } finally {\n" +
                CatUtils.completeTransaction +
                "    }\n" +
                "}\n";


        try {
            newMethod.setBody(code);
            clazz.addMethod(newMethod);
            logger.info("insert code around method " + Utils.signatureOfMethod(oldMethod) + " of class " + clazz.getName() + ": " + code);
        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }
}
