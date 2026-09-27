# Consumption Engine

**Código:** `living/economy/consumption/NeedProfile.java`; `EconomyEngine.consume`.

Por persona y día (`needs;food=1.0;water=2.0;fuel=0.3;cloth=0.01;child=0.6;foods=meal,rice,fish,...`):

- **Comida** 1 unidad, cubierta con cualquier comida en orden de preferencia (comida preparada primero) según su valor `food`;
- **agua** 2, **combustible** 0,3 (madera, carbón según su valor `fuel`), **tela** 0,01;
- un niño cuenta como 0,6 adultos (etapas de vida de Family);
- la **estación** multiplica comida y combustible (invierno ×1,25 comida, ×2 combustible);
- los **visitantes** presentes comen también; los **festivales** multiplican la demanda de sus recursos.

Se consume de los lotes más antiguos (FIFO). Lo que falta es **déficit** (`shortfall`), no se inventa: `ResourceConsumedEvent(consumido, déficit, días de comida)`. El déficit alimenta la escasez, el malestar de la aldea (`unrestFoodDays`) y las misiones.

Prueba: `peopleConsumeAndScarcityRaisesPrices`.
