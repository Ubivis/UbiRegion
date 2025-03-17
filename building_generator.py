import random
import numpy as np
import nbt
import argparse
from nbt import NBTFile, TAG_Compound, TAG_Int, TAG_List, TAG_String

def get_materials_for_biome(biome):
    """Returns a dictionary of materials based on biome type."""
    biome_materials = {
        "DESERT": {"wall": "minecraft:sandstone", "floor": "minecraft:smooth_sandstone", "roof": "minecraft:cut_sandstone", "fence": "minecraft:sandstone_wall", "banner": "minecraft:yellow_banner", "torch": "minecraft:soul_torch", "gate": "minecraft:oak_fence_gate"},
        "PLAINS": {"wall": "minecraft:stone_bricks", "floor": "minecraft:oak_planks", "roof": "minecraft:dark_oak_planks", "fence": "minecraft:cobblestone_wall", "banner": "minecraft:white_banner", "torch": "minecraft:torch", "gate": "minecraft:oak_fence_gate"},
        "TAIGA": {"wall": "minecraft:spruce_log", "floor": "minecraft:spruce_planks", "roof": "minecraft:spruce_planks", "fence": "minecraft:spruce_fence", "banner": "minecraft:blue_banner", "torch": "minecraft:torch", "gate": "minecraft:spruce_fence_gate"},
        "JUNGLE": {"wall": "minecraft:jungle_log", "floor": "minecraft:jungle_planks", "roof": "minecraft:jungle_planks", "fence": "minecraft:jungle_fence", "banner": "minecraft:green_banner", "torch": "minecraft:lantern", "gate": "minecraft:jungle_fence_gate"},
        "SAVANNA": {"wall": "minecraft:acacia_log", "floor": "minecraft:acacia_planks", "roof": "minecraft:acacia_planks", "fence": "minecraft:acacia_fence", "banner": "minecraft:orange_banner", "torch": "minecraft:torch", "gate": "minecraft:acacia_fence_gate"},
        "MOUNTAINS": {"wall": "minecraft:cobbled_deepslate", "floor": "minecraft:stone_bricks", "roof": "minecraft:polished_deepslate", "fence": "minecraft:stone_brick_wall", "banner": "minecraft:black_banner", "torch": "minecraft:lantern", "gate": "minecraft:dark_oak_fence_gate"},
    }
    return biome_materials.get(biome.upper(), biome_materials["PLAINS"])

def get_furnishings_for_room(room_type):
    """Returns a list of furniture items based on room type."""
    room_furnishings = {
        "bedroom": ["minecraft:bed", "minecraft:chest", "minecraft:carpet", "minecraft:painting"],
        "storage": ["minecraft:barrel", "minecraft:chest", "minecraft:barrel"],
        "dining_hall": ["minecraft:oak_table", "minecraft:oak_chair", "minecraft:cake", "minecraft:painting"],
        "library": ["minecraft:bookshelf", "minecraft:lectern", "minecraft:candle", "minecraft:painting"],
        "armory": ["minecraft:armor_stand", "minecraft:anvil", "minecraft:smithing_table"],
        "secret_room": ["minecraft:chest", "minecraft:redstone_torch", "minecraft:lever"],
    }
    return room_furnishings.get(room_type, [])


