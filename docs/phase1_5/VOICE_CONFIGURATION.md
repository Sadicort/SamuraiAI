# Voice Configuration

La configuración vive en el archivo CLIENT de Forge y no se lee en servidores:

| Clave | Función |
|---|---|
| `voice.enabled` | Activa/desactiva la GUI y tecla. |
| `voice.language` | `AUTO`, español, inglés, japonés, coreano, chino, portugués, francés, alemán, italiano o ruso. |
| `voice.microphone` | Nombre exacto del mixer; vacío selecciona uno compatible. |
| `voice.sensitivity` / `voice.gain` | Umbral visual y ganancia del pipeline. |
| `voice.maxRecordingSeconds` | Límite 1–120 segundos. |
| `voice.insertAutomatically` | Inserta la transcripción en chat. |
| `voice.sendAutomatically` | Envía después de insertar; desactivado por defecto. |
| `voice.holdToTalk` | Usa la tecla V como pulsar-mantener. |
| `voice.showOverlay` / `voice.showSubtitles` | Indicadores visuales. |
| `voice.debug` | Diagnóstico detallado. |

Los valores se validan y se aplican como snapshot atómico. No se guarda audio;
la selección del motor queda en Voice Core y no acepta ejecutables externos.
