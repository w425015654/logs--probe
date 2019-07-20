package com.travelsky.airline.trp.ops.telassa.probe;


import com.travelsky.airline.trp.ops.telassa.probe.cms.CharsetEncodingFilter;
import com.travelsky.airline.trp.ops.telassa.probe.editor.AnalyticsFilter;
import com.travelsky.airline.trp.ops.telassa.probe.editor.FileContent;
import com.travelsky.airline.trp.ops.telassa.probe.ibe.*;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.service.*;
import com.travelsky.airline.trp.ops.telassa.probe.user.HttpServlet;
import com.travelsky.airline.trp.ops.telassa.probe.user.MemberServiceImpl;
import com.travelsky.airline.trp.ops.telassa.probe.user.RPCUtil;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class TelassaProbe {
    /**
     * 需要 log 的用户名，为空时表示全部 log
     */
    public static List<String> LOGGABLE_USERS = new ArrayList<String>();

    private TelassaProbe() {
        throw new InstantiationError("Must not instantiate this class");
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        final Map<String, String> kvArgs = splitCommaColonStringToKV(agentArgs);

        Logger.setLoggerImplType(kvArgs.get(Logger.LOGGER_LEVEL));
        final Logger logger = Logger.getLogger(TelassaProbe.class);


        // 设置待 log 的用户名，这个必须在设置 LOGGER_LEVEL 之后调用
        setLoggableUsers(kvArgs);

        // 设置 cat domain
        setCatDomain(kvArgs);

        // 设置 cat HOME 目录
        setCatHome(kvArgs);

        try {
            logger.info("[TelassaProbe.premain] begin, agentArgs: " + agentArgs + ", Instrumentation: " + inst);
            ClassFileTransformer transformer;

            if ("HuService".equalsIgnoreCase(kvArgs.get("trp.app"))) {
                transformer = new TelassaTransformer(
                        SetCharacterEncodingFilter.class,
                        SPMNodeThread.class,
                        IBEClient.class,
                        TableProcessor.class,
                        SPMTranslation.class,
                        SPMClientThread.class,
                        JavaConnector.class,
                        SimpleHTTPServlet.class,
                        Task.class,
                        Log4jCategory.class,
                        SPMSupplierBase.class,
                        ThreadBase.class,
                        SOAPThread.class,
                        HTTPThread.class,
                        SMTPEmail.class,
                        MessageClientAXIS.class,
                        AirLowFareSearchCacheUtils.class,
                        FlightShoppping.class
                );

            } else if ("HuIBE".equalsIgnoreCase(kvArgs.get("trp.app"))) {
                transformer = new TelassaTransformer(
                        SOAPClient.class,
                        XRezWebSessionBean.class,
                        HTTPPost.class,
                        TemplateCache.class,
//                        Parser.class,
                        TRIBEAuthorisationFilter.class
                );
            } else if ("HuEditor".equalsIgnoreCase(kvArgs.get("trp.app"))) {
                transformer = new TelassaTransformer(
                        SOAPClient.class,
                        XRezWebSessionBean.class,
                        HTTPPost.class,
                        TemplateCache.class,
                        AnalyticsFilter.class,
                        FileContent.class
                );
            } else if ("HuUser".equalsIgnoreCase(kvArgs.get("trp.app"))) {
                transformer = new TelassaTransformer(
                        HttpServlet.class,
                        RPCUtil.class,
                        MemberServiceImpl.class
                );
            } else if ("3uCms".equalsIgnoreCase(kvArgs.get("trp.app"))) {
                // 在 /Users/zengfan/Projects/TSProjects/trp/3u-dev/dotCms/release/dotserver/tomcat-8.5.32/bin/catalina.sh 中新加行
                // JAVA_OPTS="$JAVA_OPTS -javaagent:/Users/zengfan/Projects/TSProjects/trp-ops/telassa/telassa-probe/target/telassa-probe-0.0.1.jar=logger.level:STDOUT,trp.app:3uCms,cat.domain:hu-dev-SIT,cat.home:/Users/zengfan/Downloads/logs/cat/"
                transformer = new TelassaTransformer(
                        CharsetEncodingFilter.class
                );
            } else {
                throw new IllegalStateException(kvArgs.toString(), null);
            }

            inst.addTransformer(transformer, true);
            logger.info("[TelassaProbe.premain] addTransformer " + transformer.getClass() + " success");

            logger.info("[TelassaProbe.premain] end");
        } catch (Exception e) {
            String msg = "Fail to load TelassaProbe , cause: " + e.toString();
            logger.log(Level.SEVERE, msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    /**
     * 设置 CAT HOME 目录
     *
     * @param kvArgs
     */
    private static void setCatHome(Map<String, String> kvArgs) {
        final Logger logger = Logger.getLogger(TelassaProbe.class);

        String catHome = kvArgs.get("cat.home");
        logger.info("CAT HOME is " + catHome);

        if (catHome == null || catHome.isEmpty()) {
            throw new IllegalStateException("CAT HOME NOT ASSIGNED, please check your vm option.");
        }
        // 通过这里来设置系统变量 CAT_HOME
        System.setProperty("CAT_HOME", catHome);
    }

    /**
     * 设置 CAT domain
     *
     * @param kvArgs
     */
    private static void setCatDomain(Map<String, String> kvArgs) {
        CatUtils.DOMAIN = kvArgs.get("cat.domain");

        if (CatUtils.DOMAIN == null || CatUtils.DOMAIN.isEmpty()) {
            throw new IllegalStateException("CAT domain NOT ASSIGNED, please check your vm option.");
        }
    }

    /**
     * Split to {@code json} like String({@code "k1:v1,k2:v2"}) to KV map({"k1"->"v1", "k2" -> "v2"}).
     */
    static Map<String, String> splitCommaColonStringToKV(String commaColonString) {
        Map<String, String> result = new HashMap<String, String>();
        if (commaColonString == null || commaColonString.trim().length() == 0) return result;

        final String[] splitKvArray = commaColonString.trim().split("\\s*,\\s*");
        for (String kvString : splitKvArray) {
            kvString = kvString.trim();
            int index = kvString.indexOf(":");
            if (index == -1) {
                continue;
            }

            result.put(kvString.substring(0, index), kvString.substring(index + 1));
        }

        return result;
    }


    /**
     * 设置可 log 的用户名
     *
     * @param kvArgs
     */
    private static void setLoggableUsers(Map<String, String> kvArgs) {
        final Logger logger = Logger.getLogger(TelassaProbe.class);

        String loggableUserStr = kvArgs.get("loggable.users");
        if (loggableUserStr == null || loggableUserStr.length() == 0) {
            logger.info("LoggableUsers is NULL, will log all anchor. ");
            return;
        }

        String[] userArray = loggableUserStr.split("~");
        for (String user : userArray) {
            LOGGABLE_USERS.add(user);
        }

        logger.info("LoggableUsers: " + LOGGABLE_USERS.toString());
    }

}
