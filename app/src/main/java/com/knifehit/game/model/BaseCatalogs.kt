package com.knifehit.game.model

object BaseCatalogs {
    val skins: List<KnifeSkin> = listOf(
        KnifeSkin(STARTER_SKIN_ID, "Classic", 0xFFD7E3EC, 1.00f, 0, 0, true, Timbre.HEAVY),
        KnifeSkin("timber", "Timber Wedge", 0xFFC47A3A, 1.05f, 80, 0, false, Timbre.HEAVY),
        KnifeSkin("wave", "Tide Splinter", 0xFF3EC7FF, 1.10f, 120, 0, false, Timbre.BLADE, adUnlockable = true),
        KnifeSkin("ember", "Ember Fang", 0xFFFF6A3D, 1.15f, 150, 0, false, Timbre.BLADE),
        KnifeSkin("frost", "Rime Needle", 0xFFB8F0FF, 1.18f, 200, 0, false, Timbre.BLADE),
        KnifeSkin("crystal", "Prism Shard", 0xFFE8A8FF, 1.22f, 400, 0, false, Timbre.ARCANE),
        KnifeSkin("neon", "Volt Dagger", 0xFF39FF14, 1.30f, 0, 5, false, Timbre.LASER, adUnlockable = true),
        KnifeSkin("magma", "Cinder Spike", 0xFFFF3344, 1.25f, 0, 8, false, Timbre.HEAVY),
        KnifeSkin("gold", "Gilded Lance", 0xFFFFD24A, 1.20f, 0, 15, false, Timbre.BLADE),
        KnifeSkin("void", "Eclipse Thorn", 0xFFB388FF, 1.40f, 0, 25, false, Timbre.ARCANE),
    )

