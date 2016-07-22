package com.the_beast_unleashed.chunkpurge.operators;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.the_beast_unleashed.chunkpurge.ModChunkPurge;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import org.apache.logging.log4j.Level;

/*
 * A class to handle the unloading of excess chunks from a WorldServer.
 * Excess loaded chunks are those that are currently not within a player's view distance,
 * forced by a chunk loader, or loaded by the world's spawn area.
 */
public class WorldChunkUnloader
{
	
	private World world;
	private HashSet<ChunkCoordIntPair> chunksToUnload;
	private long initialTime;
	
	public WorldChunkUnloader (World world)
	{
		
		this.world = world;
		
	}
	
	
	/*
	 * A flood fill algorithm to find the shape of the loaded chunks surrounding a player-occupied chunk, or seed.
	 * Will not return chunks that are further than radiusLimit from the seed. Set radiusLimit to 0 in order to
	 * ignore any limit.
	 * 
	 * 1. Set Q to the empty queue.
	 * 2. If the color of node is not equal to target-color, return.
	 * 3. Add node to Q.
	 * 4. For each element N of Q:
	 * 5.     If the color of N is equal to target-color:
	 * 6.         Set w and e equal to N.
	 * 7.         Move w to the west until the color of the node to the west of w no longer matches target-color.
	 * 8.         Move e to the east until the color of the node to the east of e no longer matches target-color.
	 * 9.         For each node n between w and e:
	 * 10.             Set the color of n to replacement-color.
	 * 11.             If the color of the node to the north of n is target-color, add that node to Q.
	 * 12.             If the color of the node to the south of n is target-color, add that node to Q.
	 * 13. Continue looping until Q is exhausted.
	 * 14. Return
	 */
	private HashSet<ChunkCoordIntPair> groupedChunksFinder(HashSet<ChunkCoordIntPair> loadedChunks, ChunkCoordIntPair seed, int radiusLimit)
	{
		
		LinkedList<ChunkCoordIntPair> queue = new LinkedList<ChunkCoordIntPair>();
		HashSet<ChunkCoordIntPair> groupedChunks = new HashSet<ChunkCoordIntPair>();
		
		if (!loadedChunks.contains(seed)) return groupedChunks;
		queue.add(seed);
		
		while (!queue.isEmpty())
		{
			
			ChunkCoordIntPair chunk = queue.remove();
			
			if (!groupedChunks.contains(chunk))
			{
				int west, east;
				
				for (west = chunk.chunkXPos;
						loadedChunks.contains(new ChunkCoordIntPair(west-1, chunk.chunkZPos))
								&& (radiusLimit == 0 || Math.abs(west-1 - seed.chunkXPos) <= radiusLimit);
						--west);
				
				for (east = chunk.chunkXPos;
						loadedChunks.contains(new ChunkCoordIntPair(east+1, chunk.chunkZPos))
								&& (radiusLimit == 0 || Math.abs(east+1 - seed.chunkXPos) <= radiusLimit);
						++east);
				
				for (int x = west; x <= east; ++x)
				{
					
					groupedChunks.add(new ChunkCoordIntPair(x, chunk.chunkZPos));
					
					if (loadedChunks.contains(new ChunkCoordIntPair(x, chunk.chunkZPos+1))
							&& (radiusLimit == 0 || Math.abs(chunk.chunkZPos+1 - seed.chunkZPos) <= radiusLimit))
					{
						
						queue.add(new ChunkCoordIntPair (x, chunk.chunkZPos+1));
						
					}
					
					if (loadedChunks.contains(new ChunkCoordIntPair(x, chunk.chunkZPos-1))
							&& (radiusLimit == 0 || Math.abs(chunk.chunkZPos-1 - seed.chunkZPos) <= radiusLimit))
					{
						
						queue.add(new ChunkCoordIntPair (x, chunk.chunkZPos-1));
						
					}
					
				}
				
			}
			
		}
		
		return groupedChunks;
	}
	
