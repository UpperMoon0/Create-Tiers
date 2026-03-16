# Create Tiers

A dynamic, customizable tiered system for Create kinetic components.

## About

**Create Tiers** is an extension for the [Create mod](https://www.curseforge.com/minecraft/mc-mods/create) that introduces a flexible tiering system for kinetic components like shafts and cogwheels. It is designed to be lightweight and highly customizable, allowing modpack authors to define their own progression without any external tech-tree dependencies.

This mod was inspired by the [Greate](https://github.com/GreateBeyondTheHorizon/Greate) mod, aiming to provide similar tiered functionality in a standalone package.

## Key Features

- **Dynamic Tier Registration**: Define as many tiers as you need! Tiers can be registered via KubeJS or datapacks during the initialization phase.
- **Customizable Performance**: Each tier can have its own Max RPM and Max Stress Units (SU) limits, allowing for deep progression balancing.
- **Individually Colored Components**: Customize the color of both shafts and cogwheels independently for each tier, creating a unique visual identity for your modpack's progression.
- **Automated Resource Generation**: No need to create dozens of models and textures. Blockstates, models, and textures are automatically generated for every registered tier at runtime.
- **Minimal Dependencies**: Requires only the Create mod, favoring flexibility without complex tech-tree requirements.

## Integration

### KubeJS Support
Create Tiers features native KubeJS integration, making it easy to register tiers directly from your startup scripts:

```javascript
// Example: Register a custom 'elite' tier
CreateTiers.registerTier('elite', 3, 1024, 4096, 0x00FFBB, 0x55FF55, 'Elite');
```
## License

This project is licensed under the MIT License
