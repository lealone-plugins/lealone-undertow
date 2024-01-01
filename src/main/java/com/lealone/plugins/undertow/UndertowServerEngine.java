/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package com.lealone.plugins.undertow;

import com.lealone.plugins.service.http.HttpServerEngine;
import com.lealone.server.ProtocolServer;

public class UndertowServerEngine extends HttpServerEngine {

    public static final String NAME = "Undertow";

    public UndertowServerEngine() {
        super(NAME);
    }

    @Override
    protected ProtocolServer createProtocolServer() {
        return new UndertowServer();
    }
}
