package com.the_beast_unleashed.chunkpurge.proxy;


import com.the_beast_unleashed.chunkpurge.ModChunkPurge;
import com.the_beast_unleashed.chunkpurge.commands.CommandChunkPurge;
import com.the_beast_unleashed.chunkpurge.events.HandlerWorldTick;
import com.the_beast_unleashed.chunkpurge.operators.HandlerConfig;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class ProxyCommon
{
	
	public void preInit(FMLPreInitializationEvent event)
	{
		
		ModChunkPurge.log = event.getModLog();
		ModChunkPurge.config = new HandlerConfig(event.getSuggestedConfigurationFile());
		
	}
	
	public void load(FMLInitializationEvent event)
	{

	}
	
	public void postInit(FMLPostInitializationEvent event)
	{
		MinecraftForge.EVENT_BUS.register(new HandlerWorldTick());
		
	}
	
	public void serverLoad(FMLServerStartingEvent event)
	{
		
		event.registerServerCommand(new CommandChunkPurge());
		
	}
	
}
