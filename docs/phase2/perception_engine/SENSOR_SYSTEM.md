# Sensor System

Interfaz `sensors.Sensor`; `SensorType` (13): `ENTITY, VISION, TOUCH, MOVEMENT, HEARING, DAMAGE, CONVERSATION, VOICE, ENVIRONMENT, LIGHT, WEATHER, BLOCK, SMELL`.

Implementaciones: `EntitySensor`, `VisionSensor`, `HearingSensor`, `MovementSensor`, `DamageSensor`, `ConversationSensor`, `VoiceSensor`, `touch.TouchSensor`, `smell.SmellSensor`, `environment.{Environment,Light,Weather,Block}Sensor`.

## Planificación (`SensorScheduler`)

Ningún sensor escanea cada tick. Intervalo base por tipo (visión 3, oído 2, movimiento 3, entidades 4, bloques 20, luz 20, clima 100, entorno 100, olfato 40; daño/conversación/voz cada pase), alargado según el tier de distancia y acortado en alerta. Un sensor que lanza una excepción pasa a `FAILED` `failureCooldownTicks`=100 y se reintenta: **un sensor roto no tumba la percepción del NPC** (`SensorState`, `SensorRecord`, probado en `SensorSystemTest`).

## Contexto y reparto de responsabilidades

`SensorContext` da a cada sensor lo que necesita (intervalo efectivo, mundo, sumidero de estímulos). Cada sensor es dueño de su parte (regresión: `EnvironmentSensor` pisaba el clima de `WeatherSensor`).

## Límites conocidos (honestos)

- `BlockSensor` escanea por **proximidad**, no por línea de visión.
- `VoiceSensor` es un canal preparado sin publicador.
- `SmellSensor` cubre humo que atraviesa paredes y rastro de alguien a la espalda, pero no modela viento ni rastros persistentes.
