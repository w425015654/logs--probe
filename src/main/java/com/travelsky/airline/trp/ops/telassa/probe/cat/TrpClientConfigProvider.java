package com.travelsky.airline.trp.ops.telassa.probe.cat;

import com.dianping.cat.configuration.ClientConfigProvider;
import com.dianping.cat.configuration.client.entity.ClientConfig;
import com.dianping.cat.configuration.client.entity.Server;
import com.travelsky.airline.trp.ops.telassa.probe.logging.Logger;
import com.travelsky.airline.trp.ops.telassa.probe.util.CatUtils;
import com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusConfig;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.jmx.BuildInfoCollector;
import io.prometheus.jmx.JmxCollector;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.travelsky.airline.trp.ops.telassa.probe.util.PrometheusConfig.JBOSS_YML;

/**
 * 使用 SPI 启动 CAT
 *
 * @author zengfan
 */
public class TrpClientConfigProvider implements ClientConfigProvider {
    private static final Logger logger = Logger.getLogger(TrpClientConfigProvider.class);
//
//    private static final Summary ibeClientLatency = Summary.build()
//            .name("ibe_client_latency_seconds").help("IBEClient latency in seconds.").labelNames("command").register();


    @Override
    public ClientConfig getClientConfig() {
        List<Server> servers = new ArrayList<Server>();
        servers.add(new Server("10.223.224.88"));
        servers.add(new Server("10.223.224.89"));
        servers.add(new Server("10.223.224.90"));
        servers.add(new Server("10.223.224.95"));
        servers.add(new Server("10.223.224.96"));

        if (CatUtils.DOMAIN == null || CatUtils.DOMAIN.isEmpty()) {
            throw new IllegalStateException("CAT domain NOT ASSIGNED, please check your vm option.");
        }

        ClientConfig config = new ClientConfig();
        config.setServers(servers);
        config.setDomain(CatUtils.DOMAIN);

        logger.info("Get CAT client config by ClientConfigProvider SPI, servers: " + servers + ", domain: " + CatUtils.DOMAIN + ", cat home: " + System.getProperty("CAT_HOME"));

        // 初始化 Prometheus
        try {
            new BuildInfoCollector().register();
            new JmxCollector(PrometheusConfig.JBOSS_YML).register();
            DefaultExports.initialize();
            HTTPServer server = new HTTPServer(2398);
            logger.info("Start prometheus http server SUCCESS. Server Port: " + server.getPort() + ", jmx config: " + JBOSS_YML);
        } catch (IOException e) {
            logger.info("Start prometheus http server FAIL. " + e.getMessage());
        } catch (MalformedObjectNameException e) {
            logger.info("Start JmxCollector FAIL. " + e.getMessage());
        } catch (Exception e) {
            logger.info("Start Prometheus Exception: " + e.getMessage());

        }

        return config;
    }
}