package com.the_beast_unleashed.chunkpurge;

import com.the_beast_unleashed.chunkpurge.operators.HandlerConfig;
import com.the_beast_unleashed.chunkpurge.proxy.ProxyCommon;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = ModChunkPurge.MODID, name = ModChunkPurge.NAME, version = ModChunkPurge.VERSION, acceptableRemoteVersions = "*")
public class ModChunkPurge
{

	public static final String MODID = "ChunkPurge";
	public static final String NAME = "Chunk Purge";
	// This has to be changed each time the jar is updated to a new version
	// it seems like there could be an easier way to do this.
	public static final String VERSION = "2.1";

	@Instance(MODID)
	public static ModChunkPurge instance;

	public static HandlerConfig config;
	public static org.apache.logging.log4j.Logger log;

	@SidedProxy(clientSide = "com.the_beast_unleashed.chunkpurge.proxy.ProxyClient", serverSide = "com.the_beast_unleashed.chunkpurge.proxy.ProxyCommon")
	public static ProxyCommon proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		proxy.preInit(event);
	}

	@EventHandler
	public void load(FMLInitializationEvent event)
	{
		proxy.load(event);
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		proxy.postInit(event);
	}
	
	@EventHandler
	public void serverLoad(FMLServerStartingEvent event)
	{
		proxy.serverLoad(event);
	}

}
