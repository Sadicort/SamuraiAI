# Memory Link

`memorylink/MemoryLinkEngine`: la relación guarda ids de recuerdos y trazas (≤ `maxMemoryLinks`); el recuerdo guarda ids de destino (`socialLinks`). Flujo: recuerdo → `SocialEvidence` (construida por el hub con el perfil de la experiencia) → `apply` → `linkSocial`. Sin objetos duplicados.
