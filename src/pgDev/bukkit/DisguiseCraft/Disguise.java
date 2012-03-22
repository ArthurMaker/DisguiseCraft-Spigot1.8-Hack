package pgDev.bukkit.DisguiseCraft;

import java.lang.reflect.Field;
import java.util.LinkedList;

import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.DataWatcher;
import net.minecraft.server.MathHelper;
import net.minecraft.server.Packet18ArmAnimation;
import net.minecraft.server.Packet201PlayerInfo;
import net.minecraft.server.Packet20NamedEntitySpawn;
import net.minecraft.server.Packet24MobSpawn;
import net.minecraft.server.Packet29DestroyEntity;
import net.minecraft.server.Packet32EntityLook;
import net.minecraft.server.Packet33RelEntityMoveLook;
import net.minecraft.server.Packet34EntityTeleport;
import net.minecraft.server.Packet35EntityHeadRotation;
import net.minecraft.server.Packet40EntityMetadata;
import net.minecraft.server.Packet5EntityEquipment;

/**
 * This is the class for every disguise object. It contains
 * the functions for creating, editing, and sending disguises.
 * @author PG Dev Team (Devil Boy, Tux2)
 */
public class Disguise {
	/**
	 * This is the list of possible mob disguises listed by
	 * their Bukkit class name.
	 * @author PG Dev Team (Devil Boy)
	 */
	public enum MobType {
		Blaze(61),
		CaveSpider(59),
		Chicken(93),
		Cow(92),
		Creeper(50),
		EnderDragon(63),
		Enderman(58),
		Ghast(56),
		Giant(53),
		IronGolem(99),
		MagmaCube(62),
		MushroomCow(96),
		Ocelot(98),
		Pig(90),
		PigZombie(57),
		Sheep(91),
		Silverfish(60),
		Skeleton(51),
		Slime(55),
		Snowman(97),
		Spider(52),
		Squid(94),
		Villager(120),
		Wolf(95),
		Zombie(54);
		
		/**
		 * The entity-type ID.
		 */
		public final byte id;
		MobType(int i) {
			id = (byte) i;
		}
		
		/**
		 * Check if the mob type is a subclass of an Entity class from Bukkit.
		 * This is extremely useful to seeing if a mob can have a certain
		 * subtype. For example: only members of the Animal class (and villagers)
		 * can have a baby form.
		 * @param cls The class to compare to
		 * @return true if the mobtype is a subclass, false otherwise
		 */
		public boolean isSubclass(Class<?> cls) {
			try {
				return cls.isAssignableFrom(Class.forName("org.bukkit.entity." + name()));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return false;
		}
		
		/**
		 * Get the MobType from its name
		 * Works like valueOf, but not case sensitive
		 * @param text The string to match with a MobType
		 * @return The MobType with the given name (null if none are found)
		 */
		public static MobType fromString(String text) {
			for (MobType m : MobType.values()) {
				if (text.equalsIgnoreCase(m.name())) {
					return m;
				}
			}
			return null;
		}
		
		/**
		 * Just a string containing the possible subtypes. This is mainly
		 * used for plugin help output.
		 */
		public static String subTypes = "player, baby, black, blue, brown, cyan, " +
			"gray, green, lightblue, lime, magenta, orange, pink, purple, red, " +
			"silver, white, yellow, sheared, charged, tiny, small, big, tamed, aggressive, " +
			"tabby, tuxedo, siamese, burning, saddled";
	}
	
	// Individual disguise stuff
	/**
	 * The entity ID that this disguise uses in its packets.
	 */
	public int entityID;
	/**
	 * The metadata contained in this disguise.
	 */
	public LinkedList<String> data; // $ means invisible player
	/**
	 * The type of mob this disguise is. (null if not a mob)
	 */
	public MobType mob; // null if player
	DataWatcher metadata;
	/*
	private double lastVectorX;
	private double lastVectorY;
	private double lastVectorZ;
	
	private double lastposX;
	private double lastposY;
	private double lastposZ;*/
	
	private int encposX;
	private int encposY;
	private int encposZ;
	
	private boolean firstpos = true;
	
