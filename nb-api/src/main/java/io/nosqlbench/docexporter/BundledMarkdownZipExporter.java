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

package io.nosqlbench.docexporter;

import io.nosqlbench.docapi.BundledMarkdownLoader;
import io.nosqlbench.docapi.DocsBinder;
import io.nosqlbench.docapi.DocsNameSpace;
import io.nosqlbench.nb.api.markdown.aggregator.MutableMarkdown;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BundledMarkdownZipExporter {

    private final BundledMarkdownProcessor[] filters;
    private final Function<Path, MutableMarkdown> parser = MutableMarkdown::new;

    public BundledMarkdownZipExporter(BundledMarkdownProcessor... filters) {
        this.filters = filters;
    }

    public void exportDocs(Path out) {
        ZipOutputStream zipstream;
        try {
            OutputStream stream = Files.newOutputStream(out, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            zipstream = new ZipOutputStream(stream);
            zipstream.setMethod(ZipOutputStream.DEFLATED);
            zipstream.setLevel(9);

            DocsBinder docsNameSpaces = BundledMarkdownLoader.loadBundledMarkdown();
            for (DocsNameSpace docs_ns : docsNameSpaces) {
                for (Path p : docs_ns) {
                    addEntry(p, p.getParent(), zipstream);
                }
            }
            zipstream.finish();
            stream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addEntry(Path p, Path r, ZipOutputStream zos) throws IOException {

        String name = r.relativize(p).toString();
        name = Files.isDirectory(p) ? (name.endsWith(File.separator) ? name : name + File.separator) : name;

        ZipEntry entry = new ZipEntry(name);

        if (Files.isDirectory(p)) {
            zos.putNextEntry(entry);
            DirectoryStream<Path> stream = Files.newDirectoryStream(p);
            for (Path path : stream) {
                addEntry(path,r,zos);
            }
        } else {
            entry.setTime(Files.getLastModifiedTime(p).toMillis());
            zos.putNextEntry(entry);

            if (p.toString().toLowerCase(Locale.ROOT).endsWith(".md")) {
                MutableMarkdown parsed = parser.apply(p);
                for (BundledMarkdownProcessor filter : this.filters) {
                    parsed = filter.apply(parsed);
                }
                zos.write(parsed.getComposedMarkdown().getBytes(StandardCharsets.UTF_8));
            } else {
                byte[] bytes = Files.readAllBytes(p);
                zos.write(bytes);
            }
        }
        zos.closeEntry();

    }

}
