/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package org.lealone.plugins.undertow;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.lealone.common.exceptions.ConfigException;
import org.lealone.common.logging.Logger;
import org.lealone.common.logging.LoggerFactory;
import org.lealone.common.util.CaseInsensitiveMap;
import org.lealone.common.util.MapUtils;
import org.lealone.db.ConnectionInfo;
import org.lealone.plugins.service.http.HttpRouter;
import org.lealone.plugins.service.http.HttpServer;
import org.lealone.server.ProtocolServerBase;

import io.undertow.Undertow;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;

public class UndertowServer extends ProtocolServerBase implements HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(UndertowServer.class);

    public static final int DEFAULT_PORT = 8080;

    private Map<String, String> config = new HashMap<>();
    private String webRoot;
    private String jdbcUrl;

    @SuppressWarnings("unused")
    private String contextPath;

    private Undertow server;

    public Undertow getUndertow() {
        return server;
    }

    private boolean inited;

    @Override
    public String getType() {
        return UndertowServerEngine.NAME;
    }

    @Override
    public String getWebRoot() {
        return webRoot;
    }

    @Override
    public void setWebRoot(String webRoot) {
        this.webRoot = webRoot;
        config.put("web_root", webRoot);
    }

    @Override
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        config.put("jdbc_url", jdbcUrl);
        System.setProperty(org.lealone.db.Constants.JDBC_URL_KEY, jdbcUrl);
    }

    @Override
    public String getHost() {
        return super.getHost();
    }

    @Override
    public void setHost(String host) {
        config.put("host", host);
    }

    @Override
    public int getPort() {
        return super.getPort();
    }

    @Override
    public void setPort(int port) {
        config.put("port", String.valueOf(port));
    }

    @Override
    public synchronized void init(Map<String, String> config) {
        if (inited)
            return;
        config = new CaseInsensitiveMap<>(config);
        config.putAll(this.config);
        String url = config.get("jdbc_url");
        if (url != null) {
            if (this.jdbcUrl == null)
                setJdbcUrl(url);
            ConnectionInfo ci = new ConnectionInfo(url);
            if (!config.containsKey("default_database"))
                config.put("default_database", ci.getDatabaseName());
            if (!config.containsKey("default_schema"))
                config.put("default_schema", "public");
        }
        if (!config.containsKey("port"))
            config.put("port", String.valueOf(DEFAULT_PORT));
        super.init(config);
        // int schedulerCount = MapUtils.getSchedulerCount(config);

        contextPath = MapUtils.getString(config, "context_path", "");
        webRoot = MapUtils.getString(config, "web_root", "./web");
        File webRootDir = new File(webRoot);
        if (!webRootDir.exists())
            webRootDir.mkdirs();
        try {
            PathResourceManager rm = new PathResourceManager(webRootDir.toPath());
            ResourceHandler resourceHandler = new ResourceHandler(rm);
            Undertow.Builder builder = Undertow.builder();
            builder.addHttpListener(getPort(), getHost());
            builder.setHandler(resourceHandler);
            // Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
            // XnioWorker.Builder workerBuilder = xnio.createWorkerBuilder();
            // workerBuilder.setWorkerIoThreads(0);
            // workerBuilder.setExternalExecutorService(Executors.newSingleThreadExecutor());
            // // worker = new UndertowWorker(workerBuilder);
            // XnioWorker worker = workerBuilder.build();
            // builder.setWorker(worker);
            server = builder.build();
            // server.setBaseDir(getBaseDir());

            HttpRouter router;
            String routerStr = config.get("router");
            if (routerStr != null) {
                try {
                    router = org.lealone.common.util.Utils.newInstance(routerStr);
                } catch (Exception e) {
                    throw new ConfigException("Failed to load router: " + routerStr, e);
                }
            } else {
                router = new UndertowRouter();
            }
            router.init(this, config);
        } catch (Exception e) {
            logger.error("Failed to init tomcat", e);
        }

        inited = true;
        this.config = null;
    }

    @Override
    public synchronized void start() {
        if (isStarted())
            return;
        if (!inited) {
            init(new HashMap<>());
        }
        super.start();
        try {
            server.start();
        } catch (Exception e) {
            logger.error("Failed to start undertow server", e);
        }
        // ShutdownHookUtils.addShutdownHook(server, () -> {
        // try {
        // server.destroy();
        // } catch (Exception e) {
        // logger.error("Failed to destroy undertow server", e);
        // }
        // });
    }

    @Override
    public synchronized void stop() {
        if (isStopped())
            return;
        super.stop();

        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                logger.error("Failed to stop undertow server", e);
            }
            server = null;
        }
    }
}
