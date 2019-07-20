package com.travelsky.airline.trp.ops.telassa.probe.util;

/**
 * DOC
 *
 * @author zengfan
 */

import com.travelsky.airline.trp.ops.telassa.probe.TelassaProbe;
import org.apache.log4j.Logger;

/**
 * log 相关功能
 *
 * @author zengfan
 */
public class LogUtils {
    // 用以输出 anchor 锚点信息
    private static final Logger telassaLog = Logger.getLogger("telassa");

    // 用于输出 editor 中的 table 变更信息
    private static final Logger editorAuditLog = Logger.getLogger("editorAudit");


    /**
     * 打一个锚点，会自动带出 telassaId，以便全局串起来
     */
    public static void anchorage(String msg) {
        if (!loggable()) {
            return;
        }

        telassaLog.info(msg);
    }


    /**
     * 输出 audit 信息
     */
    public static void editorAudit(String type, String code, String name, String oldData, String newData, boolean commit) {
        if (!loggable()) {
            return;
        }

        editorAuditLog.info(String.format("[%s][%s][%s][%s][%s][%s]", type, code, name, oldData, newData, String.valueOf(commit)));
    }


    /**
     * 判断是否可以 log
     * 如果没有配置 LOGGABLE_USERS，则都可以 log
     * 如果配置了，就必须是配置里的用户名才可以 log
     *
     * @return
     */
    private static boolean loggable() {
        if (TelassaProbe.LOGGABLE_USERS.isEmpty()) {
            return true;
        }

        String telassaId = (String) org.apache.log4j.MDC.get("telassa-id");
        for (String loggableUser : TelassaProbe.LOGGABLE_USERS) {
            if (telassaId != null && telassaId.startsWith(loggableUser)) {
                return true;
            }
        }

        return false;
    }
}
