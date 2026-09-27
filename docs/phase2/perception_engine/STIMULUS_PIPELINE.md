# Stimulus Pipeline

Clases `stimuli.{Stimulus, StimulusType, StimulusCategory, StimulusSink}` y `filters.{StimulusPipeline, StimulusFilter, StandardFilters, FilterContext}`.

`StimulusType`: `VISUAL, AUDIO, ENVIRONMENT, DAMAGE, CONVERSATION, VOICE, TOUCH, SMELL, CUSTOM`.

Cadena por defecto (`StandardFilters.chain()`), en este orden: **distancia → visibilidad → relación → prioridad → cooldown**. Cada filtro acepta, rechaza (con motivo) o reajusta. Un estímulo por debajo de la prioridad mínima se descarta (por diseño un cambio de bioma no llega al Brain); el cooldown evita que el mismo estímulo inunde al NPC.

Métricas: `stimuliRaw`, `stimuliAccepted`, `stimuliRejected` por pase. El pipeline es extensible: se añade un `StimulusFilter` sin tocar los sensores ni la atención.
