package net.slashie.serf.game;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import net.slashie.serf.SworeException;
import net.slashie.serf.action.Actor;
import net.slashie.serf.fov.FOV;
import net.slashie.serf.level.AbstractLevel;
import net.slashie.serf.level.Dispatcher;
import net.slashie.serf.level.LevelMetaData;
import net.slashie.serf.ui.CommandListener;
import net.slashie.serf.ui.UISelector;
import net.slashie.serf.ui.UserInterface;
import net.slashie.utils.FileUtil;
import net.slashie.utils.SerializableChecker;


public abstract class SworeGame implements CommandListener, PlayerEventListener, Serializable{
	//Configuration
	protected transient UserInterface ui;
	private transient UISelector uiSelector;
	
	protected Dispatcher dispatcher;
	protected Player player;
	protected AbstractLevel currentLevel;
	private boolean canSave;
	
	public void setCanSave(boolean vl){
		canSave = vl;
	}
	
	public boolean canSave(){
		return canSave;
	}
	
	private Hashtable<String, AbstractLevel> storedLevels = new Hashtable<String, AbstractLevel>();
	
	protected boolean endGame;
	
	protected long turns;
	private Hashtable<String, LevelMetaData> hashMetadata = new Hashtable<String, LevelMetaData>();
	
	public void commandSelected (Command commandCode){
		if (commandCode == CommandListener.Command.QUIT){
			finishGame();
		} else if (commandCode == CommandListener.Command.SAVE) {
			if (canSave()){
				freezeUniqueRegister();
				saveGame(player);
				exitGame();
			}
		}
	}
	
	public abstract String getFirstMessage(Actor player);

	//Callback
	public void onGameStart(int gameType) {};
	public void beforeGameStart(){};
	public void beforePlayerAction(){};
	public void afterPlayerAction(){};
	public void onGameResume(){};
	public void onGameOver(){};
	public void onGameWon(){};
	public void onLevelLoad(AbstractLevel level){};
	
	public abstract String getDeathMessage();
	public abstract Player generatePlayer(int gameType, SworeGame sworeGame);
	public abstract AbstractLevel createLevel(LevelMetaData levelMetadata);

	
	protected void run(){
		player.setFOV(getNewFOV());
		UserInterface.getUI().reset();
		UserInterface.getUI().showMessage(getFirstMessage(player));
		ui.refresh();
		beforeGameStart();
		while (!endGame){
			Actor actor = dispatcher.getNextActor();
            if (actor == player){
            	player.darken();
            	player.see();
            	ui.refresh();
				player.getGameSessionInfo().increaseTurns();
				player.getLevel().checkUnleashers(this);
			}
            if (endGame)
            	break;
            if (actor == player)
            	beforePlayerAction();
            actor.beforeActing();
            boolean acted = actor.act(); // This is a blocking operation. The game thread will wait til this changes
			actor.afterActing();
			if (acted && actor == player)
				afterPlayerAction();
			if (endGame)
            	break;
			actor.getLevel().getDispatcher().returnActor(actor);
			
			if (actor == player){
				if (currentLevel != null)
					currentLevel.updateLevelStatus();
				turns++;
			}
		}
	}
	 	
	
	
	public void resume(){
		player.setSelector(uiSelector);
		ui.setPlayer(player);
		uiSelector.setPlayer(player);
		ui.addCommandListener(this);
		ui.setGameOver(false);
		player.getLevel().addActor(player);
		player.setPlayerEventListener(this);
		endGame = false;
		turns = player.getGameSessionInfo().getTurns();
		syncUniqueRegister();
		onGameResume();
		run();
	}
	
	public void setPlayer(Player p){
		player = p;
		player.setLevel(currentLevel);
		player.setFOV(getNewFOV());
		currentLevel.setPlayer(player);
		if (player.getGameSessionInfo() == null)
			player.setGameSessionInfo(new GameSessionInfo());
		player.setSelector(uiSelector);
		ui.setPlayer(player);
		uiSelector.setPlayer(player);
		player.setPlayerEventListener(this);
		player.setGame(this);
	}
	
	protected FOV getNewFOV() {
		return new FOV();
	}

	public void newGame(int gameType){
		storedLevels = new Hashtable<String, AbstractLevel>();
		player = generatePlayer(gameType, this);
		player.setGameSessionInfo(new GameSessionInfo());
		player.setSelector(uiSelector);
		ui.setPlayer(player);
		uiSelector.setPlayer(player);
		ui.addCommandListener(this);
		ui.setGameOver(false);
		player.setPlayerEventListener(this);
		onGameStart(gameType);
		turns = 0;
		run();
	}
	
	
    public void informEvent(int code){
    	informEvent(code, null);
    }

 	public void informEvent(int code, Object param){
		switch (code){
			case Player.DEATH:
				ui.refresh();
				ui.showSystemMessage(getDeathMessage() +  " [Press Space to continue]");
				finishGame();
				break;
			case Player.EVT_GOTO_LEVEL:
				loadLevel((String)param);
				break;
		}
	}

