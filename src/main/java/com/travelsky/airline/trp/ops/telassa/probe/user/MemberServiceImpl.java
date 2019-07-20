package com.travelsky.airline.trp.ops.telassa.probe.user;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import javassist.*;

/**
 * 修改 com.travelsky.MemberService.business.impl.MemberServiceImpl#responseAddError  的具体实现，记录所有错误返回
 */
public class MemberServiceImpl implements Transformlet {
    private static final Logger logger = Logger.getLogger(MemberServiceImpl.class);

    @Override
    public boolean needTransform(String className) {
        return "com.travelsky.MemberService.business.impl.MemberServiceImpl".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException {
        this.updateResponseAddError(clazz);
    }


    /**
     * 更新 responseAddError 函数
     *
     * @param clazz
     */
    private void updateResponseAddError(CtClass clazz) throws NotFoundException {
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod("responseAddError");

        String code = "\n" +
                "String cat$errorType = String.valueOf($4);\n" +
                "String cat$errorDesc = String.valueOf($5);\n" +
                CatUtils.setRPCUtilTransactionError;

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
