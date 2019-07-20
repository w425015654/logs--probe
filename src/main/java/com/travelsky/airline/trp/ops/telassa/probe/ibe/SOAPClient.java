package com.travelsky.airline.trp.ops.telassa.probe.ibe;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.ProbeUtils;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;


/**
 * 修改 SOAPClient 的具体实现
 * 1. 重新实现 com.openjawx.xRez.jsp.SOAPClient#createSOAPCall
 * 1.1 先设置 this.headers 的值
 * 1.2 调用原生 createSOAPCall 函数
 * 1.3 设置 call 的 property, Key 为 HTTPConstants.REQUEST_HEADERS，在 Service 里才能通过 getHeader 取到
 */
public class SOAPClient implements Transformlet {
    private static final Logger logger = Logger.getLogger(SOAPClient.class);

    @Override
    public boolean needTransform(String className) {
        return "com.openjawx.xRez.jsp.SOAPClient".equals(className);
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException, CannotCompileException {
        this.updateCreateSOAPCall(clazz);
    }


    /**
     * 更新 createSOAPCall 函数
     * 这里不是直接在 createSOAPCall 前后加入行，而是在这个函数外面包一层，然后在前后分别加入行
     *
     * @param clazz
     */
    private void updateCreateSOAPCall(CtClass clazz) throws NotFoundException, CannotCompileException {
        ProbeUtils.updateCreateSOAPCall(clazz, logger);
    }
}
