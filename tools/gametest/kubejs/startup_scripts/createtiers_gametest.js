// Dedicated runtime fixture for Create Tiers GameTests.
// Loaded before block registration so native tier blocks exist exactly as they do in a real KubeJS-configured pack.
CreateTiers.registerTier(
    'gametest_native',
    900001,
    1024,
    4096,
    0x66CCFF,
    0x44AAEE,
    'GameTest Native'
)
