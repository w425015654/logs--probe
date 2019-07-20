package com.travelsky.airline.trp.ops.telassa.probe.util;

import java.util.Arrays;

/**
 * 讲 Logger 的输出转成一个 com.dianping.cat.message.Transaction 相关操作
 *
 * @author zengfan
 */
public class Logger2CatUtils {
    private static int logLength = 14;

    // 只需确保前面这几列数据都正常即可
    public static int idxOperateStatus = 0;
    public static int idxUuid = 1;
    public static int idxTxnTraceKey = 2;
    public static int idxSource = 3;
    public static int idxLogType = 4;
    public static int idxChannel = 5;
    public static int idxItinerary = 6;
    public static int idxPassengers = 7;

    // 后面这些结构，每个航司不一样，这里不再处理
//    public static int idxStartTime = 8;
//    public static int idxEndTime = 9;
//    public static int idxS1CostTime = 10;
//    public static int idxFbcInfo = 11;
//    public static int idxCostTime = 12;
    public static int idxErrorMsg = 13;     // 这个字段在 HU 里生效，在其他的航司不生效，后续这里结构需要基于每个不同的航司调整


    /**
     * 根据 loggerName 获取 CAT Transanction type 的前缀
     *
     * @return
     */
    public static String getTypePrefix(String loggerName) {
        if (loggerName == null || loggerName.isEmpty()) {
            return null;
        }

        // Shopping.** 不再截取日志取 （注意需要补齐 IBE 缓存名字部分），需要补齐 TID
        if ("AirLowSearchLogger".equals(loggerName)) {
//            return "Shopping.";
            return null;
        }

        if ("PricingLogger".equals(loggerName)) {
            return "Pricing.";
        }

        if ("PNRLogger".equals(loggerName)) {
            return "PNR.";
        }

        if ("refundLogger".equals(loggerName)) {
            return "Refund.";
        }

        if ("reshopLogger".equals(loggerName)) {
            return "Reshop.";
        }

        if ("xDistSOAPHTTPLogger".equals(loggerName)) {
            return "xDistSOAPHTTP.";
        }

        if ("UpgradePnrLogger".equals(loggerName)) {
            return "UpgradePnr.";
        }

        return null;
    }

    /**
     * 拆分如下格式 message
     * AirLowSearchLogger 格式为: [Success][83BC532F21C0FB5BCB4A4AC8F30E0ABA.2][EBDTESTN602019010409055800143135-huhkk68n1 ][B2C][OW][TSDF/][PEKHAK/17JAN19/HUGSJDCN ][1AD ][2019-01-04 09:05:57.0910][2019-01-04 09:06:02.0703][4793][]
     * <p>
     * PricingLogger 格式为: [Success][7068E567E9DDD2857E4CE9EDA63E1D65.4][EBDTESTN602018122422500600043092-huhkk68n1 ][B2C][PRICING][TSDF][OW/PEKHAK/15JAN19/HU ][1AD ][2018-12-24 22:50:06.0301][2018-12-24 22:50:06.0657][356][]
     * <p>
     * PNRLogger 格式为: [Success][0C524DF7C137F8666FBE2772AEE02994.HNAIBE10221Server15531.5][EBDTESTN602019010408570400141491-huhkk68n1 ][B2C][PACKAGEBOOKPNR][TSDF][pnrNE38W8/DR/PEKCTU/17JAN19 CTUPEK/19JAN19 ][1AD][2019-01-04 08:57:04.0122][2019-01-04 08:57:05.0559][1437][]
     * <p>
     * refundLogger 格式为: [Success][][EBDTESTN602019010410030100158478-huhkk68n1 ][B2C][REFUND][TSDF][refundETRF:8802180113080/1][][2019-01-04 10:03:00.0931][2019-01-04 10:03:03.0023][2092][]
     *
     * <p>
     * 每一列含义为
     * <p>
     * String operateStatus = (String)paramList.get("operateStatus");
     * String uuid = (String)paramList.get("uuid");
     * String txnTraceKey = (String)paramList.get("txnTraceKey");
     * String source = (String)paramList.get("source");
     * String logType = (String)paramList.get("logType");
     * String channel = (String)paramList.get("channel");
     * String itinerary = (String)paramList.get("itinerary");
     * String passengers = (String)paramList.get("passengers");
     * long startTime = Long.valueOf((String)paramList.get("startTime"));
     * long endTime = Long.valueOf((String)paramList.get("endTime"));
     * briefRQBuf.append("[").append(endTime - startTime).append("]");
     * String errorMsg = (String)paramList.get("errorMsg");
     *
     * @param message
     * @return 返回一个字符串数组，依次为
     * operateStatus、uuid、txnTraceKey、source、logType、channel、itinerary、passengers、startTime、endTime、costTime、errorMsg
     * <p>
     * logType 取值 OW/DR 等
     * channel 取值 TSDF/TSIF 等
     * source 取值 B2C/TAOBAO 等
     */
    public static String[] splitMessage(String loggerName, String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        String[] splitted = message.split("]\\[");
        String[] adjusted = adjust(splitted);


        // result 格式必须固定，暂时固定 14 列
        String[] result = new String[logLength];
        for (int i = 0; i < logLength; i++) {
            if (adjusted.length > i && adjusted[i] != null) {
                String str = adjusted[i];
                result[i] = str.replaceAll("\\[", "").replaceAll("]", "").replaceAll(" ", "");
            }
        }

//        PricingLogger 的 logType 不正确，需要从 itinerary 取
        if ("PricingLogger".equals(loggerName) && "PRICING".equals(result[idxLogType]) && result[idxItinerary] != null) {
            result[idxLogType] = result[idxItinerary].substring(0, 2);
        }

        // 不能空的地方，这里要设置初始值
        if (result[idxOperateStatus] == null) {
            result[idxOperateStatus] = "Error";
        }

        if (result[idxSource] == null || result[idxSource].isEmpty()) {
            result[idxSource] = "NVL";
        }

        if (result[idxChannel] == null || result[idxChannel].isEmpty()) {
            result[idxChannel] = "NVL";
        }

        if (result[idxItinerary] == null || result[idxItinerary].isEmpty()) {
            result[idxItinerary] = "NVL";
        }
        // 把最后的 / 去掉
        String channel = result[idxChannel];
        if (channel != null && !channel.isEmpty() && channel.lastIndexOf("/") == channel.length() - 1) {
            result[idxChannel] = channel.substring(0, channel.length() - 1);
        }
        // 对于格式 TSDF/%00EBD0JHU2292019050822065904289209*0*TRUE，TSDF/%00EBD000JHU72019050822573004431569*0*CACHE 的 channel，需要去掉中间的 EBD0JHU2292019050822065904289209
        channel = result[idxChannel];
        if (channel != null && !channel.isEmpty()) {
            int idxStart = channel.indexOf("%00");
            int idxEnd = channel.indexOf("*0*");
            if (idxStart != -1 && idxEnd != -1 && idxEnd > idxStart + 3) {
                result[idxChannel] = channel.replaceAll(channel.substring(idxStart + 3, idxEnd), "");
            }
        }

//        if (result[idxCostTime] == null || result[idxCostTime].isEmpty()) {
//            result[idxCostTime] = "0";
//        }

        return result;
    }


