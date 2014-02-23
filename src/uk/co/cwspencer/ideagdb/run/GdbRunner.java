package uk.co.cwspencer.ideagdb.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ro.redeul.google.go.config.sdk.GoSdkData;
import ro.redeul.google.go.sdk.GoSdkUtil;
import uk.co.cwspencer.gdb.Gdb;
import uk.co.cwspencer.gdb.messages.GdbEvent;
import uk.co.cwspencer.ideagdb.debug.GdbDebugProcess;

import java.io.File;
import java.io.IOException;

public class GdbRunner extends DefaultProgramRunner {
    @NotNull
    @Override
    public String getRunnerId() {
        return "GdbRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) &&
                profile instanceof GdbRunConfiguration;
    }

    protected RunContentDescriptor doExecute(Project project,
                                             RunProfileState state,
                                             RunContentDescriptor contentToReuse,
                                             ExecutionEnvironment env) throws ExecutionException {
        FileDocumentManager.getInstance().saveAllDocuments();

        return createContentDescriptor(project, env.getExecutor(), state, contentToReuse, env);
    }

    @Nullable
    protected RunContentDescriptor createContentDescriptor(final Project project,
                                                           final Executor executor,
                                                           final RunProfileState state,
                                                           RunContentDescriptor contentToReuse,
                                                           ExecutionEnvironment env) throws ExecutionException {

        final ExecutionResult result = state.execute(executor, GdbRunner.this);
        if (result == null) {
            return null;
        }

        GdbRunConfiguration configuration = ((GdbExecutionResult) result).m_configuration;
        String execName = configuration.goOutputDir.concat("/").concat(project.getName());

        if (GoSdkUtil.isHostOsWindows()) {
            execName = execName.concat(".exe");
        }

        final XDebugSession debugSession = XDebuggerManager.getInstance(project).startSession(this,
                env, contentToReuse, new XDebugProcessStarter() {
            @NotNull
            @Override
            public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
                session.setAutoInitBreakpoints(false);
                return new GdbDebugProcess(project, session, (GdbExecutionResult) result);
            }
        });

        Sdk sdk = GoSdkUtil.getGoogleGoSdkForProject(project);
        if ( sdk == null ) {
            debugSession.stop();
            return null;
        }

        final GoSdkData sdkData = (GoSdkData)sdk.getSdkAdditionalData();
        if ( sdkData == null ) {
            debugSession.stop();
            return null;
        }

        GdbDebugProcess debugProcess = ((GdbDebugProcess) debugSession.getDebugProcess());

        String goRootPath;
        try {
            goRootPath = (new File(sdkData.GO_GOROOT_PATH)).getCanonicalPath();
        } catch (IOException ignored) {
            debugSession.stop();
            return null;
        }

        final Gdb gdbProcess = debugProcess.m_gdb;

        // Queue startup commands
        gdbProcess.sendCommand("-list-features", new Gdb.GdbEventCallback() {
            @Override
            public void onGdbCommandCompleted(GdbEvent event) {
                gdbProcess.onGdbCapabilitiesReady(event);
            }
        });
        gdbProcess.sendCommand("add-auto-load-safe-path " + goRootPath);

        String pythonRuntime = goRootPath + "/src/pkg/runtime/runtime-gdb.py";
        if (GoSdkUtil.checkFileExists(pythonRuntime)) {
            gdbProcess.sendCommand("source " + pythonRuntime);
        }

        gdbProcess.sendCommand("file " + execName);

        debugSession.initBreakpoints();

        // Send startup commands
        String[] commandsArray = configuration.STARTUP_COMMANDS.split("\\r?\\n");
        for (String command : commandsArray) {
            command = command.trim();
            if (!command.isEmpty()) {
                gdbProcess.sendCommand(command);
            }
        }

        if (configuration.autoStartGdb) {
            gdbProcess.sendCommand("run");
        }

        return debugSession.getRunContentDescriptor();
    }
}
