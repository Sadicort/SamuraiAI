package yadi.samuraiai.client.voice.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yadi.samuraiai.client.voice.*;
import yadi.samuraiai.client.voice.audio.AudioDeviceManager;
import yadi.samuraiai.client.voice.core.*;
import java.util.*;

/** In-game editor backed by Forge's client config; no audio is opened here. */
public final class VoiceSettingsScreen extends Screen {
    private final Screen parent;
    private final int page;
    private String status = "Shift + clic en Mic abre esta pantalla";
    public VoiceSettingsScreen(Screen parent) { this(parent, 0); }
    private VoiceSettingsScreen(Screen parent, int page) {
        super(Component.literal("SamuraiAI Voice")); this.parent = parent; this.page = page;
    }
    @Override protected void init() {
        VoiceConfig.Values value = VoiceConfig.get();
        int left = width / 2 - 154, right = width / 2 + 4, y = height / 2 - 78;
        if (page == 0) {
            add(left,y,label("Sistema",value.enabled()),() -> save(copy(value,!value.enabled(),value.language(),value.microphone(),value.sensitivity(),value.gain(),value.maxRecordingSeconds(),value.insertAutomatically(),value.sendAutomatically(),value.holdToTalk(),value.showOverlay(),value.subtitles(),value.debug())));
            add(right,y,"Idioma: "+value.language(),() -> save(copy(value,value.enabled(),nextLanguage(value.language()),value.microphone(),value.sensitivity(),value.gain(),value.maxRecordingSeconds(),value.insertAutomatically(),value.sendAutomatically(),value.holdToTalk(),value.showOverlay(),value.subtitles(),value.debug())));
            add(left,y+24,"Micrófono: "+shortDevice(value.microphone()),() -> save(copy(value,value.enabled(),value.language(),nextDevice(value.microphone()),value.sensitivity(),value.gain(),value.maxRecordingSeconds(),value.insertAutomatically(),value.sendAutomatically(),value.holdToTalk(),value.showOverlay(),value.subtitles(),value.debug())));
            add(right,y+24,label("Insertar texto",value.insertAutomatically()),() -> save(copy(value,value.enabled(),value.language(),value.microphone(),value.sensitivity(),value.gain(),value.maxRecordingSeconds(),!value.insertAutomatically(),value.sendAutomatically(),value.holdToTalk(),value.showOverlay(),value.subtitles(),value.debug())));
            add(left,y+48,label("Envío automático",value.sendAutomatically()),() -> save(copy(value,value.enabled(),value.language(),value.microphone(),value.sensitivity(),value.gain(),value.maxRecordingSeconds(),value.insertAutomatically(),!value.sendAutomatically(),value.holdToTalk(),value.showOverlay(),value.subtitles(),value.debug())));
            add(right,y+48,label("Mantener V",value.holdToTalk()),() -> save(copy(value,value.enabled(),value.language(),value.microphone(),value.sensitivity(),value.gain(),value.maxRecordingSeconds(),value.insertAutomatically(),value.sendAutomatically(),!value.holdToTalk(),value.showOverlay(),value.subtitles(),value.debug())));
            add(left,y+72,label("Overlay",value.showOverlay()),() -> save(copy(value,value.enabled(),value.language(),value.microphone(),value.sensitivity(),value.gain(),value.maxRecordingSeconds(),value.insertAutomatically(),value.sendAutomatically(),value.holdToTalk(),!value.showOverlay(),value.subtitles(),value.debug())));
            add(right,y+72,"Opciones avanzadas",() -> minecraft.setScreen(new VoiceSettingsScreen(parent,1)));
        } else {
            add(left,y,String.format(Locale.ROOT,"Sensibilidad: %.2f",value.sensitivity()),() -> save(copy(value,value.enabled(),value.language(),value.microphone(),cycle(value.sensitivity(),.05f,.01f,1f),value.gain(),value.maxRecordingSeconds(),value.insertAutomatically(),value.sendAutomatically(),value.holdToTalk(),value.showOverlay(),value.subtitles(),value.debug())));
            add(right,y,String.format(Locale.ROOT,"Ganancia: %.2f",value.gain()),() -> save(copy(value,value.enabled(),value.language(),value.microphone(),value.sensitivity(),cycle(value.gain(),.25f,.1f,4f),value.maxRecordingSeconds(),value.insertAutomatically(),value.sendAutomatically(),value.holdToTalk(),value.showOverlay(),value.subtitles(),value.debug())));
            add(left,y+24,"Máximo: "+value.maxRecordingSeconds()+" s",() -> save(copy(value,value.enabled(),value.language(),value.microphone(),value.sensitivity(),value.gain(),value.maxRecordingSeconds()>=120?5:value.maxRecordingSeconds()+5,value.insertAutomatically(),value.sendAutomatically(),value.holdToTalk(),value.showOverlay(),value.subtitles(),value.debug())));
            add(right,y+24,label("Subtítulos",value.subtitles()),() -> save(copy(value,value.enabled(),value.language(),value.microphone(),value.sensitivity(),value.gain(),value.maxRecordingSeconds(),value.insertAutomatically(),value.sendAutomatically(),value.holdToTalk(),value.showOverlay(),!value.subtitles(),value.debug())));
            add(left,y+48,label("Debug",value.debug()),() -> save(copy(value,value.enabled(),value.language(),value.microphone(),value.sensitivity(),value.gain(),value.maxRecordingSeconds(),value.insertAutomatically(),value.sendAutomatically(),value.holdToTalk(),value.showOverlay(),value.subtitles(),!value.debug())));
            add(right,y+48,"Restaurar valores",() -> { VoiceForgeConfig.restoreDefaults(); refresh(); });
            add(left,y+72,"Opciones principales",() -> minecraft.setScreen(new VoiceSettingsScreen(parent,0)));
            add(right,y+72,"Recargar motor",() -> VoiceBootstrapService.manager().reload());
        }
        add(left,y+100,"Diagnóstico",this::diagnose);
        add(right,y+100,"Volver",() -> minecraft.setScreen(parent));
    }
    private void diagnose() {
        VoiceDiagnostics.Report report = VoiceDiagnostics.run(VoiceBootstrapService.manager());
        status = report.state()+" · mic="+(report.microphone()?report.device():"no disponible")+" · modelo="+report.model();
    }
    private void add(int x,int y,String label,Runnable action) { addRenderableWidget(new Button(x,y,150,20,Component.literal(label),button->action.run())); }
    private void save(VoiceConfig.Values values) { VoiceForgeConfig.save(values); refresh(); }
    private void refresh() { minecraft.setScreen(new VoiceSettingsScreen(parent,page)); }
    private static String label(String name,boolean value) { return name+": "+(value?"Sí":"No"); }
    private static float cycle(float value,float step,float min,float max) { float next=value+step;return next>max+.0001f?min:next; }
    private static VoiceLanguageManager.Language nextLanguage(VoiceLanguageManager.Language value) {
        var values=VoiceLanguageManager.Language.values();return values[(value.ordinal()+1)%values.length];
    }
    private static String nextDevice(String current) {
        List<String> devices=new ArrayList<>();devices.add("");new AudioDeviceManager().devices().forEach(device->devices.add(device.name()));
        int index=devices.indexOf(current);return devices.get((index+1+devices.size())%devices.size());
    }
    private static String shortDevice(String value) { if(value==null||value.isBlank())return "Automático";return value.length()>18?value.substring(0,18)+"…":value; }
    private static VoiceConfig.Values copy(VoiceConfig.Values ignored,boolean enabled,VoiceLanguageManager.Language language,String microphone,
            float sensitivity,float gain,int seconds,boolean insert,boolean send,boolean hold,boolean overlay,boolean subtitles,boolean debug) {
        return new VoiceConfig.Values(enabled,language,microphone,sensitivity,gain,seconds,insert,send,hold,overlay,subtitles,debug);
    }
    @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,int mouseX,int mouseY,float partialTick) {
        renderBackground(pose);drawCenteredString(pose,font,title,width/2,height/2-108,0xFFFFFF);
        drawCenteredString(pose,font,Component.literal(status),width/2,height/2+48,0xAAAAAA);super.render(pose,mouseX,mouseY,partialTick);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
