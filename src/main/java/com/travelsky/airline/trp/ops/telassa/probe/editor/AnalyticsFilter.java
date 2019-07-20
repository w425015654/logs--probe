package com.travelsky.airline.trp.ops.telassa.probe.editor;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.ProbeUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * Editor 入口
 * 修改 AnalyticsFilter 的具体实现
 * 1. 在 com.openjawx.xRez.analytics.AnalyticsFilter#doFilter 前加入生成 telassa-id 的代码
 */
public class AnalyticsFilter implements Transformlet {
    private static final Logger logger = Logger.getLogger(AnalyticsFilter.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjawx.xRez.analytics.AnalyticsFilter".equals(className);
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
        // 当前用户已登录，就取当前用户，否则取 EditorNotLogin-，
        // 注意这里带来的性能损耗
        final String initTelassaId = "" +
                "    String telassa$userName = null;\n" +
                "    String session$Id = null;\n" +
                "    javax.servlet.http.HttpSession cat$session = ((javax.servlet.http.HttpServletRequest)$1).getSession();\n" +
                "    if (cat$session != null) {\n" +
                "         session$Id = cat$session.getId();\n" +
                "         telassa$userName = (String)cat$session.getAttribute(\"xRezLoginUsername\");\n" +
                "    }\n" +
                TelassaUtils.initEditorTelassaId;

        ProbeUtils.doIbeEditorFilter(clazz, initTelassaId, logger);
    }
}
