# Wealth Engine

**Código:** `living/economy/wealth/WealthEngine.java`, `WealthAccount.java`.

- **Cuentas** por propietario: tesoro de asentamiento, NPC, hogar/familia, mercader, templo, facción, jugador. Monedas, propiedades (edificios), ingresos y gastos (totales y ventana de 30 días). Las monedas **nunca bajan de cero**: un pago sin fondos falla.
- **Acuñación** (`mint`): la única forma de que entren monedas al mundo, siempre con procedencia. Hoy se acuña solo en la fundación: `settlementEndowment` (500) para el tesoro de cada asentamiento y `merchantEndowment` (150) para la casa comercial. `minted()` suma todo lo acuñado.
- **Movimiento** (`transfer`, `transferUpTo`): registrado en el libro (`coins`), con motivo y referencia. Ventas, salarios, impuestos, recompensas de misión y pagos de contrato son transferencias; **el comercio nunca crea dinero**.
- **Niveles** de riqueza por patrimonio neto: pobre, modesto, próspero, rico, noble.
- La Family Engine usa las cuentas para la herencia de bienes (`../family_lineage_legacy_engine/PROPERTY_INHERITANCE.md`).

Prueba: `moneyIsNeverCreatedByTrade` (la suma de todas las cuentas es exactamente lo acuñado).
