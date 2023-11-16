/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package org.lealone.plugins.undertow;

import java.util.Map;

import org.lealone.plugins.service.http.HttpRouter;
import org.lealone.plugins.service.http.HttpServer;

public class UndertowRouter implements HttpRouter {
    @Override
    public void init(HttpServer server, Map<String, String> config) {
    }
}
