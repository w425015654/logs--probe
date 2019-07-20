package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

import java.lang.reflect.Modifier;

/**
 * 修改 com.openjaw.client.SimpleHTTPServlet 的具体实现
 * 1. 注入 generateResponse 的具体实现，获取 returnCode 和 resultCode
 * 2. 注入 doPost 的具体实现，获取当前请求的 OTA channel
 * <p>
 * 此类严重业务相关，要注意
 *
 * @author zengfan
 */
public class SimpleHTTPServlet implements Transformlet {
    private static final Logger logger = Logger.getLogger(SimpleHTTPServlet.class);

    // 添加 cat 相关字段
    public static final String CAT_RETURN_CODE = "_cat$returnCode";
    public static final String CAT_RESULT_CODE = "_cat$resultCode";
    public static final String CAT_ERROR_DESC = "_cat$errorDesc";


    @Override
    public boolean needTransform(String className) {
        return ("com.openjaw.client.SimpleHTTPServlet".equals(className));
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.addCatField(clazz);
        this.updateGenerateResponse(clazz);
        this.updateDoPost(clazz);
    }


    /**
     * 添加 Cat 相关字段 字段
     *
     * @param clazz
     */
    private void addCatField(final CtClass clazz) throws CannotCompileException {
        // 因为 Servlet 不是线程安全的，所以这两个变量要存在 ThreadLocal 里
        // 其中 CAT_RETURN_CODE 是 Integer， CAT_RESULT_CODE、CAT_ERROR_DESC 是 String
        addFieldThreadLocal(clazz, "java.lang.ThreadLocal", CAT_RETURN_CODE);
        addFieldThreadLocal(clazz, "java.lang.ThreadLocal", CAT_RESULT_CODE);
        addFieldThreadLocal(clazz, "java.lang.ThreadLocal", CAT_ERROR_DESC);
    }


    /**
     * 新增一个 ThreadLocal 字段
     *
     * @param clazz
     */
    private void addFieldThreadLocal(final CtClass clazz, String type, String fieldName) throws CannotCompileException {
        final String className = clazz.getName();

        final CtField field = CtField.make("private final " + type + " " + fieldName + " = new java.lang.ThreadLocal();", clazz);
        clazz.addField(field);
        logger.info("add new field " + fieldName + " to class " + className);
    }

    /**
     * 更新 generateResponse 函数
     *
     * @param clazz
     * @throws NotFoundException
     */
    private void updateGenerateResponse(final CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 generateResponse 函数
        final String query = "generateResponse";
        final CtMethod oldMethod = clazz.getDeclaredMethod(query);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, query, clazz, null);

        // 将原来的 query 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$generateResponse$method$renamed$by$probe";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~java.lang.reflect.Modifier.PUBLIC /* remove public */ | Modifier.PRIVATE /* add private */);

