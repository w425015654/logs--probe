package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatForkUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

/**
 * Service 入口
 * 修改 SetCharacterEncodingFilter 的具体实现
 * 1. 在 com.openjaw.travelsky.utils.SetCharacterEncodingFilter 前加入生成 telassa-id 的代码
 */
public class SetCharacterEncodingFilter implements Transformlet {
    private static final Logger logger = Logger.getLogger(SetCharacterEncodingFilter.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjaw.travelsky.utils.SetCharacterEncodingFilter".equals(className);
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
                // Telassa 上下文信息
                "    String telassa$channel = ((javax.servlet.http.HttpServletRequest)$1).getHeader(\"telassa-channel\");\n" +
                "    String telassa$rootId = ((javax.servlet.http.HttpServletRequest)$1).getHeader(\"telassa-rootId\");\n" +
                "    String telassa$rootType = ((javax.servlet.http.HttpServletRequest)$1).getHeader(\"telassa-rootType\");\n" +
                "    String telassa$rootName = ((javax.servlet.http.HttpServletRequest)$1).getHeader(\"telassa-rootName\");\n" +
                "    " + TelassaUtils.mdcTelassaContext +
                "    " + TelassaUtils.addTelassaContextData +       // 添加到 Transaction Data 中

                "    String telassa$Id = ((javax.servlet.http.HttpServletRequest)$1).getHeader(\"telassa-id\");\n" +
                "    telassa$Id = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.decodeHeader(telassa$Id);\n" +
                "    String oj$sourceName = null;\n" +
                "    " + TelassaUtils.initServiceTelassaId +
                "    " + TelassaUtils.mdcTelassaId;  // 这里不能省掉这一步，因为 initServiceTelassaId 在 IBE 访问时是不生效的

        final String code = "{\n" +
                CatForkUtils.remoteCallServer +
                CatUtils.newWebServiceTransaction +
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
