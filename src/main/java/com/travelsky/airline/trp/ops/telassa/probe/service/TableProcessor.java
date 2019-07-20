package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;


/**
 * 修改 TableProcessor 的具体实现
 * 1. 在 advancedUpdate、insertSelectDB、deleteFromDB、callStoredProc、selectFromDB、updateOrInsert 前后加上日志输出
 */
public class TableProcessor implements Transformlet {
    private static final Logger logger = Logger.getLogger(TableProcessor.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjaw.connection.jdbc.TableProcessor".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException {
        this.updateProcessTables(clazz, "advancedUpdate");
        this.updateProcessTables(clazz, "insertSelectDB");
        this.updateProcessTables(clazz, "deleteFromDB");
        this.updateProcessTables(clazz, "callStoredProc");
        this.updateProcessTables(clazz, "selectFromDB");
        this.updateProcessTables(clazz, "updateOrInsert");
    }


    /**
     * @param clazz
     * @throws NotFoundException
     */
    private void updateProcessTables(final CtClass clazz, final String subMethod) throws NotFoundException {
        final CtMethod method = clazz.getDeclaredMethod("processTables");

        // 在调用 子函数执行 前后报上 trans
        final String code = "\n{\n" +
                "    String cat$Name = \"" + subMethod + "\" + \"(\" + $0.currentTable.getAttribute(\"name\") + \")\";\n" +
                "    " + CatUtils.newTableProcessorTransaction +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中
                "    try { \n" +
                "        $_ = $proceed($$);\n" +
                "    " + CatUtils.successTransaction +
//                "        return $_;\n" +            // 这里不需要显示写 return $_;
                "    } catch (Exception e) {\n" +
                "    " + CatUtils.catchException +
                "        throw e;\n" +
                "    } finally {\n" +
                "    " + CatUtils.completeTransaction +
                "     }\n" +
                "}\n";


        try {

            method.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equalsIgnoreCase(subMethod)) {
                        m.replace(code);

                        logger.info("Instrument lines around " + subMethod + "() in " + method.getName() + " of class "
                                + method.getDeclaringClass().getName() + ": " + code);
                    }
                }
            });
        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }

}
