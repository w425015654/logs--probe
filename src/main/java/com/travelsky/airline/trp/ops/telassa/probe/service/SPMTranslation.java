package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

import java.lang.reflect.Modifier;

/**
 * 修改 SPMTranslation 的具体实现
 * 这里先添加 CAT 日志，后续添加 Telassa
 *
 * @author zengfan
 */
public class SPMTranslation implements Transformlet {
    private static final Logger logger = Logger.getLogger(SPMTranslation.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjaw.serviceProvider.SPMTranslation".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.updateProcess(clazz);
    }

    /**
     * 更新 com.openjaw.serviceProvider.SPMTranslation#process 函数
     *
     * @param clazz
     */
    private void updateProcess(CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 process 函数
        final String query = "process";
        final CtMethod oldMethod = clazz.getDeclaredMethod(query);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, query, clazz, null);

        // 将原来的 process 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$process$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~Modifier.PUBLIC /* remove public */ | Modifier.PRIVATE /* add private */);


        // 基于 OTA_AirLowFareSearchRQ 重置上层 Transaction
        // 这里处理的肯定是官网过来的请求，OTA 过来的请求在 SimpleHTTPServlet 中处理
        String renameSearchRQCode = "\n" +
                "    com.dianping.cat.message.internal.DefaultTransaction cat$peekTransaction = null;\n" +
                "    try { \n" +
                "        if ($1 != null && $1 instanceof com.openjaw.messaging.MessageWrapper) {\n" +
                "            com.openjaw.messaging.MessageWrapper cat$message = (com.openjaw.messaging.MessageWrapper)$1;\n" +
                "            org.w3c.dom.Node cat$node = cat$message.getDocument();\n" +

                "            String cat$shoppingLevel = \"" + CatUtils.TYPE_SHOPPING_2 + "\";\n" +
                CatUtils.initSearchRQType +
                CatUtils.renameSearchRQTransaction +
                "        }\n" +
                // 发生任何异常都不处理，不能影响主流程
                "    } catch (Exception e) {}\n";

        // 在调用 process 前后添加 CAT 控制
        final String code = "\n{\n" +
                renameSearchRQCode +
                "    String cat$TagName = $1.getTagName();\n" +
                "    " + CatUtils.newSPMTranslationTransaction +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中
                "    String telassa$Id = (String)org.apache.log4j.MDC.get(\"telassa-id\");\n" +
                "    String oj$sourceName = null;\n" +
                "    " + TelassaUtils.initServiceTelassaId +
                "    try { \n" +
                "        com.openjaw.messaging.ResponseMessageWrapper result = " + oldMethodRename + "($$);\n" +

                // 下级调用的 SPMClientThread 已经按 xml 内容进行判断了，这里无需再一次判断
                "    " + CatUtils.successTransaction +
                "        return result;\n" +
                "    } catch (Exception e) {\n" +
                "    " + CatUtils.catchException +
                "        throw e;\n" +
                "    } finally {\n" +
                "    " + CatUtils.completeTransaction +
                "     }\n" +
                "}\n";

        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(newMethod) + " of class " + clazz.getName() + ": " + code);

    }

}
