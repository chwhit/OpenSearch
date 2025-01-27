/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.script;

import org.opensearch.common.settings.Settings;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.ScriptPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * A script plugin that uses {@link MockScriptEngine} as the script engine for tests.
 */
public abstract class MockScriptPlugin extends Plugin implements ScriptPlugin {

    public static final String NAME = MockScriptEngine.NAME;

    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
        return new MockScriptEngine(pluginScriptLang(), pluginScripts(), nonDeterministicPluginScripts(), pluginContextCompilers());
    }

    protected abstract Map<String, Function<Map<String, Object>, Object>> pluginScripts();

    protected Map<String, Function<Map<String, Object>, Object>> nonDeterministicPluginScripts() { return Collections.emptyMap(); }

    protected Map<ScriptContext<?>, MockScriptEngine.ContextCompiler> pluginContextCompilers() {
        return Collections.emptyMap();
    }

    public String pluginScriptLang() {
        return NAME;
    }
}