	/**
	 * Constructs a new Disguise object
	 * @param entityID The entity ID of the disguise
	 * @param data The metadata of the disguise (if a player, the name goes here) (null if there is no special data)
	 * @param mob The type of mob the disguise is (null if player)
	 */
	public Disguise(int entityID, LinkedList<String> data, MobType mob) {
		this.entityID = entityID;
		this.data = data;
		this.mob = mob;
		
		initializeData();
		handleData();
	}
	
	/**
	 * Constructs a new Disguise object with a single data value
	 * @param entityID The entity ID of the disguise
	 * @param data The metadata of the disguise (if a player, the name goes here) (null if there is no special data)
	 * @param mob The type of mob the disguise is (null if player)
	 */
	public Disguise(int entityID, String data, MobType mob) {
		this.entityID = entityID;
		LinkedList<String> dt = new LinkedList<String>();
		dt.addFirst(data);
		this.data = dt;
		this.mob = mob;
		
		initializeData();
		handleData();
	}
	
	/**
	 * Constructs a new Disguise object with null data
	 * @param entityID The entity ID of the disguise
	 * @param mob The type of mob the disguise is (null if player)
	 */
	public Disguise(int entityID, MobType mob) {
		this.entityID = entityID;
		this.data = null;
		this.mob = mob;
		
		initializeData();
	}
	
	/**
	 * Set the entity ID
	 * @param entityID The ID to set
	 * @return The new Disguise object (for chaining)
	 */
	public Disguise setEntityID(int entityID) {
		this.entityID = entityID;
		return this;
	}
	
	/**
	 * Set the metadata
	 * @param data The metadata to set
	 * @return The new Disguise object (for chaining)
	 */
	public Disguise setData(LinkedList<String> data) {
		this.data = data;
		initializeData();
		handleData();
		return this;
	}
	
	/**
	 * Sets the metadata to a single value (Likely a player name)
	 * @param data The metadata to set
	 * @return The new Disguise object (for chaining)
	 */
	public Disguise setSingleData(String data) {
		if (this.data == null) {
			this.data = new LinkedList<String>();
		}
		this.data.clear();
		this.data.addFirst(data);
		metadata = new DataWatcher();
		initializeData();
		handleData();
		return this;
	}
	
	/**
	 * Adds a single metadata string
	 * @param data The metadata to add
	 * @return The new Disguise object (for chaining)
	 */
	public Disguise addSingleData(String data) {
		if (this.data == null) {
			this.data = new LinkedList<String>();
		}
		if (!this.data.contains(data)) {
			this.data.add(data);
		}
		initializeData();
		handleData();
		return this;
	}
	
	/**
	 * Set the mob type
	 * @param mob
	 * @return The new Disguise object (for chaining)
	 */
	public Disguise setMob(MobType mob) {
		this.mob = mob;
		return this;
	}
	
	public void initializeData() {
		metadata = new DataWatcher();
		metadata.a(0, (byte) 0);
		metadata.a(12, 0);
		if (mob == MobType.Sheep || mob == MobType.Pig || mob == MobType.Ghast || mob == MobType.Enderman) {
			metadata.a(16, (byte) 0);
		} else if (mob == MobType.Slime || mob == MobType.MagmaCube) {
			metadata.a(16, (byte) 3);
		}
		
		if (mob == MobType.Creeper || mob == MobType.Enderman) {
			metadata.a(17, (byte) 0);
		}
		if (mob == MobType.Ocelot) {
			metadata.a(18, (byte) 0);
		}
	}
	
