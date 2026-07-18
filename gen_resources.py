#!/usr/bin/env python3
"""Generates all repetitive resource JSON for AkumiYuukii Mods ores + mobs.

Run from the project root:  python gen_resources.py
Idempotent: safe to re-run. Does NOT overwrite texture PNGs (only creates folders + READMEs).
"""
import json, os

MOD = "akumiyuukiimods"
ROOT = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", MOD)
DATA = os.path.join(ROOT, "src", "main", "resources", "data", MOD)

# (ore_id, drop_id, min_xp, max_xp, deepslate_tint)
ORES = [
    ("tin_ore", "tin_ingot", 0, 0, False),
    ("silver_ore", "silver_ingot", 0, 0, False),
    ("ruby_ore", "ruby", 3, 7, False),
    ("sapphire_ore", "sapphire", 3, 7, False),
    ("topaz_ore", "topaz", 3, 7, False),
    ("onyx_ore", "onyx", 3, 7, False),
    ("jade_ore", "jade", 2, 5, False),
    ("cobalt_ore", "cobalt_ingot", 0, 0, False),
    ("platinum_ore", "platinum_ingot", 0, 0, False),
    ("bismuth_ore", "bismuth_ingot", 1, 3, False),
    ("rare_endgame_ore", "rare_endgame_gem", 8, 14, True),
]

# Human-readable names for lang.
ORE_NAMES = {
    "tin_ore": "Tin Ore", "silver_ore": "Silver Ore", "ruby_ore": "Ruby Ore",
    "sapphire_ore": "Sapphire Ore", "topaz_ore": "Topaz Ore", "onyx_ore": "Onyx Ore",
    "jade_ore": "Jade Ore", "cobalt_ore": "Cobalt Ore", "platinum_ore": "Platinum Ore",
    "bismuth_ore": "Bismuth Ore", "rare_endgame_ore": "Rare Endgame Ore",
}
DROP_NAMES = {
    "tin_ingot": "Tin Ingot", "silver_ingot": "Silver Ingot", "ruby": "Ruby",
    "sapphire": "Sapphire", "topaz": "Topaz", "onyx": "Onyx", "jade": "Jade",
    "cobalt_ingot": "Cobalt Ingot", "platinum_ingot": "Platinum Ingot",
    "bismuth_ingot": "Bismuth Ingot", "rare_endgame_gem": "Rare Endgame Gem",
}

# Ore worldgen placement: (vein_size, count_per_chunk, min_y, max_y)
ORE_GEN = {
    "tin_ore": (9, 12, -24, 112),
    "silver_ore": (7, 8, -40, 64),
    "ruby_ore": (5, 4, -48, 32),
    "sapphire_ore": (5, 4, -48, 32),
    "topaz_ore": (5, 5, -32, 48),
    "onyx_ore": (6, 4, -56, 16),
    "jade_ore": (6, 6, -24, 80),
    "cobalt_ore": (5, 4, -56, 24),
    "platinum_ore": (4, 3, -60, 0),
    "bismuth_ore": (6, 5, -40, 48),
    "rare_endgame_ore": (3, 2, -64, -16),
}

NAMED_MOBS = [
    ("astralite", "Astralite"), ("eternium", "Eternium"), ("chronite", "Chronite"),
    ("titanite", "Titanite"), ("draconium", "Draconium"),
]
ORE_MOBS = [
    ("tin_guardian", "Tin Guardian"), ("silver_guardian", "Silver Guardian"),
    ("ruby_guardian", "Ruby Guardian"), ("sapphire_guardian", "Sapphire Guardian"),
    ("topaz_guardian", "Topaz Guardian"), ("onyx_guardian", "Onyx Guardian"),
    ("jade_guardian", "Jade Guardian"), ("cobalt_guardian", "Cobalt Guardian"),
    ("platinum_guardian", "Platinum Guardian"), ("bismuth_guardian", "Bismuth Guardian"),
]
ALL_MOBS = NAMED_MOBS + ORE_MOBS


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        if isinstance(obj, str):
            f.write(obj)
        else:
            json.dump(obj, f, indent=2, ensure_ascii=False)
    print("  wrote", os.path.relpath(path, ROOT))


