package vg.civcraft.mc.namelayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.namelayer.command.CommandHandler;
import vg.civcraft.mc.namelayer.database.AssociationList;
import vg.civcraft.mc.namelayer.database.GroupManagerDao;
import vg.civcraft.mc.namelayer.group.AutoAcceptHandler;
import vg.civcraft.mc.namelayer.group.BlackList;
import vg.civcraft.mc.namelayer.group.DefaultGroupHandler;
import vg.civcraft.mc.namelayer.listeners.AssociationListener;
import vg.civcraft.mc.namelayer.listeners.MercuryMessageListener;
import vg.civcraft.mc.namelayer.listeners.PlayerListener;
import vg.civcraft.mc.namelayer.misc.ClassHandler;
import vg.civcraft.mc.namelayer.misc.NameCleanser;
import vg.civcraft.mc.namelayer.permission.PermissionType;


public class NameLayerPlugin extends ACivMod{
	private static AssociationList associations;
	private static BlackList blackList;
	private static GroupManagerDao groupManagerDao;
	private static DefaultGroupHandler defaultGroupHandler;
	private static NameLayerPlugin instance;
	private static AutoAcceptHandler autoAcceptHandler;
	private CommandHandler handle;
	private static ManagedDatasource db;
	private static boolean loadGroups = true;
	private static int groupLimit = 10;
	private static boolean createGroupOnFirstJoin;
	private FileConfiguration config;
	private boolean mercuryEnabled;
	
	@Override
	public void onEnable() {
		super.onEnable(); // Need to call this to properly initialize this mod
		saveDefaultConfig();
		reloadConfig();
		config = getConfig();
		loadGroups = config.getBoolean("groups.enable", true);
		groupLimit = config.getInt("groups.grouplimit", 10);
		createGroupOnFirstJoin = config.getBoolean("groups.creationOnFirstJoin", true);
		instance = this;
		mercuryEnabled = Bukkit.getPluginManager().isPluginEnabled("Mercury");
		loadDatabases();
		ClassHandler.Initialize(Bukkit.getServer());
		new NameAPI(new GroupManager(), associations);
		NameCleanser.load(config.getConfigurationSection("name_cleanser"));
		registerListeners();
		if (loadGroups){
			PermissionType.initialize();
			blackList = new BlackList();
			groupManagerDao.loadGroupsInvitations();
			defaultGroupHandler = new DefaultGroupHandler();
			autoAcceptHandler = new AutoAcceptHandler(groupManagerDao.loadAllAutoAccept());
			handle = new CommandHandler();
			handle.registerCommands();
		}
	}
	
	// Calling this for ACivMod
	@Override
	public void onLoad(){
		super.onLoad();
	}
	
	public void registerListeners(){
		getServer().getPluginManager().registerEvents(new AssociationListener(), this);
		getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		if (isMercuryEnabled()){
			getServer().getPluginManager().registerEvents(new MercuryMessageListener(), this);
		}
	}


	// Adding in rate limiting infras for use in commands.
	private static Map<String, Long> cmdLimits = new HashMap<String, Long>();
	private static Map<String, Long> tabLimits = new HashMap<String, Long>();
	private static Map<String, String> cmdWarnings = new HashMap<String, String>();
	private static Map<String, String> tabWarnings = new HashMap<String, String>();
	private static Set<String> globalLimits = new HashSet<String>();
	private static Map<String, Map<UUID, Long>> rateLimiter = new ConcurrentHashMap<String, Map<UUID, Long>>();
	private static UUID GLOB = new UUID(0L, 0L);
	public static boolean rateLimit(UUID player, String cmd, boolean tab) {
		return rateLimit(player, cmd, true, tab);
	}
	public static String rateLimitMessage(String cmd, boolean tab) {
		if (tab) {
			return tabWarnings.get(cmd);
		} else {
			return cmdWarnings.get(cmd);
		}
	}
	private static boolean rateLimit(UUID player, String cmd, boolean chkGlobal, boolean tab) {
		if (tab && !tabLimits.containsKey(cmd)) return false;
		if (!tab && !cmdLimits.containsKey(cmd)) return false;

		String cmd2 = tab ? (cmd + "tb") : cmd;
		long minDelay = tab ? tabLimits.get(cmd) : cmdLimits.get(cmd);
		boolean ret = false;
		if (chkGlobal && globalLimits.contains(cmd)) {
			ret = rateLimit(GLOB, cmd2, minDelay);
			if (ret) {
				return ret;
			}
		}
		ret = rateLimit(player, cmd2, minDelay);
		return ret;
	}

