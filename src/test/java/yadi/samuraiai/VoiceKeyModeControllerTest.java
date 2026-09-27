package yadi.samuraiai;

import org.junit.jupiter.api.Test;
import yadi.samuraiai.client.voice.VoiceKeyModeController;
import static org.junit.jupiter.api.Assertions.*;

class VoiceKeyModeControllerTest {
    @Test void holdModeEmitsOnlyPressAndReleaseEdges() {
        var controller=new VoiceKeyModeController();
        assertEquals(VoiceKeyModeController.Action.START,controller.update(true,true));
        assertEquals(VoiceKeyModeController.Action.NONE,controller.update(true,true));
        assertEquals(VoiceKeyModeController.Action.STOP,controller.update(true,false));
        assertEquals(VoiceKeyModeController.Action.NONE,controller.update(true,false));
    }
    @Test void toggleModeAndResetCannotLeakHeldState() {
        var controller=new VoiceKeyModeController();
        assertEquals(VoiceKeyModeController.Action.NONE,controller.update(false,true));
        assertEquals(VoiceKeyModeController.Action.START,controller.update(true,true));
        controller.reset();
        assertEquals(VoiceKeyModeController.Action.START,controller.update(true,true));
    }
}
