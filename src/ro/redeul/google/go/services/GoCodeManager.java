package ro.redeul.google.go.services;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import ro.redeul.google.go.lang.psi.GoFile;
import ro.redeul.google.go.lang.psi.toplevel.GoImportDeclaration;
import ro.redeul.google.go.lang.psi.toplevel.GoImportDeclarations;
import ro.redeul.google.go.lang.psi.visitors.GoImportUsageCheckingVisitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Toader Mihai Claudiu <mtoader@gmail.com>
 * <p/>
 * Date: 7/15/11
 * Time: 7:50 AM
 */
public class GoCodeManager {

    private GoCodeManager() {
    }

    public static GoCodeManager getInstance(Project project) {
        return ServiceManager.getService(project, GoCodeManager.class);
    }

    public Collection<GoImportDeclaration> findUnusedImports(GoFile file) {

        Map<String, GoImportDeclaration> imports =
            new HashMap<String, GoImportDeclaration>();

        for (GoImportDeclarations importDeclarations : file.getImportDeclarations()) {
            for (GoImportDeclaration declaration : importDeclarations.getDeclarations()) {
                String visiblePackageName = declaration.getVisiblePackageName();
                if (!"".equals(visiblePackageName) && !"C".equals(visiblePackageName)) {
                    imports.put(visiblePackageName, declaration);
                }
            }
        }

        new GoImportUsageCheckingVisitor(imports).visitFile(file);

        return imports.values();
    }
}
