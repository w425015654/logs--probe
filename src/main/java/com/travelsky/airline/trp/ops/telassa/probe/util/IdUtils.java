package com.travelsky.airline.trp.ops.telassa.probe.util;

/**
 * Id 相关操作
 *
 * @author zengfan
 */
public class IdUtils {

    /**
     * 根据 userName 生成 IBE 中的 telassaId
     *
     * @return
     */
    public static String getIbeTelassaId(String userName, String sessionId, String catMessageId) {

        userName = (userName == null || userName.length() == 0) ? "IbeNotLogin" : userName;
        sessionId = sessionId != null ? sessionId : java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        sessionId = sessionId.replace("~", "");

        // 依次是 userName~JSESSISONID~Cat MessageId
        String result = String.format("%s~%s~%s",
                userName,
                sessionId,
                catMessageId);

        return result;
    }

    /**
     * 生成 Service 中的 telassaId
     *
     * @return
     */
    public static String getServiceTelassaId(String sourceName, String sessionId, String catMessageId) {

        sourceName = (sourceName == null || sourceName.length() == 0) ? "NoServiceSource" : sourceName;
        sessionId = sessionId != null ? sessionId : java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        sessionId = sessionId.replace("~", "");

        // 依次是 sourceName~JSESSISONID~Cat MessageId
        String result = String.format("%s~%s~%s",
                sourceName,
                sessionId,
                catMessageId);

        return result;
    }


    /**
     * 生成 UserService 中的 telassaId
     *
     * @return
     */
    public static String getUserServiceTelassaId(String sourceName, String sessionId, String catMessageId) {

        sourceName = (sourceName == null || sourceName.length() == 0) ? "NoUserSource" : sourceName;
        sessionId = sessionId != null ? sessionId : java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        sessionId = sessionId.replace("~", "");

        // 依次是 sourceName~JSESSISONID~Cat MessageId
        String result = String.format("%s~%s~%s",
                sourceName,
                sessionId,
                catMessageId);

        return result;
    }

    /**
     * 根据 userName 生成 Editor 中的 telassaId
     *
     * @return
     */
    public static String getEditorTelassaId(String userName, String sessionId, String catMessageId) {

        userName = (userName == null || userName.length() == 0) ? "EditorNotLogin" : userName;
        sessionId = sessionId != null ? sessionId : java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        sessionId = sessionId.replace("~", "");

        // 依次是 userName~JSESSISONID~Cat MessageId
        String result = String.format("%s~%s~%s",
                userName,
                sessionId,
                catMessageId);

        return result;
    }


    /**
     * 根据 userName 生成 CMS 中的 telassaId
     *
     * @return
     */
    public static String getCMSTelassaId(String sourceName, String sessionId, String catMessageId) {
        sourceName = (sourceName == null || sourceName.length() == 0) ? "NoCMSSource" : sourceName;
        sessionId = sessionId != null ? sessionId : java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        sessionId = sessionId.replace("~", "");

        // 依次是 sourceName~JSESSISONID~Cat MessageId
        String result = String.format("%s~%s~%s",
                sourceName,
                sessionId,
                catMessageId);

        return result;
    }

}