	public void handleData() {
		if (mob != null) {
			if (data != null) {
				if (data.contains("baby")) {
					metadata.watch(12, -23999);
				} else {
					metadata.watch(12, 0);
				}
				
				if (data.contains("black")) {
					metadata.watch(16, (byte) 15);
				} else if (data.contains("blue")) {
					metadata.watch(16, (byte) 11);
				} else if (data.contains("brown")) {
					metadata.watch(16, (byte) 12);
				} else if (data.contains("cyan")) {
					metadata.watch(16, (byte) 9);
				} else if (data.contains("gray")) {
					metadata.watch(16, (byte) 7);
				} else if (data.contains("green")) {
					metadata.watch(16, (byte) 13);
				} else if (data.contains("lightblue")) {
					metadata.watch(16, (byte) 3);
				} else if (data.contains("lime")) {
					metadata.watch(16, (byte) 5);
				} else if (data.contains("magenta")) {
					metadata.watch(16, (byte) 2);
				} else if (data.contains("orange")) {
					metadata.watch(16, (byte) 1);
				} else if (data.contains("pink")) {
					metadata.watch(16, (byte) 6);
				} else if (data.contains("purple")) {
					metadata.watch(16, (byte) 10);
				} else if (data.contains("red")) {
					metadata.watch(16, (byte) 14);
				} else if (data.contains("silver")) {
					metadata.watch(16, (byte) 8);
				} else if (data.contains("white")) {
					metadata.watch(16, (byte) 0);
				} else if (data.contains("yellow")) {
					metadata.watch(16, (byte) 4);
				} else if (data.contains("sheared")) {
					metadata.watch(16, (byte) 16);
				}
				
				if (data.contains("charged")) {
					metadata.watch(17, (byte) 1);
				}
				
				if (data.contains("tiny")) {
					metadata.watch(16, (byte) 1);
				} else if (data.contains("small")) {
					metadata.watch(16, (byte) 2);
				} else if (data.contains("big")) {
					metadata.watch(16, (byte) 4);
				}
				
				if (data.contains("sitting")) {
					try {
						metadata.a(16, (byte) 1);
					} catch (IllegalArgumentException e) {
						metadata.watch(16, (byte) 1);
					}
				} else if (data.contains("aggressive")) {
					if (mob == MobType.Wolf) {
						try {
							metadata.a(16, (byte) 2);
						} catch (IllegalArgumentException e) {
							metadata.watch(16, (byte) 2);
						}
					} else if (mob == MobType.Ghast) {
						metadata.watch(16, (byte) 1);
					} else if (mob == MobType.Enderman) {
						metadata.watch(17, (byte) 1);
					}
				} else if (data.contains("tamed")) {
					try {
						metadata.a(16, (byte) 4);
					} catch (IllegalArgumentException e) {
						metadata.watch(16, (byte) 4);
					}
				}
				
				if (data.contains("tabby")) {
					metadata.watch(18, (byte) 2);
				} else if (data.contains("tuxedo")) {
					metadata.watch(18, (byte) 1);
				} else if (data.contains("siamese")) {
					metadata.watch(18, (byte) 3);
				}
				
				if (data.contains("saddled")) {
					metadata.watch(16, (byte) 1);
				}
				
				if (data.contains("crouched")) {
					metadata.watch(0, (byte) 2);
				} else if (data.contains("sprinting")) {
					metadata.watch(0, (byte) 8);
				} else if (data.contains("burning")) {
					metadata.watch(0, (byte) 1);
				}
				
				Byte held = getHolding();
				if (held != null) {
					metadata.watch(16, held.byteValue());
				}
			}
		}
	}
	
	/**
	 * Clone the Disguise object
	 * @return A clone of this Disguise object
	 */
	public Disguise clone() {
		return new Disguise(entityID, data, mob);
	}
	
	/**
	 * See if the disguises match
	 * @param other The disguise to compare with
	 * @return
	 */
	public boolean equals(Disguise other) {
		return (entityID == other.entityID && data.equals(other.data) && mob == other.mob);
	}
	
	/**
	 * Check if the disguise is of another player
	 * @return true if it is a player disguise, false otherwise
	 */
	public boolean isPlayer() {
		return (mob == null && !data.equals("$"));
	}
	
	/**
	 * Get the color of the disguise
	 * @return The disguise color (null if no color)
	 */
	public String getColor() {
		String[] colors = {"black", "blue", "brown", "cyan", "gray", "green",
			"lightblue", "lime", "magenta", "orange", "pink", "purple", "red",
			"silver", "white", "yellow", "sheared"};
		if (data != null) {
			for (String color : colors) {
				if (data.contains(color)) {
					return color;
				}
			}
		}
		return null;
	}
	
	/**
	 * Get the size of the disguise
	 * @return The disguise size (null if no special size)
	 */
	public String getSize() {
		String[] sizes = {"tiny", "small", "big"};
		if (data != null) {
			for (String size : sizes) {
				if (data.contains(size)) {
					return size;
				}
			}
		}
		return null;
	}
	