	private static boolean rateLimit(UUID player, String cmd, long minDelay) {
		boolean ret = false;
		long rn = System.currentTimeMillis();
		Map<UUID, Long> rL = rateLimiter.get(cmd);
		if (rL == null) {
			rL = new ConcurrentHashMap<UUID, Long>();
			rateLimiter.put(cmd, rL);
		}

		Long ln = rL.get(player);
		if (ln != null && ln >= (rn - minDelay)) {
			ret = true;
		}
		rL.put(player, rn);
			
		return ret;
	}

	public void configureLimiter() {
		// loop through each segment and look for this
		// commandLimits:
		//  nlpm:
		//   useDefaults: true
		//   minDelay: 2000
		//   tabDelay: 1000
		//   global: false
		//   warning: You are using this command too often! Slow down.
		//   tabWarning: Back off on the Tab use! Give us a chance to catch up.
		ConfigurationSection commandLimits = config.getConfigurationSection("commandLimits");
		if (commandLimits != null) {
			long defaultDelay = 2000l;
			long defaultTabDelay = 1000l;
			String defaultWarning = "Slow down! Consider using /nl for high volume activities.";
			String defaultTabWarning = "Please don't bash tab complete, let us catch up!";
			if (commandLimits.contains("defaults")) {
				ConfigurationSection commandLimit = commandLimits.getConfigurationSection("defaults");
				if (commandLimit.contains("minDelay")) {
					defaultDelay = commandLimit.getLong("minDelay", defaultDelay);
				} 
				if (commandLimit.contains("tabDelay")) {
					defaultTabDelay = commandLimit.getLong("tabDelay", defaultTabDelay);
				}
				if (commandLimit.contains("warning")) {
					defaultWarning = commandLimit.getString("warning", defaultWarning);
				}
				if (commandLimit.contains("tabWarning")) {
					defaultTabWarning = commandLimit.getString("tabWarning", defaultTabWarning);
				}
			}

			for (String cmdKey : commandLimits.getKeys(false)) {
				if (cmdKey.equalsIgnoreCase("defaults")) continue;

				ConfigurationSection commandLimit = commandLimits.getConfigurationSection(cmdKey);

				if (commandLimit.contains("useDefaults") && commandLimit.getBoolean("useDefaults", false)) {
					cmdLimits.put(cmdKey, defaultDelay);
					tabLimits.put(cmdKey, defaultTabDelay);
					cmdWarnings.put(cmdKey, defaultWarning);
					tabWarnings.put(cmdKey, defaultTabWarning);
				}
				if (commandLimit.contains("minDelay")) {
					cmdLimits.put(cmdKey, commandLimit.getLong("minDelay", defaultDelay));
				} 
				if (commandLimit.contains("tabDelay")) {
					tabLimits.put(cmdKey, commandLimit.getLong("tabDelay", defaultTabDelay));
				}
				if (commandLimit.contains("global") && commandLimit.getBoolean("global", false)) {
					globalLimits.add(cmdKey);
				}
				if (commandLimit.contains("warning")) {
					cmdWarnings.put(cmdKey, commandLimit.getString("warning", defaultWarning));
				}
				if (commandLimit.contains("tabWarning")) {
					tabWarnings.put(cmdKey, commandLimit.getString("tabWarning", defaultTabWarning));
				}
			}
		}
				

	}
	// configured in config.yml.
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!loadGroups)
			return false;
		return handle.execute(sender, cmd, args);
	}

	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args){
		if (!loadGroups)
			return null;
		return handle.complete(sender, cmd, args);
	}

	public void onDisable() {
		if (db != null) {
			try {
				db.close();
			} catch (Exception e) {
				getLogger().log(Level.INFO, "Failed to close database gracefully on shutdown.", e);
			}
		}
	}
	
	public static NameLayerPlugin getInstance(){
		return instance;
	}
	
	public void loadDatabases(){
		String host = config.getString("sql.hostname", "localhost");
		int port = config.getInt("sql.port", 3306);
		String dbname = config.getString("sql.dbname", "namelayer");
		String username = config.getString("sql.username");
		String password = config.getString("sql.password");
		int poolsize = config.getInt("sql.poolsize", 10);
		long connectionTimeout = config.getLong("sql.connection_timeout", 10000l);
		long idleTimeout = config.getLong("sql.idle_timeout", 600000l);
		long maxLifetime = config.getLong("sql.max_lifetime", 7200000l);
		try {
			db = new ManagedDatasource(this, username, password, host, port, dbname,
					poolsize, connectionTimeout, idleTimeout, maxLifetime);
			db.getConnection().close();
		} catch (Exception se) {
			NameLayerPlugin.log(Level.WARNING, "Could not connect to DataBase, shutting down!");
			Bukkit.shutdown();
			return;
		}
		
		if (!db.isManaged()) {
			// First "migration" is conversion from old system to new, and lives outside AssociationList and GroupManagerDao.
			boolean isNew = true;
			try (Connection connection = db.getConnection();
					PreparedStatement checkNewInstall = connection.prepareStatement("SELECT * FROM db_version LIMIT 1;");
					// See if this was a new install. If it was, db_version statement will fail. If it isn't, it'll succeed.
					//   If the version statement fails, return true; this is new install, carryon.
					ResultSet rs = checkNewInstall.executeQuery();) {
				isNew = !rs.next();
			} catch (SQLException se) {
				NameLayerPlugin.log(Level.INFO, "New installation: Welcome to Namelayer!");
			}

			if (!isNew) {
				try (Connection connection = db.getConnection();
						PreparedStatement migrateInstall = connection.prepareStatement( 
								"INSERT INTO managed_plugin_data (plugin_name, current_migration_number, last_migration)"
									+ " SELECT plugin_name, max(db_version), `timestamp` FROM db_version WHERE plugin_name = '"
									+ this.getName() + "' LIMIT 1;");) {
					int rows = migrateInstall.executeUpdate();
					if (rows == 1) {
						NameLayerPlugin.log(Level.INFO, "Migration successful!");
					} else {
						Bukkit.shutdown();
						NameLayerPlugin.log(Level.SEVERE, "Migration failed; db_version exists but uncaptured. Could be version problem.");
						return;
					}
				} catch (SQLException se) {
					Bukkit.shutdown();
					// Migration failed...
					NameLayerPlugin.log(Level.SEVERE, "Migration failure!");
					return;
				}
			}
		} else {
			NameLayerPlugin.log(Level.INFO, "Welcome back, oldtimer.");
		}


		associations = new AssociationList(getLogger(), db);
		associations.registerMigrations();
		
		if (loadGroups) {
			groupManagerDao = new GroupManagerDao(getLogger(), db);
			groupManagerDao.registerMigrations();
			NameLayerPlugin.log(Level.INFO, "Removing any cycles...");
			groupManagerDao.removeCycles();
		}
		
		long begin_time = System.currentTimeMillis();

		try {
			getLogger().log(Level.INFO, "Update prepared, starting database update.");
			if (!db.updateDatabase()) {
				getLogger().log(Level.SEVERE, "Update failed, terminating Bukkit.");
				Bukkit.shutdown();
			}
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Update failed, terminating Bukkit. Cause:", e);
			Bukkit.shutdown();
		}

		getLogger().log(Level.INFO, "Database update took {0} seconds", (System.currentTimeMillis() - begin_time) / 1000);

	}
	
	/**
	 * @return Returns the AssocationList.
	 */
	public static AssociationList getAssociationList(){
		return associations;
	}
	/**
	 * @return Returns the GroupManagerDatabase.
	 */
	public static GroupManagerDao getGroupManagerDao(){
		return groupManagerDao;
	}
	
	public static void log(Level level, String message){
		if (level == Level.INFO)
			Bukkit.getLogger().log(level, "[NameLayer:] Info follows\n" +
			message);
		else if (level == Level.WARNING)
			Bukkit.getLogger().log(level, "[NameLayer:] Warning follows\n" +
					message);
		else if (level == Level.SEVERE)
			Bukkit.getLogger().log(level, "[NameLayer:] Stack Trace follows\n --------------------------------------\n" +
					message +
					"\n --------------------------------------");
	}
	/**
	 * Updates the version number for a plugin. You must specify what 
	 * the current version number is.
	 * @param currentVersion- The current version of the plugin.
	 * @param pluginName- The plugin name.
	 * @return Returns the new version of the db.
	 */
	@Deprecated
	public static void insertVersionNum(int currentVersion, String pluginName){
		throw new UnsupportedOperationException("insertVersionNum is no longer supported. Extend CivModCore and use ManagedDatasource"); 
	}
	/**
	 * Checks the version of a specific plugin's db.
	 * @param name- The name of the plugin.
	 * @return Returns the version of the plugin or 0 if none was found.
	 */
	@Deprecated
	public static int getVersionNum(String pluginName){
		throw new UnsupportedOperationException("getVersionNum is no longer supported. Extend CivModCore and use ManagedDatasource");
	}
	
	public static String getSpecialAdminGroup(){
		return "Name_Layer_Special";
	}
	
	public static boolean createGroupOnFirstJoin() {
		return createGroupOnFirstJoin;
	}

	@Override
	protected String getPluginName() {
		return "NameLayerPlugin";
	}

	public static boolean isMercuryEnabled() {
		return getInstance().mercuryEnabled;
	}
	
	public int getGroupLimit(){
		return groupLimit;
	}
	
	public static BlackList getBlackList() {
		return blackList;
	}
	
	public static AutoAcceptHandler getAutoAcceptHandler() {
		return autoAcceptHandler;
	}
	
	public static DefaultGroupHandler getDefaultGroupHandler() {
		return defaultGroupHandler;
	}
}
