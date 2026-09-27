# Corrección de empaquetado del runtime Whisper

## Síntoma

El entorno de desarrollo podía compilar `EmbeddedWhisperEngine`, pero el mod
instalado mostraba que Whisper no se podía cargar.

## Causa

La dependencia estaba declarada únicamente como `implementation`. Esa
configuración la dejaba disponible para compilar y ejecutar desde Gradle, pero
no la incluía dentro del JAR distribuible. El artefacto instalado no contenía
ni las clases Java de Whisper.cpp, ni JNA, ni `whisper.dll`.

## Solución

La dependencia ahora se declara mediante `jarJar(implementation(...))`, con un
rango Maven válido y la versión fijada a `1.4.0`:

```groovy
jarJar(implementation(group: 'io.github.ggerganov', name: 'whispercpp', version: '[1.4.0,1.5)')) {
    jarJar.pin(it, '1.4.0')
}
```

El artefacto distribuible ahora es `samuraiai-1.0-SNAPSHOT.jar`, sin sufijo.
Forge extrae desde él los JAR internos y registra automáticamente el runtime
nativo. El archivo `*-dev.jar` es únicamente la salida de desarrollo y no debe
instalarse.

## Evidencia de verificación

`gradle clean build` terminó con `BUILD SUCCESSFUL`. El contenido del artefacto
incluye:

- `META-INF/jarjar/whispercpp-1.4.0.jar`
- `META-INF/jarjar/jna-5.13.0.jar`
- `win32-x86-64/whisper.dll`
- `META-INF/jarjar/metadata.json`

También pasó el GameTest de Forge y la prueba de compilación de todo el código.

## Instalación

Copiar únicamente `build/libs/samuraiai-1.0-SNAPSHOT.jar` a la carpeta
`mods`. No usar el archivo `samuraiai-1.0-SNAPSHOT-dev.jar`, porque ese es el
JAR del proyecto sin las dependencias Jar-in-Jar.

El modelo sigue descargándose automáticamente en el primer inicio y se valida
por tamaño y SHA-256 antes de cargarlo.