	/**
	 * Set whether or not the disguise is crouching
	 * @param crouched True to make it crouch, False for standing
	 */
	public void setCrouch(boolean crouched) {
		if (crouched) {
			if (!data.contains("crouched")) {
				data.add("crouched");
			}
			metadata.watch(0, (byte) 2);
		} else {
			if (data.contains("crouched")) {
				data.remove("crouched");
			}
			metadata.watch(0, (byte) 0);
		}
	}
	
	/**
	 * Gets the block ID this disguise is holding (according to the metadata)
	 * @return The block ID of the held block (null if not holding anything)
	 */
	public Byte getHolding() {
		if (data != null) {
			for (String one : data) {
				if (one.startsWith("holding")) {
					String[] parts = one.split(":");
					try {
						return Byte.valueOf(parts[1]);
					} catch (NumberFormatException e) {
						System.out.println("DisguiseCraft could not parse the byte of an Enderman holding block!");
					}
				}
			}
		}
		return null;
	}
	
	// Packet creation methods
	public Packet24MobSpawn getMobSpawnPacket(Location loc) {
		if (mob != null) {
			int x = MathHelper.floor(loc.getX() *32D);
			int y = MathHelper.floor(loc.getY() *32D);
			int z = MathHelper.floor(loc.getZ() *32D);
			if(firstpos) {
				encposX = x;
				encposY = y;
				encposZ = z;
				firstpos = false;
			}
			Packet24MobSpawn packet = new Packet24MobSpawn();
			packet.a = entityID;
			packet.b = mob.id;
			packet.c = (int) x;
			packet.d = (int) y;
			packet.e = (int) z;
			packet.f = DisguiseCraft.degreeToByte(loc.getYaw());
			packet.g = DisguiseCraft.degreeToByte(loc.getPitch());
			packet.h = packet.f;
			try {
				Field metadataField = packet.getClass().getDeclaredField("i");
				metadataField.setAccessible(true);
				metadataField.set(packet, metadata);
			} catch (Exception e) {
				System.out.println("DisguiseCraft was unable to set the metadata for a " + mob.name() +  " disguise!");
				e.printStackTrace();
			}
			
			// Ender Dragon fix
			if (mob == MobType.EnderDragon) {
				packet.f = (byte) (packet.f - 128);
			}
			// Chicken fix
			if (mob == MobType.Chicken) {
				packet.g = (byte) (packet.g * -1);
			}
			return packet;
		} else {
			return null;
		}
	}
	
	public Packet20NamedEntitySpawn getPlayerSpawnPacket(Location loc, short item) {
		if (mob == null && !data.equals("$")) {
			Packet20NamedEntitySpawn packet = new Packet20NamedEntitySpawn();
	        packet.a = entityID;
	        packet.b = data.getFirst();
	        int x = MathHelper.floor(loc.getX() *32D);
			int y = MathHelper.floor(loc.getY() *32D);
			int z = MathHelper.floor(loc.getZ() *32D);
			if(firstpos) {
				encposX = x;
				encposY = y;
				encposZ = z;
				firstpos = false;
			}
	        packet.c = (int) x;
	        packet.d = (int) y;
	        packet.e = (int) z;
	        packet.f = DisguiseCraft.degreeToByte(loc.getYaw());
	        packet.g = DisguiseCraft.degreeToByte(loc.getPitch());
	        packet.h = item;
	        return packet;
		} else {
			return null;
		}
	}
	
	public Packet29DestroyEntity getEntityDestroyPacket() {
		return new Packet29DestroyEntity(entityID);
	}
	
	public Packet5EntityEquipment getEquipmentChangePacket(short slot, ItemStack item) {
		if (isPlayer()) {
			Packet5EntityEquipment packet;
			if (item == null) {
				packet = new Packet5EntityEquipment();
				packet.a = entityID;
				packet.b = slot;
				packet.c = -1;
				packet.d = 0;
			} else {
				packet = new Packet5EntityEquipment(entityID, slot, ((CraftItemStack) item).getHandle());
			}
			return packet;
		} else {
			return null;
		}
	}
	
	public Packet32EntityLook getEntityLookPacket(Location loc) {
		Packet32EntityLook packet = new Packet32EntityLook();
		packet.a = entityID;
		packet.b = 0;
		packet.c = 0;
		packet.d = 0;
		packet.e = DisguiseCraft.degreeToByte(loc.getYaw());
		packet.f = DisguiseCraft.degreeToByte(loc.getPitch());
		
		// EnderDragon specific
		if (mob == MobType.EnderDragon) {
			packet.e = (byte) (packet.e - 128);
		}
		// Chicken fix
		if (mob == MobType.Chicken) {
			packet.f = (byte) (packet.f * -1);
		}
		return packet;
	}
	
