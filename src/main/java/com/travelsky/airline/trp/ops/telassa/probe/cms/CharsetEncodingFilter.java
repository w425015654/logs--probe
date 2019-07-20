package com.travelsky.airline.trp.ops.telassa.probe.cms;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

/**
 * CMS 入口
 * 修改 CharsetEncodingFilter 的具体实现
 */
public class CharsetEncodingFilter implements Transformlet {
    private static final Logger logger = Logger.getLogger(CharsetEncodingFilter.class);

    @Override
    public boolean needTransform(String className) {
        return "com.dotmarketing.filters.CharsetEncodingFilter".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.updateDoFilter(clazz);
    }


    /**
     * 更新 doFilter 函数
     * 直接在函数最前面插入代码
     *
     * @param clazz
     */
    private void updateDoFilter(CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 doFilter 函数
        final String doFilter = "doFilter";
        final CtMethod oldMethod = clazz.getDeclaredMethod(doFilter);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, doFilter, clazz, null);

        // 将原来的 doFilter 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$doFilter$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);

        // 在 doFilter 入口处设置 MDC, 并输出 log trans begin
        final String getTelassaId = "" +
                "    String telassa$Id = ((javax.servlet.http.HttpServletRequest)$1).getHeader(\"telassa-id\");\n" +
                "    telassa$Id = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.decodeHeader(telassa$Id);\n" +
                "    String oj$sourceName = null;\n" +
                "    " + TelassaUtils.initCMSTelassaId ;

        final String code = "{\n" +
                CatUtils.newCMSTransaction +
                getTelassaId +
                CatUtils.addRemoteAddress +
                "    try{\n" +
                "        " + oldMethodRename + "($$);\n" +
                "        " + CatUtils.successTransaction +
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
