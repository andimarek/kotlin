/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;

public class UndeclaredInputsGuard {

    private static Set<String> declaredInputs = null;
    private static final Set<String> undeclaredInputs = new HashSet<>();
    private static final String rootDir = System.getProperty("test.instrumenter.root.dir");
    private static final String buildDir = System.getProperty("test.instrumenter.build.dir");

    static {
        try {
            Path declaredInputsFile = Paths.get(System.getProperty("test.instrumenter.declared.inputs.file"));
            InputStream inputStream = Files.newInputStream(declaredInputsFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            declaredInputs = reader.lines().collect(toCollection(HashSet::new));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void checkFile(String path) {
        // short circuit and deduplication
        if (path == null || declaredInputs == null || undeclaredInputs.contains(path)) {
            return;
        }
        // using File instead of Path because it's more lightweight
        // some paths from user code are relative, so we convert them to absolute (if not already)
        File file = new File(path).getAbsoluteFile();

        if (isUndeclaredInput(file)) {
            // convert path like '/a/../b' to '/b'
            // this is an expensive operation, so we try to do it as rarely as possible
            File canonicalFile = convertToCanonicalIfNecessary(file);

            if (canonicalFile.equals(file) || isUndeclaredInput(canonicalFile)) {
                UndeclaredInputEvent.emit(file.toString());
                undeclaredInputs.add(path);
            }
        }
    }

    private static boolean isUndeclaredInput(File file) {
        // filter out paths outside the root project (like Gradle caches or Konan files)
        boolean insideRootProjectDir = file.getPath().startsWith(rootDir);
        // filter out paths pointing to project's build directory
        // (tests sometimes write files there and then read them back)
        boolean notInsideCurrentProjectBuildDir = !file.getPath().startsWith(buildDir);

        return insideRootProjectDir &&
               notInsideCurrentProjectBuildDir &&
               !declaredInputs.contains(file.getPath());
    }

    private static File convertToCanonicalIfNecessary(File file) {
        if (file.getPath().contains(".") || file.getPath().contains("..")) {
            try {
                return file.getCanonicalFile();
            }
            catch (IOException e) {
                System.out.println("Unable to get canonical path for " + file.getPath());
            }
        }
        return file;
    }
}
