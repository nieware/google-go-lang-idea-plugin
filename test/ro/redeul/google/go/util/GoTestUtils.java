/*
 * Copyright 2000-2008 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ro.redeul.google.go.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.intellij.openapi.application.PluginPathManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.LocalTimeCounter;
import org.junit.Assert;

/**
 * Utility class, that contains various methods for testing
 */
public abstract class GoTestUtils {
    public static final String TEMP_FILE = "temp.go";

    public static final String MARKER_CARET = "<caret>";
    public static final String MARKER_BEGIN = "/*begin*/";
    public static final String MARKER_END = "/*end*/";
//  public static final String GRAILS_JAR = "grails-web.jar";
//  public static final String GROOVY_JAR = "groovy-all.jar";
//  public static final String GROOVY_JAR_17 = "groovy-all-1.7.jar";

//  public static String getMockJdkHome() {
//    return getAbsoluteTestDataPath() + "/mockJDK";
//  }
//
//  public static String getMockGroovyLibraryHome() {
//    return getAbsoluteTestDataPath() + "/mockGroovyLib";
//  }
//
//  public static String getMockGroovy1_7LibraryHome() {
//    return getAbsoluteTestDataPath() + "/mockGroovyLib1.7";
//  }
//
//  public static String getRealGroovy1_7LibraryHome() {
//    return getAbsoluteTestDataPath() + "/realGroovy17/";
//  }
//
//  public static String getMockGroovy1_7LibraryName() {
//    return getMockGroovy1_7LibraryHome()+"/groovy-all-1.7.jar";
//  }
//

    public static String getTestDataPath() {
        String pluginHome =
            FileUtil.toSystemIndependentName(
                PluginPathManager.getPluginHomePathRelative(
                    "google-go-language")) + "/testdata/";

        String communityPath =
            PlatformTestUtil.getCommunityPath()
                            .replace(File.separatorChar, '/');

        String path =
            String.format("%s%s/", communityPath, pluginHome);

        if (new File(path).exists())
            return path;

        return String.format("%s%s/../", communityPath, pluginHome);
    }


    public static PsiFile createPseudoPhysicalGoFile(final Project project, final String text) throws IncorrectOperationException {
        return createPseudoPhysicalFile(project, TEMP_FILE, text);
    }

    public static PsiFile createPseudoPhysicalFile(final Project project, final String fileName, final String text)
        throws IncorrectOperationException {
        return PsiFileFactory.getInstance(project).createFileFromText(
                fileName,
                FileTypeManager.getInstance().getFileTypeByFileName(fileName),
                text,
                LocalTimeCounter.currentTime(),
                true);
    }

    public static String removeBeginMarker(String text) {
        int index = text.indexOf(MARKER_BEGIN);
        return text.substring(0, index) + text.substring(
            index + MARKER_BEGIN.length());
    }

    public static String removeEndMarker(String text) {
        int index = text.indexOf(MARKER_END);
        return text.substring(0, index) + text.substring(
            index + MARKER_END.length());
    }


    public static List<String> readInput(String filePath) throws IOException {

        String content = new String(FileUtil.loadFileText(new File(filePath).getCanonicalFile()));
        Assert.assertNotNull(content);

        List<String> input = new ArrayList<String>();

        int separatorIndex;
        content = StringUtil.replace(content, "\r", ""); // for MACs

        // Adding input  before -----
        String[] parts = content.split("\n(?:/\\*\\*)?-{3,10}[\r\n]+");
        Collections.addAll(input, parts);
//        while ((separatorIndex = content.indexOf("\n(/**)?-----")) >= 0) {
//            input.add(content.substring(0, separatorIndex));
//            content = content.substring(separatorIndex + 1);
//            while (StringUtil.startsWithChar(content, '-')) {
//                content = content.substring(1);
//            }
//            if (StringUtil.startsWithChar(content, '\n')) {
//                content = content.substring(1);
//            }
//        }
//        // Result - after -----
//        if (content.endsWith("\n")) {
//            content = content.substring(0, content.length() - 1);
//        }
//        input.add(content);

        Assert.assertTrue("No data found in source file", input.size() > 0);
        Assert.assertNotNull("Test output points to null", input.size() > 1);

        return input;
    }

    public static void writeTestFile(String data, String parseTree, String fileName) throws IOException {
        FileUtil.writeToFile(new File(fileName), (data + "\n/**-----\n" + parseTree).getBytes());
    }
}