    val targets: List<TargetBlueprint> = listOf(
        t("old-tree", "Old Tree Trunk", 70f, 0xFF6B3F1F, 0xFFC48A4A, TargetStyle.WOOD, 5),
        t("oak-stump", "Oak Stump", 65f, 0xFF8A5A2B, 0xFFE0B070, TargetStyle.WOOD, 4),
        t("bamboo", "Bamboo Log", 58f, 0xFF7FA64A, 0xFFD6E89A, TargetStyle.WOOD, 6),
        t("moss-disc", "Moss Disc", 62f, 0xFF3F6B3A, 0xFF9ED17A, TargetStyle.WOOD, 3),
        t("pine-ring", "Pine Ring", 60f, 0xFF5A3A22, 0xFFD2A679, TargetStyle.WOOD, 5),
        t("iron-shield", "Iron Shield", 68f, 0xFF8A93A0, 0xFFE8EEF5, TargetStyle.METAL, 4, 10),
        t("portcullis", "Portcullis Wheel", 64f, 0xFF5C6570, 0xFFC9D2DC, TargetStyle.METAL, 3, 12),
        t("banner-wheel", "Banner Wheel", 66f, 0xFF8B1E2D, 0xFFE8C36A, TargetStyle.SECTORS, 4, 8),
        t("steel-gear", "Steel Gear", 63f, 0xFF9AA3AD, 0xFFFFF6D8, TargetStyle.METAL, 2, 14),
        t("bronze-medal", "Bronze Medallion", 61f, 0xFFB87333, 0xFFFFE0A3, TargetStyle.METAL, 3, 8),
        t("coral-disc", "Coral Disc", 67f, 0xFFE07A8A, 0xFFFFD0C8, TargetStyle.STONE, 3),
        t("shell", "Tide Shell", 60f, 0xFFF0D2B0, 0xFFFFF6E8, TargetStyle.WEDGES, 6),
        t("urchin", "Urchin Ring", 55f, 0xFF6A4C9A, 0xFFE0C8FF, TargetStyle.STONE, 8, 16),
        t("pearl-ring", "Pearl Ring", 58f, 0xFFE8F0F4, 0xFF9FD7E8, TargetStyle.SECTORS, 4),
        t("kelp-wheel", "Kelp Wheel", 64f, 0xFF2F8A6A, 0xFFB6F0C8, TargetStyle.WOOD, 5),
        t("clay-plate", "Clay Plate", 66f, 0xFFC4784A, 0xFFF0C8A0, TargetStyle.STONE, 3),
        t("spice-wheel", "Spice Wheel", 70f, 0xFFD45A2A, 0xFFFFE08A, TargetStyle.WEDGES, 8),
        t("woven-mat", "Woven Mat", 62f, 0xFFC8A05A, 0xFFF5E0B0, TargetStyle.SECTORS, 6),
        t("brass-tray", "Brass Tray", 64f, 0xFFC9A227, 0xFFFFF0B0, TargetStyle.METAL, 3, 10),
        t("mosaic-disc", "Mosaic Disc", 60f, 0xFF3A6B8A, 0xFFE8C84A, TargetStyle.SECTORS, 8),
        t("ice-disc", "Ice Disc", 68f, 0xFF8FD4F0, 0xFFF5FFFF, TargetStyle.STONE, 4),
        t("frost-stone", "Frosted Stone", 63f, 0xFF6A7A88, 0xFFD0E8F8, TargetStyle.STONE, 3),
        t("snow-ring", "Snow Ring", 60f, 0xFFE8F4FA, 0xFF9CC8E0, TargetStyle.WOOD, 4),
        t("glacier-shard", "Glacier Shard", 72f, 0xFF6EC8E8, 0xFFE8FFFF, TargetStyle.WEDGES, 6),
        t("frozen-rune", "Frozen Rune", 65f, 0xFF4A6A88, 0xFFB8E0FF, TargetStyle.STONE, 5),
        t("molten-ingot", "Molten Ingot", 66f, 0xFFFF5A20, 0xFFFFE08A, TargetStyle.METAL, 3, 8),
        t("anvil-plate", "Anvil Plate", 70f, 0xFF4A4A52, 0xFFFF8A3A, TargetStyle.METAL, 2, 10),
        t("ember-wheel", "Ember Wheel", 64f, 0xFFB03020, 0xFFFFC04A, TargetStyle.SECTORS, 6),
        t("slag-disc", "Slag Disc", 58f, 0xFF3A2A28, 0xFFFF6A3A, TargetStyle.STONE, 3),
        t("cinder-ring", "Cinder Ring", 61f, 0xFF8A2A18, 0xFFFFB060, TargetStyle.WOOD, 4),
        t("pizza", "Pizza", 78f, 0xFFE8B84A, 0xFFE03A3A, TargetStyle.WEDGES, 8),
        t("cake", "Birthday Cake", 72f, 0xFFF0C0D0, 0xFFFFF4D8, TargetStyle.WEDGES, 6),
        t("watermelon", "Watermelon", 74f, 0xFF2E8B57, 0xFFFF6B7A, TargetStyle.WEDGES, 8),
        t("orange", "Orange Slice", 68f, 0xFFFFA040, 0xFFFFF0C8, TargetStyle.WEDGES, 8),
        t("donut", "Glazed Donut", 70f, 0xFFE0A060, 0xFFFFC0D8, TargetStyle.WEDGES, 6),
        t("clock-face", "Clock Face", 68f, 0xFFF0E6C8, 0xFF2A2A28, TargetStyle.SECTORS, 12, 12),
        t("orrery", "Orrery", 64f, 0xFFB8A070, 0xFFE8D8A8, TargetStyle.METAL, 4, 8),
        t("brass-gear", "Brass Gear", 62f, 0xFFC9A227, 0xFFFFF0C0, TargetStyle.METAL, 2, 16),
        t("astrolabe", "Astrolabe", 66f, 0xFF8A9AA8, 0xFFE8F0FF, TargetStyle.SECTORS, 8),
        t("star-chart", "Star Chart", 60f, 0xFF1A2040, 0xFFE8D48A, TargetStyle.SECTORS, 8),
        t("data-core", "Data Core", 64f, 0xFF12E0C8, 0xFF7A5CFF, TargetStyle.SECTORS, 6),
        t("holo-disc", "Holo Disc", 61f, 0xFF39FF14, 0xFF00F5FF, TargetStyle.METAL, 3, 10),
        t("circuit-ring", "Circuit Ring", 63f, 0xFF142028, 0xFF00F5FF, TargetStyle.WOOD, 5),
        t("pixel-wheel", "Pixel Wheel", 58f, 0xFFFF2BD6, 0xFF39FF14, TargetStyle.SECTORS, 8),
        t("neon-hex", "Neon Hex", 66f, 0xFF7A5CFF, 0xFFFF2BD6, TargetStyle.METAL, 4, 6),
        t("rune-disc", "Rune Disc", 70f, 0xFF2A1848, 0xFFE0B0FF, TargetStyle.STONE, 5),
        t("singularity", "Singularity Ring", 62f, 0xFF0A0618, 0xFFB388FF, TargetStyle.SECTORS, 6),
        t("eclipse-plate", "Eclipse Plate", 74f, 0xFF1A1028, 0xFFFFE08A, TargetStyle.STONE, 4),
        t("void-ring", "Void Ring", 60f, 0xFF12081C, 0xFF7A5CFF, TargetStyle.WOOD, 4),
        t("obsidian", "Obsidian Disc", 68f, 0xFF1C1218, 0xFF8A90A0, TargetStyle.STONE, 3),
    )

