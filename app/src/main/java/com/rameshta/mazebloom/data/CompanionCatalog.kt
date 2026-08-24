package com.rameshta.mazebloom.data

enum class CompanionFamily { MAMMAL, BIRD, INSECT, GARDEN, WATER }

enum class CompanionMotion { BOUNCE, HOP, PROWL, WADDLE, FLAP, HOVER, SCUTTLE, CRAWL, SWIM, PULSE }

enum class CompanionTrait {
    ROUND_EARS, POINT_EARS, LONG_EARS, FLOPPY_EARS, HORNS, ANTLERS, MANE, TRUNK,
    MASK, STRIPES, PATCHES, QUILLS, BEAK, LONG_BEAK, CREST, OWL_EYES, FAN_TAIL,
    LONG_LEGS, ANTENNAE, WINGS, FOUR_WINGS, SPOTS, GLOW, PINCERS, SEGMENTS,
    SHELL, WEBBED_FEET, GILLS, FINS, TENTACLES, LONG_BODY, SPIKES, TURRET_EYES,
    FLUFFY_TAIL, LONG_SNOUT, WOOL, SNOUT, TUSKS,
}

data class CompanionDefinition(
    val id: String,
    val displayName: String,
    val family: CompanionFamily,
    val motion: CompanionMotion,
    val primaryArgb: Long,
    val secondaryArgb: Long,
    val accentArgb: Long,
    val traits: Set<CompanionTrait> = emptySet(),
    val price: Int = 0,
)

data class CompanionCollectionState(
    val selectedId: String = CompanionCatalog.FIRST_COMPANION_ID,
    val purchasedIds: Set<String> = emptySet(),
    val adCreditsById: Map<String, Int> = emptyMap(),
) {
    val ownedIds: Set<String> get() = purchasedIds + CompanionCatalog.FIRST_COMPANION_ID

    fun adCredit(companionId: String): Int = adCreditsById[companionId] ?: 0
}

enum class CompanionPurchaseResult {
    PURCHASED,
    ALREADY_OWNED,
    INSUFFICIENT_COINS,
    FEATURE_LOCKED,
}

enum class CompanionAdRewardResult {
    PROGRESS,
    UNLOCKED,
    ALREADY_OWNED,
    FEATURE_LOCKED,
    DUPLICATE_REWARD,
}

data class CompanionAdRewardOutcome(
    val result: CompanionAdRewardResult,
    val creditedCoins: Int,
    val remainingCoins: Int,
)

object CompanionCatalog {
    const val UNLOCK_CAMPAIGN_COMPLETIONS = 10
    const val FIRST_COMPANION_ID = "meadow_mouse"
    const val REWARDED_AD_CREDIT = 50

    private fun entry(
        id: String,
        name: String,
        family: CompanionFamily,
        motion: CompanionMotion,
        primary: Long,
        secondary: Long,
        accent: Long,
        vararg traits: CompanionTrait,
    ) = CompanionDefinition(id, name, family, motion, primary, secondary, accent, traits.toSet())