        // 在调用 generateResponse 前后添加 CAT 控制
        // 主要是保存当前要返回的状态位 resultCode 和 returnCode
        // 第一个变量是 resultCode
        final String code = "\n{\n" +
                "    int result = " + oldMethodRename + "($$);\n" +
                "    " + CAT_RETURN_CODE + ".set(" + "Integer.valueOf(result));\n" +
                "    " + CAT_RESULT_CODE + ".set(" + "$1);\n" +
                "    " + CAT_ERROR_DESC + ".set(" + "$2);\n" +
                "    return result;\n" +
                "}\n";

        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(oldMethod) + " of class " + clazz.getName() + ": " + code);
    }


    /**
     * 更新 doPost 函数
     *
     * @param clazz
     */
    private void updateDoPost(CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 doPost 函数
        final String query = "doPost";
        final CtMethod oldMethod = clazz.getDeclaredMethod(query);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, query, clazz, null);

        // 将原来的 doPost 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$doPost$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~Modifier.PUBLIC /* remove public */ | Modifier.PRIVATE /* add private */);

        // 基于 OTA_AirLowFareSearchRQ 重置上层 Transaction，这个必须在 newOTATransaction 之前执行
        String renameSearchRQCode = "\n" +
                "    com.dianping.cat.message.internal.DefaultTransaction cat$peekTransaction = null;\n" +
                "    String cat$newType = \"" + CatUtils.TYPE_SHOPPING_2 + "\" + cat$channel;\n" +
                CatUtils.renameSearchRQTransaction;

        // 在调用 doPost 前后添加 CAT 控制
        final String code = "\n{\n" +
                "    String cat$serviceType = $1.getParameter(\"serviceType\");\n\n" +
                "    com.openjaw.client.AvailableService availableService = $0.getRequestedService(cat$serviceType);\n" +
                "    if (availableService == null) {\n" +
                "        cat$serviceType = \"NVL\";\n" +
                "    } else {\n" +
                "        cat$serviceType = availableService.name();\n" +
                "    }\n" +

                // 尝试获取 channel，解析 XML 错误不做任何操作，直接 catch 即可
                // 对于 OTA_AirLowFareSearchRQ，则需要设置为 Shopping.2
                "    String cat$channel = null;\n" +
                "    try{\n" +
                "        String requestXML = $1.getParameter(\"requestXML\");\n" +
                "        if(requestXML != null && requestXML.length() != 0) {\n" +
                "            org.w3c.dom.Document cat$node = com.openjaw.utils.DOMUtilities.parse(requestXML);\n" +
                "            cat$channel = com.openjaw.travelsky.utils.LogHelper.getMessageSource(cat$node.getDocumentElement());\n" +
                "            " + renameSearchRQCode +
                "        }\n" +
                "    } catch (Exception e) {} \n" +

                "    " + CatUtils.newOTATransaction +

                // Telassa 上下文信息
                "    String telassa$channel = cat$channel;\n" +
                "    String telassa$rootId = com.dianping.cat.Cat.getCurrentMessageId();\n" +
                "    String telassa$rootType = \"" + CatUtils.TYPE_OTA_PREFIX + "\".concat(cat$channel);\n" +
                "    String telassa$rootName = cat$serviceType;\n" +
                "    " + TelassaUtils.mdcTelassaContext +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中

                // 如果没有 source，则基于 cat$channel 重置 telassa$id 的 sourceName
                "    String telassa$Id = (String)org.apache.log4j.MDC.get(\"telassa-id\");\n" +
                "    if (telassa$Id != null && telassa$Id.startsWith(\"NoSource\") && cat$channel != null && cat$channel.length() != 0) {\n" +
                "        telassa$Id = null;\n" +
                "        String oj$sourceName = \"OTA.\".concat(cat$channel);\n" +
                "    " + TelassaUtils.initServiceTelassaId +
                "    }\n" +

                "    try { \n" +
                "        " + oldMethodRename + "($$);\n" +
                // 设置 Transaction 执行结果
                // returnCode = 200, resultCode = "1" 表示成功执行
                // 如果 returnCode 不为 200，则取 returnCode 作为 Trans 执行结果
                // 如果 returnCode 为 200，但是resultCode 不为 1，则取 resultCode 作为 Trans 执行结果
                "        Integer returnCode = (Integer)" + CAT_RETURN_CODE + ".get();\n" +
                "        String resultCode = (String)" + CAT_RESULT_CODE + ".get();\n" +
                "        String cat$errorDesc = (String)" + CAT_ERROR_DESC + ".get();\n" +
                "        String cat$statusCode = null;\n" +

                "        if (returnCode != null && returnCode.equals(Integer.valueOf(200)) && " +
                "            resultCode != null && resultCode.equals(\"1\")){\n" +
                "            cat$statusCode = com.dianping.cat.message.Message.SUCCESS;\n" +
                "        } else if(returnCode != null && !returnCode.equals(Integer.valueOf(200))) {\n" +
                "            cat$statusCode = String.valueOf(returnCode);\n" +
                "        } else {\n" +
                "            cat$statusCode = resultCode;\n" +
                "        }\n" +
                "        " + CatUtils.addErrorDescTransaction +
                "        " + CatUtils.statusTransaction +
                "    } catch (Exception e) {\n" +
                "    " + CatUtils.catchException +
                "        throw e;\n" +
                "     } finally {\n" +
                "    " + CatUtils.completeTransaction +
                // 清除 ThreadLocal 内的值，避免内存泄露
                "        " + CAT_RETURN_CODE + ".remove();\n" +
                "        " + CAT_RESULT_CODE + ".remove();\n" +
                "        " + CAT_ERROR_DESC + ".remove();\n" +
                "     }\n" +
                "}\n";


        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(newMethod) + " of class " + clazz.getName() + ": " + code);
    }
}