def readme(path, text):
    os.makedirs(path, exist_ok=True)
    p = os.path.join(path, "PUT_TEXTURES_HERE.txt")
    if not os.path.exists(p):
        with open(p, "w", encoding="utf-8") as f:
            f.write(text)
        print("  folder", os.path.relpath(path, ROOT))


# ---------- Blockstates + block/item models ----------
def gen_blocks():
    for ore_id, drop_id, *_ in ORES:
        # blockstate: single variant pointing at the block model
        w(os.path.join(ASSETS, "blockstates", ore_id + ".json"),
          {"variants": {"": {"model": f"{MOD}:block/{ore_id}"}}})
        # block model: cube_all using the block texture
        w(os.path.join(ASSETS, "models", "block", ore_id + ".json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{ore_id}"}})
        # item model for the block item
        w(os.path.join(ASSETS, "models", "item", ore_id + ".json"),
          {"parent": f"{MOD}:block/{ore_id}"})

    # drop item models (generated flat item)
    seen = set()
    for _, drop_id, *_ in ORES:
        if drop_id in seen:
            continue
        seen.add(drop_id)
        w(os.path.join(ASSETS, "models", "item", drop_id + ".json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{drop_id}"}})

    # spawn egg item models (use vanilla template so they always render, tint from Java)
    for mob_id, _ in ALL_MOBS:
        w(os.path.join(ASSETS, "models", "item", mob_id + "_spawn_egg.json"),
          {"parent": "minecraft:item/template_spawn_egg"})