def create_structure_schematic(structure_type="TOWER", biome="PLAINS", size=10, floors=3, rooms=4):
    """Generates a biome-specific tower or castle schematic with decorations, banners, and lighting."""
    filename = f"{biome.lower()}_{structure_type.lower()}.schem"
    if structure_type == "TOWER":
        create_tower_schematic(biome, size // 2, floors * 4, filename)
    elif structure_type == "CASTLE":
        create_castle_schematic(biome, size, floors, rooms, filename)

def create_tower_schematic(biome, radius, height, filename):
    """Generates a biome-specific tower or castle schematic with decorations, banners, and lighting."""
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
    
    # Adding battlements, banners, and torches at the top level
    for x in range(-radius, radius + 1):
        for z in range(-radius, radius + 1):
            if x**2 + z**2 >= (radius - 1)**2 and x**2 + z**2 <= radius**2:
                if (x + z) % 2 == 0:
                    tower[x + radius, height - 1, z + radius] = materials["fence"]
                if (x + z) % 4 == 0:
                    tower[x + radius, height, z + radius] = materials["banner"]
                if (x + z) % 6 == 0:
                    tower[x + radius, height - 2, z + radius] = materials["torch"]
    
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
    pass

def create_castle_schematic(biome, size, floors, rooms, filename):
    materials = get_materials_for_biome(biome)
    height = floors * 4
    structure = np.full((size, height, size), "minecraft:air")
    room_types = ["bedroom", "storage", "dining_hall", "library", "armory"]
    has_secret_room = random.choice([True, False])
    
    # Generate walls
    for y in range(height):
        for x in range(size):
            for z in range(size):
                if x == 0 or x == size - 1 or z == 0 or z == size - 1:
                    structure[x, y, z] = materials["wall"]
    
    # Generate floors
    for floor in range(1, floors):
        y = floor * 4
        for x in range(1, size - 1):
            for z in range(1, size - 1):
                structure[x, y, z] = materials["floor"]

    # Assign rooms randomly per floor
    for floor in range(floors):
        assigned_rooms = random.sample(room_types, min(len(room_types), rooms))
        if has_secret_room and floor == floors - 1:
            assigned_rooms.append("secret_room")        
        for room in assigned_rooms:
            furnishings = get_furnishings_for_room(room)
            if "secret_room" in assigned_rooms:
                print("  - Secret room added with hidden lever and chest!")
    
    # Add doors and gates
    structure[size // 2, 0, 0] = materials["gate"]
    structure[size // 2, 1, 0] = "minecraft:air"
    
    # Add windows and torches
    for y in range(3, height, 4):
        for x in [1, size - 2]:
            structure[x, y, size // 2] = "minecraft:glass_pane"
        for z in [1, size - 2]:
            structure[size // 2, y, z] = "minecraft:glass_pane"
        structure[size // 2, y - 1, size // 2] = materials["torch"]
    
    # Add stairs connecting floors
    for floor in range(floors - 1):
        y = (floor + 1) * 4 - 1
        for i in range(3):
            structure[1 + i, y, 1] = "minecraft:oak_stairs[facing=east]"
    
    # Add battlements for castle
    if structure_type == "CASTLE":
        for x in range(size):
            for z in range(size):
                if x == 0 or x == size - 1 or z == 0 or z == size - 1:
                    if (x + z) % 2 == 0:
                        structure[x, height - 1, z] = materials["fence"]
    
    # Add banners
    structure[1, height - 2, 1] = materials["banner"]
    structure[size - 2, height - 2, size - 2] = materials["banner"]
    
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
        for x in range(size):
            for z in range(size):
                block = structure[x, y, z]
                if block != "minecraft:air":
                    if block not in palette_map:
                        palette_map[block] = len(palette_map)
                    block_data.append(TAG_Int(palette_map[block]))
                    block_index += 1
    
    for block, index in palette_map.items():
        root["Palette"][block] = TAG_Int(index)
    
    root["BlockData"] = TAG_List(block_data)
    root["Size"] = TAG_List([TAG_Int(size), TAG_Int(height), TAG_Int(size)])
    schem_nbt.tags.append(root)
    
    schem_nbt.write_file(filename)
    print(f"Castle schematic saved as {filename}")
    pass
    
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate a Minecraft schematic for a Tower or Castle.")
    parser.add_argument("structure_type", choices=["TOWER", "CASTLE"], help="Type of structure to generate")
    parser.add_argument("biome", choices=list(get_materials_for_biome().keys()), help="Biome in which the structure is placed")
    parser.add_argument("size", type=int, help="Size of the structure (radius for towers, width for castles)")
    parser.add_argument("floors", type=int, help="Number of floors in the structure")
    parser.add_argument("rooms", type=int, nargs='?', default=4, help="Number of rooms per floor (only for castles)")
    
    args = parser.parse_args()
    create_structure_schematic(args.structure_type, args.biome, args.size, args.floors, args.rooms)
