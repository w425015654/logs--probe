package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * 修改 com.travelsky.ebuild.clientapi.FlightShoppping.FlightShoppping#doFlightShopping 的具体实现，用来判断是否命中 Ebuild 缓存
 *
 * @author zengfan
 */
public class FlightShoppping implements Transformlet {
    private static final Logger logger = Logger.getLogger(FlightShoppping.class);

    @Override
    public boolean needTransform(String className) {
        return ("com.travelsky.ebuild.clientapi.FlightShoppping.FlightShoppping".equals(className));
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException, IOException {
        this.updateDoFlightShopping(clazz);
    }


    /**
     * 更新 doFlightShopping 函数
     *
     * @param clazz
     * @throws NotFoundException
     */
    private void updateDoFlightShopping(final CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 process 函数
        final String process = "doFlightShopping";
        final CtMethod oldMethod = clazz.getDeclaredMethod(process);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, process, clazz, null);

        // 将原来的 doFlightShopping 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$doFlightShopping$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~Modifier.PUBLIC /* remove public */ | Modifier.PRIVATE /* add private */);

        final String code = "\n{\n" +
                // 这里不新建 Transaction，否则整体时间由上级 Transaction 控制时间，没法指定时间，所以干脆直接修改上级 Transaction
                "    com.dianping.cat.message.spi.MessageManager message$Manager= com.dianping.cat.message.internal.DefaultMessageManager.getInstance();\n" +
                "    com.dianping.cat.message.internal.DefaultTransaction cat$peekTransaction = (com.dianping.cat.message.internal.DefaultTransaction)(message$Manager.getPeekTransaction());\n" +
                "    try { \n" +
                "        com.travelsky.ebuild.clientapi.FlightShoppping.Output cat$result = " + oldMethodRename + "($$);\n" +
                // 从 cat$result.getResult().getWarningType().getMessage() 获取 Ebuild 缓存信息
                "        String cat$cacheType = null;\n" +
                "        if (cat$result != null) {\n" +
                "            com.travelsky.ebuild.clientapi.FlightShoppping.Result shoppingResult = cat$result.getResult();\n" +
                "            if (shoppingResult != null) {\n" +
                "                com.travelsky.ebuild.clientapi.FlightShoppping.WarningType warningType = shoppingResult.getWarningType();\n" +
                "                if (warningType != null) {\n" +
                "                    cat$cacheType = warningType.getMessage();\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +

                // 对于格式 TSDF/%00EBD0JHU2292019050822065904289209*0*TRUE，TSDF/%00EBD000JHU72019050822573004431569*0*CACHE 的 channel，需要去掉中间的 EBD0JHU2292019050822065904289209
                "        if (cat$cacheType != null && !cat$cacheType.isEmpty()) {\n" +
                "            int idxStart = cat$cacheType.indexOf(\"%00\");\n" +
                "            int idxEnd = cat$cacheType.indexOf(\"*0*\");\n" +
                "            if (idxStart != -1 && idxEnd != -1 && idxEnd > idxStart + 3) {\n" +
                "                cat$cacheType = cat$cacheType.replaceAll(cat$cacheType.substring(idxStart + 3, idxEnd), \"\");\n" +
                "            }\n" +
                "        }\n" +

                "        if (cat$peekTransaction != null && cat$cacheType != null) {\n" +
                "            String cat$originName = cat$peekTransaction.getName();\n" +         // 设置 Shopping.1 的 Name 值，标记是否走了缓存
                "            String cat$newName = cat$originName + \".\"+ cat$cacheType;\n" +
                "            cat$peekTransaction.setName(cat$newName);\n" +

                "            cat$peekTransaction.pushData(\"cache1Type=\".concat(cat$cacheType));\n" +
                "        }\n" +
                "        return cat$result;\n" +
                "    } catch (Exception e) {\n" + // 有异常原样抛出
                "        throw e;\n" +
                "    } finally {\n" +
                "    }\n" +
                "}\n";

        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(newMethod) + " of class " + clazz.getName() + ": " + code);
    }
}