    /**
     * 3U 和 HU 格式稍有不同，
     *
     * @param splitted
     * @return
     */
    public static String[] adjust(String[] splitted) {
        String[] result = new String[logLength];

        // 3U 日志输出少一列，只有 11 列，没有第 2 列 uuid，需要调整一下
        if (splitted.length == 11) {
            result[idxOperateStatus] = splitted[idxOperateStatus];

            for (int i = 2; i < logLength; i++) {
                result[i] = splitted[i - 1];
            }

        } else if (splitted.length >= 8) {
            // 只是必须有 8 列
            // 12 或者 14 都是 HU 的日志格式
            result = splitted.clone();
        }

        return result;
    }


    public static void main(String[] args) {
        System.out.println(Arrays.toString(splitMessage("AirLowSearchLogger", "[Success][1DD016812373533597B77251234B0F82.HNAIBE10221Server15531.16/S1:32PtBumfzlwK3YrMCt2IShOIyOBzyQAABQn7CQEAAAA=][EBDTESTN602019050516334500064508-huhkk102n3 ][B2C][DR][TSIF][PEKBRU/08MAY19/ BRUPEK/10MAY19/ ][2AD 1CH ][2019-05-05 16:33:45.0061][2019-05-05 16:33:45.0576][][][515][0.4093s]")));

        System.out.println(Arrays.toString(splitMessage("AirLowSearchLogger", "[Success][D80C49905974DD22DCE6CD4B8D8074F3.HUIBEServer10.1][EBD0JHU2302019050823155704486077-huairvip1 ][B2C][OW][TSDF/%00EBD0JHU2302019050823155704486077*0*TRUE][SHESZX/22MAY19/HUCN ][1AD ][2019-05-08 23:15:57.0922][2019-05-08 23:15:58.0543][][][621][]")));
        System.out.println(Arrays.toString(splitMessage("AirLowSearchLogger", "[Success][FD78A6A3A672A1D841AE6DC83F365688.HUIBEServer9.2][EBD0JHU2302019050823163804487859-huairvip1 ][B2C][OW][TSDF/%00EBD000JHU72019050822573004431569*0*CACHE][SYXXIY/12JUN19/HUJD9HCN ][1AD ][2019-05-08 23:16:38.0228][2019-05-08 23:16:38.0377][][][149][]")));

        System.out.println(Arrays.toString(splitMessage("PNRLogger", " [Success][][EBDTESTN602019050516561100077099-huhkk68n1 ][B2C][PNRCANCEL][TSDF][PNRCancel:PFWV7Y/SYXPEK][][2019-05-05 16:56:11.0245][2019-05-05 16:56:12.0699][][][1454][]")));
//
        System.out.println(Arrays.toString(splitMessage("PricingLogger", "[Success][0A4EFE4F9AE1B4B1697DE4DC00F80C80.HNAIBE10221Server15531.17][EBDTESTN602019050517152200086567-huhkk68n1 ][B2C][PRICING][TSDF][OW/HAKURC/07MAY19/UQ ][1AD ][2019-05-05 17:15:22.0345][2019-05-05 17:15:22.0448][][][103][]")));
        System.out.println(Arrays.toString(splitMessage("PricingLogger", "[Success][1DD016812373533597B77251234B0F82.HNAIBE10221Server15531.28][EBDTESTN602019050516364400066246-huhkk102n3 ][B2C][PRICING][TSIF][DR/PEKSZX/15MAY19/SZXBRU/16MAY19/BRUPEK/17MAY19/][1AD 0CH ][2019-05-05 16:36:44.0322][2019-05-05 16:36:45.0633][][/ADT/NL6MFCN/key(RuleTariff::Rule::OWRT::Routing::Footnote1::Footnote2::PassengerType::FareType::OriginAddonTariff::OriginAddonFootnote1::OriginAddonFootnote2::DestinationAddonTariff::DestinationAddonFootnote1::DestinationAddonFootnote2::TravelAgencyCode::IataNumber::DepartmentCode::Origin::Destination::FareSource::FbrBaseTariff::FbrBaseRule::AccountCode::FbrBaseFareBasis::OriginAddonRouting::DestinationAddonRouting)/901::WB30::2::0000::1J::::ADT::XEX::927::T::::::::::HKK102::10000035::::BJS::BRU::ATPCO::004::CM02::::NL6MFCN::0000::/ADT/NL6MFCN/key(RuleTariff::Rule::OWRT::Routing::Footnote1::Footnote2::PassengerType::FareType::OriginAddonTariff::OriginAddonFootnote1::OriginAddonFootnote2::DestinationAddonTariff::DestinationAddonFootnote1::DestinationAddonFootnote2::TravelAgencyCode::IataNumber::DepartmentCode::Origin::Destination::FareSource::FbrBaseTariff::FbrBaseRule::AccountCode::FbrBaseFareBasis::OriginAddonRouting::DestinationAddonRouting)/901::WB30::2::0000::1J::::ADT::XEX::927::T::::::::::HKK102::10000035::::BJS::BRU::ATPCO::004::CM02::::NL6MFCN::0000::/ADT/NL6M1CN/key(RuleTariff::Rule::OWRT::Routing::Footnote1::Footnote2::PassengerType::FareType::OriginAddonTariff::OriginAddonFootnote1::OriginAddonFootnote2::DestinationAddonTariff::DestinationAddonFootnote1::DestinationAddonFootnote2::TravelAgencyCode::IataNumber::DepartmentCode::Origin::Destination::FareSource::FbrBaseTariff::FbrBaseRule::AccountCode::FbrBaseFareBasis::OriginAddonRouting::DestinationAddonRouting)/901::WB30::2::0000::1J::::ADT::XEX::::::::::::::HKK102::10000035::::BJS::BRU::ATPCO::004::CA02::::NL6M1CN::::][1311][]")));
//        System.out.println(Arrays.toString(splitMessage("PricingLogger", "[Error][Last requst did not reach handler of server, can not get transaction key.][B2C][PRICING][TSDF][PEKCTU/11JAN19/3U ][1AD ][2019-01-04 15:32:20.0421][2019-01-04 15:32:20.0501][80][com.travelsky.ibe.exceptions.IBELocalException:Exceptioncaughtinmethodsocket.connect():classjava.net.ConnectExceptionConnectionrefused]")));
//
        System.out.println(Arrays.toString(splitMessage("reshopLogger", "[Success][5F041D43D708C84D8F572FC4C370FCDA.HNAIBE10221Server15531.19][EBDTESTN602019043010304701014079-huhkk68n1 ][IBE][RESHOP][TSDF][PEKCTU/02MAY19 /NWB57N][1AD][2019-04-30 10:30:47.0379][2019-04-30 10:30:48.0980][][][1601][[1601]ADT_PNR:NWB57N]")));
//
        System.out.println(Arrays.toString(splitMessage("xDistSOAPHTTPLogger", "[Success][3B685B930653AE560B99B415A3B6DE88.HNAIBE10221Server15531.7][][B2C][queryUserPassenger][][][][][][65][]")));
//
        System.out.println(Arrays.toString(splitMessage("UpgradePnrLogger", "[Success][][EBDTESTN602019010811095000141628-huhkk68n1 ][B2C][UPGRADEPNR][TSDF][PEKFOC2019-01-24][2AD][2019-01-08 11:09:50.0779][2019-01-08 11:09:52.0199][1420][[1420]orderNO='201901081029195687']")));

    }
}