	/*
	 * Populate chunksToUnload with chunks that are isolated from all players, chunk loaders, and the spawn.
	 * 
	 * Use a flood-fill algorithm to find the set of all loaded chunks in the world which link back
	 * to a chunk watcher through other loaded chunks. The idea is to find the isolated chunks
	 * which do NOT link back to a valid chunk watcher, and unload those. 
	 * 
	 * This is a better alternative to simply unloading all chunks outside of a player's view radius.
	 * Unloading chunks while not unloading their neighbors would result in tps-spikes due to the breaking
	 * of energy nets and the like. This approach should reduce the severity of those tps-spikes.
	 * 
	 * ChunkProviderServer.loadedChunks is a private field, so an access transformer is required to access it. 
	 */
	private void populateChunksToUnload()
	{
		
		chunksToUnload = new HashSet<ChunkCoordIntPair>();
		
		if (world.getChunkProvider() instanceof ChunkProviderServer)
		{
			// The set of chunks that are currently loaded in this world by all mechanisms.
			HashSet<ChunkCoordIntPair> loadedChunks = new HashSet<ChunkCoordIntPair>();
			
			// The set of chunks that are loaded as a result of players.
			HashSet<ChunkCoordIntPair> playerLoadedChunks = new HashSet<ChunkCoordIntPair>();
			// The set of chunks that are loaded due to chunk loading tickets.
			HashSet<ChunkCoordIntPair> forceLoadedChunks = new HashSet<ChunkCoordIntPair>();
			// The set of chunks that are loaded as a result of the world spawn area.
			HashSet<ChunkCoordIntPair> spawnLoadedChunks = new HashSet<ChunkCoordIntPair>();
			
			List<EntityPlayer> listPlayers = world.playerEntities;
			
			int radiusLimit;
			
			final int CHUNK_WIDTH = 16;
			
			// The expected radius of loaded chunks around a player
			final int PLAYER_RADIUS = MinecraftServer.getServer().getConfigurationManager().getViewDistance();
			// The expected radius of loaded chunks around a chunk-loading ticket
			final int TICKET_RADIUS = 0;
			// The expected radius of loaded chunks around the spawn chunk.
			final int SPAWN_RADIUS = 8;
			
			// Add these values to our above expectations to prevent the flood filling algorithm from returning
			// chunks outside of the resulting radius.
			// Each is separate so you can configure them independently
			int PLAYER_LIMIT = ModChunkPurge.config.pradius;
			int TICKET_LIMIT = ModChunkPurge.config.tradius;
			int SPAWN_LIMIT = ModChunkPurge.config.sradius;
			
			
			// Want to deal with chunk coordinates, not chunk objects.
			for (Chunk chunk : (List<Chunk>) ((ChunkProviderServer) world.getChunkProvider()).loadedChunks)
			{
				
				loadedChunks.add(chunk.getChunkCoordIntPair());
				
			}
			
			radiusLimit = (int) Math.ceil(PLAYER_RADIUS + PLAYER_LIMIT);
			
			for (EntityPlayer player : listPlayers)
			{
				
				if (!(player instanceof FakePlayer))
				{
					
					ChunkCoordIntPair playerChunkCoords = new ChunkCoordIntPair(player.chunkCoordX, player.chunkCoordZ);
					playerLoadedChunks.addAll(groupedChunksFinder(loadedChunks, playerChunkCoords, radiusLimit));
					
				}
				
			}
			
			radiusLimit = (int) Math.ceil(TICKET_RADIUS + TICKET_LIMIT);
			
			for (ChunkCoordIntPair coord : world.getPersistentChunks().keySet())
			{
				
				forceLoadedChunks.addAll(groupedChunksFinder(loadedChunks, coord, radiusLimit));
				
			}
			
			radiusLimit = (int) Math.ceil(SPAWN_RADIUS + SPAWN_LIMIT);
			
			if (world.provider.canRespawnHere() && DimensionManager.shouldLoadSpawn(world.provider.getDimensionId()))
			{

				ChunkCoordIntPair spawnChunkCoords = new ChunkCoordIntPair(
						(int) Math.floor(world.getSpawnPoint().getX() / CHUNK_WIDTH),
						(int) Math.floor(world.getSpawnPoint().getY() / CHUNK_WIDTH));
				
				spawnLoadedChunks.addAll(groupedChunksFinder(loadedChunks, spawnChunkCoords, radiusLimit));
				
			}
			
			for (ChunkCoordIntPair coord : loadedChunks)
			{
				
				if (!playerLoadedChunks.contains(coord)
						&& !forceLoadedChunks.contains(coord)
						&& !spawnLoadedChunks.contains(coord))
				{
					
					chunksToUnload.add(coord);
					
				}
				
			}
			
		}
		
	}
	
	/*
	 * Analyze the chunks that are currently loaded in this world. Select loaded chunks that are isolated from any chunk watchers, 
	 * and queue these isolated chunks for unloading.
	 */
	
	public void unloadChunks()
	{
		initialTime = MinecraftServer.getCurrentTimeMillis();
		
		populateChunksToUnload();
		
		if (this.world.getChunkProvider() instanceof ChunkProviderServer)
		{
			
			for (ChunkCoordIntPair coord : chunksToUnload)
			{
				//((ChunkProviderServer) this.world.getChunkProvider()).unloadAllChunks();
				//in source calling unload all chunks just calls this function anyways so
				//this is probably what we want
				((ChunkProviderServer) this.world.getChunkProvider()).dropChunk(coord.chunkXPos, coord.chunkZPos);
			}
			
		}
		
		if (ModChunkPurge.config.debug)
		{
			ModChunkPurge.log.log(Level.INFO, "Queued " + String.valueOf(chunksToUnload.size())
					+ " chunks for unload in dimension " + this.world.provider.getDimensionName()
					+ " (" + String.valueOf(this.world.provider.getDimensionId())
					+ ") in " + String.valueOf(MinecraftServer.getCurrentTimeMillis() - this.initialTime)
					+ " milliseconds.");
			
		}
		
	}
	
}
