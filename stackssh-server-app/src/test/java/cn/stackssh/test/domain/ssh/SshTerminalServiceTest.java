package cn.stackssh.test.domain.ssh;

import cn.stackssh.domain.ssh.adapter.port.ISshSessionPort;
import cn.stackssh.domain.ssh.adapter.port.ITerminalRecordingPort;
import cn.stackssh.domain.ssh.adapter.port.ITerminalSessionPort;
import cn.stackssh.domain.ssh.model.entity.TerminalSessionEntity;
import cn.stackssh.domain.ssh.model.valobj.DangerousCommandProperties;
import cn.stackssh.domain.ssh.service.terminal.SshTerminalService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SshTerminalServiceTest {

    @Mock
    private ISshSessionPort sshSessionPort;
    @Mock
    private ITerminalSessionPort terminalSessionPort;
    @Mock
    private ITerminalRecordingPort terminalRecordingPort;

    @Test
    public void shouldRequireApprovalBeforeDangerousInteractiveCommand() {
        DangerousCommandProperties properties = new DangerousCommandProperties();
        properties.setDangerousCommands(List.of("rm -rf /"));
        SshTerminalService service = new SshTerminalService(
                sshSessionPort, terminalSessionPort, terminalRecordingPort, properties);

        when(sshSessionPort.isConnected("c1")).thenReturn(true);
        when(terminalSessionPort.openTerminal("c1", 120, 24)).thenReturn("t1");
        when(terminalSessionPort.sessionExists("t1")).thenReturn(true);

        TerminalSessionEntity session = service.openTerminal("c1", 120, 24, "u1");
        Assert.assertEquals("u1", session.getOwnerUserId());

        try {
            service.writeTerminal("t1", "rm -rf /\r");
            Assert.fail("dangerous command should be blocked before approval");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("危险命令"));
        }
        verify(terminalSessionPort, never()).write(anyString(), anyString());

        service.approveDangerousCommand("t1", "rm -rf /");
        service.writeTerminal("t1", "rm -rf /\r");
        verify(terminalSessionPort).write("t1", "rm -rf /\r");
        Assert.assertTrue(service.isSessionOwner("t1", "u1"));
        Assert.assertEquals(1, service.listActiveSessions("c1", "u1").size());
    }

    @Test
    public void shouldRequireApprovalBeforeDangerousExecCommand() {
        DangerousCommandProperties properties = new DangerousCommandProperties();
        properties.setDangerousCommands(List.of("rm -rf /"));
        SshTerminalService service = new SshTerminalService(
                sshSessionPort, terminalSessionPort, terminalRecordingPort, properties);

        when(sshSessionPort.isConnected("c1")).thenReturn(true);
        when(terminalSessionPort.openTerminal("c1", 120, 24)).thenReturn("t2");
        when(terminalSessionPort.sessionExists("t2")).thenReturn(true);
        when(terminalSessionPort.readAgentBuffer("t2")).thenReturn("", "", "", "");

        service.openTerminal("c1", 120, 24, "u1");
        try {
            service.executeCommand("t2", "rm -rf /");
            Assert.fail("dangerous exec should be blocked before approval");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("危险命令"));
        }

        service.approveDangerousCommand("t2", "rm -rf /");
        service.executeCommand("t2", "rm -rf /");
        verify(terminalSessionPort).setAgentCapture("t2", true);
        verify(terminalSessionPort).write("t2", "rm -rf /\n");
        verify(terminalSessionPort).setAgentCapture("t2", false);
    }
}