    val all: List<CompanionDefinition> = listOf(
        // Mammals
        entry("meadow_mouse", "Meadow Mouse", CompanionFamily.MAMMAL, CompanionMotion.SCUTTLE, 0xFF8A7567, 0xFFE8D7C8, 0xFFF1A8B8, CompanionTrait.ROUND_EARS),
        entry("moon_bunny", "Moon Bunny", CompanionFamily.MAMMAL, CompanionMotion.HOP, 0xFFD8D4D0, 0xFFFFF7EA, 0xFFF29CB2, CompanionTrait.LONG_EARS),
        entry("ember_fox", "Ember Fox", CompanionFamily.MAMMAL, CompanionMotion.PROWL, 0xFFD56B3F, 0xFFFFE4C3, 0xFF42352E, CompanionTrait.POINT_EARS),
        entry("garden_cat", "Garden Cat", CompanionFamily.MAMMAL, CompanionMotion.PROWL, 0xFF6E7885, 0xFFE8EDF0, 0xFF8BC6A4, CompanionTrait.POINT_EARS, CompanionTrait.STRIPES),
        entry("clover_dog", "Clover Dog", CompanionFamily.MAMMAL, CompanionMotion.BOUNCE, 0xFFB98555, 0xFFF4D9B7, 0xFF6B4A37, CompanionTrait.FLOPPY_EARS, CompanionTrait.PATCHES),
        entry("bamboo_panda", "Bamboo Panda", CompanionFamily.MAMMAL, CompanionMotion.BOUNCE, 0xFFF2EEE5, 0xFF3A403C, 0xFF84AD72, CompanionTrait.ROUND_EARS, CompanionTrait.PATCHES),
        entry("eucalyptus_koala", "Eucalyptus Koala", CompanionFamily.MAMMAL, CompanionMotion.BOUNCE, 0xFF9FA8A7, 0xFFDDE4DF, 0xFF58645F, CompanionTrait.ROUND_EARS),
        entry("honey_bear", "Honey Bear", CompanionFamily.MAMMAL, CompanionMotion.BOUNCE, 0xFF8C674A, 0xFFDAB98B, 0xFFE1A83A, CompanionTrait.ROUND_EARS),
        entry("fern_deer", "Fern Deer", CompanionFamily.MAMMAL, CompanionMotion.HOP, 0xFFB37B55, 0xFFF2D7B3, 0xFF5E8C65, CompanionTrait.ANTLERS, CompanionTrait.SPOTS),
        entry("sunset_lion", "Sunset Lion", CompanionFamily.MAMMAL, CompanionMotion.PROWL, 0xFFE0A54C, 0xFFF4D58A, 0xFF9A5738, CompanionTrait.MANE, CompanionTrait.ROUND_EARS),
        entry("marigold_tiger", "Marigold Tiger", CompanionFamily.MAMMAL, CompanionMotion.PROWL, 0xFFE58C3B, 0xFFFFD995, 0xFF4A3B32, CompanionTrait.POINT_EARS, CompanionTrait.STRIPES),
        entry("river_elephant", "River Elephant", CompanionFamily.MAMMAL, CompanionMotion.BOUNCE, 0xFF8D9BA8, 0xFFDCE5E9, 0xFFF4A9B4, CompanionTrait.ROUND_EARS, CompanionTrait.TRUNK),

        // Birds
        entry("rose_robin", "Rose Robin", CompanionFamily.BIRD, CompanionMotion.FLAP, 0xFF8C5B4E, 0xFFE7A18F, 0xFFF2C55B, CompanionTrait.BEAK, CompanionTrait.WINGS),
        entry("moss_owl", "Moss Owl", CompanionFamily.BIRD, CompanionMotion.FLAP, 0xFF6F6A55, 0xFFC9C29B, 0xFF84A26E, CompanionTrait.BEAK, CompanionTrait.WINGS, CompanionTrait.OWL_EYES),
        entry("rainbow_parrot", "Rainbow Parrot", CompanionFamily.BIRD, CompanionMotion.FLAP, 0xFF3F9A67, 0xFF55A7C4, 0xFFF0B43F, CompanionTrait.BEAK, CompanionTrait.WINGS, CompanionTrait.CREST),
        entry("snow_penguin", "Snow Penguin", CompanionFamily.BIRD, CompanionMotion.WADDLE, 0xFF303A3D, 0xFFF4F2E8, 0xFFF0A33A, CompanionTrait.BEAK, CompanionTrait.WINGS, CompanionTrait.WEBBED_FEET),
        entry("coral_flamingo", "Coral Flamingo", CompanionFamily.BIRD, CompanionMotion.WADDLE, 0xFFF08EA0, 0xFFFFC2C8, 0xFF3C3E3F, CompanionTrait.LONG_BEAK, CompanionTrait.WINGS, CompanionTrait.LONG_LEGS),
        entry("royal_peacock", "Royal Peacock", CompanionFamily.BIRD, CompanionMotion.FLAP, 0xFF287B75, 0xFF3D65A5, 0xFFD8B43E, CompanionTrait.BEAK, CompanionTrait.WINGS, CompanionTrait.CREST, CompanionTrait.FAN_TAIL),
        entry("citrus_toucan", "Citrus Toucan", CompanionFamily.BIRD, CompanionMotion.FLAP, 0xFF303537, 0xFFF4E9D7, 0xFFF19A38, CompanionTrait.LONG_BEAK, CompanionTrait.WINGS),
        entry("mint_hummingbird", "Mint Hummingbird", CompanionFamily.BIRD, CompanionMotion.HOVER, 0xFF54A389, 0xFF7CC7C4, 0xFFD75777, CompanionTrait.LONG_BEAK, CompanionTrait.WINGS),
        entry("pond_duck", "Pond Duck", CompanionFamily.BIRD, CompanionMotion.WADDLE, 0xFF6F9B62, 0xFFE3E9C9, 0xFFF0A33A, CompanionTrait.BEAK, CompanionTrait.WINGS, CompanionTrait.WEBBED_FEET),
        entry("lily_swan", "Lily Swan", CompanionFamily.BIRD, CompanionMotion.WADDLE, 0xFFF1F0E8, 0xFFD5D8D3, 0xFFE69943, CompanionTrait.BEAK, CompanionTrait.WINGS, CompanionTrait.LONG_BODY),
        entry("storm_eagle", "Storm Eagle", CompanionFamily.BIRD, CompanionMotion.FLAP, 0xFF76563F, 0xFFF1E8D4, 0xFFE5B13E, CompanionTrait.BEAK, CompanionTrait.WINGS),
        entry("forest_kiwi", "Forest Kiwi", CompanionFamily.BIRD, CompanionMotion.SCUTTLE, 0xFF75644E, 0xFFB6A47E, 0xFFE1B368, CompanionTrait.LONG_BEAK, CompanionTrait.LONG_LEGS),

        // Insects
        entry("lucky_ladybug", "Lucky Ladybug", CompanionFamily.INSECT, CompanionMotion.SCUTTLE, 0xFFD94D48, 0xFF2E3633, 0xFFF3D45E, CompanionTrait.ANTENNAE, CompanionTrait.WINGS, CompanionTrait.SPOTS),
        entry("monarch_butterfly", "Monarch Butterfly", CompanionFamily.INSECT, CompanionMotion.HOVER, 0xFFF09835, 0xFF3B342F, 0xFFFFD061, CompanionTrait.ANTENNAE, CompanionTrait.FOUR_WINGS),
        entry("honey_bee", "Honey Bee", CompanionFamily.INSECT, CompanionMotion.HOVER, 0xFFF0B83F, 0xFF493E32, 0xFFD9EDF1, CompanionTrait.ANTENNAE, CompanionTrait.WINGS, CompanionTrait.STRIPES),
        entry("blue_dragonfly", "Blue Dragonfly", CompanionFamily.INSECT, CompanionMotion.HOVER, 0xFF4B91A8, 0xFF9BD5D6, 0xFF405C78, CompanionTrait.ANTENNAE, CompanionTrait.FOUR_WINGS, CompanionTrait.LONG_BODY),
        entry("twilight_firefly", "Twilight Firefly", CompanionFamily.INSECT, CompanionMotion.HOVER, 0xFF414C48, 0xFF829C68, 0xFFE8E866, CompanionTrait.ANTENNAE, CompanionTrait.WINGS, CompanionTrait.GLOW),
        entry("jade_beetle", "Jade Beetle", CompanionFamily.INSECT, CompanionMotion.SCUTTLE, 0xFF34816D, 0xFF70B494, 0xFFE8C35A, CompanionTrait.ANTENNAE, CompanionTrait.WINGS, CompanionTrait.SHELL),
        entry("luna_moth", "Luna Moth", CompanionFamily.INSECT, CompanionMotion.HOVER, 0xFF9ACB9A, 0xFFDCE9C2, 0xFF8A7DA8, CompanionTrait.ANTENNAE, CompanionTrait.FOUR_WINGS),
        entry("meadow_grasshopper", "Meadow Grasshopper", CompanionFamily.INSECT, CompanionMotion.HOP, 0xFF6E9D55, 0xFFAFC276, 0xFF4D6444, CompanionTrait.ANTENNAE, CompanionTrait.LONG_LEGS, CompanionTrait.WINGS),
        entry("tiny_ant", "Tiny Ant", CompanionFamily.INSECT, CompanionMotion.SCUTTLE, 0xFF5A4539, 0xFF8A6550, 0xFFE6B95F, CompanionTrait.ANTENNAE, CompanionTrait.SEGMENTS),
        entry("leaf_mantis", "Leaf Mantis", CompanionFamily.INSECT, CompanionMotion.SCUTTLE, 0xFF72A65B, 0xFFB5CE76, 0xFF476B43, CompanionTrait.ANTENNAE, CompanionTrait.PINCERS, CompanionTrait.LONG_LEGS),
        entry("summer_cicada", "Summer Cicada", CompanionFamily.INSECT, CompanionMotion.HOVER, 0xFF7D7A56, 0xFFC3BB86, 0xFF8BC7BF, CompanionTrait.ANTENNAE, CompanionTrait.WINGS, CompanionTrait.SEGMENTS),
        entry("berry_caterpillar", "Berry Caterpillar", CompanionFamily.INSECT, CompanionMotion.CRAWL, 0xFF8CB65E, 0xFFBFD77B, 0xFFD9687C, CompanionTrait.ANTENNAE, CompanionTrait.SEGMENTS, CompanionTrait.LONG_BODY),

        // Garden and reptile companions
        entry("dewdrop_snail", "Dewdrop Snail", CompanionFamily.GARDEN, CompanionMotion.CRAWL, 0xFF8EAD72, 0xFFB97B58, 0xFF6B91A0, CompanionTrait.ANTENNAE, CompanionTrait.SHELL),
        entry("lily_frog", "Lily Frog", CompanionFamily.GARDEN, CompanionMotion.HOP, 0xFF65A45F, 0xFFA8D17D, 0xFFF2C65F, CompanionTrait.WEBBED_FEET, CompanionTrait.SPOTS),
        entry("moss_turtle", "Moss Turtle", CompanionFamily.GARDEN, CompanionMotion.CRAWL, 0xFF709463, 0xFF496E52, 0xFFD7B85C, CompanionTrait.SHELL, CompanionTrait.SPOTS),
        entry("thistle_hedgehog", "Thistle Hedgehog", CompanionFamily.GARDEN, CompanionMotion.SCUTTLE, 0xFF8A674F, 0xFFD8B78C, 0xFF4F4037, CompanionTrait.QUILLS, CompanionTrait.POINT_EARS),
        entry("orchid_chameleon", "Orchid Chameleon", CompanionFamily.GARDEN, CompanionMotion.CRAWL, 0xFF72A865, 0xFFB3D47A, 0xFFD86D8B, CompanionTrait.TURRET_EYES, CompanionTrait.LONG_BODY),
        entry("sunstone_gecko", "Sunstone Gecko", CompanionFamily.GARDEN, CompanionMotion.SCUTTLE, 0xFFE29B59, 0xFFF2CB7A, 0xFF6C7852, CompanionTrait.SPOTS, CompanionTrait.LONG_BODY),

        // Aquatic companions
        entry("blush_axolotl", "Blush Axolotl", CompanionFamily.WATER, CompanionMotion.SWIM, 0xFFF2A5B5, 0xFFFFD4D9, 0xFFB75F82, CompanionTrait.GILLS, CompanionTrait.LONG_BODY),
        entry("river_otter", "River Otter", CompanionFamily.WATER, CompanionMotion.SWIM, 0xFF7C5C47, 0xFFD8B88D, 0xFF5A8A89, CompanionTrait.ROUND_EARS, CompanionTrait.LONG_BODY),
        entry("silver_dolphin", "Silver Dolphin", CompanionFamily.WATER, CompanionMotion.SWIM, 0xFF6E9FB7, 0xFFBBD9E3, 0xFF3C708D, CompanionTrait.FINS, CompanionTrait.LONG_BODY),
        entry("cloud_whale", "Cloud Whale", CompanionFamily.WATER, CompanionMotion.SWIM, 0xFF668EA4, 0xFFAED0D8, 0xFFF1D78C, CompanionTrait.FINS),
        entry("coral_octopus", "Coral Octopus", CompanionFamily.WATER, CompanionMotion.PULSE, 0xFFD77675, 0xFFF0A79B, 0xFF764F8B, CompanionTrait.TENTACLES, CompanionTrait.SPOTS),
        entry("golden_seahorse", "Golden Seahorse", CompanionFamily.WATER, CompanionMotion.SWIM, 0xFFD9A749, 0xFFF0CF79, 0xFF5D8D83, CompanionTrait.FINS, CompanionTrait.LONG_BODY, CompanionTrait.SPIKES),

        // Expansion companions stay at the end so existing Coin prices never change.
        entry("acorn_squirrel", "Acorn Squirrel", CompanionFamily.MAMMAL, CompanionMotion.SCUTTLE, 0xFFB87946, 0xFFEAD0A4, 0xFF6D4B37, CompanionTrait.POINT_EARS, CompanionTrait.FLUFFY_TAIL),
        entry("alpine_goat", "Alpine Goat", CompanionFamily.MAMMAL, CompanionMotion.HOP, 0xFFC9BBA1, 0xFFF0E3CD, 0xFF725F4F, CompanionTrait.FLOPPY_EARS, CompanionTrait.HORNS),
        entry("lavender_crane", "Lavender Crane", CompanionFamily.BIRD, CompanionMotion.WADDLE, 0xFFD7C5DC, 0xFFF5EEF3, 0xFFB86673, CompanionTrait.LONG_BEAK, CompanionTrait.WINGS, CompanionTrait.LONG_LEGS),
        entry("cherry_cockatoo", "Cherry Cockatoo", CompanionFamily.BIRD, CompanionMotion.FLAP, 0xFFF2EADB, 0xFFE8798E, 0xFFE4B84A, CompanionTrait.BEAK, CompanionTrait.WINGS, CompanionTrait.CREST),
        entry("poppy_cricket", "Poppy Cricket", CompanionFamily.INSECT, CompanionMotion.HOP, 0xFF6E8E4D, 0xFFA9C36C, 0xFFD95D57, CompanionTrait.ANTENNAE, CompanionTrait.WINGS, CompanionTrait.LONG_LEGS, CompanionTrait.SEGMENTS),
        entry("iris_weevil", "Iris Weevil", CompanionFamily.INSECT, CompanionMotion.SCUTTLE, 0xFF5F547E, 0xFF9889B4, 0xFFE1BD59, CompanionTrait.ANTENNAE, CompanionTrait.SHELL, CompanionTrait.SPOTS, CompanionTrait.LONG_SNOUT),
        entry("rain_salamander", "Rain Salamander", CompanionFamily.GARDEN, CompanionMotion.CRAWL, 0xFF516E5E, 0xFF83A879, 0xFF74A8C0, CompanionTrait.SPOTS, CompanionTrait.LONG_BODY),
        entry("vine_snake", "Vine Snake", CompanionFamily.GARDEN, CompanionMotion.CRAWL, 0xFF67965C, 0xFFA7C67E, 0xFFD6A74B, CompanionTrait.LONG_BODY, CompanionTrait.STRIPES),
        entry("velvet_spider", "Velvet Spider", CompanionFamily.GARDEN, CompanionMotion.SCUTTLE, 0xFF704E5A, 0xFFB78191, 0xFFE3B052, CompanionTrait.SPOTS),
        entry("sunset_koi", "Sunset Koi", CompanionFamily.WATER, CompanionMotion.SWIM, 0xFFE98455, 0xFFF4D8B0, 0xFF3D7590, CompanionTrait.FINS, CompanionTrait.SPOTS),
        entry("tidepool_crab", "Tidepool Crab", CompanionFamily.WATER, CompanionMotion.SCUTTLE, 0xFFD76F58, 0xFFF3A782, 0xFF6C526C, CompanionTrait.PINCERS, CompanionTrait.SHELL),
        entry("moon_jellyfish", "Moon Jellyfish", CompanionFamily.WATER, CompanionMotion.PULSE, 0xFFA88FD1, 0xFFDDCBF0, 0xFF6BBAC0, CompanionTrait.TENTACLES, CompanionTrait.GLOW),

        // Second expansion: eight additions per family, still appended for price stability.
        entry("cedar_raccoon", "Cedar Raccoon", CompanionFamily.MAMMAL, CompanionMotion.SCUTTLE, 0xFF7C8583, 0xFFD9D3C5, 0xFF3F4847, CompanionTrait.ROUND_EARS, CompanionTrait.MASK),
        entry("frost_wolf", "Frost Wolf", CompanionFamily.MAMMAL, CompanionMotion.PROWL, 0xFF8796A5, 0xFFDCE5E9, 0xFF536675, CompanionTrait.POINT_EARS),
        entry("maple_moose", "Maple Moose", CompanionFamily.MAMMAL, CompanionMotion.HOP, 0xFF916747, 0xFFD7B68C, 0xFF5D4638, CompanionTrait.ROUND_EARS, CompanionTrait.ANTLERS),
        entry("cloud_sheep", "Cloud Sheep", CompanionFamily.MAMMAL, CompanionMotion.BOUNCE, 0xFFF0E5D2, 0xFFFFFCF5, 0xFFA68F76, CompanionTrait.FLOPPY_EARS, CompanionTrait.WOOL),
        entry("daisy_cow", "Daisy Cow", CompanionFamily.MAMMAL, CompanionMotion.BOUNCE, 0xFFF2ECE1, 0xFF5C4B43, 0xFFE3B94B, CompanionTrait.FLOPPY_EARS, CompanionTrait.HORNS, CompanionTrait.PATCHES),
        entry("cocoa_monkey", "Cocoa Monkey", CompanionFamily.MAMMAL, CompanionMotion.BOUNCE, 0xFF7A5944, 0xFFD6A679, 0xFF51392F, CompanionTrait.ROUND_EARS),
        entry("orchard_pig", "Orchard Pig", CompanionFamily.MAMMAL, CompanionMotion.BOUNCE, 0xFFE8A0A7, 0xFFF6CBD0, 0xFFA85E69, CompanionTrait.ROUND_EARS, CompanionTrait.SNOUT),
        entry("dapple_leopard", "Dapple Leopard", CompanionFamily.MAMMAL, CompanionMotion.PROWL, 0xFFD4A052, 0xFFF3D18B, 0xFF564238, CompanionTrait.POINT_EARS, CompanionTrait.SPOTS),
        entry("cedar_blue_jay", "Cedar Blue Jay", CompanionFamily.BIRD, CompanionMotion.FLAP, 0xFF4B7095, 0xFFC7DAE7, 0xFF263D5A, CompanionTrait.BEAK, CompanionTrait.WINGS, CompanionTrait.CREST),
        entry("scarlet_cardinal", "Scarlet Cardinal", CompanionFamily.BIRD, CompanionMotion.FLAP, 0xFFC94F4A, 0xFFE9816B, 0xFF3E3431, CompanionTrait.BEAK, CompanionTrait.WINGS, CompanionTrait.CREST),
        entry("moon_heron", "Moon Heron", CompanionFamily.BIRD, CompanionMotion.WADDLE, 0xFFB7BBC5, 0xFFE8E7E2, 0xFFD6A24F, CompanionTrait.LONG_BEAK, CompanionTrait.WINGS, CompanionTrait.LONG_LEGS),
        entry("orchard_woodpecker", "Orchard Woodpecker", CompanionFamily.BIRD, CompanionMotion.FLAP, 0xFF3A4142, 0xFFE7D8C1, 0xFFC94E42, CompanionTrait.LONG_BEAK, CompanionTrait.WINGS, CompanionTrait.CREST),
        entry("golden_pheasant", "Golden Pheasant", CompanionFamily.BIRD, CompanionMotion.FLAP, 0xFFD89B35, 0xFF7F4C35, 0xFF2E7566, CompanionTrait.BEAK, CompanionTrait.WINGS, CompanionTrait.CREST, CompanionTrait.FAN_TAIL),
        entry("dawn_pelican", "Dawn Pelican", CompanionFamily.BIRD, CompanionMotion.WADDLE, 0xFFF1DCC3, 0xFFF9F2E5, 0xFFE8A652, CompanionTrait.LONG_BEAK, CompanionTrait.WINGS, CompanionTrait.WEBBED_FEET),
        entry("meadow_quail", "Meadow Quail", CompanionFamily.BIRD, CompanionMotion.SCUTTLE, 0xFFB5976F, 0xFFE5D2AC, 0xFF7C5D49, CompanionTrait.BEAK, CompanionTrait.WINGS, CompanionTrait.CREST),
        entry("azure_kingfisher", "Azure Kingfisher", CompanionFamily.BIRD, CompanionMotion.HOVER, 0xFF2F8294, 0xFF71B7B2, 0xFFE77745, CompanionTrait.LONG_BEAK, CompanionTrait.WINGS, CompanionTrait.CREST),
        entry("amber_wasp", "Amber Wasp", CompanionFamily.INSECT, CompanionMotion.HOVER, 0xFFE5AD35, 0xFF49392E, 0xFFD9EFF0, CompanionTrait.ANTENNAE, CompanionTrait.WINGS, CompanionTrait.STRIPES),
        entry("velvet_bumblebee", "Velvet Bumblebee", CompanionFamily.INSECT, CompanionMotion.HOVER, 0xFFE7B84E, 0xFF514238, 0xFFEDD9B4, CompanionTrait.ANTENNAE, CompanionTrait.WINGS, CompanionTrait.STRIPES, CompanionTrait.SEGMENTS),
        entry("speckled_beetle", "Speckled Beetle", CompanionFamily.INSECT, CompanionMotion.SCUTTLE, 0xFF8C456B, 0xFFC77FA1, 0xFFEBC75B, CompanionTrait.ANTENNAE, CompanionTrait.WINGS, CompanionTrait.SHELL, CompanionTrait.SPOTS),
        entry("glasswing_butterfly", "Glasswing Butterfly", CompanionFamily.INSECT, CompanionMotion.HOVER, 0xFF7FBAC0, 0xFFCDE9E7, 0xFF594D73, CompanionTrait.ANTENNAE, CompanionTrait.FOUR_WINGS),
        entry("orchid_stick_insect", "Orchid Stick Insect", CompanionFamily.INSECT, CompanionMotion.CRAWL, 0xFF687D45, 0xFFA2B96D, 0xFFC4687A, CompanionTrait.ANTENNAE, CompanionTrait.LONG_LEGS, CompanionTrait.LONG_BODY),
        entry("ruby_mosquito", "Ruby Mosquito", CompanionFamily.INSECT, CompanionMotion.HOVER, 0xFF9B4C53, 0xFFD18B8E, 0xFFD7E4DB, CompanionTrait.ANTENNAE, CompanionTrait.WINGS, CompanionTrait.LONG_LEGS, CompanionTrait.LONG_SNOUT),
        entry("azure_lacewing", "Azure Lacewing", CompanionFamily.INSECT, CompanionMotion.HOVER, 0xFF4A9BAB, 0xFFAEE0D4, 0xFFE3C35C, CompanionTrait.ANTENNAE, CompanionTrait.FOUR_WINGS, CompanionTrait.LONG_BODY),
        entry("copper_termite", "Copper Termite", CompanionFamily.INSECT, CompanionMotion.SCUTTLE, 0xFF9A6845, 0xFFC79C70, 0xFF5A4437, CompanionTrait.ANTENNAE, CompanionTrait.SEGMENTS),
        entry("pond_newt", "Pond Newt", CompanionFamily.GARDEN, CompanionMotion.CRAWL, 0xFF557B69, 0xFF8EB19B, 0xFF67A3B3, CompanionTrait.SPOTS, CompanionTrait.LONG_BODY),
        entry("ruby_skink", "Ruby Skink", CompanionFamily.GARDEN, CompanionMotion.SCUTTLE, 0xFFB75C51, 0xFFE99A79, 0xFF684B45, CompanionTrait.STRIPES, CompanionTrait.LONG_BODY),
        entry("mushroom_slug", "Mushroom Slug", CompanionFamily.GARDEN, CompanionMotion.CRAWL, 0xFF8D7F5E, 0xFFB9A36F, 0xFFD56B62, CompanionTrait.ANTENNAE, CompanionTrait.LONG_BODY),
        entry("pebble_tortoise", "Pebble Tortoise", CompanionFamily.GARDEN, CompanionMotion.CRAWL, 0xFF7C8464, 0xFF4F654D, 0xFFC9A65B, CompanionTrait.SHELL, CompanionTrait.SPOTS),
        entry("golden_toad", "Golden Toad", CompanionFamily.GARDEN, CompanionMotion.HOP, 0xFFC6A747, 0xFFE2CC72, 0xFF6F6845, CompanionTrait.WEBBED_FEET, CompanionTrait.SPOTS),
        entry("bramble_iguana", "Bramble Iguana", CompanionFamily.GARDEN, CompanionMotion.CRAWL, 0xFF5F8758, 0xFF91B26D, 0xFFB86A58, CompanionTrait.SPIKES, CompanionTrait.LONG_BODY),
        entry("clover_worm", "Clover Worm", CompanionFamily.GARDEN, CompanionMotion.CRAWL, 0xFF78A85F, 0xFFAED07A, 0xFFE08A56, CompanionTrait.ANTENNAE, CompanionTrait.SEGMENTS, CompanionTrait.LONG_BODY),
        entry("moss_alligator", "Moss Alligator", CompanionFamily.GARDEN, CompanionMotion.CRAWL, 0xFF4F7356, 0xFF75986B, 0xFFE0BF62, CompanionTrait.LONG_SNOUT, CompanionTrait.SPIKES, CompanionTrait.LONG_BODY),
        entry("pearl_seal", "Pearl Seal", CompanionFamily.WATER, CompanionMotion.SWIM, 0xFFAAB7B9, 0xFFE9EFE9, 0xFF65757C, CompanionTrait.FINS),
        entry("arctic_walrus", "Arctic Walrus", CompanionFamily.WATER, CompanionMotion.SWIM, 0xFF8C7665, 0xFFD2B99F, 0xFFF2E6CE, CompanionTrait.FINS, CompanionTrait.TUSKS),
        entry("blue_lobster", "Blue Lobster", CompanionFamily.WATER, CompanionMotion.SCUTTLE, 0xFF3C78A2, 0xFF70AACA, 0xFFD6A653, CompanionTrait.PINCERS, CompanionTrait.SHELL),
        entry("reef_clownfish", "Reef Clownfish", CompanionFamily.WATER, CompanionMotion.SWIM, 0xFFF08B3E, 0xFFF4F0DA, 0xFF3B3C3C, CompanionTrait.FINS, CompanionTrait.STRIPES),
        entry("emerald_pufferfish", "Emerald Pufferfish", CompanionFamily.WATER, CompanionMotion.PULSE, 0xFF629F78, 0xFFA8C99A, 0xFFD8B653, CompanionTrait.FINS, CompanionTrait.SPIKES, CompanionTrait.SPOTS),
        entry("sapphire_manta", "Sapphire Manta", CompanionFamily.WATER, CompanionMotion.SWIM, 0xFF416E96, 0xFF7CA6BE, 0xFFE6D5A2, CompanionTrait.FINS, CompanionTrait.LONG_BODY),
        entry("pearl_narwhal", "Pearl Narwhal", CompanionFamily.WATER, CompanionMotion.SWIM, 0xFFB9CBD8, 0xFFEAF3F3, 0xFFA987C0, CompanionTrait.FINS, CompanionTrait.HORNS, CompanionTrait.LONG_BODY),
        entry("sunstar_starfish", "Sunstar Starfish", CompanionFamily.WATER, CompanionMotion.PULSE, 0xFFD9835D, 0xFFF0B082, 0xFFC44F63, CompanionTrait.TENTACLES, CompanionTrait.SPOTS),
    ).mapIndexed { index, definition ->
        definition.copy(price = if (index == 0) 0 else (index + 1) * 50)
    }

    private val byId = all.associateBy(CompanionDefinition::id)

    init {
        check(all.size == 100)
        check(byId.size == all.size)
        check(all.first().id == FIRST_COMPANION_ID && all.first().price == 0)
        check(all.drop(1).map(CompanionDefinition::price) == (100..5_000 step 50).toList())
    }

    fun find(id: String): CompanionDefinition? = byId[id]
}
