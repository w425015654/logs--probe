package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * 修改 com.openjaw.connection.java.JavaConnectorBase 所有子类的具体实现
 *
 * @author zengfan
 */
public class JavaConnector implements Transformlet {
    private static final Logger logger = Logger.getLogger(JavaConnector.class);

    @Override
    public boolean needTransform(String className) {
        return ("com.openjaw.connection.travelsky.hainan.AdditionalProductsConnector".equals(className) ||
                "com.openjawx.airancillariesmanager.connectors.AirAncillariesManagerConnector".equals(className) ||
                "com.openjaw.connectors.hotelbe.CtripConnector".equals(className) ||
                "com.openjaw.connector.travelsky.domestic.DomesticConnector".equals(className) ||
                "com.openjaw.connector.travelsky.settlement.DomesticConnector".equals(className) ||
                "com.openjaw.connectors.elong.ElongConnector".equals(className) ||
                "com.openjaw.connector.travelsky.hucc.HUCCConnector".equals(className) ||
                "com.openjaw.connectors.travelsky.hainan.HainanAirlinesConnector".equals(className) ||
                "com.openjaw.connectors.travelsky.insurance.InsuranceConnector".equals(className) ||
                "com.openjaw.travelsky.insurance.InsuranceConnector".equals(className) ||
                "com.openjaw.xPromotion.connectors.LandingPageConnector".equals(className) ||
                "com.openjawx.xLocation.connectors.LocationServiceProvider".equals(className) ||
                "com.openjaw.connector.MockServerConnector".equals(className) ||
                "com.openjawx.notification.connectors.NotificationConnector".equals(className) ||
                "com.openjaw.travelsky.redis.RedisConnector".equals(className) ||
                "com.openjaw.connectors.travelsky.sms.SMSConnector".equals(className) ||
                "com.openjaw.travelsky.connector.spnr.SPNRConnector".equals(className) ||
                "com.openjaw.connectors.sz.SZConnector".equals(className) ||
                "com.openjaw.connection.session.pooled.cluster.SessionTestConnection".equals(className) ||
                "com.openjawx.xHotel.connectors.xHotelsConnector".equals(className) ||
                "com.openjawx.xLocation.connectors.xLocationConnector".equals(className) ||
                "com.openjaw.xorg.connectors.xOrgConnector".equals(className) ||
                "com.openjaw.xPromotion.connectors.xPromotionConnector".equals(className) ||
                "com.openjawx.xReport.connectors.xReportConnector".equals(className));
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException, IOException {
        this.updateProcess(clazz);
    }


    /**
     * 更新 process 函数
     *
     * @param clazz
     * @throws NotFoundException
     */
    private void updateProcess(final CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 process 函数
        final String process = "process";
        final CtMethod oldMethod = clazz.getDeclaredMethod(process);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, process, clazz, null);

        // 将原来的 process 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$process$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~Modifier.PUBLIC /* remove public */ | Modifier.PRIVATE /* add private */);


        //  Shopping.3
        String renameSearchRQCode = "\n" +
                "    com.dianping.cat.message.internal.DefaultTransaction cat$peekTransaction = null;\n" +
                "    try { \n" +
                "        if ($1 != null && $1 instanceof org.w3c.dom.Document) {\n" +
                "            org.w3c.dom.Document cat$node = (org.w3c.dom.Document)$1;\n" +
                "            org.w3c.dom.Element cat$msg = cat$node.getDocumentElement();\n" +

                "            String cat$shoppingLevel = \"" + CatUtils.TYPE_SHOPPING_1 + "\";\n" +
                CatUtils.initSearchRQType +
                CatUtils.renameSearchRQTransaction +
                "        }\n" +
                // 发生任何异常都不处理，不能影响主流程
                "    } catch (Exception e) {}\n";

        final String code = "\n{\n" +
                "    " + CatUtils.newJavaConnectorTransaction +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中
                "    " + renameSearchRQCode +
                "    String telassa$Id = (String)org.apache.log4j.MDC.get(\"telassa-id\");\n" +
                "    String oj$sourceName = null;\n" +
                "    " + TelassaUtils.initServiceTelassaId +
                "    try { \n" +
                "        org.w3c.dom.Document result = " + oldMethodRename + "($$);\n" +

                "        String cat$statusCode = com.dianping.cat.message.Message.SUCCESS;\n" +
                "        String cat$errorDesc = null;\n" +
                "        org.w3c.dom.Document cat$document = result;\n" +

                "        " + CatUtils.setStatusCodeByDocument +
                "        " + CatUtils.addErrorDescTransaction +
                "        " + CatUtils.statusTransaction +

                "        return result;\n" +
                "     }  catch (Exception e) {\n" +
                "    " + CatUtils.catchException +
                "        throw e;\n" +
                "     } finally {\n" +
                "    " + CatUtils.completeTransaction +
                "     }\n" +
                "}\n";

        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(newMethod) + " of class " + clazz.getName() + ": " + code);
    }
}
