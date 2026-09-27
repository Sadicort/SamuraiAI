# Family Traditions

**Código:** `living/family/traditions/TraditionDetector.java`.

Las tradiciones **emergen de la historia repetida**, no se asignan:

- un oficio practicado en **dos o más generaciones** → `oficio:<x>`;
- una reliquia pasada **más de una vez**;
- los antepasados honrados en **Obon en dos años distintos**;
- una escuela marcial seguida a lo largo de **varias generaciones**.

Se detectan cada día; una nueva se anota en la memoria («Nace una tradición: …») y publica `TraditionEmergedEvent`. Pesan en la sucesión (`tradition`) y en las sugerencias de oficio.

Prueba: `reputationHonourTraditionsAndStories`.
