# Fase 1.7 — Decisiones necesarias y ampliaciones propuestas

Fecha: 2026-09-18. Son propuestas, no funcionalidades implementadas ni cambios
aprobados del documento maestro. La guía original permanece intacta.

## Aclaración que bloquea ahora

La regla 2 impide implementar tras cualquier fallo crítico; el objetivo de la
fase exige precisamente reparar los fallos existentes. Se propone aclararla así:

> Un fallo crítico bloquea la certificación, la publicación y el avance a la
> siguiente fase. Dentro de Fase 1.7 se permiten su diagnóstico, corrección y
> pruebas de regresión. Nunca se declara READY mientras queden fallos críticos.

El usuario confirmó esta interpretación el 2026-09-18 (respuesta «sí»).
Se permiten las reparaciones dentro de Fase 1.7; siguen bloqueadas certificación,
publicación certificada y Fase 2. No se autorizaron nuevas funciones de gameplay.

## Ajustes técnicos que conviene incorporar

| Tema | Riesgo de una lectura literal | Propuesta |
|---|---|---|
| Client/Server Bootstrap: «Nunca ambos» | Excluir el servidor integrado de singleplayer | Distinguir distribución física de lado lógico; el cliente físico aloja ambos lados lógicos al abrir un mundo |
| Runtime sin entidad y soporte opcional | Romper controladores sin cuerpo o estados descargados | Exigir entidad válida a runtimes físicos activos; definir capacidades y estados para los demás |
| Estados de lifecycle | Las listas incluyen distintos estados de actividad | Separar lifecycle de actividad/hibernación y documentar todas las transiciones permitidas |
| Nombres de paquetes obligatorios | Crear estructuras duplicadas por cumplir nombres | Mapear responsabilidades existentes y migrar solo donde haya una necesidad demostrable |
| Jackson obligatorio | Añadir otra biblioteca JSON aunque ya se usa Gson | Mantener Gson salvo requisito concreto que justifique Jackson |
| Builds solo certificados | Impedir generar el artefacto necesario para probarlo | Permitir builds de desarrollo; bloquear únicamente publicación/distribución certificada |
| Pruebas después de las partes previas | Posponer regresiones hasta el final | Probar cada reparación y reservar la certificación integral para la etapa final |
| Opcionalidad frente a certificación | Marcar ausencia opcional como fallo universal o saltar pruebas necesarias | Perfiles explícitos: con/sin CustomNPCs, voz habilitada/deshabilitada, cliente/dedicado; no confundir SKIPPED con PASS |

En Forge, el servidor lógico también existe dentro del cliente físico en
singleplayer; `Dist.CLIENT` no identifica el hilo ni el lado lógico de una
operación. Referencia: [documentación oficial de lados de Forge 1.19.x](https://docs.minecraftforge.net/en/1.19.x/concepts/sides/).

## Datos que harán falta, pero no bloquean todo el trabajo local

- Actualizaciones firmadas: definir quién publica y firma el manifest remoto,
  URL de distribución, clave pública de confianza y política de rotación.
  No incluir claves privadas en el mod. Un SHA256 comprueba contenido contra un
  valor esperado; no sustituye una firma de origen. No inventar un servicio ni
  declarar verificada una firma inexistente.
- Certificación de voz: disponer de un micrófono utilizable y pruebas de audio
  autorizadas para cada escenario. La captura debe seguir iniciándose por una
  acción explícita del jugador. Un test sintético no certifica el dispositivo.
- Rendimiento: fijar escenarios y hardware de referencia antes de establecer
  umbrales de aceptación; conservar versiones, JVM, calentamiento y repeticiones.
- La propia guía indica que faltan los capítulos 67–72 de los originales.
  Si existen documentos adicionales, incorporarlos; no inventar requisitos para
  rellenar esa numeración.

## Añadidos recomendados que refuerzan la guía

1. Vincular cada certificado al hash del JAR, configuración de prueba,
   dependencias y evidencia. Un cambio posterior invalida ese certificado.
2. Inventario de licencias, bibliotecas nativas y modelos redistribuidos; pruebas
   del JAR empaquetado, no solo del classpath de desarrollo.
3. Descargas con límites de bytes, validación del destino, protección frente a
   rutas fuera de la carpeta administrada y pruebas de disco lleno/corte de red.
4. Identificador de generación en bootstrap y sesiones: una respuesta antigua
   no puede reactivar un motor cancelado ni sustituir uno más reciente.
5. Cierre verificable de contextos nativos, micrófono, streams y workers al
   cancelar, cambiar modelo o cerrar el cliente; límites de cola explícitos.
6. Diagnósticos sin texto de conversaciones ni audio; redacción de rutas y datos
   personales antes de compartir informes. Retención y tamaño acotados.
7. Casos negativos para cada mecanismo de reparación: no basta probar éxito;
   comprobar que no destruye la última versión válida ni entra en reintentos
   ilimitados.

Estas propuestas no implementan Behavior Engine, combate, patrulla ni nuevas
funciones de gameplay. No se requieren decisiones del usuario sobre cada detalle
interno: se podrán resolver según arquitectura y pruebas tras aclarar la regla
de parada.
