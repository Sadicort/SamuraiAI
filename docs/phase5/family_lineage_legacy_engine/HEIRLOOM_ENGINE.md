# Heirloom Engine

**Código:** `living/family/heritage/Heirloom.java`; `FamilyEngine.registerHeirloom/transferHeirloom/heirloomEvent/heirloomLost`.

- Una reliquia es un objeto cuya historia importa: una katana más vieja que su dueño, el martillo del fundador. Guarda quién la hizo o tuvo primero, quién la tiene, a qué familia pertenece, cuándo se hizo, cada transferencia y cada suceso.
- Su **valor simbólico** crece con las generaciones por las que pasa y la historia que acumula.
- `itemId` es el id que llevaría el objeto físico. **Límite:** hoy no se estampa en un objeto de Minecraft; la reliquia vive en el registro.
- **Perdida** → misión «{reliquia} de la familia {nombre}» (y campaña «El legado de los …»); hallada → vuelve a la familia.
- Pasar una reliquia más de una vez crea una **tradición** familiar.

Comandos (op): `family heirloom create <npc> <tipo> <nombre>`, `give <reliquia> <npc>`, `lost <reliquia>`, `found <reliquia>`, `show <nombre>`.
