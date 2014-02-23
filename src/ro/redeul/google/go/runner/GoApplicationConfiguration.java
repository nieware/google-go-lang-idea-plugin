package ro.redeul.google.go.runner;

import com.intellij.execution.CantRunException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import ro.redeul.google.go.config.sdk.GoSdkData;
import ro.redeul.google.go.runner.ui.GoRunConfigurationEditorForm;
import ro.redeul.google.go.sdk.GoSdkUtil;

import java.io.File;
import java.util.*;

/**
 * Author: Toader Mihai Claudiu <mtoader@gmail.com>
 * <p/>
 * Date: Aug 19, 2010
 * Time: 2:53:03 PM
 */
public class GoApplicationConfiguration extends ModuleBasedConfiguration<GoApplicationModuleBasedConfiguration> {

    private static final String TITLE = "go build";

    public String scriptName = "";
    public String scriptArguments = "";
    public String builderArguments = "";
    public Boolean goBuildBeforeRun = false;
    public String goOutputDir = "";
    public String workingDir = "";
    public String envVars = "";
    public Boolean goVetEnabled = false;

    public GoApplicationConfiguration(String name, Project project, GoRunConfigurationType configurationType) {
        super(name, new GoApplicationModuleBasedConfiguration(project), configurationType.getConfigurationFactories()[0]);
    }

    @Override
    public Collection<Module> getValidModules() {
        Module[] modules = ModuleManager.getInstance(getProject()).getModules();
        return Arrays.asList(modules);
    }

    @Override
    protected ModuleBasedConfiguration createInstance() {
        return new GoApplicationConfiguration(getName(), getProject(), GoRunConfigurationType.getInstance());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (scriptName == null || scriptName.length() == 0)
            throw new RuntimeConfigurationException("Please select the file to run.");
        if (goBuildBeforeRun != null &&
                goBuildBeforeRun &&
                (goOutputDir == null || goOutputDir.isEmpty())) {
            throw new RuntimeConfigurationException("Please select the directory for the executable.");
        }
        if (workingDir == null || workingDir.isEmpty()) {
            throw new RuntimeConfigurationException("Please select the application working directory.");
        } else {
            File dir = new File(workingDir);

            if (!dir.exists()) {
                throw new RuntimeConfigurationException("The selected application working directory does not appear to exist.");
            }

            if (!dir.isDirectory()) {
                throw new RuntimeConfigurationException("The selected application working directory does not appear to be a directory.");
            }
        }

        super.checkConfiguration();
    }

