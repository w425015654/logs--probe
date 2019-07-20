package com.travelsky.airline.trp.ops.telassa.probe.util;

import com.github.promeg.pinyinhelper.Pinyin;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @since 2.6.0
 */
public class Utils {
    /**
     * String like {@code ScheduledFuture scheduleAtFixedRate(Runnable, long, long, TimeUnit)}
     * for {@link  java.util.concurrent.ScheduledThreadPoolExecutor#scheduleAtFixedRate}.
     *
     * @param method method object
     * @return method signature string
     */
    public static String signatureOfMethod(final CtMethod method) throws NotFoundException {
        final StringBuilder stringBuilder = new StringBuilder();

        final String returnType = method.getReturnType().getSimpleName();
        final String methodName = method.getName();
        stringBuilder.append(returnType).append(" ")
                .append(methodName).append("(");

        final CtClass[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            CtClass parameterType = parameterTypes[i];
            if (i != 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(parameterType.getSimpleName());
        }

        stringBuilder.append(")");
        return stringBuilder.toString();
    }


    /**
     * 数组转为字符串
     *
     * @param args
     * @return
     */
    public static String arrayToString(String[] args) {
        if (args == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            builder.append(arg).append(" ");
        }

        return builder.toString();
    }


    /**
     * 通过 Webservice Header 传递的中文需要编码一下
     * 编码错误的话，就把原文返回
     */
    public static String encodeHeader(String header) {
        if (header == null) {
            return null;
        }

        try {
            return java.net.URLEncoder.encode(header, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return header;
    }

    /**
     * 通过 Webservice Header 传递的中文需要解码一下
     * 解码错误也不能影响主流程
     */
    public static String decodeHeader(String header) {
        if (header == null) {
            return null;
        }

        try {
            return java.net.URLDecoder.decode(header, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return header;
    }

    /**
     * 在 url 后面添加参数
     * 如: /common/spinner.do 添加为 /common/spinner.do?key=value
     *
     * @param url
     * @param key
     * @param value
     * @return
     */
    public static String addQueryString(String url, String key, String value) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // 跳转到港航首页时，不能带参数，否则会 404
        if (url.contains("www.hongkongairlines.com")) {
            return url;
        }

        // 已添加过 key 的不再添加，避免覆盖
        if (url.contains(key + "=")) {
            return url;
        }

        // 63 表示 ？
        return url.contains("?") ?
                url + "&" + key + "=" + value :
                url + "?" + key + "=" + value;
    }

    /**
     * 解析出 url 中的 queryString 键值对
     *
     * @param url
     * @return
     */
    public static Map<String, String> parseQueryString(String url) {
        Map<String, String> result = new HashMap<String, String>();
        if (url == null || url.isEmpty()) {
            return result;
        }

        int index = url.indexOf("?");
        if (index != -1) {
            url = url.substring(index + 1);
        }

        String[] splitted = url.split("&");
        for (String str : splitted) {
            String pair[] = str.split("=", 2);
            if (pair.length != 2) {
                result.put(pair[0], "");
            } else {
                result.put(pair[0], pair[1]);
            }
        }

        return result;
    }


    /**
     * 从
     * /../../profile/config/xsl/login/OTA_ReadRQ.xsl
     * /../../deeplink/config/xsl/entryRedirect.xsl
     * 格式里取出 Name 值，即 OTA_ReadRQ、entryRedirect
     *
     * @param xls
     * @return
     */
    public static String getXlsTransformName(String xls) {
        if (xls == null) {
            return "UNKNOWN";
        }

        int endIdx = xls.lastIndexOf('.');
        if (endIdx == -1) {
            endIdx = xls.length();
        }
        String result = xls.substring(xls.lastIndexOf('/') + 1, endIdx);

        if (result.length() == 0) {
            return "UNKNOWN";
        }

        return result;
    }

    /**
     * 将中文字符串转拼音，并将首字母大写
     * 如 海航机票+酒店产品预订成功 转成 HaiHangJiPiao+JiuDianChanPinYuDingChengGong
     * 订单成功取消-海南航空 转成 DingDanChengGongQuXiao-HaiNanHangKong
     *
     * @param str
     * @return
     */
    public static String toPinyin(String str) {
        if (str == null || str.length() == 0) {
            return "EMPTY";
        }

        // 先全部转成小写，按 分隔
        try {
            StringBuilder result = new StringBuilder();

            String lowercase = Pinyin.toPinyin(str, " ").toLowerCase();
            String[] splitted = lowercase.split(" ");

            for (int i = 0; i < splitted.length; i++) {
                char[] charArray = splitted[i].toCharArray();
                // 首字母如果是小写字母，则转大写
                if (Character.isLowerCase(charArray[0])) {
                    charArray[0] -= 32;
                }
                result.append(String.valueOf(charArray));
            }

            return result.toString();
        } catch (Exception e) {
            // 发生异常不处理，不能影响正常流程
        }

        return str;
    }

    /**
     * 提取 IBEClient 接口里面的 Exception 的信息, 用于记录 errorDesc 字段
     *
     * @return
     */
    public static String parseIBEClientException(Exception e) {
        String errorDesc = e.getMessage();
        if (errorDesc == null) {
            return errorDesc;
        }

        try {

            // 只取第一行
            int index = errorDesc.indexOf("\r\n");
            if (index == -1) {
                index = errorDesc.indexOf("\n");
                if (index == -1) {
                    index = errorDesc.length();
                }
            }
            errorDesc = errorDesc.substring(0, index);

            // 如果有 错误信息为：,则截取此之后的字符串
//            index = errorDesc.indexOf("错误信息为：");
//            if (index != -1) {
//                return errorDesc.substring(index + "错误信息为：".length());
//            }

            // replace 掉跟 异常类名一样的部分
            String className = e.getClass().getName();
            errorDesc = errorDesc.replace(className + ":", "").trim();

            // 针对不同的异常，要进一步处理
            if ("com.travelsky.ibe.exceptions.RTNotAuthorizedException".equals(className)) {
                // 截掉 NKQC4R:AUTHORITY 前面的 PNR 信息，以便归类
                index = errorDesc.indexOf(":");
                if (index != -1) {
                    errorDesc = errorDesc.substring(index + 1);
                }
            } else if ("com.travelsky.ibe.exceptions.RTPNRCancelledException".equals(className)) {
                // 截取 PNR:KXP8GM was entirely cancelled. was entirely cancelled.||app:huhkk102n3||customno:37207||validationno:11||ServerAddress:10.221.136.60:6891||OK ServerVer  20140715/EBDTESTN602019060418144700060399-huhkk102n3 ||
                // 中的有效部分，即 was entirely cancelled.
                index = errorDesc.indexOf("was entirely cancelled.");
                if (index != -1) {
                    errorDesc = "PNR was entirely cancelled.";
                }
            } else if ("com.travelsky.ibe.exceptions.RTNoPNRException".equals(className)) {
                // 截掉 No such PNR:JR78RK 后面的 PNR 信息，以便归类
                index = errorDesc.indexOf("No such PNR:");
                if (index != -1) {
                    errorDesc = "No such PNR";
                }
            } else if ("com.travelsky.ibe.exceptions.DecodeErrorException".equals(className)) {
                index = errorDesc.indexOf("RT Decode error");
                if (index != -1) {
                    errorDesc = "RT Decode error";
                }
            } else if ("com.travelsky.ibe.exceptions.DETRException".equals(className)) {
                if (errorDesc.contains("Host Error:")) {
                    String fullMsg = e.getMessage();
                    index = fullMsg.indexOf("Host Error:");
                    int indexEnd = fullMsg.indexOf("app:");
                    if (indexEnd > index) {
                        errorDesc = fullMsg.substring(index, indexEnd);
                    }
                }
            } else if ("com.travelsky.ibe.exceptions.SSFltUnableException".equals(className)) {
                // 指定的航班在指定的日期不执行或指定的舱位已经无法订取。(3U 8156 G 03JUN XNNCTU NN2 UNABLE
                // 截取 。 前面的部分
                index = errorDesc.indexOf("。");
                if (index != -1) {
                    errorDesc = errorDesc.substring(0, index);
                }
            } else if ("com.travelsky.ibe.exceptions.ReshopException".equals(className)) {
                // <message>新行程SHOPPING无结果[0FS_FRONT;HostName:nf5280z53-app;All Duration:5;RequestProcess:0;]</message>
                if (errorDesc.contains("<Error>")) {
                    String fullMsg = e.getMessage();
                    index = fullMsg.indexOf("<message>");
                    int indexEnd = fullMsg.indexOf("</message>");
                    if (indexEnd > index) {
                        errorDesc = fullMsg.substring(index, indexEnd);
                    }
                }
            }

            return errorDesc.replace("\n", "");
        } catch (Exception e1) {
            // 异常不做任何处理，只截取部分文本进行返回
            return errorDesc.substring(0, 128).replace("\n", "");
        }

    }


    public static void main(String[] args) {
//        String aa = encodeHeader("屠星辰test~AEBCEDBD906D833241CB087156C79E07~hu-dev-SIT-c0a8099f-429268-4155");
//        System.out.println(aa);
//        System.out.println(decodeHeader("IbeNotLogin%7E9F4FBAB4B7FC395337AA8D3568F0BDD3%7Ehu-dev-SIT-c0a81f3c-429300-1124"));
//
//        System.out.println();
//
//        String session$Id = "Scheduler:Insurance claim 5 Minute:232323";
//        System.out.println(session$Id.substring(session$Id.indexOf(":") + 1, session$Id.lastIndexOf(":")));
//
//        System.out.println(addQueryString("/common/spinner.do", "telassa-id", "333"));
//        System.out.println(parseQueryString("/common/spinner.do?telassa-id=333&ddd=222"));


//        System.out.println(getXlsTransformName("/../../profile/config/xsl/login/OTA_ReadRQ.xsl"));
//        System.out.println(getXlsTransformName(null));
//        System.out.println(getXlsTransformName(""));
//        System.out.println(getXlsTransformName("/../../deeplink/config/xsl/entryRedirect.xsl"));

//        System.out.println(toPinyin(null));
//        System.out.println(toPinyin("海航机票+酒店产品预订成功"));
//        System.out.println(toPinyin("订单成功取消-海南航空"));


//        System.out.println(parseIBEClientException(new Exception("")));
//
//        System.out.println(parseIBEClientException(new SSFltUnableException("\tcom.travelsky.ibe.exceptions.SSFltUnableException: 指定的航班在指定的日期不执行或指定的舱位已经无法订取。(3U 8156 G 03JUN XNNCTU NN2 UNABLE \n" +
//                "UNABLE TO SELL.PLEASE CHECK THE AVAILABILITY WITH \"AV\" AGAIN) \n" +
//                "EBD00HIAL72019060315060823689017 \n" +
//                "app:3ub2c \n" +
//                "customno:0 \n" +
//                "validationno:20 \n" +
//                "ServerAddress:10.8.128.230:6891 \n" +
//                "OK ServerVer 20140715/EBD00HIAL72019060315060823689017-3ub2c ")));
//
//
//        System.out.println(parseIBEClientException(new IBEException("com.travelsky.ibe.exceptions.IBEException: java.lang.Exception: 预付费座位预定,doASR失败:ASRErrorException \n" +
//                "3U8107/Y/07JUN19/CZXTAO/32Z ASR FAILED \n" +
//                "Y 0 1 2 3 \n" +
//                "4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 \n" +
//                "RF X X X $ $ X $ $ $ $ $ $ $ $ $ $ X X $ $ $ $ $ $ $ $ X X X FR \n" +
//                "RE X X X $ $ X X $ $ $ $ $ $ $ $ $ $ X X $ $ $ $ $ $ $ $ X X X ER \n" +
//                "RD X X X $ $ X X $ $ $ $ $ $ $ $ $ $ X X $ $ $ $ $ $ $ $ X X X DR \n" +
//                "R = = = = = E E = = = = = = = = = = E = = = = = = = = = = = = R \n" +
//                "L = = = = = E E = = = = = = = = = = E = = = = = = = = = = = = L \n" +
//                "LC X X X ! $ X X $ $ $ $ $ $ $ $ $ $ X X $ $ $ $ $ $ $ $ X X X CL \n" +
//                "LB X X X $ $ X X $ ! $ $ $ $ $ $ $ $ X X $ $ $ $ $ $ $ $ X X X BL \n" +
//                "LA X X X $ $ X $ ! $ $ $ $ $ $ $ $ X $ $ $ $ $ $ $ $ X X X AL \n" +
//                "4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 + \n" +
//                "\u001E \n" +
//                "app:3ub2c \n" +
//                "customno:0 \n" +
//                "validationno:20 \n" +
//                "ServerAddress:10.8.128.230:6891 \n" +
//                "OK ServerVer 20140715/EBD00HIAL62019060315223423765945-3ub2c ")));
//
//        System.out.println(parseIBEClientException(new IBEException("\tcom.travelsky.ibe.exceptions.IBEException: com.travelsky.ibe.exceptions.FlightShoppingException: 错误信息为：<Error><code>160101</code><message>无可用AV</message> \n" +
//                "app:huairvip1 \n" +
//                "customno:0 \n" +
//                "validationno:15 \n" +
//                "ServerAddress:10.8.208.4:6891 \n" +
//                "OK ServerVer 20140715/EBD0JHU2292019060311001613564874-huairvip1 ")));
//
//        System.out.println(parseIBEClientException(new FlightShoppingException("\tcom.travelsky.ibe.exceptions.FlightShoppingException: 错误信息为：<Error><code>160101</code><message>无可用AV</message> \n" +
//                "app:huairvip1 \n" +
//                "customno:0 \n" +
//                "validationno:15 \n" +
//                "ServerAddress:10.8.208.4:6891 \n" +
//                "OK ServerVer 20140715/EBD0JHU2302019060311004221509651-huairvip1 \n")));
//
//
//        System.out.println(parseIBEClientException(new ETDZFailedException("\tcom.travelsky.ibe.exceptions.ETDZFailedException: Error : INACTIVE \n" +
//                "&#30; \n" +
//                "app:huairvip1 \n" +
//                "customno:0 \n" +
//                "validationno:15 \n" +
//                "ServerAddress:10.8.208.4:6891 \n" +
//                "OK ServerVer 20140715/EBD0JHU2292019060311284013671592-huairvip1 ")));
//
//
//        System.out.println(parseIBEClientException(new SearchOneException("\tcom.travelsky.ibe.exceptions.SearchOneException: EBuild API try check agency's OfficeIdOwnedByCarrier and System, but can not get DA info of User huhkk102n3 from USAS. Please check MCSS/USAS or contact EBuild to ignore the param check. \n" +
//                "app:huhkk102n3 \n" +
//                "customno:37207 \n" +
//                "validationno:11 \n" +
//                "ServerAddress:10.221.136.60:6891 \n" +
//                "OK ServerVer 20140715/EBDTESTN602019060311013400002922-huhkk102n3 ")));
//
//
//        System.out.println(parseIBEClientException(new NetworkConnectionException("com.travelsky.ibe.exceptions.NetworkConnectionException: java.net.SocketTimeoutException: Read timed out \n")));
//        System.out.println(parseIBEClientException(new AccessDenyException("com.travelsky.ibe.exceptions.AccessDenyException: Access Denied 10.221.155.33:huairetd1:HAK969:0:9")));
//        System.out.println(parseIBEClientException(new RTNotAuthorizedException("com.travelsky.ibe.exceptions.RTNotAuthorizedException: MTK9GF:AUTHORITY")));
//
//        System.out.println(parseIBEClientException(new RTNotAuthorizedException("\tcom.travelsky.ibe.exceptions.RTNotAuthorizedException: NKQC4J:AUTHORITY \n" +
//                "app:huhkk68n1 \n" +
//                "customno:0 \n" +
//                "validationno:9 \n" +
//                "ServerAddress:10.221.136.60:6891 \n" +
//                "OK ServerVer 20140715/EBDTESTN602019060215315001327381-huhkk68n1 ")));
//
//
//        System.out.println(parseIBEClientException(new DecodeErrorException("\tcom.travelsky.ibe.exceptions.DecodeErrorException: DecodeErrorException: DecodeErrorException: RT Decode error: input pnr is PWP3VC \n" +
//                "out put is \n" +
//                "1.兄越铧 2.远芮 3.招芮 4.招衅 HPWP3V \n" +
//                "5.NKG/T NKG/T025-80295553/NANJING TUZHILV TICKET SERVICE CO., LTD./FUCHAO \n" +
//                "ABCDEFG \n" +
//                "6.TL/1216/10APR19/NKG166 \n" +
//                "7.SSR FOID \n" +
//                "8.SSR FOID \n" +
//                "9.SSR FOID \n" +
//                "10.SSR FOID \n" +
//                "11.SSR OTHS 1E MF8237 CXLD DUE TO DUPLICATE WITH MLC0CC \n" +
//                "12.SSR ADTK 1E BY NKG10APR19/1216 OR CXL MF8237 V05MAY \n" +
//                "13.OSI MF CTCT13817461929 \n" +
//                "14.OSI MF CTCM15861604787/P1 + \n" +
//                "15.OSI MF CTCM13580159432/P3/4 - \n" +
//                "16.OSI MF CTCM15822933681/P2 \n" +
//                "17.RMK TJ AUTH CAN684/T \n" +
//                "18.RMK SCZL \n" +
//                "19.RMK CA/MTJH49 \n" +
//                "20.NKG166 \n" +
//                "app:huhkk102n3 \n" +
//                "customno:37207 \n" +
//                "validationno:11 \n" +
//                "ServerAddress:10.221.136.60:6891 \n" +
//                "OK ServerVer 20140715/EBDTESTN602019060215592301333554-huhkk102n3 ")));
//
//        System.out.println(parseIBEClientException(new com.travelsky.ibe.exceptions.DETRException("com.travelsky.ibe.exceptions.DETRException: com.travelsky.ibe.exceptions.IBEException: Host Error: \r\n" +
//                "Current ET Data:ET PASSENGER DATA NOT FOUND \r\n" +
//                "app:huhkk68n1 \n" +
//                "customno:0 \n" +
//                "validationno:9 \n" +
//                "ServerAddress:10.221.136.60:6891 \n" +
//                "OK ServerVer 20140715/EBDTESTN602019060611010600003140-huhkk68n1 ")));
//
//
//        System.out.println(parseIBEClientException(new ReshopException("\tcom.travelsky.ibe.exceptions.ReshopException: 错误信息为：<Error> \n" +
//                "<code>600004</code> \n" +
//                "<message>新行程SHOPPING无结果[0FS_FRONT;HostName:nf5280z53-app;All Duration:5;RequestProcess:0;]</message> \n" +
//                "app:3ub2c \n" +
//                "customno:0 \n" +
//                "validationno:20 \n" +
//                "ServerAddress:10.8.128.230:6891 \n" +
//                "OK ServerVer 20140715/EBD00HIAL62019060413285628928815-3ub2c ")));
//
//
//        System.out.println(parseIBEClientException(new SSBannedPassengerException("\tcom.travelsky.ibe.exceptions.SSBannedPassengerException: 旅客1已被人民法院限制高消费和相关消费,有问题请与执行法院联系. \n" +
//                "app:3ub2c \n" +
//                "customno:0 \n" +
//                "validationno:20 \n" +
//                "ServerAddress:10.8.128.230:6891 \n" +
//                "OK ServerVer 20140715/EBD00HIAL52019060413285928899520-3ub2c ")));

//        System.out.println(parseIBEClientException(new 	com.travelsky.ibe.exceptions.NetworkConnectionException	("\tcom.travelsky.ibe.exceptions.NetworkConnectionException: java.net.SocketTimeoutException: Read timed out \n" +
//                "at java.net.SocketInputStream.socketRead0(Native Method) \n" +
//                "at java.net.SocketInputStream.read(SocketInputStream.java:152) \n" +
//                "at java.net.SocketInputStream.read(SocketInputStream.java:122) \n" +
//                "at java.io.DataInputStream.readFully(DataInputStream.java:195) \n" +
//                "at java.io.DataInputStream.readFully(DataInputStream.java:169) \n" +
//                "at com.travelsky.util.CommandReader2.getCommand2(CommandReader2.java:63) ")));

//        System.out.println(parseIBEClientException(new com.travelsky.ibe.exceptions.PMSimultaneousModificationException("com.travelsky.ibe.exceptions.PMSimultaneousModificationException: PNR同步修改错误，请重新提交修改请求，PNR可能已经发生修改，可能需要重新检查输入的参数 \n" +
//                "app:3ub2c \n" +
//                "customno:0 \n" +
//                "validationno:20 \n" +
//                "ServerAddress:10.8.128.230:6891 \n" +
//                "OK ServerVer 20140715/EBD00HIAL52019060909515225452367-3ub2c  ")));
//
//
//        System.out.println(parseIBEClientException(new com.travelsky.ibe.exceptions.NoAvailableResourceException("\tcom.travelsky.ibe.exceptions.NoAvailableResourceException: No Available SearchOne http Connection or SearchOne http Connection Failedi:search12:10.5.74.39:2913 \n" +
//                "app:3ub2c \n" +
//                "customno:0 \n" +
//                "validationno:20 \n" +
//                "ServerAddress:10.8.128.230:6891 \n" +
//                "OK ServerVer 20140715/EBD00HIAL72019060909310025359866-3ub2c ")));
//
//        System.out.println(parseIBEClientException(new com.travelsky.ibe.exceptions.RefundException("\tcom.travelsky.ibe.exceptions.RefundException: com.travelsky.ibe.exceptions.RefundException: 错误信息为：<?xml version=\"1.0\" encoding=\"GBK\"?><FareInterface><Output><Error><errorCode>302:HISTORY FARE IS ERROR,PLEASE HANDLE IT MANUALLY.</errorCode></Error></Output></FareInterface> \n" +
//                "app:3ub2c \n" +
//                "customno:0 \n" +
//                "validationno:20 \n" +
//                "ServerAddress:10.8.128.230:6891 \n" +
//                "OK ServerVer 20140715/EBD00HIAL62019060909100725260984-3ub2c ")));
//
//
//        System.out.println(parseIBEClientException(new com.travelsky.ibe.exceptions.PMSegmentMissingException("\tcom.travelsky.ibe.exceptions.PMSegmentMissingException: 请检查航段是否格式正确或缺失。(CHECK SEGMENT) \n" +
//                "app:3ub2c \n" +
//                "customno:0 \n" +
//                "validationno:20 \n" +
//                "ServerAddress:10.8.128.230:6891 \n" +
//                "OK ServerVer 20140715/EBD00HIAL72019060909230625325569-3ub2c \n")));
//
//
//        System.out.println(parseIBEClientException(new 	com.travelsky.ibe.exceptions.IBEException("\tcom.travelsky.ibe.exceptions.IBEException: CAN NOT PRINT NEW REFUND \n" +
//                "app:3ub2c \n" +
//                "customno:0 \n" +
//                "validationno:20 \n" +
//                "ServerAddress:10.8.128.230:6891 \n" +
//                "OK ServerVer 20140715/EBD00HIAL62019060909435425408211-3ub2c ")));
//        System.out.println(parseIBEClientException(new com.travelsky.ibe.exceptions.SellSeatException("com.travelsky.ibe.exceptions.SellSeatException: PNR Error :3U 8742 D 26AUG CANCKG NN1 UNABLE \n" +
//                "UNABLE TO SELL.PLEASE CHECK THE AVAILABILITY WITH \"AV\" AGAIN \n" +
//                "&#30; \n" +
//                "app:3ub2c \n" +
//                "customno:0 \n" +
//                "validationno:20 \n" +
//                "ServerAddress:10.8.128.230:6891 \n" +
//                "OK ServerVer 20140715/EBD00HIAL72019061910030034682732-3ub2c \n" +
//                "\n" +
//                "at com.travelsky.ibe.client.IBEClient.original$query$method$renamed$by$telassa(IBEClient.java:1247) \n" +
//                "at com.travelsky.ibe.client.IBEClient.query(IBEClient.java) \n" +
//                "at com.travelsky.ibe.client.pnr.SellSeat.commit1(SellSeat.java:2073) \n" +
//                "at com.openjaw.connector.travelsky.domestic.messages.OTA_AirBook.processMessage(OTA_AirBook.java:862) \n" +
//                "at com.openjaw.connector.travelsky.domestic.DomesticConnector.original$process$method$renamed$by$telassa(DomesticConnector.java:90) \n" +
//                "at com.openjaw.connector.travelsky.domestic.DomesticConnector.process(DomesticConnector.java) \n" +
//                "at com.openjaw.connection.java.JavaConnectorBase.process(JavaConnectorBase.java:124) \n" +
//                "at com.openjaw.connection.java.JavaConnection.processMessage(JavaConnection.java:242) \n" +
//                "at com.openjaw.connection.Connection.processMessage(Connection.java:396) \n" +
//                "at com.openjaw.serviceProvider.SPMSupplierThread.applySend(SPMSupplierThread.java:445) \n" +
//                "at com.openjaw.serviceProvider.SPMSupplierThread.original$process$method$renamed$by$telassa(SPMSupplierThread.java:633) \n" +
//                "at com.openjaw.serviceProvider.SPMSupplierThread.process(SPMSupplierThread.java) \n" +
//                "at com.openjaw.serviceProvider.SPMSupplierThread.run(SPMSupplierThread.java:794) \n" +
//                "at java.lang.Thread.run(Thread.java:745) ")));
    }
}
