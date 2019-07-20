package com.travelsky.airline.trp.ops.telassa.probe.ibe;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import javassist.*;

/**
 * 修改 com.openjaw.struts.HTTPPost  的具体实现，记录所有页面错误跳转
 * 1. 修改 com.openjaw.struts.HTTPPost#addErrorWithParam(java.lang.String, java.lang.String, java.lang.String) 的具体实现，设置当前 Transaction 为 ERROR
 * 2. 修改 com.openjaw.struts.HTTPPost#addFormError(java.lang.String, java.lang.String, java.lang.String, java.lang.String) 的具体实现，设置当前 Transaction 为 ERROR
 * 3. 修改 com.openjaw.struts.HTTPPost#addFieldError(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String) 的具体实现，设置当前 Transaction 为 ERROR
 * 4. 修改 com.openjaw.struts.HTTPPost#addValueNoUpdate(java.lang.String, java.lang.String) 的具体实现，在第一个参数为 ShowError 时输出错误信息
 */
public class HTTPPost implements Transformlet {
    private static final Logger logger = Logger.getLogger(HTTPPost.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjaw.struts.HTTPPost".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException {
        this.updateAddErrorWithParam(clazz);
        this.updateAddFormError(clazz);
        this.updateAddFieldError(clazz);
        this.updateAddValueNoUpdate(clazz);
    }


    /**
     * 更新 addErrorWithParam 函数
     *
     * @param clazz
     */
    private void updateAddErrorWithParam(CtClass clazz) throws NotFoundException {
        // 实例化类型池对象
        ClassPool classPool = ClassPool.getDefault();
        // 获取 3 个 String 类型参数集合
        CtClass[] paramTypes = {classPool.get(String.class.getName()), classPool.get(String.class.getName()), classPool.get(String.class.getName())};
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod("addErrorWithParam", paramTypes);

        String code = "\n" +
                "String cat$errorType = \"ParamError\";\n" +
                "String cat$errorDesc = " +
                "\"errorName=\" + String.valueOf($1) + \", \" + " +
                "\"param1=\" + String.valueOf($2) + \", \" + " +
                "\"param2=\" + String.valueOf($3);\n" +
                CatUtils.setHttpPostTransactionError +
                CatUtils.logFormValidationEvent;

        try {
            // 最前面插入代码
            method.insertBefore(code);
            logger.info("Insert code before " + method.getName() + " of class "
                    + method.getDeclaringClass().getName() + ": " + code);
        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * 更新 addFormError 函数
     *
     * @param clazz
     */
    private void updateAddFormError(CtClass clazz) throws NotFoundException {
        // 实例化类型池对象
        ClassPool classPool = ClassPool.getDefault();
        // 获取 4 个 String 类型参数集合
        CtClass[] paramTypes = {classPool.get(String.class.getName()), classPool.get(String.class.getName()), classPool.get(String.class.getName()), classPool.get(String.class.getName())};
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod("addFormError", paramTypes);

//        在报错的地方直接输出调用堆栈，是不是更好
//                或者，发现底层被置 error 了，就在 .do 的入口把参数带出来

        String code = "\n" +
                "String cat$errorType = \"FormError\";\n" +
                "String cat$errorDesc = " +
                "\"validator=\" + String.valueOf($1) + \", \" + " +
                "\"form=\" + String.valueOf($2) + \", \" + " +
                "\"param1=\" + String.valueOf($3) + \", \" + " +
                "\"param2=\" + String.valueOf($4);\n" +
                CatUtils.setHttpPostTransactionError +
                CatUtils.logFormValidationEvent;

        try {
            // 最前面插入代码
            method.insertBefore(code);
            logger.info("Insert code before " + method.getName() + " of class "
                    + method.getDeclaringClass().getName() + ": " + code);
        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * 更新 addFieldError 函数
     *
     * @param clazz
     */
    private void updateAddFieldError(CtClass clazz) throws NotFoundException {
        // 实例化类型池对象
        ClassPool classPool = ClassPool.getDefault();
        // 获取 5 个 String 类型参数集合
        CtClass[] paramTypes = {classPool.get(String.class.getName()), classPool.get(String.class.getName()), classPool.get(String.class.getName()), classPool.get(String.class.getName()), classPool.get(String.class.getName())};
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod("addFieldError", paramTypes);

        String code = "\n" +
                "String cat$errorType = \"FieldError\";\n" +
                "String cat$errorDesc = " +
                "\"validator=\" + String.valueOf($1) + \", \" + " +
                "\"form=\" + String.valueOf($2) + \", \" + " +
                "\"field=\" + String.valueOf($3) + \", \" + " +
                "\"param1=\" + String.valueOf($4) + \", \" + " +
                "\"param2=\" + String.valueOf($5);\n" +
                CatUtils.setHttpPostTransactionError +
                CatUtils.logFormValidationEvent;

        try {
            // 最前面插入代码
            method.insertBefore(code);
            logger.info("Insert code before " + method.getName() + " of class "
                    + method.getDeclaringClass().getName() + ": " + code);
        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }


    /**
     * 更新 addValueNoUpdate 函数
     *
     * @param clazz
     */
    private void updateAddValueNoUpdate(CtClass clazz) throws NotFoundException {
        // 实例化类型池对象
        ClassPool classPool = ClassPool.getDefault();
        // 获取 2 个 String 类型参数集合
        CtClass[] paramTypes = {classPool.get(String.class.getName()), classPool.get(String.class.getName())};
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod("addValueNoUpdate", paramTypes);

        String code = "\n" +
                "if (\"ShowError\".equalsIgnoreCase($1)) {\n" +
                "    String cat$errorType = \"IbeFormError\";\n" +
                "    String cat$errorDesc = " +
                "    \"name=\" + String.valueOf($1) + \", \" + " +
                "    \"value=\" + String.valueOf($2);\n" +

                CatUtils.setHttpPostTransactionError +
                CatUtils.logFormValidationEvent +
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
