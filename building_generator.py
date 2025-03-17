import random
import numpy as np
import nbt
from nbt import NBTFile, TAG_Compound, TAG_Int, TAG_List, TAG_String

def get_materials_for_biome(biome):
    """Returns a dictionary of materials based on biome type."""
    biome_materials = {
        "DESERT": {"wall": "minecraft:sandstone", "floor": "minecraft:smooth_sandstone", "roof": "minecraft:cut_sandstone", "fence": "minecraft:sandstone_wall"},
        "PLAINS": {"wall": "minecraft:stone_bricks", "floor": "minecraft:oak_planks", "roof": "minecraft:dark_oak_planks", "fence": "minecraft:cobblestone_wall"},
        "TAIGA": {"wall": "minecraft:spruce_log", "floor": "minecraft:spruce_planks", "roof": "minecraft:spruce_planks", "fence": "minecraft:spruce_fence"},
        "JUNGLE": {"wall": "minecraft:jungle_log", "floor": "minecraft:jungle_planks", "roof": "minecraft:jungle_planks", "fence": "minecraft:jungle_fence"},
        "SAVANNA": {"wall": "minecraft:acacia_log", "floor": "minecraft:acacia_planks", "roof": "minecraft:acacia_planks", "fence": "minecraft:acacia_fence"},
        "MOUNTAINS": {"wall": "minecraft:cobbled_deepslate", "floor": "minecraft:stone_bricks", "roof": "minecraft:polished_deepslate", "fence": "minecraft:stone_brick_wall"},
    }
    return biome_materials.get(biome.upper(), biome_materials["PLAINS"])

def create_tower_schematic(biome="PLAINS", radius=5, height=20, filename="tower.schem"):
    """Generates a biome-specific tower schematic with decorations and battlements."""
    materials = get_materials_for_biome(biome)
    tower = np.full((radius * 2 + 1, height, radius * 2 + 1), "minecraft:air")
    
    for y in range(height):
        for x in range(-radius, radius + 1):
            for z in range(-radius, radius + 1):
                if x**2 + z**2 <= radius**2:
                    if x**2 + z**2 >= (radius - 1)**2:
                        tower[x + radius, y, z + radius] = materials["wall"]
    
    floor_heights = []
    next_floor = 5 + random.randint(0, 3)
    while next_floor < height - 1:
        floor_heights.append(next_floor)
        for x in range(-radius, radius + 1):
            for z in range(-radius, radius + 1):
                if x**2 + z**2 < (radius - 1)**2:
                    tower[x + radius, next_floor, z + radius] = materials["floor"]
        next_floor += 5 + random.randint(0, 3)
    
    for y in range(height):
        tower[radius, y, radius - 1] = "minecraft:ladder[facing=north]"
    
    tower[radius, 0, radius] = "minecraft:oak_door[half=lower]"
    tower[radius, 1, radius] = "minecraft:oak_door[half=upper]"
    
    for y in range(3, height, 4):
        for dx in [-radius + 1, radius - 1]:
            tower[dx + radius, y, radius] = "minecraft:glass_pane"
        for dz in [-radius + 1, radius - 1]:
            tower[radius, y, dz + radius] = "minecraft:glass_pane"
    
    if floor_heights:
        tower[radius - 1, floor_heights[-1], radius - 1] = "minecraft:campfire"
        tower[radius + 1, floor_heights[-1], radius] = "minecraft:bed"
        tower[radius - 1, floor_heights[-1], radius] = "minecraft:chest"
    
    # Adding battlements at the top level
    for x in range(-radius, radius + 1):
        for z in range(-radius, radius + 1):
            if x**2 + z**2 >= (radius - 1)**2 and x**2 + z**2 <= radius**2:
                if (x + z) % 2 == 0:
                    tower[x + radius, height - 1, z + radius] = materials["fence"]
    
    schem_nbt = NBTFile()
    root = TAG_Compound()
    root["Version"] = TAG_Int(2)
    root["Materials"] = TAG_String("minecraft")
    root["Blocks"] = TAG_List()
    root["Palette"] = TAG_Compound()
    palette_map = {}
    block_data = []
    
    block_index = 0
    for y in range(height):
        for x in range(radius * 2 + 1):
            for z in range(radius * 2 + 1):
                block = tower[x, y, z]
                if block != "minecraft:air":
                    if block not in palette_map:
                        palette_map[block] = len(palette_map)
                    block_data.append(TAG_Int(palette_map[block]))
                    block_index += 1
    
    for block, index in palette_map.items():
        root["Palette"][block] = TAG_Int(index)
    
    root["BlockData"] = TAG_List(block_data)
    root["Size"] = TAG_List([TAG_Int(radius * 2 + 1), TAG_Int(height), TAG_Int(radius * 2 + 1)])
    schem_nbt.tags.append(root)
    
    schem_nbt.write_file(filename)
    print(f"Tower schematic saved as {filename}")
    
create_tower_schematic("PLAINS")
