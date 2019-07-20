package com.travelsky.airline.trp.ops.telassa.probe.util;

import com.sun.org.apache.xpath.internal.NodeSet;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * DOM 相关操作
 *
 * @author zengfan
 */
public class DomUtils {


    /**
     * org.w3c.dom.NodeList 转成 String 输出
     *
     * @param nodes
     * @return
     */
    public static String nodeListToString(NodeList nodes) {
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }

        String result = null;
        try {
            StringWriter stringWriter = new StringWriter();

            DOMSource domSource = new DOMSource();
            StreamResult streamResult = new StreamResult(stringWriter);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            for (int i = 0; i < nodes.getLength(); ++i) {
                domSource.setNode(nodes.item(i));
                transformer.transform(domSource, streamResult);
            }
            result = stringWriter.toString();
            result = result.replace(" xmlns=\"http://www.opentravel.org/OTA/2003/05\"", "");
        } catch (Exception e) {
            // 异常不处理，就当是取错误信息失败，不能抛异常处理影响主流程
        }

        return result;
    }

    /**
     * org.w3c.dom.Node 转成 String 输出
     *
     * @param node
     * @return
     */
    public static String nodeToString(Node node, String tagName) {
        String domain = CatUtils.DOMAIN;
        if (domain == null) {
            return tagName + " *** DOMAIN IS NULL ***.";
        }
        domain = domain.toLowerCase();

        // 生产的 domain 发往 生产的 kafka，测试的 domain 发往测试的 kafka
        if (domain.endsWith("prd")) {
            return tagName + " *** OMITTED AT PRD ***.";
        }


        // 对已忽略的 RQ 返回 Ignored
        if ("OTA_AirLowFareSearchRQ".equalsIgnoreCase(tagName) ||
                "OJ_SuperPNRWriteRQ".equalsIgnoreCase(tagName) ||
                "OJ_SuperPNRReserveID_RQ".equalsIgnoreCase(tagName) ||
                "OTA_TravelItineraryReadRQ".equalsIgnoreCase(tagName)) {
            return tagName + " *** OMITTED ***.";
        }


        if (node == null) {
            return null;
        }

        String result = null;
        try {
            StringWriter stringWriter = new StringWriter();

            // 将 Password 字段加密
            Node tobeOutput = encryptNode(node, "Password");
            if (tobeOutput == null) {
                return "";
            }

            DOMSource domSource = new DOMSource();
            StreamResult streamResult = new StreamResult(stringWriter);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            domSource.setNode(tobeOutput);
            transformer.transform(domSource, streamResult);
            result = stringWriter.toString();
        } catch (Exception e) {
            // 异常不处理，就当是取错误信息失败，不能抛异常处理影响主流程
        }

        return result;
    }


    /**
     * 获取 CacheType 中的信息，看 Errors 节点是否有值，没有则问 CacheHit
     *
     * @param document
     * @return
     */
    public static String getSearchCacheType(Document document) {
        if (document == null) {
            return "NVL";
        }

        String type = selectSingleNodeAsString(document, "OTA_AirLowFareSearchRS/Errors/Error/@Type");
        if (!"CacheMiss".equals(type) && !"ReadCacheError".equals(type)) {
            return "CacheHit";
        }

        return type;
    }

    /**
     * 将 node 中 tagName 对应的字段加密
     *
     * @param node
     * @param tagName
     */
    private static Node encryptNode(Node node, String tagName) {
        if (node instanceof Document) {
            org.w3c.dom.NodeList tryToFind = ((Document) node).getElementsByTagName(tagName);
            if (tryToFind == null) {
                return node;
            }

            // 如果需要修改，则 copy 出来修改
            Node result = node.cloneNode(true);
            org.w3c.dom.NodeList tobeEncrypt = ((Document) result).getElementsByTagName(tagName);
            for (int i = 0; i < tobeEncrypt.getLength(); ++i) {
                tobeEncrypt.item(i).setTextContent("***");
            }

            return result;
        }
        return null;
    }


    /**
     * 基于 SearchRQ 生成 Cat Name，
     * 如 OTA_AirLowFareSearchRQ，取到 TSIF.OW.1AD1CH
     *
     * @param node
     */
    public static String getSearchRQCatName(Node node) {
        if (node == null || !(node instanceof Document)) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        Element element = ((Document) node).getDocumentElement();
        if (element == null || !("OTA_AirLowFareSearchRQ".equalsIgnoreCase(element.getTagName())
                || "ota:OTA_AirLowFareSearchRQ".equalsIgnoreCase(element.getTagName()))) {
            return null;
        }


        sb.append(getRQArea(element)).append(".");                  // 国际/国内: TSIF/TSDF
        sb.append(getRQItineraryIdentifier(element)).append(".");   // 行程类型: OW/DR/DM
        sb.append(getRQTravelerSummary(element)).append(".");       // 旅客类型: 1ADT/1CHD
        sb.append(getRQBookingClass(element));                      // 仓位类型: YMT

        return sb.toString();
    }


    /**
     * 获取 RQ 是 TSIF/TSDF，取不到的时候返回 NVL;
     * 全部都是国内时，返回 TSDF，有一个国际就返回 TSIF，不能判断就返回 NVL
     *
     * @param element
     * @return
     */
    private static String getRQArea(Element element) {
        NodeList ods = selectNodeList(element, "OriginDestinationInformation");
        if (ods == null || ods.getLength() == 0) {
            return "NVL";
        }

        for (int i = 0; i < ods.getLength(); ++i) {
            Node od = ods.item(i);
            String orgContext = selectSingleNodeAsString(od, "OriginLocation/@CodeContext");
            String orgLocation = selectSingleNodeAsString(od, "OriginLocation/@LocationCode");
            // 只要有一个 NVL，那么就是 NVL；只要有一个 TSIF，那么就是 TSIF
            String orgAreaCode = getAreaCode(orgContext, orgLocation);
            if (!("TSDF".equals(orgAreaCode))) {
                return orgAreaCode;
            }

            String dstContext = selectSingleNodeAsString(od, "DestinationLocation/@CodeContext");
            String dstLocation = selectSingleNodeAsString(od, "DestinationLocation/@LocationCode");
            // 只要有一个 NVL，那么就是 NVL；只要有一个 TSIF，那么就是 TSIF
            String dstAreaCode = getAreaCode(dstContext, dstLocation);
            if (!("TSDF".equals(dstAreaCode))) {
                return dstAreaCode;
            }
        }

        return "TSDF";
    }


    /**
     * 获取 RQ 的行程 OW/DR/DM 编号，取不到的时候返回 NVL;
     *
     * @param element
     * @return
     */
    private static String getRQItineraryIdentifier(Element element) {
        String identifier = selectSingleNodeAsString(element, "@TransactionIdentifier");
        if (null == identifier || identifier.isEmpty()) {
            return "NVL";
        } else if ("O".equals(identifier)) {
            return "OW";
        } else if ("D".equals(identifier)) {
            return "DR";
        }

        return identifier;
    }

    /**
     * 获取 RQ 的行程明细，逐个行程组装查询条件, HAKPEK/20190620;
     *
     * @param node
     * @return
     */
    public static String getRQItineraryDetail(Node node) {
        if (node == null || !(node instanceof Document)) {
            return null;
        }

        Element element = ((Document) node).getDocumentElement();
        StringBuilder sb = new StringBuilder();

        NodeList ods = selectNodeList(element, "OriginDestinationInformation");
        if (ods == null || ods.getLength() == 0) {
            return null;
        }

        for (int i = 0; i < ods.getLength(); ++i) {
            Node od = ods.item(i);
            String deptDateTime = selectSingleNodeAsString(od, "DepartureDateTime").replace("-", "");
            String orgLocation = selectSingleNodeAsString(od, "OriginLocation/@LocationCode");
            String dstLocation = selectSingleNodeAsString(od, "DestinationLocation/@LocationCode");

            sb.append(orgLocation).append(dstLocation).append("/").append(deptDateTime).append(";");
        }

        return sb.toString();
    }

    /**
     * 获取 RQ 的 IATA 明细，逐个行程组装查询条件, HAKPEK，这里不带日期
     * 对于国际网站，暂时没有 IATA 三字码，直接取原来的值
     *
     * @param node
     * @return
     */
    public static String getRQIATA(Node node) {
        if (node == null || !(node instanceof Document)) {
            return null;
        }

        Element element = ((Document) node).getDocumentElement();
        StringBuilder sb = new StringBuilder();

        NodeList ods = selectNodeList(element, "OriginDestinationInformation");
        if (ods == null || ods.getLength() == 0) {
            return null;
        }

        for (int i = 0; i < ods.getLength(); ++i) {
            Node od = ods.item(i);
            String orgLocation = selectSingleNodeAsString(od, "OriginLocation/@LocationCode");
            String dstLocation = selectSingleNodeAsString(od, "DestinationLocation/@LocationCode");

            String orgIATA = location2IATA(orgLocation);
            String dstIATA = location2IATA(dstLocation);

            sb.append(orgIATA).append(dstIATA).append("/");
        }

        if (sb.length() != 0) {
            // 去掉最后的 /
            return sb.substring(0, sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * locationCode 转 IATA 三字码，如果没有则保持原状
     *
     * @param locationCode
     * @return
     */
    private static String location2IATA(String locationCode) {
        if (locationCode == null || locationCode.length() == 0) {
            return null;
        }

        String result = AirportCode.TSDF_IATA.get(locationCode);
        if (result == null || result.length() == 0) {
            return locationCode;
        }

        return result;
    }


    /**
     * 取 RQ 的旅客信息 1ADT1CHD，取不到的时候返回 NVL
     *
     * @param element
     * @return
     */
    private static String getRQTravelerSummary(Element element) {
        StringBuilder sb = new StringBuilder();

        NodeList travelerList = selectNodeList(element, "TravelerInfoSummary/AirTravelerAvail/PassengerTypeQuantity");
        for (int i = 0; i < travelerList.getLength(); ++i) {
            Node traveler = travelerList.item(i);
            String code = selectSingleNodeAsString(traveler, "@Code");
            String quantity = selectSingleNodeAsString(traveler, "@Quantity");

            // 0 个的不需要列出来
            if ("0".equals(quantity)) {
                continue;
            }

            sb.append(quantity).append(code);
        }

        if (sb.length() == 0) {
            return "NVL";
        }

        return sb.toString();
    }

    /**
     * 获取 RQ 的预定仓位
     *
     * @param element
     * @return
     */
    private static String getRQBookingClass(Element element) {
        StringBuilder sb = new StringBuilder();
        NodeList bookingClasses = selectNodeList(element, "SpecificFlightInfo/BookingClassPref");
        for (int i = 0; i < bookingClasses.getLength(); ++i) {
            Node bookingClass = bookingClasses.item(i);
            String code = selectSingleNodeAsString(bookingClass, "@ResBookDesigCode");
            sb.append(code);
        }

        if (sb.length() == 0) {
            return "NVL";
        }

        return sb.toString();
    }

    /**
     * 获取对应 code 的编码
     *
     * @param context
     * @param locationCode
     * @return
     */
    private static String getAreaCode(String context, String locationCode) {
        if ("LocationId".equalsIgnoreCase(context)) {
            if (AirportCode.TSDF_IATA.containsKey(locationCode)) {
                return "TSDF";
            }

            // LocationId 的中文机场以 _CN 结尾
            if (locationCode != null && locationCode.endsWith("_CN")) {
                return "TSDF";
            }

            // 上海浦东的 code 是 CitCnSHANGHA017, 需要特殊判断一下
            if (locationCode != null && (locationCode.startsWith("CitCn") || locationCode.startsWith("CityCn"))) {
                return "TSDF";
            }
        } else if ("IATA".equalsIgnoreCase(context)) {
            if (AirportCode.TSDF.contains(locationCode)) {
                return "TSDF";
            }
        } else {
            return "NVL";
        }

        return "TSIF";
    }


    /**
     * 代码 copy 自 OpenJawXPathAPI
     *
     * @param n
     * @return
     */
    public static String getNodeValueAsString(Node n) {
        if (n == null) {
            return "";
        } else {
            String value = n.getNodeValue();
            if (value != null) {
                return value;
            } else {
                StringBuilder buf = new StringBuilder();
                if (n.getNodeType() == 1) {
                    NodeList nl = n.getChildNodes();

                    for (int j = 0; j < nl.getLength(); ++j) {
                        if (nl.item(j).getNodeType() == 3) {
                            buf.append(nl.item(j).getNodeValue());
                        } else if (nl.item(j).getNodeType() == 4) {
                            buf.append(nl.item(j).getNodeValue());
                        }
                    }
                }

                return buf.toString();
            }
        }
    }

    /**
     * 代码 copy 自 OpenJawXPathAPI
     *
     * @param node
     * @param xpath
     * @return
     */
    public static String selectSingleNodeAsString(Node node, String xpath) {
        if (xpath != null && xpath.length() != 0) {
            node = selectSingleNode(node, xpath);
            return node == null ? "" : getNodeValueAsString(node);
        } else {
            return getNodeValueAsString(node);
        }
    }


    /**
     * 代码 copy 自 OpenJawXPathAPI
     *
     * @param node
     * @param xpath
     * @return
     */
    public static Node selectSingleNode(Node node, String xpath) {
        if (xpath.equals("/")) {
            return node;
        } else if (xpath.equals("")) {
            return node;
        } else {
            if (xpath.startsWith("/")) {
                xpath = xpath.substring(1);
            }

            NodeSet ns = getMulIndexNodeSet(node, xpath, true);
            return ns != null && ns.getLength() != 0 ? ns.item(0) : null;
        }
    }


    /**
     * 代码 copy 自 OpenJawXPathAPI
     *
     * @param node
     * @param xpath
     * @param singleNode
     * @return
     */
    private static NodeSet getMulIndexNodeSet(Node node, String xpath, boolean singleNode) {
        try {
            if (xpath.startsWith("/")) {
                xpath = xpath.substring(1);
            }

            String newXpath = "";
            String remainderXpath = "";
            boolean mulIndex = false;
            if (xpath.indexOf("[") != xpath.lastIndexOf("[")) {
                mulIndex = true;
                newXpath = xpath.substring(0, xpath.indexOf("]") + 1);
                remainderXpath = xpath.substring(xpath.indexOf("]") + 1);
            }

            if (!mulIndex) {
                return getNodeSet(node, xpath, singleNode);
            } else {
                NodeSet returnNodeSet = new NodeSet();
                NodeSet tempNodeSet = getNodeSet(node, newXpath, singleNode);
                if (tempNodeSet != null) {
                    for (int i = 0; i < tempNodeSet.getLength(); ++i) {
                        NodeSet ns = getMulIndexNodeSet(tempNodeSet.item(i), remainderXpath, singleNode);
                        if (ns != null) {
                            for (int j = 0; j < ns.getLength(); ++j) {
                                returnNodeSet.addNode(ns.item(j));
                            }
                        }
                    }
                }

                return returnNodeSet;
            }
        } catch (Throwable var11) {
//            cat.warn("Error applying XPath expression " + xpath, var11);
            return new NodeSet();
        }
    }


    /**
     * 代码 copy 自 OpenJawXPathAPI
     *
     * @param node
     * @param XPath
     * @param singleNode
     * @return
     */
    private static NodeSet getNodeSet(Node node, String XPath, boolean singleNode) {
        try {
            NodeSet returnNodeSet = new NodeSet();
            String remXPath = XPath;
            if (node == null) {
                return null;
            } else {
                if (XPath.startsWith("/")) {
                    remXPath = XPath.substring(1, XPath.length());
                }

                boolean lastNode = false;
                int nextSlash = remXPath.indexOf("/");
                String nodeName;
                int brack;
                if (nextSlash != -1) {
                    brack = remXPath.indexOf("[");
                    int indexEnd = remXPath.indexOf("]");
                    if (brack != -1 && brack <= nextSlash && indexEnd >= nextSlash) {
                        if (remXPath.length() == indexEnd + 1) {
                            nodeName = remXPath;
                            lastNode = true;
                        } else {
                            nodeName = remXPath.substring(0, indexEnd + 1);
                            remXPath = remXPath.substring(indexEnd + 2, remXPath.length());
                        }
                    } else {
                        nodeName = remXPath.substring(0, nextSlash);
                        remXPath = remXPath.substring(nextSlash + 1, remXPath.length());
                    }
                } else {
                    nodeName = remXPath;
                    lastNode = true;
                }

                if (nodeName.indexOf(":") != -1) {
                    brack = nodeName.indexOf("[");
                    if (brack == -1 || brack > nodeName.indexOf(":")) {
                        nodeName = nodeName.substring(nodeName.indexOf(":") + 1, nodeName.length());
                    }
                }

                String index = null;
                if (nodeName.endsWith("]")) {
                    index = nodeName.substring(nodeName.indexOf("[") + 1, nodeName.indexOf("]"));
                    nodeName = nodeName.substring(0, nodeName.indexOf("["));
                }

                if (nodeName.startsWith("@")) {
                    NamedNodeMap attributes = node.getAttributes();
                    if (attributes != null && attributes.getLength() != 0) {
                        String attName = nodeName.substring(1, nodeName.length());
                        Node attrNode = null;

                        for (int i = 0; i < attributes.getLength(); ++i) {
                            Node tempNode = attributes.item(i);
                            if (attName.equals(tempNode.getNodeName()) || attName.equals(tempNode.getLocalName())) {
                                attrNode = tempNode;
                                break;
                            }
                        }

                        if (attrNode == null) {
                            return null;
                        } else {
                            returnNodeSet.addNode(attrNode);
                            return returnNodeSet;
                        }
                    } else {
                        return null;
                    }
                } else {
                    NodeSet matchingChildren;
                    int j;
                    NodeSet ns;
                    int k;
                    if (nodeName.equals("..")) {
                        Node parent = node.getParentNode();
                        if (parent == null) {
                            return returnNodeSet;
                        }

                        if (lastNode) {
                            returnNodeSet.addNode(parent);
                        } else {
                            if (XPath.startsWith("/")) {
                                remXPath = XPath.substring(nodeName.length() + 1, XPath.length());
                            }

                            if (singleNode && index == null) {
                                matchingChildren = getNodeSet(parent, remXPath, singleNode);
                                if (matchingChildren != null) {
                                    returnNodeSet.addNode(matchingChildren.elementAt(0));
                                }
                            } else {
                                matchingChildren = new NodeSet();
                                matchingChildren.addNode(parent);
                                if (index != null) {
                                    matchingChildren = applyIndex(matchingChildren, index);
                                }

                                for (j = 0; j < matchingChildren.getLength(); ++j) {
                                    ns = getNodeSet(matchingChildren.item(j), remXPath, singleNode);
                                    if (ns != null) {
                                        for (k = 0; k < ns.getLength(); ++k) {
                                            returnNodeSet.addNode(ns.elementAt(k));
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        NodeList nl = node.getChildNodes();
                        if (nl == null) {
//                            if (debug) {
//                                cat.debug("No child nodes");
//                            }

                            return returnNodeSet;
                        }

                        if (lastNode) {
                            for (j = 0; j < nl.getLength(); ++j) {
                                if (getNameNoNS(nl.item(j)).equals(nodeName) || "*".equals(nodeName)) {
                                    returnNodeSet.addNode(nl.item(j));
                                }
                            }

                            if (index != null) {
                                returnNodeSet = applyIndex(returnNodeSet, index);
                            }
                        } else {
                            if (XPath.startsWith("/")) {
                                remXPath = XPath.substring(nodeName.length() + 1, XPath.length());
                            }

                            if (singleNode && index == null) {
                                for (j = 0; j < nl.getLength(); ++j) {
                                    if (getNameNoNS(nl.item(j)).equals(nodeName) || "*".equals(nodeName)) {
                                        ns = getNodeSet(nl.item(j), remXPath, singleNode);
                                        if (ns != null) {
                                            returnNodeSet.addNode(ns.elementAt(0));
                                        }
                                    }
                                }
                            } else {
                                matchingChildren = new NodeSet();

                                for (j = 0; j < nl.getLength(); ++j) {
                                    if (getNameNoNS(nl.item(j)).equals(nodeName) || "*".equals(nodeName)) {
                                        matchingChildren.addNode(nl.item(j));
                                    }
                                }

                                if (index != null) {
                                    matchingChildren = applyIndex(matchingChildren, index);
                                }

                                for (j = 0; j < matchingChildren.getLength(); ++j) {
                                    ns = getNodeSet(matchingChildren.item(j), remXPath, singleNode);
                                    if (ns != null) {
                                        for (k = 0; k < ns.getLength(); ++k) {
                                            returnNodeSet.addNode(ns.elementAt(k));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    return returnNodeSet;
                }
            }
        } catch (Exception var15) {
//            cat.error("Error aplying xpath expression " + XPath + ": " + var15.getMessage(), var15);
            return null;
        }
    }


    /**
     * 代码 copy 自 OpenJawXPathAPI
     *
     * @param ns
     * @param index
     * @return
     */
    private static NodeSet applyIndex(NodeSet ns, String index) {
        int num = 0;
        index = index.trim();
        int indexOfEquals = index.indexOf("=");

        try {
            NodeSet returnNs = new NodeSet();
            if (index.length() == 0) {
                return ns;
            } else {
                if (index.equals("last()")) {
                    num = ns.getLength() - 1;
                } else {
                    if (indexOfEquals != -1) {
                        String firstPart = index.substring(0, indexOfEquals);
                        boolean notEquals = firstPart.endsWith("!");
                        if (notEquals) {
                            firstPart = firstPart.substring(0, firstPart.length() - 1);
                        }

                        firstPart = firstPart.trim();
//                        if (debug) {
//                            cat.debug("Not equals=" + notEquals + " :'" + firstPart + "'");
//                        }

                        String secondPart = index.substring(indexOfEquals + 1, index.length()).trim();
                        if (secondPart.startsWith("'")) {
                            secondPart = secondPart.substring(1, secondPart.length());
                        }

                        if (secondPart.endsWith("'")) {
                            secondPart = secondPart.substring(0, secondPart.length() - 1);
                        }

                        NodeSet tempNs = new NodeSet();

                        for (int i = 0; i < ns.getLength(); ++i) {
                            NodeSet retNs = getNodeSet(ns.elementAt(i), firstPart, false);
                            if (retNs != null && retNs.getLength() > 0) {
                                for (int j = 0; j < retNs.getLength(); ++j) {
                                    String nodeVal = getNodeValueAsString(retNs.elementAt(j));
                                    boolean addIn = notEquals ^ nodeVal.equals(secondPart);
                                    if (addIn) {
                                        tempNs.addNode(ns.elementAt(i));
                                        break;
                                    }

//                                    if (debug) {
//                                        cat.debug((addIn ? "Added" : "Rejected") + " nodeVal'" + nodeVal + "' secondPart '" + secondPart + "'");
//                                    }
                                }
                            } else if (secondPart.length() == 0) {
                                tempNs.addNode(ns.elementAt(i));
                            } else {
                                boolean addIn = notEquals ^ (new String("")).equals(secondPart);
                                if (addIn) {
                                    tempNs.addNode(ns.elementAt(i));
                                    break;
                                }
                            }
                        }

                        return tempNs;
                    }

                    num = Integer.parseInt(index) - 1;
                }

                if (num < ns.getLength()) {
                    returnNs.addNode(ns.elementAt(num));
                }

                return returnNs;
            }
        } catch (Exception var14) {
//            cat.error("Error applying predicate [" + index + "] in an xpath expression: " + var14.getMessage(), var14);
            return ns;
        }
    }


    /**
     * 代码 copy 自 OpenJawXPathAPI
     *
     * @param n
     * @return
     */
    public static String getNameNoNS(Node n) {
        String nodeName = n.getNodeName();
        if (nodeName.indexOf(":") != -1) {
            nodeName = nodeName.substring(nodeName.indexOf(":") + 1, nodeName.length());
        }

        return nodeName;
    }

    /**
     * 代码 copy 自 OpenJawXPathAPI
     *
     * @param node
     * @param xpath
     * @return
     */
    public static NodeList selectNodeList(Node node, String xpath) {
        if (xpath.startsWith("/")) {
            xpath = xpath.substring(1);
        }

        NodeSet ns = getMulIndexNodeSet(node, xpath, false);
        return ns;
    }

    /**
     * xml 转 Document
     *
     * @param xmlString
     * @return
     */
    private static Document convertStringToXMLDocument(String xmlString) {
        //Parser that produces DOM object trees from XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        //API to obtain DOM Document instance
        DocumentBuilder builder = null;
        try {
            //Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();

            //Parse the content to Document object
            return builder.parse(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        CatUtils.DOMAIN = "hu-dev-sit";

//        String xml = "<OTA_AirLowFareSearchRQ xmlns=\"http://www.opentravel.org/OTA/2003/05\" MaxResponses=\"30\" TransactionIdentifier=\"O\" Version=\"2.005\">\n" +
//                "\t<POS>\n" +
//                "        <Source ISOCountry=\"CN\" ISOCurrency=\"CNY\" PseudoCityCode=\"3P\" Channel=\"B2B\">\n" +
//                "            <RequestorID ID=\"RONGYT\">\n" +
//                "                <CompanyName Code=\"RONGYT\" CompanyShortName=\"RONGYT\"/>\n" +
//                "            </RequestorID>\n" +
//                "        </Source>\n" +
//                "    </POS>\n" +
//                "    <OriginDestinationInformation>\n" +
//                "        <DepartureDateTime>2018-04-30</DepartureDateTime>\n" +
//                "        <OriginLocation CodeContext=\"IATA\" LocationCode=\"HAK\"/>\n" +
//                "        <DestinationLocation CodeContext=\"IATA\" LocationCode=\"PEK\"/>\n" +
//                "    </OriginDestinationInformation>\n" +
//                "    <ota:SpecificFlightInfo>\n" +
//                "        <ota:BookingClassPref ResBookDesigCode=\"Y\"/>\n" +
//                "        <ota:BookingClassPref ResBookDesigCode=\"F\"/>\n" +
//                "        <ota:BookingClassPref ResBookDesigCode=\"W\"/>\n" +
//                "    </ota:SpecificFlightInfo>\n" +
//                "    <TravelPreferences ETicketDesired=\"true\"/>\n" +
//                "    <TravelerInfoSummary>\n" +
//                "        <AirTravelerAvail>\n" +
//                "            <PassengerTypeQuantity Code=\"ADT\" Quantity=\"1\"/>\n" +
//                "            <PassengerTypeQuantity Code=\"CHD\" Quantity=\"1\"/>\n" +
//                "            \n" +
//                "        </AirTravelerAvail>\n" +
//                "    </TravelerInfoSummary>\n" +
//                "</OTA_AirLowFareSearchRQ>";

//        String xml = "<ota:OTA_AirLowFareSearchRQ xmlns:ota=\"http://www.opentravel.org/OTA/2003/05\"\n" +
//                "                            xmlns=\"http://www.opentravel.org/OTA/2003/05\" MaxResponses=\"150\" PackageType=\"F\"\n" +
//                "                            PrimaryLangID=\"zh\" RetransmissionIndicator=\"false\"\n" +
//                "                            SessionID=\"61FB988FA8B8001D1A6CC544E2DEEC53\" Target=\"Cache\" TransactionIdentifier=\"O\"\n" +
//                "                            Version=\"2.005\" xPromotionId=\"\" xPromotionProfileId=\"\">\n" +
//                "    <POS>\n" +
//                "        <Source Channel=\"IBE\" ISOCountry=\"CN\" ISOCurrency=\"CNY\" PseudoCityCode=\"AIR\">\n" +
//                "            <RequestorID ID=\"IBE\"/>\n" +
//                "        </Source>\n" +
//                "        <Agent Agent=\"\" SourceAddress=\"0:0:0:0:0:0:0:1\" Timestamp=\"2019-05-31T04:27:04\" URL=\"0:0:0:0:0:0:0:1\"/>\n" +
//                "    </POS>\n" +
//                "    <OriginDestinationInformation>\n" +
//                "        <DepartureDateTime>2019-07-17</DepartureDateTime>\n" +
//                "        <OriginLocation CodeContext=\"LocationId\" LocationCode=\"CITY_BJS_CN\"/>\n" +
//                "        <DestinationLocation CodeContext=\"LocationId\" LocationCode=\"CitCnSHANGHA017\"/>\n" +
//                "    </OriginDestinationInformation>\n" +
//                "    <SpecificFlightInfo>\n" +
//                "        <BookingClassPref ResBookDesigCode=\"Y\"/>\n" +
//                "    </SpecificFlightInfo>\n" +
//                "    <TravelPreferences ETicketDesired=\"true\"/>\n" +
//                "    <TravelerInfoSummary xmlns:regexpUtils=\"http://xml.apache.org/xslt/java/com.openjaw.travelsky.utils.RegexpUtils\">\n" +
//                "        <SeatsRequested infantsOnLaps=\"true\">2</SeatsRequested>\n" +
//                "        <AirTravelerAvail>\n" +
//                "            <PassengerTypeQuantity Code=\"ADT\" Quantity=\"1\"/>\n" +
//                "            <PassengerTypeQuantity Code=\"CHD\" Quantity=\"1\"/>\n" +
//                "            <PassengerTypeQuantity Code=\"INF\" Quantity=\"0\"/>\n" +
//                "            <AirTraveler>\n" +
//                "                <PassengerTypeQuantity Code=\"ADT\" Quantity=\"1\"/>\n" +
//                "                <TravelerRefNumber/>\n" +
//                "            </AirTraveler>\n" +
//                "            <AirTraveler>\n" +
//                "                <PassengerTypeQuantity Code=\"CHD\" Quantity=\"1\"/>\n" +
//                "                <TravelerRefNumber/>\n" +
//                "            </AirTraveler>\n" +
//                "        </AirTravelerAvail>\n" +
//                "    </TravelerInfoSummary>\n" +
//                "</ota:OTA_AirLowFareSearchRQ>";

        String xml = "<ota:OTA_AirLowFareSearchRQ DeviceID=\"\" EchoToken=\"FS_HAKPEK20JUN19OYADT1CHD0\" MaxResponses=\"150\" PackageType=\"F\"\n" +
                "                            PrimaryLangID=\"zh\" RetransmissionIndicator=\"false\"\n" +
                "                            SessionID=\"6053F6A5F47355F4A3D16BE50414F212.9\" TransactionIdentifier=\"O\" Version=\"2.005\"\n" +
                "                            xPromotionId=\"\" xPromotionProfileId=\"\" xmlns:ota=\"http://www.opentravel.org/OTA/2003/05\">\n" +
                "    <ota:POS>\n" +
                "        <ota:Source Channel=\"IBE\" ISOCountry=\"CN\" ISOCurrency=\"CNY\" PseudoCityCode=\"AIR\">\n" +
                "            <ota:RequestorID ID=\"IBE\"/>\n" +
                "        </ota:Source>\n" +
                "        <ota:Agent Agent=\"\" SourceAddress=\"0:0:0:0:0:0:0:1\" Timestamp=\"2019-06-01T09:07:40\" URL=\"0:0:0:0:0:0:0:1\"/>\n" +
                "    </ota:POS>\n" +
                "    <ota:OriginDestinationInformation>\n" +
                "        <ota:DepartureDateTime>2019-06-20</ota:DepartureDateTime>\n" +
                "        <ota:OriginLocation CodeContext=\"IATA\" LocationCode=\"HAK\"/>\n" +
                "        <ota:DestinationLocation CodeContext=\"IATA\" LocationCode=\"PEK\"/>\n" +
                "    </ota:OriginDestinationInformation>\n" +
                "    <ota:SpecificFlightInfo>\n" +
                "        <ota:BookingClassPref ResBookDesigCode=\"Y\">\n" +
                "            <AssociatedBrands xmlns=\"http://www.opentravel.org/OTA/2003/05\">\n" +
                "                <AssociatedBrand Code=\"SPECIAL_PACKAGE\"/>\n" +
                "                <AssociatedBrand Code=\"TOURISMROVER\"/>\n" +
                "                <AssociatedBrand Code=\"SPECIAL\"/>\n" +
                "                <AssociatedBrand Code=\"FULLPRICE\"/>\n" +
                "                <AssociatedBrand Code=\"BESTSELLING\"/>\n" +
                "            </AssociatedBrands>\n" +
                "        </ota:BookingClassPref>\n" +
                "        <Airline Code=\"GS\" xmlns=\"http://www.opentravel.org/OTA/2003/05\"/>\n" +
                "        <Airline Code=\"JD\" xmlns=\"http://www.opentravel.org/OTA/2003/05\"/>\n" +
                "        <Airline Code=\"CN\" xmlns=\"http://www.opentravel.org/OTA/2003/05\"/>\n" +
                "    </ota:SpecificFlightInfo>\n" +
                "    <ota:TravelPreferences ETicketDesired=\"true\"/>\n" +
                "    <ota:TravelerInfoSummary>\n" +
                "        <ota:SeatsRequested infantsOnLaps=\"true\">1</ota:SeatsRequested>\n" +
                "        <ota:AirTravelerAvail>\n" +
                "            <ota:PassengerTypeQuantity Code=\"ADT\" Quantity=\"1\"/>\n" +
                "            <ota:PassengerTypeQuantity Code=\"CHD\" Quantity=\"0\"/>\n" +
                "            <ota:PassengerTypeQuantity Code=\"INF\" Quantity=\"0\"/>\n" +
                "            <ota:AirTraveler>\n" +
                "                <ota:PassengerTypeQuantity Code=\"ADT\" Quantity=\"1\"/>\n" +
                "                <ota:TravelerRefNumber/>\n" +
                "            </ota:AirTraveler>\n" +
                "        </ota:AirTravelerAvail>\n" +
                "    </ota:TravelerInfoSummary>\n" +
                "</ota:OTA_AirLowFareSearchRQ>";

        Document document = convertStringToXMLDocument(xml);
        Element element = document.getDocumentElement();

        System.out.println(selectSingleNodeAsString(element, "@TransactionIdentifier"));


        NodeList ODIs = selectNodeList(element, "OriginDestinationInformation");
        for (int i = 0; i < ODIs.getLength(); ++i) {
            Node ODI = ODIs.item(i);
            String departuretime = selectSingleNodeAsString(ODI, "DepartureDateTime");
            String orglocation = selectSingleNodeAsString(ODI, "OriginLocation/@LocationCode");
            String dstlocation = selectSingleNodeAsString(ODI, "DestinationLocation/@LocationCode");
            System.out.println("depturetime: " + departuretime + ", org: " + orglocation + ", dst: " + dstlocation);
        }

//        NodeList bookingPrefs = selectNodeList(element, "SpecificFlightInfo/BookingClassPref");
//        for (int i = 0; i < bookingPrefs.getLength(); ++i) {
//            Node bookingPref = bookingPrefs.item(i);
//            String code = selectSingleNodeAsString(bookingPref, "@ResBookDesigCode");
//            System.out.println("code: " + code);
//        }

//
//        NodeList travelerAvailList = selectNodeList(element, "TravelerInfoSummary/AirTravelerAvail");
//        for (int i = 0; i < travelerAvailList.getLength(); ++i) {
//            Node travelerAvail = travelerAvailList.item(i);
//            String code = selectSingleNodeAsString(travelerAvail, "PassengerTypeQuantity/@Code");
//            String quantity = selectSingleNodeAsString(travelerAvail, "PassengerTypeQuantity/@Quantity");
//            System.out.println("Code: " + code + ", quantity: " + quantity);
//        }

        System.out.println(getSearchRQCatName(document));

        System.out.println(getRQItineraryDetail(document));
    }


}
