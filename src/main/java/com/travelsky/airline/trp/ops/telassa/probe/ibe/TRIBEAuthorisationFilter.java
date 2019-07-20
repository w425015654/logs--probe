package com.travelsky.airline.trp.ops.telassa.probe.ibe;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.ProbeUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * IBE 入口, 需配在 com.openjawx.xRez.filter.RequestBinder 之后，这样才能取到当前用户名
 * 修改 TRIBEAuthorisationFilter 的具体实现
 * 1. 在 com.openjaw.travelsky.utils.TRIBEAuthorisationFilter#doFilter 前加入生成 telassa-id 的代码
 *
 * 设置 Shopping.1.*
 * 可以在此 Filter 入口取到当前调用的 ibeForm 的内容，但是不准备在这里取，因为 ibeForm 内容变化比较大，不然 OTA_AirLowFareSearchRQ 稳定
 * DOMUtilities.serialise(((com.openjawx.xRez.jsp.xRezWebSessionBean)((HttpServletRequest) request).getSession().getAttribute("sessionBean")).getXMLForm("ibeForm").getAsDocument())
 */
public class TRIBEAuthorisationFilter implements Transformlet {
    private static final Logger logger = Logger.getLogger(TRIBEAuthorisationFilter.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjaw.ibe.security.TRIBEAuthorisationFilter".equals(className);
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
        // BusinessMonitorFilter 在 RequestBinder 之前，那边没法取到 session，所以放到 TRIBEAuthorisationFilter 里
        // 当前用户已登录，就取当前用户，否则取 IbeNotLogin-，
        // 注意这里带来的性能损耗

        final String initTelassaId = "" +
                "    String telassa$userName = null;\n" +
                "    if (com.openjaw.http.RequestHolder.checkHttpServletRequestExists()) {\n" +
                "        Object sessionBean = com.openjaw.http.RequestHolder.getCurrentRequest().getSession().getAttribute(\"sessionBean\");\n" +
                "        if (sessionBean != null) {\n" +
                "            com.openjawx.xRez.jsp.xRezXMLForm ibeForm = ((com.openjawx.xRez.jsp.xRezWebSessionBean) sessionBean).getXMLForm(\"ibeForm\");\n" +
                "            telassa$userName = ibeForm != null ? ibeForm.getValue(\"username\") : null;\n" +
                "        }\n" +
                "    }\n" +
                "    javax.servlet.http.HttpSession session = ((javax.servlet.http.HttpServletRequest)$1).getSession();\n" +
                "    String session$Id = session.getId();\n" +
                TelassaUtils.initIbeTelassaId;

        ProbeUtils.doIbeEditorFilter(clazz, initTelassaId, logger);
    }
}
