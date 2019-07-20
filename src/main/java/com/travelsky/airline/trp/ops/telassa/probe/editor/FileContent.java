package com.travelsky.airline.trp.ops.telassa.probe.editor;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

/**
 * 修改 com.openjaw.tables.file.FileContent 的具体实现
 * 1. 在 com.openjaw.tables.file.FileContent#updateRow 前加入日志，输出修改前后的值
 */
public class FileContent implements Transformlet {
    private static final Logger logger = Logger.getLogger(FileContent.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjaw.tables.file.FileContent".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.updateUpdateRow(clazz);
    }


    /**
     * 更新 updateRow 函数
     * 直接在函数最前面插入代码
     *
     * @param clazz
     */
    private void updateUpdateRow(CtClass clazz) throws NotFoundException, CannotCompileException {
        // 实例化类型池对象
//        ClassPool classPool = ClassPool.getDefault();

        // 复制 transform 函数
//        CtClass[] paramTypes = {
//                classPool.get("com.openjaw.tables.RowHashMap"),
//                classPool.get("java.util.Map"),
//                classPool.get("java.lang.Boolean")};

        // 取到函数，这里有 3 个 updateRow 函数，不一定取到哪一个。。
        final CtMethod method = clazz.getDeclaredMethod("updateRow");


        // 在调用 info 之前, 判断如果是指定的 Logger，则进行解析并输出一个 com.dianping.cat.message.Transaction
        final String code = "\n{\n" +
                "    try { \n" +
                "        String oldData = ((com.openjaw.tables.RowHashMap)$1).toString();\n" +
                "        String newData = ((com.openjaw.tables.RowHashMap)$2).toString();\n" +

                "        com.openjaw.tables.TableBase table = (com.openjaw.tables.TableBase)($0.getOwner());\n" +
//                "        String userName = table.getUsername();\n" +
//                "        String address = table.getAddress();\n" +
                "        String type = table.getType();\n" +
                "        String code = table.getCode();\n" +
                "        String name = table.getName();\n" +
                "        com.travelsky.airline.trp.ops.telassa.probe.util.LogUtils.editorAudit(type, code, name, oldData, newData, $3);\n" +
                "    } catch (Exception e) {\n" +
                // 发生任何异常都不处理，不能影响主流程
                "    } finally {\n" +
                "    }\n" +
                "}\n";


        method.insertBefore(code);
        logger.info("insert code before method " + Utils.signatureOfMethod(method) + " of class " + clazz.getName() + ": " + code);
    }
}