	private void finishGame(){
		if (!player.isDoNotRecordScore()){
			onGameOver();
		}
		exitGame();
	}
	
	public void exitGame(){
		//levelNumber = -1;
		currentLevel.disableTriggers();
		currentLevel = null;
		ui.removeCommandListener(this);
		ui.setGameOver(true);
		player.setPlayerEventListener(null);
		ui.shutdown();
		endGame = true;
	}

	
	public void wonGame(){
		onGameWon();
		finishGame();
	}
	
	protected void loadLevel(String levelID) {
		String formerLevelID = null; 
		if (currentLevel != null){
			formerLevelID = currentLevel.getID();
			AbstractLevel storedLevel = storedLevels.get(formerLevelID);
			if (storedLevel == null){
				if (currentLevel.isPersistent())
					storedLevels.put(formerLevelID, currentLevel);
			}  
		} else {
			formerLevelID = "_BACK";
		}
		AbstractLevel storedLevel = storedLevels.get(levelID);
		if (storedLevel != null) {
			currentLevel = storedLevel;
			player.setLevel(currentLevel);
			player.setPosition(currentLevel.getExitFor(formerLevelID));
			onLevelLoad(currentLevel);
		} else {
			try {
				currentLevel = createLevel(getMetaData(levelID));
				currentLevel.setPlayer(player);
				ui.setPlayer(player);
				uiSelector.setPlayer(player);
				if(currentLevel.getExitFor(formerLevelID) != null){
					player.setPosition(currentLevel.getExitFor(formerLevelID));
				} else if(currentLevel.getExitFor("_START") != null) {
					player.setPosition(currentLevel.getExitFor("_START"));
				}
				onLevelLoad(currentLevel);
			} catch (SworeException sworee){
				crash("Error while creating level "+levelID, sworee);
			}
		}
		
		dispatcher = currentLevel.getDispatcher();
		
		if (!dispatcher.contains(player)){
			dispatcher.addActor(player);
		}
		ui.levelChange();
	}
	
	protected LevelMetaData getMetaData(String levelID) {
		return hashMetadata.get(levelID);
	}
	
	public void addMetaData(String levelId, LevelMetaData metadata){
		hashMetadata.put(levelId, metadata);
	}

	public void setLevel(AbstractLevel level){
		currentLevel = level;
		player.setLevel(level);
		dispatcher = currentLevel.getDispatcher();
		ui.levelChange();
		
		
	}
	public Player getPlayer(){
		return player;
	}
	
	public static String getVersion(){
		return "0.28 - r254";
	}
	
	public void setInterfaces(UserInterface pui, UISelector ps){
		ui = pui;
		uiSelector = ps;
	}
	
	public static void crash(String message, Throwable exception){
    	System.out.println("Serf runtime "+ getVersion()+": Unrecoverable Error: "+message);
        System.out.println(exception.getMessage());
        exception.printStackTrace();
        
		try {
			//FileOutputStream fos = new FileOutputStream(new File("critical-error-"+new Date().toString()+".txt"));
			FileOutputStream fos = new FileOutputStream(new File("critical-error-"+System.currentTimeMillis()+".txt"));
			PrintStream ps = new PrintStream(fos);
	        exception.printStackTrace(ps);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}  
        System.exit(-1);
    }
	
	public static void crash(String message){
		crash(message,new RuntimeException(message));
    }
	
	private static List<String> reports = new ArrayList<String>();
	
	public static void addReport(String report){
		reports.add(report);
	}
	
	public static List<String> getReports(){
		return reports;
	}

	private static List<String> uniqueRegister = new ArrayList<String>();
	private List<String> uniqueRegisterObjectCopy = new ArrayList<String>();
	
	public void syncUniqueRegister(){
		uniqueRegister = uniqueRegisterObjectCopy;
	}
	
	public void freezeUniqueRegister(){
		uniqueRegisterObjectCopy = uniqueRegister;
	}
	
	public static boolean wasUniqueGenerated(String itemID){
		return uniqueRegister.contains(itemID);
	}
	
	public static void registerUniqueGenerated (String itemID){
		uniqueRegister.add(itemID);
	}
	
	public void saveGame(Player p) throws SworeException{
		String filename = "savegame/"+p.getName()+".sav";
		p.setSelector(null);
		try {
			SerializableChecker sc = new SerializableChecker();
			sc.writeObject(this);
			sc.close();
			
			File saveDirectory = new File("savegame");
			if (saveDirectory.exists()){
				if (!saveDirectory.isDirectory()){
					throw new SworeException("Save directory 'savegame' can't be created");
				}
			} else {
				if (!saveDirectory.mkdir()){
					throw new SworeException("Save directory 'savegame' can't be created");
				}
			}
			ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(filename));
			os.writeObject(this);
			os.close();
			
			
		} catch (IOException ioe){
			crash("Error saving the game", ioe);
		}
	}
	
	public static void permadeath(Player p){
		String filename = "savegame/"+p.getSaveFilename()+".sav";
		if (FileUtil.fileExists(filename)) {
			FileUtil.deleteFile(filename);
		}
	}
	
	public boolean isGameOver(){
		return endGame;
	}
	
}