# ---------- Loot tables ----------
def gen_loot():
    for ore_id, drop_id, min_xp, max_xp, tint in ORES:
        # Gems (ruby/sapphire/etc.) use fortune-affected ore drop; ingot-ores drop raw 1:1.
        table = {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{
                    "type": "minecraft:item",
                    "name": f"{MOD}:{drop_id}",
                    "functions": [
                        {"function": "minecraft:apply_bonus",
                         "enchantment": "minecraft:fortune",
                         "formula": "minecraft:ore_drops"},
                        {"function": "minecraft:explosion_decay"}
                    ]
                }],
                "conditions": [{"condition": "minecraft:survives_explosion"}]
            }]
        }
        w(os.path.join(DATA, "loot_tables", "blocks", ore_id + ".json"), table)

    # mob loot: each ore-mob drops its ore's gem/ingot sometimes; named mobs drop endgame gem.
    for mob_id, _ in ORE_MOBS:
        drop = mob_id.replace("_guardian", "")
        # map guardian -> that ore's drop item
        mapping = {o.replace("_ore", ""): d for o, d, *_ in ORES}
        drop_item = mapping.get(drop, "tin_ingot")
        w(os.path.join(DATA, "loot_tables", "entities", mob_id + ".json"),
          {"type": "minecraft:entity", "pools": [{
              "rolls": 1,
              "entries": [{"type": "minecraft:item", "name": f"{MOD}:{drop_item}",
                           "functions": [{"function": "minecraft:set_count",
                                          "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}]}]
          }]})
    for mob_id, _ in NAMED_MOBS:
        w(os.path.join(DATA, "loot_tables", "entities", mob_id + ".json"),
          {"type": "minecraft:entity", "pools": [{
              "rolls": 1,
              "entries": [{"type": "minecraft:item", "name": f"{MOD}:rare_endgame_gem",
                           "functions": [{"function": "minecraft:set_count",
                                          "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]}]
          }]})


# ---------- Worldgen (configured + placed features) ----------
def gen_worldgen():
    for ore_id, _, _, _, tint in ORES:
        size, count, min_y, max_y = ORE_GEN[ore_id]
        # configured feature: replace stone (+ deepslate) with our ore
        cfg = {
            "type": "minecraft:ore",
            "config": {
                "size": size,
                "discard_chance_on_air_exposure": 0.0,
                "targets": [
                    {"target": {"predicate_type": "minecraft:tag_match",
                                "tag": "minecraft:stone_ore_replaceables"},
                     "state": {"Name": f"{MOD}:{ore_id}"}},
                    {"target": {"predicate_type": "minecraft:tag_match",
                                "tag": "minecraft:deepslate_ore_replaceables"},
                     "state": {"Name": f"{MOD}:{ore_id}"}}
                ]
            }
        }
        w(os.path.join(DATA, "worldgen", "configured_feature", ore_id + ".json"), cfg)
        # placed feature: count + spread + height range + biome (handled by biome modification)
        placed = {
            "feature": f"{MOD}:{ore_id}",
            "placement": [
                {"type": "minecraft:count", "count": count},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range",
                 "height": {"type": "minecraft:uniform",
                            "min_inclusive": {"absolute": min_y},
                            "max_inclusive": {"absolute": max_y}}},
                {"type": "minecraft:biome"}
            ]
        }
        w(os.path.join(DATA, "worldgen", "placed_feature", ore_id + ".json"), placed)


# ---------- Textures folders ----------
def gen_texture_folders():
    readme(os.path.join(ASSETS, "textures", "block"),
           "Bo texture cac quang (16x16 PNG) vao day. Ten file khop ore id:\n" +
           "\n".join(o + ".png" for o, *_ in ORES) + "\n")
    seen = []
    for _, d, *_ in ORES:
        if d not in seen:
            seen.append(d)
    readme(os.path.join(ASSETS, "textures", "item"),
           "Bo texture item roi ra (16x16 PNG) vao day. Ten file khop drop id:\n" +
           "\n".join(d + ".png" for d in seen) + "\n")
    readme(os.path.join(ASSETS, "textures", "entity"),
           "Bo texture quai (kieu skin nguoi 64x64 PNG) vao day. Ten file khop mob id:\n" +
           "\n".join(m + ".png" for m, _ in ALL_MOBS) + "\n")


# ---------- Lang ----------
def gen_lang():
    entries = {}
    for ore_id, *_ in ORES:
        entries[f"block.{MOD}.{ore_id}"] = ORE_NAMES[ore_id]
    for drop_id, name in DROP_NAMES.items():
        entries[f"item.{MOD}.{drop_id}"] = name
    for mob_id, name in ALL_MOBS:
        entries[f"entity.{MOD}.{mob_id}"] = name
        entries[f"item.{MOD}.{mob_id}_spawn_egg"] = name + " Spawn Egg"
    entries["itemGroup.akumiyuukiimods.ores"] = "AkumiYuukii - Quang & Vat pham"
    entries["itemGroup.akumiyuukiimods.spawn_eggs"] = "AkumiYuukii - Trung quai"
    # merge into existing en_us.json
    path = os.path.join(ASSETS, "lang", "en_us.json")
    existing = {}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            existing = json.load(f)
    existing.update(entries)
    w(path, existing)


# ---------- Block/tool tags (so ores are pickaxe-mineable and drop) ----------
def gen_tags():
    ore_blocks = [f"{MOD}:{ore_id}" for ore_id, *_ in ORES]
    # mineable with pickaxe
    w(os.path.join(DATA, "..", "minecraft", "tags", "blocks", "mineable", "pickaxe.json"),
      {"replace": False, "values": ore_blocks})
    # require at least iron tool for most; endgame needs diamond
    iron = [b for b in ore_blocks if not b.endswith("rare_endgame_ore")]
    diamond = [f"{MOD}:rare_endgame_ore"]
    w(os.path.join(DATA, "..", "minecraft", "tags", "blocks", "needs_iron_tool.json"),
      {"replace": False, "values": iron})
    w(os.path.join(DATA, "..", "minecraft", "tags", "blocks", "needs_diamond_tool.json"),
      {"replace": False, "values": diamond})


def main():
    print("Generating resources...")
    gen_blocks()
    gen_loot()
    gen_worldgen()
    gen_tags()
    gen_texture_folders()
    gen_lang()
    print("Done.")


if __name__ == "__main__":
    main()
