package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;


/**
 * 修改 org.apache.log4j.Category#info(java.lang.Object) 的具体实现
 * 1. 判断当前 logger 是否业务 logger，如果是便输出一个 CAT com.dianping.cat.message.Transaction
 */
public class Log4jCategory implements Transformlet {
    private static final Logger logger = Logger.getLogger(Log4jCategory.class);

    @Override
    public boolean needTransform(String className) {
        return "org.apache.log4j.Category".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.updateInfo(clazz);
    }


    /**
     * 更新 info 函数
     * 按一下格式生成 Transaction
     * Type:  Log名字.source
     * Name:    channel.logType.passengers
     *
     * @param clazz
     * @throws NotFoundException
     */
    private void updateInfo(final CtClass clazz) throws NotFoundException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        pool.importPackage("com.dianping.cat.message");

        final String info = "info";
        final CtMethod method = clazz.getDeclaredMethod(info);

        // 在调用 info 之前, 判断如果是指定的 Logger，则进行解析并输出一个 com.dianping.cat.message.Transaction
        final String code = "\n{\n" +
                "    com.dianping.cat.message.Transaction cat$Transaction = null;\n" +
                "    try { \n" +
                "        String _cat$msg = (String)$1;\n" +
                "        String _logger$name = $0.getName();\n" +
                "        String _cat$typePrefix = com.travelsky.airline.trp.ops.telassa.probe.util.Logger2CatUtils.getTypePrefix(_logger$name);\n" +
                "        String[] _splitted$msg = com.travelsky.airline.trp.ops.telassa.probe.util.Logger2CatUtils.splitMessage(_logger$name, _cat$msg);\n" +

                "        if (_cat$typePrefix != null && _splitted$msg != null) {\n" +
                "            int idxSource = com.travelsky.airline.trp.ops.telassa.probe.util.Logger2CatUtils.idxSource;\n" +
                "            int idxChannel = com.travelsky.airline.trp.ops.telassa.probe.util.Logger2CatUtils.idxChannel;\n" +
//                "            int idxCostTime = com.travelsky.airline.trp.ops.telassa.probe.util.Logger2CatUtils.idxCostTime;\n" +
//                "            int idxOperateStatus = com.travelsky.airline.trp.ops.telassa.probe.util.Logger2CatUtils.idxOperateStatus;\n" +
                "            int idxTxnTraceKey = com.travelsky.airline.trp.ops.telassa.probe.util.Logger2CatUtils.idxTxnTraceKey;\n" +
                "            int idxPassengers = com.travelsky.airline.trp.ops.telassa.probe.util.Logger2CatUtils.idxPassengers;\n" +
                "            int idxErrorMsg = com.travelsky.airline.trp.ops.telassa.probe.util.Logger2CatUtils.idxErrorMsg;\n" +
                "            int idxLogType = com.travelsky.airline.trp.ops.telassa.probe.util.Logger2CatUtils.idxLogType;\n" +
                "            int idxItinerary = com.travelsky.airline.trp.ops.telassa.probe.util.Logger2CatUtils.idxItinerary;\n" +

                "            String trans$type = _cat$typePrefix.concat(_splitted$msg[idxSource]);\n" +

                // Transaction name 取值为 channel.logType.passengers
                "            String trans$name = _splitted$msg[idxChannel];\n" +
                "            if(_splitted$msg[idxLogType] != null && !_splitted$msg[idxLogType].isEmpty()){\n" +
                "                if(!trans$name.isEmpty()) {\n" +
                "                    trans$name = trans$name.concat(\".\");\n" +
                "                }\n" +
                "                trans$name = trans$name.concat(_splitted$msg[idxLogType]);\n" +
                "            }\n" +
                "            if(_splitted$msg[idxPassengers] != null && !_splitted$msg[idxPassengers].isEmpty()){\n" +
                "                if(!trans$name.isEmpty()) {\n" +
                "                    trans$name = trans$name.concat(\".\");\n" +
                "                }\n" +
                "                trans$name = trans$name.concat(_splitted$msg[idxPassengers]);\n" +
                "            }\n" +

                "            if (trans$type != null && !trans$type.isEmpty() && trans$name != null && !trans$name.isEmpty()){ \n" +
                // 这里不新建 Transaction，否则整体时间由上级 Transaction 控制时间，没法指定时间，所以干脆直接修改上级 Transaction
                "                com.dianping.cat.message.spi.MessageManager message$Manager= com.dianping.cat.message.internal.DefaultMessageManager.getInstance();\n" +
                "                cat$Transaction = message$Manager.getPeekTransaction();\n" +
                "                ((com.dianping.cat.message.internal.DefaultTransaction)cat$Transaction).setType(trans$type);\n" +
                "                ((com.dianping.cat.message.internal.DefaultTransaction)cat$Transaction).setName(trans$name);\n" +
//
                "                cat$Transaction.addData(\"TID=\".concat(_splitted$msg[idxTxnTraceKey]));\n" +

                "                if(_splitted$msg[idxItinerary] != null && !_splitted$msg[idxItinerary].isEmpty()) {\n" +
                "                    cat$Transaction.addData(\"itinerary=\".concat(_splitted$msg[idxItinerary]));\n" +
                "                }\n" +
                "                if(_splitted$msg[idxErrorMsg] != null && !_splitted$msg[idxErrorMsg].isEmpty()) {\n" +
                "                    cat$Transaction.addData(\"msg=\".concat(_splitted$msg[idxErrorMsg]));\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    } catch (Exception e) {\n" +
                // 发生任何异常都不处理，不能影响主流程
                "    } finally {\n" +
                // 这里没有新建 cat$Transaction，所以也无需 complete
//                "        " + CatUtils.completeTransaction +
                "    }\n" +
                "}\n";


        method.insertBefore(code);
        logger.info("insert code before method " + Utils.signatureOfMethod(method) + " of class " + clazz.getName() + ": " + code);
    }
}
