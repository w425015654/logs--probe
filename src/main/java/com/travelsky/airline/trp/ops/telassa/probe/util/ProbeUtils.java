package com.travelsky.airline.trp.ops.telassa.probe.util;

import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import javassist.*;

/**
 * 探针相关通用函数
 *
 * @author zengfan
 */
public class ProbeUtils {


    /**
     * 新增一个字段，默认不赋值 null
     *
     * @param clazz
     */
    public static void addFieldUnassigned(final CtClass clazz, String type, String fieldName, Logger logger) throws CannotCompileException {
        final String className = clazz.getName();

        final CtField field = CtField.make("private final " + type + " " + fieldName + ";", clazz);
        clazz.addField(field);
        logger.info("add new field " + fieldName + " to class " + className);
    }


    /**
     * 处理 ibe、editor filter 入口
     */
    public static void doIbeEditorFilter(CtClass clazz, String initTelassaId, Logger logger) throws NotFoundException, CannotCompileException {
        // 复制 doFilter 函数
        final String doFilter = "doFilter";
        final CtMethod oldMethod = clazz.getDeclaredMethod(doFilter);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, doFilter, clazz, null);

        // 将原来的 doFilter 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$doFilter$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);

        final String anchorageUri = "" +
                "    if(uri$for$cat != null && (uri$for$cat.endsWith(\".do\") || uri$for$cat.endsWith(\".servlet\"))){\n" +
                "        com.travelsky.airline.trp.ops.telassa.probe.util.LogUtils.anchorage(uri$for$cat);\n" +
                "    }\n";

        final String code = "{\n" +
                "    String cat$queryString = ((javax.servlet.http.HttpServletRequest)$1).getQueryString();\n" +
                "    java.util.Map cat$queryParams = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.parseQueryString(cat$queryString);\n" +

                "    String telassa$channel = null;\n" +
                "    String telassa$rootId = null;\n" +
                "    String telassa$rootType = null;\n" +
                "    String telassa$rootName = null;\n" +

                "    " + CatForkUtils.remoteRedirectServer +
                "    " + CatUtils.newIbeEditorUrlTransaction +
                "    " + TelassaUtils.mdcTelassaContext +            // 记录 Telassa 上下文信息
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中
                "    try{\n" +
                "        String telassa$Id = (String)(cat$queryParams.get(\"telassa-id\"));\n" +
                "        telassa$Id = com.travelsky.airline.trp.ops.telassa.probe.util.Utils.decodeHeader(telassa$Id);\n" +
                // 如果有 telassa$Id, 表示是由上一个请求通过 serverRedirect 或者 clientRedirect 过来的，则直接 MDC telassaId 即可
                "        if (telassa$Id != null && !telassa$Id.isEmpty()) {\n" +
                "            " + TelassaUtils.mdcTelassaId +
                "        } else {\n" +
                "            " + initTelassaId +
                "        }\n" +

                CatUtils.addRemoteAddress +
                // 记录当前访问 url, 必须放在 MDC telassaId 之后
                "        " + anchorageUri +
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


    /**
     * 处理 ibe、userService createSOAPCall 入口
     */
    public static void updateCreateSOAPCall(CtClass clazz, Logger logger) throws NotFoundException, CannotCompileException {
        // 复制 createSOAPCall 函数
        final String createSOAPCall = "createSOAPCall";
        final CtMethod oldMethod = clazz.getDeclaredMethod(createSOAPCall);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, createSOAPCall, clazz, null);

        // 将原来的 createSOAPCall 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$createSOAPCall$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);
//        oldMethod.setModifiers(oldMethod.getModifiers() & ~java.lang.reflect.Modifier.PUBLIC /* remove public */ | Modifier.PRIVATE /* add private */);

        // 在调用 createSOAPCall 前后，包一层，以便注入我们需要的代码
        // OJ 实现的 createSOAPCall 里会设置 MessageContext 的 HTTPConstants.REQUEST_HEADERS property，但是这样无效，需要在 caller 里再设置一次

        String remoteCallTelassa;
        String remoteCallCat;
        if ("com.openjawx.xRez.jsp.SOAPClient".equals(clazz.getName())) {
            remoteCallTelassa = TelassaUtils.remoteCallServiceClient;
            remoteCallCat = CatForkUtils.remoteCallServiceClient;
        } else {
            remoteCallTelassa = TelassaUtils.remoteCallUserServiceClient;
            remoteCallCat = CatForkUtils.remoteCallUserServiceClient;
        }


        final String code = "\n{\n" +
                remoteCallTelassa +
                remoteCallCat +
                "    String requestHeaders = org.apache.axis.transport.http.HTTPConstants.REQUEST_HEADERS;\n" +
                "    org.apache.axis.client.Call call = " + oldMethodRename + "($$);\n" +
                "    call.setProperty(requestHeaders, call.getMessageContext().getProperty(requestHeaders));\n" +
                "    return call;\n" +
                "}\n";

        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(oldMethod) + " of class " + clazz.getName() + ": " + code);

    }
}
