package com.travelsky.airline.trp.ops.telassa.probe.ibe;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * 修改 org.apache.xalan.xsltc.compiler.Parser 的具体实现, 记录报错的信息
 * 1. 在 org.apache.xalan.xsltc.compiler.Parser#reportError 里输出一个 CAT Event
 */
public class Parser implements Transformlet {
    private static final Logger logger = Logger.getLogger(Parser.class);

    @Override
    public boolean needTransform(String className) {
        return "org.apache.xalan.xsltc.compiler.Parser".equals(className);
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
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod("reportError");


        String code = "\n" +
                "if ($2 != null && $2 instanceof org.apache.xalan.xsltc.compiler.util.ErrorMsg) {\n" +
                "    org.apache.xalan.xsltc.compiler.util.ErrorMsg cat$errorMsg = (org.apache.xalan.xsltc.compiler.util.ErrorMsg)$2;\n" +
                "    String cat$name = \"ParserException\";\n" +
                "    String cat$errorCode = \"ERROR\";\n" +
                "    if (cat$errorMsg.isWarningError()) {\n" +
                "        cat$errorCode = \"WARNING\";\n" +
                "    }\n" +
                "    String cat$eventDesc = cat$errorMsg.toString();\n" +
                CatUtils.logXslExceptEvent +
                "}\n";


        try {
            // 最前面插入代码
            method.insertBefore(code);
            logger.info("Insert code before " + method.getName() + " of class "
                    + method.getDeclaringClass().getName() + ": " + code);
        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }
}