    @NotNull
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new GoRunConfigurationEditorForm(getProject());
    }

    public void readExternal(final Element element) throws InvalidDataException {
        PathMacroManager.getInstance(getProject()).expandPaths(element);
        super.readExternal(element);
        XmlSerializer.deserializeInto(this, element);
        readModule(element);
    }

    public void writeExternal(final Element element) throws WriteExternalException {
        super.writeExternal(element);
        XmlSerializer.serializeInto(this, element);
        writeModule(element);
        PathMacroManager.getInstance(getProject()).collapsePathsRecursively(element);
    }

    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {

        final Project project = getProject();

        if (this.workingDir.isEmpty()) {
            this.workingDir = project.getBaseDir().getCanonicalPath();
        }

        CommandLineState state = new CommandLineState(env) {

            @NotNull
            @Override
            protected OSProcessHandler startProcess() throws ExecutionException {

                Sdk sdk = GoSdkUtil.getGoogleGoSdkForProject(getProject());
                if ( sdk == null ) {
                    throw new CantRunException("No Go Sdk defined for this project");
                }

                final GoSdkData sdkData = (GoSdkData)sdk.getSdkAdditionalData();
                if ( sdkData == null ) {
                    throw new CantRunException("No Go Sdk defined for this project");
                }

                String goExecName = sdkData.GO_BIN_PATH;

                String projectDir = project.getBasePath();

                if (projectDir == null) {
                    throw new CantRunException("Could not retrieve the project directory");
                }

                Map<String,String> sysEnv = GoSdkUtil.getExtendedSysEnv(sdkData, projectDir, envVars);

                if (goVetEnabled) {
                    try {
                        ToolWindowManager manager = ToolWindowManager.getInstance(project);
                        ToolWindow window = manager.getToolWindow(GoCommonConsoleView.ID);

                        if (GoCommonConsoleView.consoleView == null) {
                            GoCommonConsoleView.consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
                        }
                        ConsoleView consoleView = GoCommonConsoleView.consoleView;

                        if (window == null) {
                            window = manager.registerToolWindow(GoCommonConsoleView.ID, false, ToolWindowAnchor.BOTTOM);

                            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
                            Content content = contentFactory.createContent(GoCommonConsoleView.consoleView.getComponent(), "", false);
                            window.getContentManager().addContent(content);
                            window.setIcon(GoSdkUtil.getProjectIcon(sdk));
                            window.setToHideOnEmptyContent(true);
                        }
                        window.setTitle(TITLE);

                        window.show(EmptyRunnable.getInstance());

                        String[] goEnv = GoSdkUtil.convertEnvMapToArray(sysEnv);

                        String command = String.format(
                                "%s vet ./...",
                                goExecName
                        );

                        Runtime rt = Runtime.getRuntime();
                        Process proc = rt.exec(command, goEnv, new File(projectDir));
                        OSProcessHandler handler = new OSProcessHandler(proc, null);
                        GoCommonConsoleView.consoleView.attachToProcess(handler);
                        consoleView.print(String.format("%s%n", command), ConsoleViewContentType.NORMAL_OUTPUT);
                        handler.startNotify();

                        if (proc.waitFor() == 0) {
                            VirtualFileManager.getInstance().syncRefresh();
                            consoleView.print(String.format("%nFinished running go vet on project %s%n", projectDir), ConsoleViewContentType.NORMAL_OUTPUT);
                        } else {
                            consoleView.print(String.format("%nCouldn't vet project %s%n", projectDir), ConsoleViewContentType.ERROR_OUTPUT);
                            throw new CantRunException(String.format("Error while processing %s vet command.", goExecName));
                        }
                    } catch (Exception e) {
                        throw new CantRunException(String.format("Error while processing %s vet command.", goExecName));
                    }
                }

                if (!goBuildBeforeRun) {
                    // Just run
                    GeneralCommandLine commandLine = new GeneralCommandLine();

                    commandLine.setExePath(goExecName);
                    commandLine.addParameter("run");
                    if (builderArguments != null && builderArguments.trim().length() > 0) {
                        commandLine.getParametersList().addParametersString(builderArguments);
                    }

                    commandLine.addParameter(scriptName);
                    if (scriptArguments != null && scriptArguments.trim().length() > 0) {
                        commandLine.getParametersList().addParametersString(scriptArguments);
                    }

                    commandLine.getEnvironment().putAll(sysEnv);
                    commandLine.setWorkDirectory(workingDir);

                    return GoApplicationProcessHandler.runCommandLine(commandLine);
                }


                // Build and run
                String execName = goOutputDir.concat("/").concat(getProject().getName());

                if (GoSdkUtil.isHostOsWindows()) {
                    execName = execName.concat(".exe");
                }

/*<< HEAD
        if (file != null) {
            for (VirtualFile sourceRoot : sourceRoots) {

                if (VfsUtil.isAncestor(sourceRoot, file, true)) {
                    String relativePath = VfsUtil.getRelativePath(file.getParent(), sourceRoot, File.separatorChar);
                    GoProjectSettings setting = GoProjectSettings.getInstance(module.getProject());
                    String compiledFileName;
                    if (setting.getState().BUILD_SYSTEM_TYPE == GoProjectSettings.BuildSystemType.Install) {
                        //compiledFileName = module.getProject().getBasePath() + "/bin/" + relativePath;
                        compiledFileName = module.getProject().getBasePath() + "/bin/" + file.getNameWithoutExtension();
                    }else{
                        compiledFileName = CompilerPaths.getModuleOutputPath(module, false)
                                                + "/go-bins/" + relativePath + "/" + file.getNameWithoutExtension();
                    }
*/
                try {
                    ToolWindowManager manager = ToolWindowManager.getInstance(project);
                    ToolWindow window = manager.getToolWindow(GoCommonConsoleView.ID);

                    if (GoCommonConsoleView.consoleView == null) {
                        GoCommonConsoleView.consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
                    }
                    ConsoleView consoleView = GoCommonConsoleView.consoleView;

                    if (window == null) {
                        window = manager.registerToolWindow(GoCommonConsoleView.ID, false, ToolWindowAnchor.BOTTOM);

                        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
                        Content content = contentFactory.createContent(consoleView.getComponent(), "", false);
                        window.getContentManager().addContent(content);
                        window.setIcon(GoSdkUtil.getProjectIcon(sdk));
                        window.setToHideOnEmptyContent(true);
                    }
                    window.setTitle(TITLE);

                    window.show(EmptyRunnable.getInstance());

                    String[] goEnv = GoSdkUtil.convertEnvMapToArray(sysEnv);
                    String[] command = GoSdkUtil.computeGoBuildCommand(goExecName, builderArguments, execName, scriptName);

                    Runtime rt = Runtime.getRuntime();
                    Process proc = rt.exec(command, goEnv);
                    OSProcessHandler handler = new OSProcessHandler(proc, null);
                    consoleView.attachToProcess(handler);
                    consoleView.print(String.format("%s%n", StringUtil.join(command, " ")), ConsoleViewContentType.NORMAL_OUTPUT);
                    handler.startNotify();

                    if (proc.waitFor() == 0) {
                        VirtualFileManager.getInstance().syncRefresh();
                        consoleView.print(String.format("%nFinished building project %s%n", execName), ConsoleViewContentType.NORMAL_OUTPUT);
                    } else {
                        consoleView.print(String.format("%nCould't build project %s%n", execName), ConsoleViewContentType.ERROR_OUTPUT);
                        throw new CantRunException(String.format("Error while processing %s build command.", goExecName));
                    }
                } catch (Exception e) {
                    throw new CantRunException(String.format("Error while processing %s build command.", goExecName));
                }

                // Now run the build
                GeneralCommandLine commandLine = new GeneralCommandLine();

                commandLine.setExePath(execName);
                commandLine.setWorkDirectory(workingDir);
                if (scriptArguments != null && scriptArguments.trim().length() > 0) {
                    commandLine.getParametersList().addParametersString(scriptArguments);
                }

                return GoApplicationProcessHandler.runCommandLine(commandLine);
            }
        };

        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(project));
        state.addConsoleFilters(new GoConsoleFilter(project, project.getBasePath()));
        return state;
    }

    @Override
    public String suggestedName() {
        try {
            return scriptName.equals("") ? "go run" : GoSdkUtil.getVirtualFile(scriptName).getName();
        } catch (NullPointerException ignored) {
            return "go run";
        }
    }

}