	public Packet33RelEntityMoveLook getEntityMoveLookPacket(Location look) {
		Packet33RelEntityMoveLook packet = new Packet33RelEntityMoveLook();
		packet.a = entityID;
		MovementValues movement = getMovement(look);
		encposX += movement.x;
		encposY += movement.y;
		encposZ += movement.z;
		packet.b = (byte) movement.x;
		packet.c = (byte) movement.y;
		packet.d = (byte) movement.z;
		packet.e = DisguiseCraft.degreeToByte(look.getYaw());
		packet.f = DisguiseCraft.degreeToByte(look.getPitch());
		
		// EnderDragon specific
		if (mob == MobType.EnderDragon) {
			packet.e = (byte) (packet.e - 128);
		}
		// Chicken fix
		if (mob == MobType.Chicken) {
			packet.f = (byte) (packet.f * -1);
		}
		return packet;
	}
	
	public Packet34EntityTeleport getEntityTeleportPacket(Location loc) {
		Packet34EntityTeleport packet = new Packet34EntityTeleport();
		packet.a = entityID;
		int x = (int) MathHelper.floor(32D * loc.getX());
		int y = (int) MathHelper.floor(32D * loc.getY());
		int z = (int) MathHelper.floor(32D * loc.getZ());
		packet.b = x;
		packet.c = y;
		packet.d = z;
		encposX = x;
		encposY = y;
		encposZ = z;
		packet.e = DisguiseCraft.degreeToByte(loc.getYaw());
		packet.f = DisguiseCraft.degreeToByte(loc.getPitch());
		
		// EnderDragon specific
		if (mob == MobType.EnderDragon) {
			packet.e = (byte) (packet.e - 128);
		}
		// Chicken fix
		if (mob == MobType.Chicken) {
			packet.f = (byte) (packet.f * -1);
		}
		return packet;
	}
	
	public Packet40EntityMetadata getEntityMetadataPacket() {
		Packet40EntityMetadata packet = new Packet40EntityMetadata();
		packet.a = entityID;
		try {
			Field metadataField = packet.getClass().getDeclaredField("b");
			metadataField.setAccessible(true);
			metadataField.set(packet, metadata);
		} catch (Exception e) {
			System.out.println("DisguiseCraft was unable to set the metadata for a " + mob.name() +  " disguise!");
			e.printStackTrace();
		}
		return packet;
	}
	
	public Packet201PlayerInfo getPlayerInfoPacket(Player player, boolean show) {
		Packet201PlayerInfo packet = null;
		if (isPlayer()) {
			int ping;
			if (show) {
				ping = ((CraftPlayer) player).getHandle().ping;
			} else {
				ping = 9999;
			}
			packet = new Packet201PlayerInfo(data.getFirst(), show, ping);
		}
		return packet;
	}
	
	public MovementValues getMovement(Location to) {
		int x = MathHelper.floor(to.getX() *32D);
		int y = MathHelper.floor(to.getY() *32D);
		int z = MathHelper.floor(to.getZ() *32D);
		int diffx = x - encposX;
		int diffy = y - encposY;
		int diffz = z - encposZ;
		return new MovementValues(diffx, diffy, diffz, DisguiseCraft.degreeToByte(to.getYaw()), DisguiseCraft.degreeToByte(to.getPitch()));
	}
	
	public Packet35EntityHeadRotation getHeadRotatePacket(Location loc) {
		return new Packet35EntityHeadRotation(entityID, DisguiseCraft.degreeToByte(loc.getYaw()));
	}
	
	public Packet18ArmAnimation getAnimationPacket(PlayerAnimationType animation) {
		Packet18ArmAnimation packet = new Packet18ArmAnimation();
		packet.a = entityID;
		if (animation == PlayerAnimationType.ARM_SWING) {
			packet.b = 1;
		}
		return packet;
	}
	
	public Packet40EntityMetadata getMetadataPacket() {
		return new Packet40EntityMetadata(entityID, metadata);
	}
}
