package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

import java.lang.reflect.Modifier;


/**
 * 修改 com.openjaw.connection.smtp.SMTPEmail 的具体实现
 */
public class SMTPEmail implements Transformlet {
    private static final Logger logger = Logger.getLogger(SMTPEmail.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjaw.connection.smtp.SMTPEmail".equals(className);
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
        // 复制 send 函数
        final CtMethod oldMethod = clazz.getDeclaredMethod("send");
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, "send", clazz, null);

        // 将原来的 send 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$send$method$renamed$by$probe";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~Modifier.PUBLIC /* remove public */ | Modifier.PRIVATE /* add private */);

        // 在调用 send 前后添加 CAT 控制
        final String code = "\n\n{\n" +
                "    com.sun.mail.smtp.SMTPMessage cat$message = $0.message;\n" +
                "    String subject$name = \"EMPTY\";\n" +
                "    if (cat$message != null) {\n" +
                "        subject$name = cat$message.getSubject();\n" +
                "    }\n" +
                // 中文标题在 CAT 里会被转 Base64 编码，所以这里先转拼音
                "    subject$name = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.toPinyin(subject$name);\n" +
                "    " + CatUtils.newSMTPEmailTransaction +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中
                "    try { \n" +
                "        if (cat$message != null) {\n" +
                "            javax.mail.Address[] cat$address = cat$message.getAllRecipients();" +
                "            String cat$recipients = \"\";\n" +
                "            for (int i = 0; i < cat$address.length; i++) {\n" +
                "                 cat$recipients = cat$recipients.concat(cat$address[i].toString()).concat(\";\");\n" +
                "            }\n" +
                "            cat$Transaction.addData(\"subject=\".concat(cat$message.getSubject()));\n" +
                "            cat$Transaction.addData(\"recipients=\".concat(cat$recipients));\n" +
                "        }\n\n" +
                "        " + oldMethodRename + "($$);\n" +
                "        " + CatUtils.successTransaction +
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
