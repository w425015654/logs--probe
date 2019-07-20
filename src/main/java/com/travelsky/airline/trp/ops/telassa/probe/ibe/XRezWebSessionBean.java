package com.travelsky.airline.trp.ops.telassa.probe.ibe;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatForkUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.Utils;
import javassist.*;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.lang.reflect.Modifier;

/**
 * 修改 XRezWebSessionBean  的具体实现
 * 1. 在 serverRedirect、clientRedirect 入口处，设置 CAT 的 remoteCallServiceClient
 */
public class XRezWebSessionBean implements Transformlet {
    private static final Logger logger = Logger.getLogger(XRezWebSessionBean.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjawx.xRez.jsp.xRezWebSessionBean".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.updateServerRedirect(clazz);
        this.updateClientRedirect(clazz);


        this.updateApplyXSL(clazz);
    }

    /**
     * 更新 serverRedirect 函数
     *
     * @param clazz
     */
    private void updateServerRedirect(CtClass clazz) throws NotFoundException, CannotCompileException {
        // 复制 serverRedirect 函数
        final String serverRedirect = "serverRedirect";
        final CtMethod oldMethod = clazz.getDeclaredMethod(serverRedirect);
        final CtMethod newMethod = CtNewMethod.copy(oldMethod, serverRedirect, clazz, null);

        // 将原来的 serverRedirect 改名，并置为 private，以免被外部调用
        final String oldMethodRename = "original$serverRedirect$method$renamed$by$telassa";
        oldMethod.setName(oldMethodRename);
        oldMethod.setModifiers(oldMethod.getModifiers() & ~Modifier.PUBLIC /* remove protected */ | Modifier.PRIVATE /* add private */);

        // 在调用 process 前后添加 CAT 控制
        final String code = "\n{\n" +
                "    " + CatUtils.newIbeForwardTransaction +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中
                "    try { \n" +
                "        " + oldMethodRename + "($$);\n" +
                "        " + CatUtils.successTransaction +
                "    } catch (Exception e) {\n" +
                "    " + CatUtils.catchException +
                "        throw e;\n" +
                "    } finally {\n" +
                "    " + CatUtils.completeTransaction +
                "     }\n" +
                "}\n";

        newMethod.setBody(code);
        clazz.addMethod(newMethod);
        logger.info("insert code around method " + Utils.signatureOfMethod(oldMethod) + " of class " + clazz.getName() + ": " + code);
    }


    /**
     * 更新 clientRedirect 函数
     *
     * @param clazz
     * @throws NotFoundException
     */
    private void updateClientRedirect(final CtClass clazz) throws NotFoundException {
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod("clientRedirect");

        String code = "" +
                TelassaUtils.remoteRedirectClient +
                CatForkUtils.remoteRedirectClient;

        try {
            // 最前面插入代码
            method.insertBefore(code);
            logger.info("Insert code before " + method.getName() + " of class "
                    + method.getDeclaringClass().getName() + ": " + code);
        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }


    /**
     * 判断当前如果是 OTA_AirLowFareSearchRQ，那么就设置 Shopping.1.*
     *
     * @param clazz
     */
    private void updateApplyXSL(CtClass clazz) throws NotFoundException {
        // 实例化类型池对象
        ClassPool classPool = ClassPool.getDefault();
        // 获取 4 个 String 类型参数集合
        CtClass[] paramTypes = {classPool.get(Source.class.getName()), classPool.get(String.class.getName()), classPool.get(Result.class.getName())};
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod("applyXSL", paramTypes);

        // 代码执行完之后，第三个参数 javax.xml.transform.Result 里面就是解析好的 RQ
        String code = "\n{" +
                "    com.dianping.cat.message.internal.DefaultTransaction cat$peekTransaction = null;\n" +
                "    try { \n" +
                "        if ($3 != null && $3 instanceof javax.xml.transform.dom.DOMResult) {\n" +
                "            javax.xml.transform.dom.DOMResult cat$domResult = (javax.xml.transform.dom.DOMResult)$3;\n" +
                "            org.w3c.dom.Node cat$node = cat$domResult.getNode();\n\n" +

                "            String cat$shoppingLevel = \"" + CatUtils.TYPE_SHOPPING_3 + "\";\n" +
                CatUtils.initSearchRQType +
                CatUtils.renameSearchRQTransaction +
                "        }\n" +
                "    } catch (Exception e) {\n" +
                // 发生任何异常都不处理，不能影响主流程
                "    } finally {\n" +
                // 这里没有新建 cat$Transaction，所以也无需 complete
//                "        " + CatUtils.completeTransaction +
                "    }\n" +
                "}\n";

        try {
            // 最后面插入代码
            method.insertAfter(code);
            logger.info("Insert code after " + method.getName() + " of class "
                    + method.getDeclaringClass().getName() + ": " + code);
        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }

}
