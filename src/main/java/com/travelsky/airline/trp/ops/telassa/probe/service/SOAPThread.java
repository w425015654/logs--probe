package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

import java.lang.reflect.Modifier;


/**
 * 修改 com.openjaw.connection.soap.SOAPThread 的具体实现
 */
public class SOAPThread implements Transformlet {
    private static final Logger logger = Logger.getLogger(SOAPThread.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjaw.connection.soap.SOAPThread".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.updateProcess(clazz);
    }


    /**
     * @param clazz
     * @throws NotFoundException
     */
    private void updateProcess(final CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 process 函数
        final CtMethod oldMethod = clazz.getDeclaredMethod("process");
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, "process", clazz, null);

        // 将原来的 query 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$process$method$renamed$by$probe";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~Modifier.PROTECTED /* remove public */ | Modifier.PRIVATE /* add private */);

        // 在调用 process 前后添加 CAT 控制
        final String code = "\n\n{\n" +
                "    String local$name = \"NVL\";\n" +
                "    if ($1 != null) {\n" +
                "        org.w3c.dom.Document cat$dom = (org.w3c.dom.Document)$1;\n" +
                "        org.w3c.dom.Element cat$root = cat$dom.getDocumentElement();\n" +
                "        local$name = String.valueOf(com.openjaw.utils.DOMUtilities.getLocalName(cat$root));\n" +
                "    }\n" +
                "    " + CatUtils.newSOAPThreadTransaction +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中
                "    try { \n" +
                "        java.lang.Object result = " + oldMethodRename + "($$);\n" +
                "        " + CatUtils.successTransaction +
                "        return result;\n" +
                "    } catch (Exception e) {\n" +
                "    " + CatUtils.catchException +
                "        throw e;\n" +
                "     } finally {\n" +
                "    " + CatUtils.completeTransaction +
                "     }\n" +
                "}\n";

        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(oldMethod) + " of class " + clazz.getName() + ": " + code);

    }

}
