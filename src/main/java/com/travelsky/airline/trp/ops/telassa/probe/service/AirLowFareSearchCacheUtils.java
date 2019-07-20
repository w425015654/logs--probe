package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * 修改 com.openjaw.travelsky.redis.AirLowFareSearchCacheUtils#getAirLowFareSearchRSFromCache 的具体实现，用来判断是否命中缓存
 *
 * @author zengfan
 */
public class AirLowFareSearchCacheUtils implements Transformlet {
    private static final Logger logger = Logger.getLogger(AirLowFareSearchCacheUtils.class);

    @Override
    public boolean needTransform(String className) {
        return ("com.openjaw.travelsky.redis.AirLowFareSearchCacheUtils".equals(className));
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException, IOException {
        this.updateGetAirLowFareSearchRSFromCache(clazz);
    }


    /**
     * 更新 getAirLowFareSearchRSFromCache 函数
     *
     * @param clazz
     * @throws NotFoundException
     */
    private void updateGetAirLowFareSearchRSFromCache(final CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 process 函数
        final String process = "getAirLowFareSearchRSFromCache";
        final CtMethod oldMethod = clazz.getDeclaredMethod(process);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, process, clazz, null);

        // 将原来的 getAirLowFareSearchRSFromCache 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$getAirLowFareSearchRSFromCache$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~Modifier.PUBLIC /* remove public */ | Modifier.PRIVATE /* add private */);

        final String code = "\n{\n" +
                // 这里不新建 Transaction，否则整体时间由上级 Transaction 控制时间，没法指定时间，所以干脆直接修改上级 Transaction
                "    com.dianping.cat.message.spi.MessageManager message$Manager= com.dianping.cat.message.internal.DefaultMessageManager.getInstance();\n" +
                "    com.dianping.cat.message.internal.DefaultTransaction cat$peekTransaction = (com.dianping.cat.message.internal.DefaultTransaction)(message$Manager.getPeekTransaction());\n" +
                "    org.w3c.dom.Document cat$result = null;\n" +
                "    try { \n" +
                "        cat$result = " + oldMethodRename + "($$);\n" +
                "        String cat$cacheType = com.travelsky.airline.trp.ops.telassa.probe.util.DomUtils.getSearchCacheType(cat$result);\n" +

                "        if (cat$peekTransaction != null && cat$cacheType != null) {\n" +
                "            String cat$originName = cat$peekTransaction.getName();\n" +         // 设置上一级 SPMClientThread.OTA_AirLowFareSearchRQ 的 Name 值，标记是否走了缓存
                "            String cat$newName = cat$originName + \".\"+ cat$cacheType;\n" +
                "            cat$peekTransaction.setName(cat$newName);\n" +

                "            cat$peekTransaction.pushData(\"cache2Type=\".concat(cat$cacheType));\n" + // 设置上一级 SPMClientThread.OTA_AirLowFareSearchRQ 的 cache2Type 属性
                "        }\n" +
                "    } catch (Exception e) {" +     // 有异常也不处理
                "    } finally {\n" +
                "        return cat$result;\n" +
                "    }\n" +
                "}\n";

        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(newMethod) + " of class " + clazz.getName() + ": " + code);
    }
}