    val worlds: List<WorldDef> = listOf(
        WorldDef(
            1, "Whispering Woods",
            WorldTheme(0xFF07140C, 0xFF163022, 0xFF7DFF9A, 0xFFC8F5A0, 0xFF39FF14, 0xFF8FBF88, ParticleKind.LEAVES),
            listOf("old-tree", "oak-stump", "bamboo", "moss-disc", "pine-ring"),
            BossBlueprint("elder-trunk", "Great Elder Trunk", "old-tree", 1.28f),
        ),
        WorldDef(
            2, "Medieval Citadel",
            WorldTheme(0xFF10141C, 0xFF2A3140, 0xFFE8C36A, 0xFFC9D2DC, 0xFFFFE08A, 0xFFB8C0C8, ParticleKind.SPARKS),
            listOf("iron-shield", "portcullis", "banner-wheel", "steel-gear", "bronze-medal"),
            BossBlueprint("colossal-shield", "Colossal Iron Shield", "iron-shield", 1.30f),
        ),
        WorldDef(
            3, "Sunken Reef",
            WorldTheme(0xFF041820, 0xFF0A3A48, 0xFF3EC7FF, 0xFFFF8AB0, 0xFF7AF0FF, 0xFF5AB0C8, ParticleKind.BUBBLES),
            listOf("coral-disc", "shell", "urchin", "pearl-ring", "kelp-wheel"),
            BossBlueprint("kraken-eye", "Giant Kraken Eye", "urchin", 1.32f),
        ),
        WorldDef(
            4, "Desert Bazaar",
            WorldTheme(0xFF24140A, 0xFF5A3A18, 0xFFFFC04A, 0xFFE07A3A, 0xFFFFE08A, 0xFFE0C080, ParticleKind.SAND),
            listOf("clay-plate", "spice-wheel", "woven-mat", "brass-tray", "mosaic-disc"),
            BossBlueprint("great-spice", "Great Spice Wheel", "spice-wheel", 1.28f),
        ),
        WorldDef(
            5, "Frozen Peaks",
            WorldTheme(0xFF08141C, 0xFF1A3A50, 0xFFB8F0FF, 0xFFE8FFFF, 0xFF8FD4F0, 0xFFC8E8F8, ParticleKind.SNOW),
            listOf("ice-disc", "frost-stone", "snow-ring", "glacier-shard", "frozen-rune"),
            BossBlueprint("glacier-core", "Glacier Core", "glacier-shard", 1.30f),
        ),
        WorldDef(
            6, "Volcanic Forge",
            WorldTheme(0xFF1A0808, 0xFF401010, 0xFFFF6A3D, 0xFFFFC04A, 0xFFFF3344, 0xFFC06040, ParticleKind.EMBERS),
            listOf("molten-ingot", "anvil-plate", "ember-wheel", "slag-disc", "cinder-ring"),
            BossBlueprint("magma-anvil", "Magma Anvil", "anvil-plate", 1.30f),
        ),
        WorldDef(
            7, "Sweet Bakery",
            WorldTheme(0xFF241018, 0xFF5A2838, 0xFFFF8AB0, 0xFFFFE08A, 0xFFFFC0D8, 0xFFF0C0C8, ParticleKind.CRUMBS),
            listOf("pizza", "cake", "watermelon", "orange", "donut"),
            BossBlueprint("colossal-pizza", "Colossal Pizza", "pizza", 1.34f),
        ),
        WorldDef(
            8, "Clockwork Observatory",
            WorldTheme(0xFF12100C, 0xFF2A2418, 0xFFE8C36A, 0xFFC9A227, 0xFFFFF0B0, 0xFFC8B888, ParticleKind.GEARS),
            listOf("clock-face", "orrery", "brass-gear", "astrolabe", "star-chart"),
            BossBlueprint("grand-clock", "Grand Clock Face", "clock-face", 1.28f),
        ),
        WorldDef(
            9, "Neon Cyberpunk",
            WorldTheme(0xFF06080E, 0xFF141828, 0xFF00F5FF, 0xFFFF2BD6, 0xFF39FF14, 0xFF5A6A88, ParticleKind.NEON),
            listOf("data-core", "holo-disc", "circuit-ring", "pixel-wheel", "neon-hex"),
            BossBlueprint("firewall-core", "Firewall Core", "data-core", 1.30f),
        ),
        WorldDef(
            10, "Void Sanctum",
            WorldTheme(0xFF050308, 0xFF160C24, 0xFFB388FF, 0xFFFFE08A, 0xFFE0B0FF, 0xFF6A5088, ParticleKind.VOID),
            listOf("rune-disc", "singularity", "eclipse-plate", "void-ring", "obsidian"),
            BossBlueprint("eclipse-eye", "Eclipse Eye", "eclipse-plate", 1.34f),
        ),
    )

    private fun t(
        id: String,
        name: String,
        radiusDp: Float,
        base: Long,
        accent: Long,
        style: TargetStyle,
        rings: Int = 4,
        notches: Int = 8,
    ) = TargetBlueprint(id, name, radiusDp, base, accent, style, rings, notches)

    fun targetMap(): Map<String, TargetBlueprint> = targets.associateBy { it.id }
    fun skinMap(): Map<String, KnifeSkin> = skins.associateBy { it.id }
    fun worldMap(): Map<Int, WorldDef> = worlds.associateBy { it.id }
}
