package com.travelsky.airline.trp.ops.telassa.probe.service;

import com.travelsky.airline.trp.ops.telassa.probe.Transformlet;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.TelassaUtils;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;


/**
 * 修改 Task  的具体实现
 */
public class Task implements Transformlet {
    private static final Logger logger = Logger.getLogger(Task.class);


    @Override
    public boolean needTransform(String className) {
        return ("com.openjaw.scheduler.tasks.Task".equals(className));
    }

    @Override
    public void doTransform(CtClass clazz) throws NotFoundException {
        this.updateExecute(clazz);
    }


    /**
     * 更新 execute 函数, 在 notify 之前:
     * 1. 在 com.openjaw.scheduler.tasks.Task#execute 中的 spm.process(req) 前后加上 Cat Transaction
     *
     * @param clazz
     * @throws NotFoundException
     */
    private void updateExecute(final CtClass clazz) throws NotFoundException {
        // 取到函数
        final CtMethod method = clazz.getDeclaredMethod("execute");

        final String statement = "\n{\n" +
                "    String task$name = null;\n" +
                // 这里调用的时候上下文已经是 com.openjaw.serviceProvider.ServiceProviderManager spm 了，所以不能再使用 $0.getName() 来获取任务名称
                "    com.openjaw.messaging.ProcessContext process$Context = ((com.openjaw.messaging.MessageWrapper)$1).getProcessContext();\n" +
                "    if (process$Context != null) {\n" +
                "        String session$Id = process$Context.getSessionID();\n" +
                "        if (session$Id != null & session$Id.length() !=0) {\n" +
                "            task$name = session$Id.substring(session$Id.indexOf(\":\") + 1, session$Id.lastIndexOf(\":\"));\n" +
                "        }\n" +
                "    }\n" +
                "    " + CatUtils.newTaskTransaction +

                // Telassa 上下文信息
                "    String telassa$rootId = com.dianping.cat.Cat.getCurrentMessageId();\n" +
                "    String telassa$channel = \"" + CatUtils.CHANNEL_TASK + "\";\n" +
                "    String telassa$rootType = \"" + CatUtils.TYPE_TASK + "\";\n" +
                "    String telassa$rootName = task$name;\n" +
                "    " + TelassaUtils.mdcTelassaContext +
                "    " + TelassaUtils.addTelassaContextData +        // 添加到 Transaction Data 中

                // 基于 cat$channel 重置 telassa$id 的 sourceName, 跑定时任务时，OJ 的 jsessionId 里已经包含了 Task 名字，就不再重复输出了
                "    if (task$name != null && task$name.length() != 0) {\n" +
                "        String telassa$Id = null;\n" +
                "        String oj$sourceName = \"Task\";\n" +
                "    " + TelassaUtils.initServiceTelassaId +
                "    }\n" +

                "    try { \n" +
                "        $_ = $proceed($$);\n" +

                // 通过判断是否有 Error 节点来判断 RQ 是否执行成功
                "        String cat$statusCode = com.dianping.cat.message.Message.SUCCESS;\n" +
                "        String cat$errorDesc = null;\n" +
                "        org.w3c.dom.Document cat$document = null;\n" +
                "        String cat$responseSource = null;\n" +

                "        if ($_ != null) {" +
                "            cat$document = $_.getDocument();\n" +
                "            cat$responseSource = $_.getSource();\n" +
                "        }\n" +

                "    " + CatUtils.setStatusCodeByDocument +
                "    " + CatUtils.addResponseSourceTransaction +
                "    " + CatUtils.addErrorDescTransaction +
                "    " + CatUtils.statusTransaction +
                "    } catch (Exception e) {\n" +
                "    " + CatUtils.catchException +
                "        throw e;\n" +
                "    } finally {\n" +
                "    " + CatUtils.completeTransaction +
                "     }\n" +
                "}\n";

        try {
            // 替换代码
            method.instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equalsIgnoreCase("process")) {
                        m.replace(statement);

                        logger.info("Instrument lines in execute() in " + method.getName() + " of class "
                                + method.getDeclaringClass().getName() + ": " + statement);
                    }
                }
            });

        } catch (CannotCompileException e1) {
            e1.printStackTrace();
        }
    }
}
