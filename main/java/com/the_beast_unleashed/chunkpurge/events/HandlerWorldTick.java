package com.the_beast_unleashed.chunkpurge.events;

import com.the_beast_unleashed.chunkpurge.ModChunkPurge;
import com.the_beast_unleashed.chunkpurge.operators.WorldChunkUnloader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HandlerWorldTick
{

	// Contains a list of dimension ID's that have ticked on the server during this session
	public List<Integer> dim = new ArrayList<Integer>();
	// Contains the current tick count for each dimension
	// The index of this list relates to index of dim List
	private List<Integer> count = new ArrayList<Integer>();
	// Contains a list of dimension ID's from the config that are to be ChunkPurged
	private List<String> configIDs = new ArrayList<String>();
	// Holds the current Dimension ID during each WorldTickEvent
	private int id;
	// Holds the index number to compare dim List to count List
	private int index;
	// Holds the current dimension tick to compare against the configured tick delay
	private int tick;
	
	@SubscribeEvent
	// WorldTickEvent throws an event every time EACH DIMENSION ticks. This means there are more
	// Tick Events the more dimensions are currently loaded
	public void onWorldTick(TickEvent.WorldTickEvent event)
	{
	// Why do anything if the mod is disabled?
	if(ModChunkPurge.config.enabled)
	{
		// Check which dimension is ticking
		id = event.world.provider.getDimensionId();
		// Has this dimension ticked before during this server session?
		if(dim.contains(id))
		// This is NOT the first tick of a new dimension
		{
			// Where in our Dimension-List is this dimension ID saved?
			index = dim.indexOf(id);
			// Our dimension ID is saved at Dimension-List index X,
			// which means the tick count for this dimension is saved at Count-List index X
			// Lets add 1 to the tick count of that dimension
			tick = count.get(index) + 1;
			// For some odd reason, Forge (at least in version 1.7.10-10.13.4.1490-1.7.10)
			// throws 2 tick events for every Dimension Tick
			// Since we don't want to confuse the player with this information, just multiply
			// their configured tick delay by 2 and see if the current Dimension needs to be purged
			if(tick >= ModChunkPurge.config.chunkUnloadDelay * 2)
			{
				// The dimension tick is higher or equal to the configured delay, reset the tick counter
				count.set(index, 0);
				// Is this dimension configured to unload? Lets get the dimlist as an array and check
				configIDs = Arrays.asList(ModChunkPurge.config.dimlist.split(","));
				// If this dimension is NOT to be unloaded, just don't do anything else
				// if it IS to be unloaded, run the WorldChunkUnloader Class and purge chunks
				if(configIDs.contains(String.valueOf(id)))
				{
					WorldChunkUnloader worldChunkUnloader = new WorldChunkUnloader(event.world);
					worldChunkUnloader.unloadChunks();
					return;
				}
			return;
			}else
			{
				// If the current tick is LOWER than the configured tick delay, set the count to the
				// new tick (adds 1 on each pass)
				count.set(index, tick);
				return;
			}
		}else
		{
			// This is the first time this dimension has ticked during this server session
			// add a new line at index X to dim List with the Dimension ID and a new line
			// at index X to count List with 1 recorded tick.
			dim.add(id);
			count.add(1);
			return;
		}
	}
	}

}
