package com.the_beast_unleashed.chunkpurge.operators;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

import net.minecraftforge.fml.client.config.GuiConfig;

public class HandlerConfig
{
	
	private Configuration config;
	private Properties properties;
	
	public int chunkUnloadDelay;
	public boolean enabled;
	public boolean debug;
	public int pradius;
	public int tradius;
	public int sradius;
	public String dimlist;
	
	public HandlerConfig(File configurationFile)
	{
		
		config = new Configuration(configurationFile);
		config.load();
		properties = new Properties();
		
		properties.enabled = config.get(Configuration.CATEGORY_GENERAL, "enable", true,
				"Setting to false will prevent any attempts to unload chunks."
				+ "\nChange in game with /chunkpurge enable <true|false>"
				+ "\nDefault: true");
		
		enabled = properties.enabled.getBoolean(true);
		
		properties.chunkUnloadDelay = config.get(Configuration.CATEGORY_GENERAL, "chunkUnloadDelay", 600,
				"The number of ticks to wait between chunk unloading attempts. Must be an integer greater than 0."
				+ "\nChange in game with /chunkpurge delay <ticks>"
				+ "\nDefault: 600");
		
		chunkUnloadDelay = properties.chunkUnloadDelay.getInt(600);
		
		properties.debug = config.get(Configuration.CATEGORY_GENERAL, "debug", false,
				"Logs the number of chunks unloaded from each dimension, and how long is spent calculating which chunks to unload."
				+ "\nChange in game with /chunkpurge debug <true|false>"
				+ "\nDefault: false");
		
		debug = properties.debug.getBoolean(false);
		
		properties.pradius = config.get(Configuration.CATEGORY_GENERAL, "praduius", 4,
				"The number of chunks around a player outside of player view range to ignore while unloading chunks."
				+ "\nChange in game with /chunkpurge pradius <# of chunks>"
				+ "\nDefault: 4");
		
		pradius = properties.pradius.getInt(5);
		
		properties.tradius = config.get(Configuration.CATEGORY_GENERAL, "traduius", 5,
				"The number of chunks around a forced chunk ticket to ignore while unloading chunks."
				+ "\nChange in game with /chunkpurge tradius <# of chunks>"
				+ "\nDefault: 5");
		
		tradius = properties.tradius.getInt(3);
		
		properties.sradius = config.get(Configuration.CATEGORY_GENERAL, "sraduius", 3,
				"The number of chunks around the spawn chunks to ignore while unloading chunks."
				+ "\nChange in game with /chunkpurge tradius <# of chunks>"
				+ "\nDefault: 3");
		
		sradius = properties.sradius.getInt(4);
		
		properties.dimlist = config.get(Configuration.CATEGORY_GENERAL, "dimlist", "0",
				"A comma seperated list of dimension ID's that ChunkPurge should quarry and purge"
				+ "\nChange in game with /chunkpurge dimlist add OR /chunkpurge dimlist remove <dim #,dim #,ect.>"
				+ "\nExample for running ChunkPurge on Overworld, Nether, and End (0,-1,1)"
				+ "\nDefault is Overworld Only (you can add any registered dimensions from any mod)"
				+ "\nDefault: 0");
		
		dimlist = properties.dimlist.getString();
		
		config.save();
		
	}
	
	public void saveConfig()
	{
		
		
		properties.chunkUnloadDelay.set(chunkUnloadDelay);
		properties.debug.set(debug);
		properties.enabled.set(enabled);
		properties.pradius.set(pradius);
		properties.tradius.set(tradius);
		properties.sradius.set(sradius);
		properties.dimlist.set(dimlist);
		config.save();
		
	}
	
	
	private class Properties
	{
		
		Property chunkUnloadDelay;
		Property enabled;
		Property debug;
		Property pradius;
		Property tradius;
		Property sradius;
		Property dimlist;
		
	}
	
}
