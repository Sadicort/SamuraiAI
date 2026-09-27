# Price Engine

**Código:** `living/economy/prices/PriceEngine.java`, `PricePoint.java`.

`precio = valor_base × Π factores`, con:

| Factor | Cálculo |
| --- | --- |
| oferta/demanda | `(demanda / oferta)^elasticidad` (`priceElasticity` 0,6), acotado; demanda = lo necesario para cubrir `coverTargetDays` (7) días |
| estación | los cultivos cuestan menos en su cosecha y más fuera de ella |
| guerra | el peligro de la región encarece comida, herramientas y combustible |
| producción | lo que el asentamiento no produce lleva prima de importación creciente con la distancia |
| escasez/excedente | el ánimo del mercado sobre los números (pánico `panicDemand` 1,25 en escasez) |
| riqueza | los asentamientos más ricos pagan más |

El precio se mueve hacia el objetivo gradualmente (`priceSmoothing` 0,35) y se recalcula cada `priceIntervalMinutes` (60) minutos de Deiliora, **nunca por tick**. Un cambio mayor que `priceEventThreshold` (5 %) publica `PriceChangedEvent(antes, después, factor principal)`. `PricePoint` conserva oferta, demanda y factores para explicar un precio (`/samuraiai living economy prices <aldea>`).
