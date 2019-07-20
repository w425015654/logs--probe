package com.travelsky.airline.trp.ops.telassa.probe.ibe;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.lang.reflect.Modifier;
import java.util.Hashtable;
import java.util.Properties;

/**
 * 修改 com.openjaw.utils.TemplateCache 的具体实现, 记录所有转换的 XSL
 * 1. 在 com.openjaw.utils.TemplateCache#transform(javax.xml.transform.Source, javax.xml.transform.Result, java.lang.String, java.util.Hashtable, java.util.Properties) 前后包裹一层 CAT Transaction
 */
public class TemplateCache implements Transformlet {
    private static final Logger logger = Logger.getLogger(TemplateCache.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjaw.utils.TemplateCache".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.updateTransform(clazz);
    }


    /**
     * 更新 transform 函数
     *
     * @param clazz
     */
    private void updateTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        // 实例化类型池对象
        ClassPool classPool = ClassPool.getDefault();

        // 复制 transform 函数
        CtClass[] paramTypes = {
                classPool.get(Source.class.getName()),
                classPool.get(Result.class.getName()),
                classPool.get(String.class.getName()),
                classPool.get(Hashtable.class.getName()),
                classPool.get(Properties.class.getName())};

        // 取到函数
        final CtMethod oldMethod = clazz.getDeclaredMethod("transform", paramTypes);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, "transform", clazz, null);

        // 将原来的 transform 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$transform$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~java.lang.reflect.Modifier.PUBLIC /* remove public */ | Modifier.PRIVATE /* add private */);

        final String code = "{\n" +
                "    String cat$name = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.getXlsTransformName($3);\n" +
                "    com.dianping.cat.message.Transaction cat$Transaction = com.dianping.cat.Cat.newTransaction(\"" + CatUtils.TYPE_XSL_TRANSFORM + "\", cat$name);\n" +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中
                "    if (cat$Transaction != null) {\n" +
                "        cat$Transaction.addData(\"xls=\".concat($3));\n" +
                // $1 第一个参数 xRezSource 里的 param 是 private 变量，取不到
//                "        if ($1 != null) {\n" +
//                "            com.openjawx.xRez.sax.xRezSource source = (com.openjawx.xRez.sax.xRezSource)$1;" +
//                "            java.util.Map sourceParam = source.get" +
//                "            cat$Transaction.addData(\"sourceParam=\".concat($3));\n" +
//                "        }\n" +
                "        if ($4 != null) {\n" +
                "            cat$Transaction.addData(\"params=\".concat($4.toString()));\n" +
                "        }\n" +
                "    }\n" +
                "    try{\n" +
                "        " + oldMethodRename + "($$);\n" +
                "    " + CatUtils.successTransaction +
                "    } catch (Exception e) {\n" +
                "    " + CatUtils.catchException +
                "        throw e;\n" +
                "    } finally {\n" +
                "    " + CatUtils.completeTransaction +
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
