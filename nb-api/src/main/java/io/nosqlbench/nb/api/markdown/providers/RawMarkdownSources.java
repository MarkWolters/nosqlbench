/*
 * Copyright (c) 2022 nosqlbench
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nosqlbench.nb.api.markdown.providers;

import io.nosqlbench.nb.api.content.Content;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The internal
 */
public class RawMarkdownSources {

    public static List<Content<?>> getAllMarkdown() {
        ServiceLoader<RawMarkdownSource> loader = ServiceLoader.load(RawMarkdownSource.class);
        List<Content<?>> content = new ArrayList<>();
        loader.iterator().forEachRemaining(d -> content.addAll(d.getMarkdownInfo()));
        return content;
    }
}
