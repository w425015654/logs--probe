package com.travelsky.airline.trp.ops.telassa.probe.util;

/**
 * Prometheus conf 配置信息，直接从文件里读不好读，这里写死到 String 里
 *
 * @author zengfan
 */
public class PrometheusConfig {
    // JBoss 配置信息
    public static String JBOSS_YML = "---\n" +
            "lowercaseOutputName: true\n" +
            "lowercaseOutputLabelNames: true\n" +
            "whitelistObjectNames: \n" +
            " # Whitelist objects to be collected, for performance reason\n" +
            " # see https://github.com/prometheus/jmx_exporter/issues/246#issuecomment-367573931\n" +
            " # Each object in the rules below has to be added to whitelistObjectNames too !\n" +
            " # note that rules use regex (like \"foo.*\", whereas the whitelist use globbing expressions (like \"foo*\")\n" +
            " - \"jboss.as:subsystem=messaging-activemq,server=*\"\n" +
            " - \"jboss.as:subsystem=datasources,data-source=*,statistics=*\"\n" +
            " - \"jboss.as:subsystem=datasources,xa-data-source=*,statistics=*\"\n" +
            " - \"jboss.as:subsystem=transactions*\"\n" +
            " - \"jboss.as:subsystem=undertow,server=*,http-listener=*\"\n" +
            " - \"java.lang:type=OperatingSystem\"\n" +
            " # - \"java.lang:*\"\n" +
            "rules:\n" +
            "  - pattern: 'java.lang<type=OperatingSystem><>(committed_virtual_memory|free_physical_memory|free_swap_space|total_physical_memory|total_swap_space)_size:'\n" +
            "    name: os_$1_bytes\n" +
            "    type: GAUGE\n" +
            "    attrNameSnakeCase: true\n" +
            "\n" +
            "  - pattern: 'java.lang<type=OperatingSystem><>((?!process_cpu_time)\\w+):'\n" +
            "    name: os_$1\n" +
            "    type: GAUGE\n" +
            "    attrNameSnakeCase: true\n" +
            "    \n" +
            "  - pattern: \"^jboss.as<subsystem=messaging-activemq, server=.+, jms-(queue|topic)=(.+)><>(.+):\"\n" +
            "    attrNameSnakeCase: true\n" +
            "    name: wildfly_messaging_$3\n" +
            "    labels:\n" +
            "      $1: $2\n" +
            "\n" +
            "  - pattern: \"^jboss.as<subsystem=datasources, (?:xa-)*data-source=(.+), statistics=(.+)><>(.+):\"\n" +
            "    attrNameSnakeCase: true\n" +
            "    name: wildfly_datasource_$2_$3\n" +
            "    labels:\n" +
            "      name: $1\n" +
            "\n" +
            "  - pattern: \"^jboss.as<subsystem=transactions><>number_of_(.+):\"\n" +
            "    attrNameSnakeCase: true\n" +
            "    name: wildfly_transaction_$1\n" +
            "\n" +
            "  - pattern: \"^jboss.as<subsystem=undertow, server=(.+), http-listener=(.+)><>(bytes_.+|error_count|processing_time|request_count):\"\n" +
            "    attrNameSnakeCase: true\n" +
            "    name: wildfly_undertow_$3\n" +
            "    labels:\n" +
            "      server: $1\n" +
            "      http_listener: $2\n";
}
