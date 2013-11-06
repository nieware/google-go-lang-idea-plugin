package ro.redeul.google.go.compilation;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import org.jetbrains.annotations.NotNull;
import ro.redeul.google.go.GoFileType;
import ro.redeul.google.go.config.sdk.GoSdkData;
import ro.redeul.google.go.config.sdk.GoTargetOs;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

public class GoInstallCompiler implements TranslatingCompiler {

    Project project;

    VirtualFile currentFile;

    public GoInstallCompiler(Project project) {
        this.project = project;
    }

    @Override
    public boolean isCompilableFile(VirtualFile virtualFile, CompileContext compileContext) {
        return virtualFile.getFileType() == GoFileType.INSTANCE;
    }

    @Override
    public void compile(CompileContext compileContext, Chunk<Module> moduleChunk, VirtualFile[] virtualFiles, OutputSink outputSink) {
        String basePath = compileContext.getProject().getBasePath();
        Path srcPath = Paths.get(basePath+"/src");
        HashSet<String> packages = new HashSet<String>();

        // get the file currently open in the editor
        // invokeAndWait is used to call getSelectedTextEditor from the event dispatch thread
        ModalityState ms = ModalityState.NON_MODAL;
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            public void run() {
                try {
                    //noinspection ConstantConditions
                    Document currentDoc  = FileEditorManager.getInstance(project).getSelectedTextEditor().getDocument();
                    currentFile = FileDocumentManager.getInstance().getFile(currentDoc);
                } catch(Exception e) { /*ignore */ }
            }
        }, ms);

        // we ignore the list of files we get and only use the current file
        //for (VirtualFile currentFile : virtualFiles) {
        Path fullPath = Paths.get(currentFile.getParent().getPath());
        compileContext.addMessage(CompilerMessageCategory.INFORMATION, "fullPath: "+fullPath.toString(), null, -1, -1);
        compileContext.addMessage(CompilerMessageCategory.INFORMATION, "srcPath:  "+srcPath.toString(), null, -1, -1);
        try {
            if(fullPath.startsWith(srcPath)) {
                String importPath = fullPath.toString().substring(srcPath.toString().length()+1); // include final slash
                packages.add(importPath);
                compileContext.addMessage(CompilerMessageCategory.INFORMATION, "importPath "+importPath, null, -1, -1);
            } else {
                compileContext.addMessage(CompilerMessageCategory.WARNING, fullPath+" is not in the 'src' directory of the workspace", null, -1, -1);
            }
        } catch (IndexOutOfBoundsException e) { /* ignore */ }
        //}

        // build the command line
        GeneralCommandLine command = new GeneralCommandLine();
        final Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
        final GoSdkData goSdkData = (GoSdkData) projectSdk.getSdkAdditionalData();
        command.setExePath(goSdkData.GO_BIN_PATH);
        command.addParameter("install");

        for (String pkg : packages) {
            command.addParameter(pkg);
        }

        command.setWorkDirectory(basePath);

        HashMap<String, String> envparams = new HashMap<String, String>();
        String GOROOT = projectSdk.getHomePath();
        compileContext.addMessage(CompilerMessageCategory.INFORMATION, "Setting GOROOT to "+GOROOT, null, -1, -1);
        envparams.put("GOROOT", GOROOT);
        String GOPATH = project.getBasePath();
        compileContext.addMessage(CompilerMessageCategory.INFORMATION, "Setting GOPATH to "+GOPATH, null, -1, -1);
        envparams.put("GOPATH", GOPATH);
        command.setEnvParams(envparams);

        compileContext.addMessage(CompilerMessageCategory.INFORMATION, "running cmd: "+command.getCommandLineString(), null, -1, -1);

        CompilationTaskWorker compilationTaskWorker = new CompilationTaskWorker(
                new GoCompilerOutputStreamParser(basePath));
        compilationTaskWorker.executeTask(command, basePath, compileContext);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Go Install Compiler";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean validateConfiguration(CompileScope compileScope) {
        return true;
    }
}
