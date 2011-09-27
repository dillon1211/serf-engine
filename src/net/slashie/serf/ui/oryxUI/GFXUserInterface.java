package net.slashie.serf.ui.oryxUI;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.SwingUtilities;

import net.slashie.libjcsi.CharKey;
import net.slashie.serf.action.Action;
import net.slashie.serf.action.Actor;
import net.slashie.serf.action.EnvironmentInfo;
import net.slashie.serf.action.Message;
import net.slashie.serf.baseDomain.AbstractItem;
import net.slashie.serf.game.Equipment;
import net.slashie.serf.game.GameSessionInfo;
import net.slashie.serf.game.Player;
import net.slashie.serf.game.SworeGame;
import net.slashie.serf.level.AbstractCell;
import net.slashie.serf.level.AbstractFeature;
import net.slashie.serf.level.BufferedLevel;
import net.slashie.serf.ui.ActionCancelException;
import net.slashie.serf.ui.AppearanceFactory;
import net.slashie.serf.ui.CommandListener;
import net.slashie.serf.ui.Effect;
import net.slashie.serf.ui.UserCommand;
import net.slashie.serf.ui.UserInterface;
import net.slashie.serf.ui.oryxUI.effects.GFXEffect;
import net.slashie.utils.ImageUtils;
import net.slashie.utils.Line;
import net.slashie.utils.Position;
import net.slashie.utils.PropertyFilters;
import net.slashie.utils.swing.BorderedMenuBox;
import net.slashie.utils.swing.CallbackActionListener;
import net.slashie.utils.swing.CallbackKeyListener;
import net.slashie.utils.swing.CallbackMouseListener;
import net.slashie.utils.swing.CleanButton;
import net.slashie.utils.swing.GFXMenuItem;
import net.slashie.utils.swing.SimpleGFXMenuItem;


/** 
 *  Shows the level using characters.
 *  Informs the Actions and Commands of the player.
 * 	Must be listening to a System Interface
 */

public abstract class GFXUserInterface extends UserInterface implements Runnable {
	//Attributes
	private int xrange = 11;
	private int yrange = 8;
	
	//Components
	public SwingInformBox messageBox;
	public AddornedBorderTextArea persistantMessageBox;
	private Action target;
	
	private boolean eraseOnArrival; // Erase the buffer upon the arrival of a new msg
	private boolean flipFacing;
	private Vector<String> messageHistory = new Vector<String>(10);
	
	// Relations

 	protected transient SwingSystemInterface si;

 	private Font FNT_MESSAGEBOX;
 	private Font FNT_PERSISTANTMESSAGEBOX;
 	protected int tileSize;
	private BufferedImage 
		TILE_LINE_STEPS, 
		TILE_LINE_AIM,
		TILE_SCAN;
	protected BufferedImage BORDER1, BORDER2, BORDER3, BORDER4, IMG_STATUSSCR_BGROUND, IMG_BORDERS, IMG_ICON;
	private int GADGETSIZE;
	protected Color COLOR_BORDER_OUT, COLOR_BORDER_IN, COLOR_WINDOW_BACKGROUND, COLOR_BOLD;
	private Color
		COLOR_LAST_MESSAGE = Color.WHITE,
		COLOR_OLD_MESSAGE = Color.GRAY;


	public static Font FNT_TEXT;
	public static Font FNT_TITLE;
	public static Font FNT_DIALOGUEIN;
	public static Font FNT_MONO;
	
	// Setters
	/** Sets the object which will be informed of the player commands.
     * this corresponds to the Game object */
	
	//Getters

    // Smart Getters
    public Position getAbsolutePosition(Position insideLevel){
    	Position relative = Position.subs(insideLevel, player.getPosition());
		return Position.add(PC_POS, relative);
	}

	public Position VP_START = new Position(0,0),
				VP_END = new Position (31,18),
				PC_POS = new Position (12,9);
	private boolean flipEnabled = true;
	
    public void setFlipFacing(boolean val){
    	if (flipEnabled)
    		flipFacing = val;
    }

    private boolean [][] FOVMask;
    
    private Color TRANSPARENT_GRAY = new Color(20,20,20,180);
    private Color MAP_NOSOLID_LOS = new Color(98,96,85,150);
    private Color MAP_NOSOLID = new Color(86,77,65,150);
    private Color MAP_SOLID = new Color(83,83,83);
    private void examineLevelMap(){
		messageBox.setVisible(false);
		isCursorEnabled = false;
		saveMapLayer();
		int lw = level.getWidth();
		int lh = level.getHeight();
		int remnantx = (int)((740 - (lw * 3))/2.0d); 
		int remnanty = (int)((480 - (lh * 3))/2.0d);
		Graphics2D g = si.getDrawingGraphics(getUILayer());
		g.setColor(TRANSPARENT_GRAY);
		g.fillRect(0,0,800,600);
		Color cellColor = null;
		Position runner = new Position(0,0,player.getPosition().z);
		boolean isBufferedLevel = level instanceof BufferedLevel;
		for (int x = 0; x < level.getWidth(); x++, runner.x++, runner.y = 0)
			for (int y = 0; y < level.getHeight(); y++, runner.y++){
				if (isBufferedLevel && !((BufferedLevel)level).remembers(x,y,runner.z)) 
					//cellColor = Color.BLACK;
					continue;
				else {
					AbstractCell current = level.getMapCell(runner);
					if (level.isVisible(x,y,runner.z)){
						if (current == null)
							//cellColor = Color.BLACK;
							continue;
						else if (level.getExitOn(runner) != null)
							cellColor = Color.RED;
						else if (current.isSolid())
							cellColor = MAP_SOLID;
						else 
							cellColor = MAP_NOSOLID_LOS;
						
					} else {
						if (current == null)
							//cellColor = Color.BLACK;
							continue;
						else if (level.getExitOn(runner) != null)
							cellColor = Color.RED;
						else if (current.isSolid())
							cellColor = MAP_SOLID;
						else  
							cellColor = MAP_NOSOLID;
					}
					if (player.getPosition().x == x && player.getPosition().y == y)
						cellColor = Color.RED;
				}
				g.setColor(cellColor);
				//g.fillOval(30+remnantx+x*5, 30+remnanty+y*5, 5,5);
				g.fillRect(30+remnantx+x*3, 30+remnanty+y*3, 3,3);
			}
			si.commitLayer(getUILayer());	
		
		
		si.waitKeys(CharKey.ENTER, CharKey.SPACE, CharKey.ESC);
		messageBox.setVisible(true);
		isCursorEnabled = true;
		resetMapLayer();
	}
    
    protected void enterScreen(){
    	messageBox.setVisible(false);
    	isCursorEnabled = false;
    }
    
    protected void leaveScreen(){
    	messageBox.setVisible(true);
    	isCursorEnabled = true;
    }
    
    public int getUILayer(){
    	return 0;
    }
    
    public int getMapLayer(){
    	return 0;
    }
    
    public void showMessageHistory(){
    	enterScreen();
		saveMapLayer();
		si.drawImage(getUILayer(), IMG_STATUSSCR_BGROUND);
		si.print(getUILayer(), 1, 1, "Message Buffer", COLOR_BOLD);
		for (int i = 0; i < 22; i++){
			if (i >= messageHistory.size())
				break;
			si.print(getUILayer(), 1,i+2, (String)messageHistory.elementAt(messageHistory.size()-1-i), Color.WHITE);
		}
		
		si.print(getUILayer(), 55, 24, "[ Space to Continue ]", Color.WHITE);
		si.commitLayer(getUILayer());
		si.waitKeys(CharKey.ENTER, CharKey.SPACE, CharKey.ESC);
		resetMapLayer();
		leaveScreen();
	}
    
    private Position getRelativeMapCoordinates_recycle = new Position(0,0);
    private Position getRelativeMapCoordinates(MouseEvent e) {
    	int mouseX = e.getX();
    	int mouseY = e.getY();
    	mouseX -= PC_POS.x * tileSize;
    	mouseY -= PC_POS.y * tileSize;
    	if (mouseX > 0)
    		mouseX = (int)Math.floor(mouseX / tileSize);
    	else
    		mouseX = (int)Math.ceil(mouseX / tileSize) - 1;
    	if (mouseY > 0)
    		mouseY = (int)Math.floor(mouseY / tileSize);
    	else
    		mouseY = (int)Math.ceil(mouseY / tileSize) - 1;
    	getRelativeMapCoordinates_recycle.x = mouseX;
    	getRelativeMapCoordinates_recycle.y = mouseY;
    	return getRelativeMapCoordinates_recycle;
	}
    
    //Interactive Methods
    public void doLook(){
    	((GFXUISelector)getPlayer().getSelector()).deactivate();
    	si.setCursor(LOOK_CURSOR);
    	BlockingQueue<String> selectionHandler = new LinkedBlockingQueue<String>();
    	KeyListener cbkl = new CallbackKeyListener<String>(selectionHandler){
    		@Override
    		public void keyPressed(KeyEvent e) {
    			try {
					CharKey input = new CharKey(SwingSystemInterface.charCode(e));
					if (input.code == CharKey.ENTER || input.code == CharKey.SPACE || input.code == CharKey.ESC){
						handler.put("BREAK");
					} else if (input.code == CharKey.m){
						handler.put("MORE");
					} else if (GFXUISelector.isArrow(input)){
						handler.put("MOVE_CURSOR:"+input.code);
					}
				} catch (InterruptedException e1) {} 
    		}
    	};
    	si.addKeyListener(cbkl);
    	MouseListener cbml = null;
    	if (useMouse){
	    	cbml = new CallbackMouseListener<String>(selectionHandler){
	    		@Override
	    		public void mouseClicked(MouseEvent e) {
	    			if (e.getButton() == MouseEvent.BUTTON1){
	    				try {
							Position p = getRelativeMapCoordinates(e);
							handler.put("SET_CURSOR:"+p.x+":"+p.y);
							if (e.getClickCount() == 2) {
								handler.put("MORE");
							}
						} catch (InterruptedException e1) {}
	    			} else if (e.getButton() == MouseEvent.BUTTON3){
		    			try {
							handler.put("BREAK");
						} catch (InterruptedException e1) {}
	    			}
	    			
	    		}
	    	};
	    	si.addMouseListener(cbml);
    	}
    	
    	String message = "Looking around. Press SPACE to exit \n ";
    	if (useMouse)
    		message = "Looking around. Right click to exit \n ";
    	
    	Position offset = new Position (0,0);
		messageBox.setForeground(COLOR_LAST_MESSAGE);
		saveMapLayer();
		Actor lookedMonster = null;
		while (true){
			int cellHeight = 0;
			Position browser = getRelativePosition(player.getPosition(), offset);
			String looked = "";
			resetAndDrawMapLayer();
			if (FOVMask[PC_POS.x + offset.x][PC_POS.y + offset.y]){
				AbstractCell choosen = level.getMapCell(browser);
				if (choosen != null)
					cellHeight = choosen.getHeight();
				List<AbstractFeature> feats = level.getFeaturesAt(browser);
				List<AbstractItem> items = level.getItemsAt(browser);
				AbstractItem item = null;
				if (items != null) {
					item = items.get(0);
				}
				lookedMonster = null;
				Actor actor = level.getActorAt(browser);
				if (choosen != null)
					looked += choosen.getDescription();
				if (feats != null){
					for (AbstractFeature feat: feats){
						looked += ", "+ feat.getDescription();
					}
				}
				if (item != null)
					if (items.size() == 1)
						looked += ", "+ item.getDescription();
					else
						looked += ", "+ item.getDescription()+" and some items";
				if (actor != null) {
					looked += ", "+ actor.getDescription();
				}
			}
			messageBox.setText(message+looked);
			si.drawImage(getUILayer(), (PC_POS.x + offset.x)*tileSize, ((PC_POS.y + offset.y)*tileSize), TILE_SCAN);
			si.commitLayer(getUILayer());
			String command = null;
			while (command == null){
				try {
					command = selectionHandler.take();
				} catch (InterruptedException e1) {}
			}
			if (command.equals("BREAK")){
				resetMapLayer();
				break;
			} else if (command.equals("MORE")){
				if (lookedMonster != null)
					showDetailedInfo(lookedMonster);
			} else if (command.startsWith("MOVE_CURSOR")){
				int charcode = Integer.parseInt(command.split(":")[1]);
				offset.add(Action.directionToVariation(GFXUISelector.toIntDirection(new CharKey(charcode))));
				if (offset.x >= xrange) offset.x = xrange;
				if (offset.x <= -xrange) offset.x = -xrange;
				if (offset.y >= yrange) offset.y = yrange;
				if (offset.y <= -yrange) offset.y = -yrange;
			} else if (command.startsWith("SET_CURSOR")){
				offset.x = Integer.parseInt(command.split(":")[1]);
				offset.y = Integer.parseInt(command.split(":")[2]);
				if (offset.x >= xrange) offset.x = xrange;
				if (offset.x <= -xrange) offset.x = -xrange;
				if (offset.y >= yrange) offset.y = yrange;
				if (offset.y <= -yrange) offset.y = -yrange;
			}

     	}
		messageBox.setText("Look mode off");
    	si.removeKeyListener(cbkl);
    	if (useMouse){
    		si.removeMouseListener(cbml);
    	}
    	((GFXUISelector)getPlayer().getSelector()).activate();
		resetMapLayer();
	}

    protected Position getRelativePosition(Position position, Position offset) {
		return Position.add(player.getPosition(), offset);
	}

	public abstract void showDetailedInfo(Actor a);

    public void chat (String message){
	   saveMapLayer();
	   showTextBox(message, 280, 30, 330, 170);
	   resetMapLayer();
	}
   
    public void showTextBox(String text, int x, int y, int w, int h){
    	showTextBox(text, x, y, w, h, false);
    }
    
	public void showTextBox(String text, int x, int y, int w, int h, final boolean keep){
		final BlockingQueue<String> selectionQueue = new LinkedBlockingQueue<String>();
		
		final CallbackKeyListener<String> cbkl = new CallbackKeyListener<String>(selectionQueue){
			@Override
			public void keyPressed(KeyEvent e) {
				try {
					CharKey x = new CharKey(SwingSystemInterface.charCode(e));
					if (x.code == CharKey.ENTER || x.code == CharKey.SPACE || x.code == CharKey.ESC)
						handler.put("OK");
				} catch (InterruptedException e1) {}
			}
		};
		
		final CallbackMouseListener<String> cbml = new CallbackMouseListener<String>(selectionQueue){
			@Override
			public void mousePressed(MouseEvent e) {
				try {
					handler.put("OK");
				} catch (InterruptedException e1) {}
			}
		};
		
		si.addKeyListener(cbkl);
		si.addMouseListener(cbml);
		addornedTextArea.addMouseListener(cbml);
		
		printTextBox(text, x, y, w, h);
		
		Runnable r = new Runnable(){
			@Override
			public void run() {
				String choice = null;
				while (choice == null){
					try {
						choice = selectionQueue.take();
					} catch (InterruptedException e1) {}
				}
				if (!keep)
					clearTextBox();
				si.removeKeyListener(cbkl);
				si.removeMouseListener(cbml);
				addornedTextArea.removeMouseListener(cbml);
			}
		};
		
		if (SwingUtilities.isEventDispatchThread()){
			// To prevent locking, should perform the selection on a separate thread
			new Thread(r).start();
		} else {
			r.run();
		}
	}
	
	public void printTextBox(String text, int x, int y, int w, int h){
		addornedTextArea.setBounds(x, y, w, h);
		addornedTextArea.setText(text);
		addornedTextArea.setVisible(true);
	}
	
	public void clearTextBox(){
		addornedTextArea.setVisible(false);
	}
	
	protected AddornedBorderTextArea addornedTextArea;

	public boolean showTextBoxPrompt(String text, int xPos, int yPos, int width, int height){
		addornedTextArea.setBounds(xPos, yPos, width, height);
		addornedTextArea.setText(text);
		addornedTextArea.setCursor(getDefaultCursor());
		
		BlockingQueue<String> selectionQueue = new LinkedBlockingQueue<String>();
		
		CallbackKeyListener<String> cbkl = new CallbackKeyListener<String>(selectionQueue){
			@Override
			public void keyPressed(KeyEvent e) {
				try {
					CharKey x = new CharKey(SwingSystemInterface.charCode(e));
					if (x.code == CharKey.Y || x.code == CharKey.y)
						handler.put("Y");
					if (x.code == CharKey.N || x.code == CharKey.n)
						handler.put("N");
				} catch (InterruptedException e1) {}
			}
		};
		
		CleanButton yesButton = new CleanButton(IMG_YES_BUTTON, getHandCursor());
		yesButton.setVisible(false);
		yesButton.setHover(IMG_YES_HOVER_BUTTON);
		yesButton.setBounds(xPos + (int)Math.round((double)width / 2.0d) - IMG_YES_BUTTON.getWidth() - 20,
				yPos + height - IMG_YES_BUTTON.getHeight() - 20,
				IMG_YES_BUTTON.getWidth(),
				IMG_YES_BUTTON.getHeight()
				//48, 96
				);
		si.add(yesButton);
		si.changeZOrder(yesButton, 1);

		CleanButton noButton = new CleanButton(IMG_NO_BUTTON, getHandCursor());
		noButton.setVisible(false);
		noButton.setHover(IMG_NO_HOVER_BUTTON);
		noButton.setBounds(xPos + (int)Math.round((double)width / 2.0d) + 20,
				yPos + height - IMG_NO_BUTTON.getHeight() - 20,
				IMG_NO_BUTTON.getWidth(),
				IMG_NO_BUTTON.getHeight()
				/*48, 96*/
		);
		yesButton.addActionListener(new CallbackActionListener<String>(selectionQueue){
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					handler.put("Y");
				} catch (InterruptedException e1) {}
			}
		});
		
		noButton.addActionListener(new CallbackActionListener<String>(selectionQueue){
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					handler.put("N");
				} catch (InterruptedException e1) {}
			}
		});
		si.add(noButton);
		si.changeZOrder(noButton, 1);
		
		addornedTextArea.setVisible(true);
		yesButton.setVisible(true);
		noButton.setVisible(true);
		si.commitLayer(getUILayer());


		si.addKeyListener(cbkl);
		
		String choice = null;
		while (choice == null){
			try {
				choice = selectionQueue.take();
			} catch (InterruptedException e1) {}
		}
		boolean ret = choice.equals("Y");
		addornedTextArea.setVisible(false);
		yesButton.setVisible(false);
		noButton.setVisible(false);
		
		si.remove(noButton);
		si.remove(yesButton);
		
		si.removeKeyListener(cbkl);
		return ret;
	}
   
   public boolean promptChat (String text, int x, int y, int w, int h){
	   saveMapLayer();
	   boolean ret = showTextBoxPrompt(text, x, y, w, h);
	   resetAndDrawMapLayer();
	   return ret;
	}

    // Drawing Methods
	public void drawEffect(Effect what){
		if (what == null)
			return;
		if (insideViewPort(getAbsolutePosition(what.getPosition()))){
			((GFXEffect)what).drawEffect(this, si);
		}
	}
	
	public boolean isOnFOVMask(int x, int y){
		return FOVMask[x][y];
	}
	
	private synchronized void drawLevel(){
		AbstractCell [][] rcells = level.getMemoryCellsAround(player.getPosition().x,player.getPosition().y, player.getPosition().z, xrange,yrange);
		EnvironmentInfo environmentInfo = player.getEnvironmentAround(xrange,yrange);
		AbstractCell [][] vcells = environmentInfo.getCellsAround();

		
		monstersOnSight.removeAllElements();
		featuresOnSight.removeAllElements();
		itemsOnSight.removeAllElements();
		
		Position runner = new Position(0,0);
		for (int y = 0; y < yrange*2+1; y++){
			runner.y = y-yrange;
			for (int x=0; x < xrange*2+1; x++){
				runner.x = x-xrange;
				
				FOVMask[PC_POS.x-xrange+x][PC_POS.y-yrange+y] = false;
				
				// Draw the visible cells
				if (vcells[x][y] == null || vcells[x][y].getID().equals("AIR")){
					if (rcells[x][y] != null && !rcells[x][y].getAppearance().getID().equals("NOTHING")){
						GFXAppearance app = (GFXAppearance)rcells[x][y].getAppearance();
						// Draw Cells
						try {
							si.drawImage(getMapLayer(),(PC_POS.x-xrange+x)*tileSize,(PC_POS.y-yrange+y)*tileSize-app.getSuperHeight(), app.getDarkImage());
						} catch (NullPointerException npe){
							Color c = si.getDrawingGraphics(0).getColor();
							si.getDrawingGraphics(getMapLayer()).setColor(Color.RED);
							si.getDrawingGraphics(getMapLayer()).fillRect((PC_POS.x-xrange+x)*tileSize,(PC_POS.y-yrange+y)*tileSize-app.getSuperHeight(), tileSize,49);
							si.getDrawingGraphics(getMapLayer()).setColor(c);
						}
					} else {
						//Draw nothing
						//si.drawImage((PC_POS.x-xrange+x)*tileSize,(PC_POS.y-yrange+y)*tileSize, "gfx/black.gif");
						//si.print(PC_POS.x-xrange+x,PC_POS.y-yrange+y, CharAppearance.getVoidAppearance().getChar(), CharAppearance.getVoidAppearance().BLACK);
					}
				} else {
					FOVMask[PC_POS.x-xrange+x][PC_POS.y-yrange+y] = true;
					GFXAppearance cellApp = (GFXAppearance)vcells[x][y].getAppearance();
					si.drawImage(getMapLayer(), (PC_POS.x-xrange+x)*tileSize,(PC_POS.y-yrange+y)*tileSize-cellApp.getSuperHeight(), cellApp.getImage());
					
					int cellHeight = vcells[x][y].getHeight();

					
					//  Draw Features
					List<AbstractFeature> feats = environmentInfo.getFeaturesAt(runner);
					if (feats != null){
						for (AbstractFeature feat: feats){
							if (feat.isVisible()) {
								GFXAppearance featApp = (GFXAppearance)feat.getAppearance();
								si.drawImage(getMapLayer(), (PC_POS.x-xrange+x)*tileSize-featApp.getSuperWidth(),(PC_POS.y-yrange+y)*tileSize-4*cellHeight-featApp.getSuperHeight(), featApp.getImage());
							}
						}
					}
					
					
					AbstractItem item = environmentInfo.getItemAt(runner);
					if (item != null){
						if (item.isVisible()){
							GFXAppearance itemApp = (GFXAppearance)item.getAppearance();
							si.drawImage(getMapLayer(), (PC_POS.x-xrange+x)*tileSize-itemApp.getSuperWidth(),(PC_POS.y-yrange+y)*tileSize-4*cellHeight -itemApp.getSuperHeight(), itemApp.getImage());
						}
					}
					
					if (yrange == y && x == xrange){
						if (player.isInvisible()){
							si.drawImage(getMapLayer(), PC_POS.x*tileSize,PC_POS.y*tileSize-4*cellHeight, ((GFXAppearance)AppearanceFactory.getAppearanceFactory().getAppearance("SHADOW")).getImage());
						}else{
							GFXAppearance playerAppearance = (GFXAppearance)player.getAppearance();
							BufferedImage playerImage = (BufferedImage)playerAppearance.getImage();
							if (flipEnabled && flipFacing){
								playerImage = ImageUtils.vFlip(playerImage);
								//flipFacing = false;
							}
							if (level.getMapCell(player.getPosition())!= null && level.getMapCell(player.getPosition()).isShallowWater())
								si.drawImage(getMapLayer(), PC_POS.x*tileSize-playerAppearance.getSuperWidth(),PC_POS.y*tileSize-playerAppearance.getSuperHeight()+16, playerImage);
							else
								si.drawImage(getMapLayer(), PC_POS.x*tileSize-playerAppearance.getSuperWidth(),PC_POS.y*tileSize-playerAppearance.getSuperHeight(), playerImage);
						}
					}
					Actor actor = environmentInfo.getActorAt(runner);
					if (actor != player && actor != null && !actor.isInvisible()){
						GFXAppearance monsterApp = (GFXAppearance) actor.getAppearance();
						si.drawImage(getMapLayer(), (PC_POS.x-xrange+x)*tileSize-monsterApp.getSuperWidth(),(PC_POS.y-yrange+y)*tileSize-4*cellHeight-monsterApp.getSuperHeight(), monsterApp.getImage());
					}
				}
				
				// Draw the features and other actors
			}
		}
	}
	
	private String lastMessage;
	private String currentText;
	private int sameMessageCount = 1;
	public void addMessage(Message message){
		if (message.getLocation().z != player.getPosition().z || !insideViewPort(getAbsolutePosition(message.getLocation()))){
			return;
		}
		
		if (messageHistory.size() > 0 && message.getText().equals(lastMessage)) {
			sameMessageCount++;
			String multiplier = "(x"+sameMessageCount+")";
			messageHistory.remove(messageHistory.size()-1);
			messageHistory.add(message.getText()+multiplier);
			if (currentText.equals("")){
				messageBox.setText(message.getText()+" "+multiplier);
			} else {
				String separator = ". ";
				if (currentText.endsWith("!")){
					separator = " ";
				} 
				messageBox.setText(currentText+separator+ message.getText()+" "+multiplier);
			}
		} else {
			sameMessageCount = 1;
			lastMessage = message.getText();
			if (eraseOnArrival){
		 		messageBox.clear();
		 		messageBox.setForeground(COLOR_LAST_MESSAGE);
		 		eraseOnArrival = false;
			}
			currentText = messageBox.getText();
			messageHistory.add(message.getText());
			if (messageHistory.size()>500)
				messageHistory.removeElementAt(0);
			messageBox.addText(message.getText());
		}
		
		dimMsg = 0;
	}

	/*private void drawCursor(){
		/*if (isCursorEnabled){
			si.restore();
			Cell underlying = player.getLevel().getMapCell(tempCursorPosition);
			si.drawImage((PC_POS.x+tempCursorPositionScr.x)*tileSize,(PC_POS.y+tempCursorPositionScr.y)*tileSize-4*underlying.getHeight(), TILE_SCAN);
			si.commitLayer(getUILayer());
		}
	}*/
	
	private boolean isCursorEnabled = false;
	protected BufferedImage IMG_YES_BUTTON;
	protected BufferedImage IMG_NO_BUTTON;
	protected BufferedImage IMG_YES_HOVER_BUTTON;
	protected BufferedImage IMG_NO_HOVER_BUTTON;
	
	private void initProperties(Properties p){
		xrange = PropertyFilters.inte(p.getProperty("XRANGE"));
		yrange = PropertyFilters.inte(p.getProperty("YRANGE"));
		
		//POS_LEVELDESC_X = PropertyFilters.inte(p.getProperty("POS_LEVELDESC_X"));
		//POS_LEVELDESC_Y = PropertyFilters.inte(p.getProperty("POS_LEVELDESC_Y"));
		
		//UPLEFTBORDER = PropertyFilters.inte(p.getProperty("UPLEFTBORDER"));
		PC_POS = PropertyFilters.getPosition(p.getProperty("PC_POS"));
		
		VP_START = Position.add(PC_POS, new Position (-xrange,-yrange));
		VP_END = Position.add(PC_POS, new Position (xrange,yrange));
		
		
		/*TILESIZE = PropertyFilters.inte(p.getProperty("TILESIZE"));*/
		COLOR_WINDOW_BACKGROUND = PropertyFilters.getColor(p.getProperty("COLOR_WINDOW_BACKGROUND"));
		COLOR_BORDER_IN = PropertyFilters.getColor(p.getProperty("COLOR_BORDER_IN"));
		COLOR_BORDER_OUT = PropertyFilters.getColor(p.getProperty("COLOR_BORDER_OUT"));
		/*COLOR_MSGBOX_ACTIVE = PropertyFilters.getColor(p.getProperty("COLOR_MSGBOX_ACTIVE"));
		COLOR_MSGBOX_INACTIVE = PropertyFilters.getColor(p.getProperty("COLOR_MSGBOX_INACTIVE"));*/
		try {
			FNT_MESSAGEBOX = PropertyFilters.getFont(p.getProperty("FNT_MESSAGEBOX"),p.getProperty("FNT_MESSAGEBOX_SIZE"));
			FNT_PERSISTANTMESSAGEBOX = PropertyFilters.getFont(p.getProperty("FNT_PERSISTANTMESSAGEBOX"),p.getProperty("FNT_PERSISTANTMESSAGEBOX_SIZE"));
			
		} catch (FontFormatException ffe){
			SworeGame.crash("Error loading the font", ffe);
		} catch (IOException ioe){
			SworeGame.crash("Error loading the font", ioe);
		}
		
		/*-- Load UI Images */
		try {
			IMG_STATUSSCR_BGROUND = ImageUtils.createImage(p.getProperty("IMG_STATUSSCR_BGROUND"));
			GADGETSIZE = PropertyFilters.inte(p.getProperty("GADGETSIZE"));
			BufferedImage IMG_GADGETS = PropertyFilters.getImage(p.getProperty("IMG_GADGETS"), p.getProperty("IMG_GADGETS_BOUNDS"));
			TILE_LINE_AIM  = ImageUtils.crearImagen(IMG_GADGETS, 0, 0, GADGETSIZE, GADGETSIZE);
			TILE_SCAN  = ImageUtils.crearImagen(IMG_GADGETS, GADGETSIZE, 0, GADGETSIZE, GADGETSIZE);
			TILE_LINE_STEPS  = ImageUtils.crearImagen(IMG_GADGETS, GADGETSIZE*2, 0, GADGETSIZE, GADGETSIZE);
			
			IMG_ICON = ImageUtils.createImage(p.getProperty("IMG_ICON"));
			COLOR_BOLD = PropertyFilters.getColor(p.getProperty("COLOR_BOLD"));
			IMG_BORDERS = PropertyFilters.getImage(p.getProperty("IMG_BORDERS"), p.getProperty("IMG_BORDERS_BOUNDS"));
			
			BORDER1 = ImageUtils.crearImagen(IMG_BORDERS, tileSize,0,tileSize,tileSize);
			BORDER2 = ImageUtils.crearImagen(IMG_BORDERS, 0,0,tileSize,tileSize);
			BORDER3 = ImageUtils.crearImagen(IMG_BORDERS, tileSize*3,0,tileSize,tileSize);
			BORDER4 = ImageUtils.crearImagen(IMG_BORDERS, tileSize*2,0, tileSize,tileSize);
			
			FNT_TITLE = PropertyFilters.getFont(p.getProperty("FNT_TITLE"), p.getProperty("FNT_TITLE_SIZE"));
			FNT_TEXT = PropertyFilters.getFont(p.getProperty("FNT_TEXT"), p.getProperty("FNT_TEXT_SIZE"));
			FNT_DIALOGUEIN  = FNT_TEXT;
			FNT_MONO = PropertyFilters.getFont(p.getProperty("FNT_MONO"), p.getProperty("FNT_MONO_SIZE"));
			
			IMG_YES_BUTTON = PropertyFilters.getImage(p.getProperty("IMG_UI"), p.getProperty("BTN_YES_BOUNDS"));
			IMG_NO_BUTTON = PropertyFilters.getImage(p.getProperty("IMG_UI"), p.getProperty("BTN_NO_BOUNDS"));
			IMG_YES_HOVER_BUTTON = PropertyFilters.getImage(p.getProperty("IMG_UI"), p.getProperty("BTN_YES_HOVER_BOUNDS"));
			IMG_NO_HOVER_BUTTON = PropertyFilters.getImage(p.getProperty("IMG_UI"), p.getProperty("BTN_NO_HOVER_BOUNDS"));
			
		} catch (Exception e){
			SworeGame.crash(e.getMessage(),e);
		}
		
		
		
		
	}
    
	private Cursor LOOK_CURSOR;
	private boolean useMouse = false;
	
	public void init(SwingSystemInterface psi, String title, UserCommand[] gameCommands, Properties UIProperties, Action target){
		super.init(gameCommands);
		this.target = target;
		tileSize = PropertyFilters.inte(UIProperties.getProperty("TILE_SIZE"));

		initProperties(UIProperties);
		//GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setDisplayMode(new DisplayMode(800,600,8, DisplayMode.REFRESH_RATE_UNKNOWN));
		
		if (UIProperties.getProperty("useMouse").equals("true")){
			useMouse  = true;
		}
		
		try {
			addornedTextArea = new AddornedBorderTextArea(
					BORDER1,
					BORDER2,
					BORDER3,
					BORDER4,
					COLOR_BORDER_OUT,
					COLOR_BORDER_IN,
					COLOR_WINDOW_BACKGROUND,
					tileSize,
					6,9,12 );
		} catch (Exception e){
			e.printStackTrace();
		}
		
		addornedTextArea.setVisible(false);
		addornedTextArea.setEnabled(false);
		addornedTextArea.setForeground(Color.WHITE);
		addornedTextArea.setBackground(Color.BLACK);
		addornedTextArea.setFont(FNT_DIALOGUEIN);
		addornedTextArea.setOpaque(false);
		addornedTextArea.setForeground(new Color(244,226,108));
		psi.add(addornedTextArea);
		
		/*-- Assign values */
		si = psi;
		FOVMask = new boolean[80][25];
		si.getDrawingGraphics(getMapLayer()).setColor(Color.BLACK);
		si.getDrawingGraphics(getMapLayer()).fillRect(0,0,800,600);
		si.commitLayer(getMapLayer());
		
		si.setIcon(IMG_ICON);
		si.setTitle(title);
		/*-- Init Components*/
		messageBox = new SwingInformBox();
		/*idList = new ListBox(psi);*/
		messageBox.setBounds(PropertyFilters.getRectangle(UIProperties.getProperty("MSGBOX_BOUNDS")));
		messageBox.setForeground(COLOR_LAST_MESSAGE);
		messageBox.setBackground(Color.BLACK);
		messageBox.setFont(FNT_MESSAGEBOX);
		messageBox.setEditable(false);
		messageBox.setVisible(false);
		messageBox.setOpaque(false);
		messageBox.setLineWrap(true);
		messageBox.setWrapStyleWord(true);
		
		psi.add(messageBox);
		
		persistantMessageBox = new AddornedBorderTextArea(BORDER1, BORDER2, BORDER3, BORDER4, 
				new Color(52,42,20),
				new Color(164,138,68),
				new Color(232,253,77),
				tileSize,
				6,9,12);
		persistantMessageBox.setBounds(520,90,260,400);
		persistantMessageBox.setVisible(false);
		persistantMessageBox.setFont(FNT_PERSISTANTMESSAGEBOX);
		persistantMessageBox.setForeground(Color.WHITE);
		psi.add(persistantMessageBox);

		LOOK_CURSOR = createCursor(UIProperties.getProperty("IMG_CURSORS"), 6, 2, 10, 4);
		
		si.setVisible(true);
	}
	
	public void setPersistantMessage(String description) {
		persistantMessageBox.setText(description);
		persistantMessageBox.setVisible(true);
	}

	/** 
	 * Checks if the point, relative to the console coordinates, is inside the
	 * ViewPort 
	 */
	public boolean insideViewPort(int x, int y){
    	//return (x>=VP_START.x && x <= VP_END.x && y >= VP_START.y && y <= VP_END.y);
		return (x>=0 && x < FOVMask.length && y >= 0 && y < FOVMask[0].length) && FOVMask[x][y];
    }

	public boolean insideViewPort(Position what){
    	return insideViewPort(what.x, what.y);
    }

	public boolean isDisplaying(Actor who){
    	return insideViewPort(getAbsolutePosition(who.getPosition()));
    }

    private Position pickPosition(String prompt, int fireKeyCode) throws ActionCancelException{
    	messageBox.setForeground(COLOR_LAST_MESSAGE);
    	messageBox.setText(prompt);
    	Position defaultTarget = null; 
   		Position nearest = getNearestActorPosition();
   		if (nearest != null){
   			defaultTarget = nearest;
   		} else {
   			defaultTarget = null;
   		}
    	
    	Position browser = null;
    	Position offset = new Position (0,0);
    	    	
    	if (defaultTarget == null) {
    		offset = new Position (0,0);
    	} else{
			offset = new Position(defaultTarget.x - player.getPosition().x, defaultTarget.y - player.getPosition().y);
		}
    	
    	if (!insideViewPort(PC_POS.x + offset.x,PC_POS.y + offset.y)){
    		offset = new Position (0,0);
    	}
    	
    	/*if (!insideViewPort(offset))
    		offset = new Position (0,0);*/
    	
    	saveMapLayer();
		//si.commitLayer(getUILayer());
		
		while (true){
			resetMapLayer();
			int cellHeight = 0;
			browser = Position.add(player.getPosition(), offset);
			String looked = "";
			
			if (FOVMask[PC_POS.x + offset.x][PC_POS.y + offset.y]){
				AbstractCell choosen = level.getMapCell(browser);
				List<AbstractFeature> feats = level.getFeaturesAt(browser);
				List<AbstractItem> items = level.getItemsAt(browser);
				if (choosen != null)
					cellHeight = choosen.getHeight();
				AbstractItem item = null;
				if (items != null) {
					item = (AbstractItem) items.get(0);
				}
				Actor actor = level.getActorAt(browser);
				if (choosen != null)
					looked += choosen.getDescription();
				if (feats != null){
					for (AbstractFeature feat: feats){
						looked += ", "+ feat.getDescription();
					}
				}
				if (actor != null)
					looked += ", "+ actor.getDescription();
				if (item != null)
					looked += ", "+ item.getDescription();
			}
			messageBox.setText(prompt+" "+looked);
			//si.print(PC_POS.x + offset.x, PC_POS.y + offset.y, '_', ConsoleSystemInterface.RED);
			drawStepsTo(PC_POS.x + offset.x, (PC_POS.y + offset.y), TILE_LINE_STEPS, cellHeight);
			
			si.drawImage(getUILayer(), (PC_POS.x + offset.x)*tileSize-2, ((PC_POS.y + offset.y)*tileSize-2) -4*cellHeight, TILE_LINE_AIM);
			si.commitLayer(getUILayer());
			CharKey x = new CharKey(CharKey.NONE);
			while (x.code != CharKey.ENTER && x.code != CharKey.SPACE && x.code != CharKey.ESC && x.code != fireKeyCode &&
				   ! x.isArrow())
				x = si.inkey();
			if (x.code == CharKey.ESC){
				resetMapLayer();
				throw new ActionCancelException();
			}
			if (x.code == CharKey.ENTER || x.code == CharKey.SPACE || x.code == fireKeyCode){
				si.commitLayer(getUILayer());
				return browser;
			}
			offset.add(Action.directionToVariation(GFXUISelector.toIntDirection(x)));

			if (offset.x >= xrange) offset.x = xrange;
			if (offset.x <= -xrange) offset.x = -xrange;
			if (offset.y >= yrange) offset.y = yrange;
			if (offset.y <= -yrange) offset.y = -yrange;
     	}
		
		
    }

	private int pickDirection(String prompt) throws ActionCancelException{
		leaveScreen();
		messageBox.setText(prompt);
		//si.commitLayer(getUILayer());
		//refresh();

		CharKey x = new CharKey(CharKey.NONE);
		while (x.code == CharKey.NONE)
			x = si.inkey();
		if (x.isArrow()){
			int ret = GFXUISelector.toIntDirection(x);
        	return ret;
		} else {
			ActionCancelException ret = new ActionCancelException(); 
			si.commitLayer(getUILayer());
			throw ret; 
		}
	}

	private AbstractItem pickEquipedItem(String prompt) throws ActionCancelException{
		enterScreen();

		List<? extends AbstractItem> equipped = player.getEquippedItems();

		if (equipped.size() == 0){
  			level.addMessage("Nothing equipped");
  			ActionCancelException ret = new ActionCancelException();
			throw ret;
  		}
  		
  		BorderedMenuBox menuBox = new BorderedMenuBox(BORDER1, BORDER2, BORDER3, BORDER4, si, COLOR_WINDOW_BACKGROUND, COLOR_BORDER_IN, COLOR_BORDER_OUT, tileSize, 6,9,12,tileSize,null);
  		
  		//menuBox.setBounds(26,6,30,11);
  		menuBox.setBounds(6,4,70,12);
  		menuBox.setMenuItems(equipped);
  		menuBox.setTitle(prompt);
  		saveMapLayer();
  		//menuBox.draw();
  		AbstractItem equiped = (AbstractItem)menuBox.getSelection();
		if (equiped == null){
			ActionCancelException ret = new ActionCancelException();
			resetMapLayer();
			throw ret;
		}
		resetMapLayer();
		leaveScreen();
		return equiped;
	}
	
	private AbstractItem pickItem(String prompt) throws ActionCancelException{
		enterScreen();
  		List inventory = player.getInventory();
  		BorderedMenuBox menuBox = new BorderedMenuBox(BORDER1, BORDER2, BORDER3, BORDER4, si, COLOR_WINDOW_BACKGROUND, COLOR_BORDER_IN, COLOR_BORDER_OUT, tileSize, 6,9,12,tileSize,null);
  		
  		menuBox.setBounds(20,20,400,500);
  		menuBox.setItemsPerPage(12);
  		menuBox.setMenuItems(inventory);
  		menuBox.setTitle(prompt);
  		saveMapLayer();
  		//menuBox.draw();
		Equipment equipment = (Equipment)menuBox.getSelection();
		if (equipment == null){
			ActionCancelException ret = new ActionCancelException();
			resetMapLayer();
			leaveScreen();
			throw ret;
		}
		resetMapLayer();
		leaveScreen();
		return equipment.getItem();
	}
	
	
	private Vector pickMultiItems(String prompt) throws ActionCancelException{
		//Equipment.eqMode = true;
		List inventory = player.getInventory();
		BorderedMenuBox menuBox = new BorderedMenuBox(BORDER1, BORDER2, BORDER3, BORDER4, si, COLOR_WINDOW_BACKGROUND, COLOR_BORDER_IN, COLOR_BORDER_OUT, tileSize, 6,9,12,tileSize+3,null);
  		menuBox.setBounds(25,3,40,18);
  		//menuBox.setPromptSize(2);
  		menuBox.setMenuItems(inventory);
  		menuBox.setTitle(prompt);
  		//menuBox.setForeColor(ConsoleSystemInterface.RED);
  		//menuBox.setBorder(true);
  		Vector ret = new Vector();
  		BorderedMenuBox selectedBox = new BorderedMenuBox(BORDER1, BORDER2, BORDER3, BORDER4, si, COLOR_WINDOW_BACKGROUND, COLOR_BORDER_IN, COLOR_BORDER_OUT, tileSize, 6,9,12,tileSize, null);
  		selectedBox.setBounds(5,3,20,18);
  		//selectedBox.setPromptSize(2);
  		selectedBox.setTitle("Selected Items");
  		selectedBox.setMenuItems(ret);
  		//selectedBox.setForeColor(ConsoleSystemInterface.RED);
  		
  		saveMapLayer();
  		
		while (true){
			selectedBox.draw();
			menuBox.draw();
			
	  		
			Equipment equipment = (Equipment)menuBox.getSelection();
			if (equipment == null)
				break;
			if (!ret.contains(equipment.getItem()))
				ret.add(equipment.getItem());
		}
		resetMapLayer();
		//Equipment.eqMode = false;
		return ret;
	}

	@Override
	public void shutdown(){
		enterScreen();
		si.cleanLayer(getUILayer());
		si.commitLayer(getUILayer());
		si.cleanLayer(getMapLayer());
		si.commitLayer(getMapLayer());
		lastMessage = null;
	}
	
	public void processQuit(){
		if (promptChat(getQuitMessage())){
			shutdown();
			player.getGameSessionInfo().setDeathCause(GameSessionInfo.QUIT);
			informPlayerCommand(CommandListener.Command.QUIT);
		}
	}
	
	
	public void processSave(){
		if (!player.getGame().canSave()){
			level.addMessage("You cannot save your game here!");
			return;
		}
		
		if (promptChat("Save your game?")){
			messageBox.setText("Saving... ");
			si.commitLayer(getUILayer());
			enterScreen();
			informPlayerCommand(CommandListener.Command.SAVE);
		}
	}

	public boolean prompt (){
		
		CharKey x = new CharKey(CharKey.NONE);
		while (x.code != CharKey.Y && x.code != CharKey.y && x.code != CharKey.N && x.code != CharKey.n)
			x = si.inkey();
		return (x.code == CharKey.Y || x.code == CharKey.y);
	}

	private int dimMsg = 0;
	public synchronized void refresh(){
		synchronized (si) {
			si.cleanLayer(getUILayer());
			si.cls(getMapLayer());
			beforeDrawLevel();
		 	drawLevel();
			beforeRefresh();
			si.commitLayer(getUILayer());
			si.commitLayer(getMapLayer());
			leaveScreen();
			if (dimMsg == 3){
				messageBox.setForeground(COLOR_OLD_MESSAGE);
				dimMsg = 0;
			}
			dimMsg++;
		  	if (!player.getFlag("KEEPMESSAGES"))
		  		eraseOnArrival = true;
		  	si.saveLayer(getMapLayer());
		}
	  	
    }
	
	public synchronized void beforeDrawLevel(){
		
	}
	
	public synchronized void beforeRefresh(){
		
	}

	public void setTargets(Action a) throws ActionCancelException{
		if (a.needsItem())
			a.setItem(pickItem(a.getPromptItem()));
		if (a.needsDirection()){
			a.setDirection(pickDirection(a.getPromptDirection()));
		}
		if (a.needsPosition()){
			if (a == target)
				a.setPosition(pickPosition(a.getPromptPosition(), CharKey.f));
			else
				a.setPosition(pickPosition(a.getPromptPosition(), CharKey.SPACE));
		}
		if (a.needsEquipedItem())
			a.setEquipedItem(pickEquipedItem(a.getPromptEquipedItem()));
		if (a.needsMultiItems()){
			a.setMultiItems(pickMultiItems(a.getPromptMultiItems()));
		}
		if (a.needsUnderlyingItem()){
			a.setItem(pickUnderlyingItem(a.getPrompUnderlyingItem()));
		}
	}
	
	private AbstractItem pickUnderlyingItem(String prompt) throws ActionCancelException{
		enterScreen();
  		List items = level.getItemsAt(player.getPosition());
  		if (items == null)
  			return null;
  		if (items.size() == 1)
  			return (AbstractItem) items.get(0);
  		BorderedMenuBox menuBox = new BorderedMenuBox(BORDER1, BORDER2, BORDER3, BORDER4, si, COLOR_WINDOW_BACKGROUND, COLOR_BORDER_IN, COLOR_BORDER_OUT, tileSize, 6,9,12,tileSize, null);
  		menuBox.setBounds(6,4,70,12);
  		menuBox.setMenuItems(items);
  		menuBox.setTitle(prompt);
  		saveMapLayer();
  		//menuBox.draw();
		AbstractItem item = (AbstractItem)menuBox.getSelection();
		
		if (item == null){
			ActionCancelException ret = new ActionCancelException();
			resetMapLayer();
			leaveScreen();
			throw ret;
		}
		resetMapLayer();
		leaveScreen();
		return item;
	}
	

	
	private int [] additionalKeys = new int[]{
				CharKey.N1, CharKey.N2, CharKey.N3, CharKey.N4,
		};
	
	private int [] itemUsageKeys = new int[]{
				CharKey.u, CharKey.e, CharKey.d, CharKey.t,
		};
	
	

 	/**
     * Shows a message inmediately; useful for system
     * messages.
     *  
     * @param x the message to be shown
     */
	public void showMessage(String x){
		messageBox.setForeground(COLOR_LAST_MESSAGE);
		messageBox.setText(x);
		//si.commitLayer(getUILayer());
	}
	
	public void showImportantMessage(String x){
		showMessage(x);
		si.waitKeys(CharKey.ENTER, CharKey.SPACE, CharKey.ESC);
	}
	
	public void showSystemMessage(String x){
		messageBox.setVisible(true);
		messageBox.setForeground(COLOR_LAST_MESSAGE);
		messageBox.setText(x);
		//si.commitLayer(getUILayer());
		si.waitKeys(CharKey.ENTER, CharKey.SPACE, CharKey.ESC);
	}
	
	public void setPlayer(Player pPlayer) {
		super.setPlayer(pPlayer);
		flipFacing = false;
	}
	
    // Runnable interface
	public void run (){}
	
//	IO Utility    
	public void waitKey (){
		CharKey x = new CharKey(CharKey.NONE);
		while (x.code == CharKey.NONE)
			x = si.inkey();
	}

	private void drawStepsTo(int x, int y, Image tile, int cellHeight){
		Position target = new Position(x,y);
		Line line = new Line(PC_POS, target);
		Position tmp = line.next();
		while (!tmp.equals(target)){
			tmp = line.next();
			si.drawImage(getUILayer(), tmp.x*tileSize+13, (tmp.y*tileSize+13)-4*cellHeight, tile);
		}
		
	}
	
	public Vector getMessageBuffer() {
		//return new Vector(messageHistory.subList(0,21));
		if (messageHistory.size()>20)
			return new Vector(messageHistory.subList(messageHistory.size()-21,messageHistory.size()));
		else
			return messageHistory;
	}
	
	public Action selectCommand (CharKey input){
		Command com = getRelatedCommand(input.code);
		informPlayerCommand(com);
		Action ret = actionSelectedByCommand;
		actionSelectedByCommand = null;
		return ret;
	}

	public abstract String getQuitMessage();

	@Override
	public boolean promptChat(String message) {
		return promptChat(message, 20,20,200,100);
	}

	public int switchChat(String title, String prompt, Color titleColor, Color textColor, String... options) {
		final Cursor defaultCursor = getDefaultCursor();
		final Cursor handCursor = getHandCursor();
		BorderedMenuBox selectionBox = new BorderedMenuBox(BORDER1, BORDER2, BORDER3, BORDER4, si, COLOR_WINDOW_BACKGROUND, COLOR_BORDER_IN, COLOR_BORDER_OUT, tileSize, 6,9,12,tileSize+6, null){
			private static final long serialVersionUID = 1L;

			@Override
			protected Cursor getDefaultCursor() {
				if (defaultCursor != null)
					return defaultCursor;
				else
					return super.getDefaultCursor();
			}
			
			@Override
			protected Cursor getHandCursor() {
				if (handCursor != null)
					return handCursor;
				else
					return super.getHandCursor();
			}
			
			@Override
			public int getDrawingLayer() {
				return getUILayer();
			}
		};
   		selectionBox.setItemsPerPage(options.length);
   		if (options.length < 5)
   			selectionBox.setBounds(80, 300, 640,250);
   		else {
   			int add = options.length - 5;
   			selectionBox.setBounds(80, 300 - add * 24, 640,250 + add * 24);
   		}
  		Vector<GFXMenuItem> menuItems = new Vector<GFXMenuItem>();
  		int i = 0;
  		for (String option: options){
  			menuItems.add(new SimpleGFXMenuItem(option,i));
  			i++;
  		}
  		
  		saveMapLayer();
  		
  		selectionBox.setMenuItems(menuItems);
  		selectionBox.setLegend(prompt);
  		selectionBox.setTitle(title);
  		selectionBox.setTitleColor(titleColor);
  		selectionBox.setForeColor(textColor);
  		selectionBox.draw();
  		
		si.commitLayer(getUILayer()); // Commit the drawn box (UNnecessary)
		SimpleGFXMenuItem itemChoice = ((SimpleGFXMenuItem)selectionBox.getSelection());
		if (itemChoice == null){
			resetAndDrawMapLayer();
			return -1;
		} else {
			resetAndDrawMapLayer();
			return itemChoice.getValue();
		}
		
	}
	
		
	public Cursor getHandCursor() {
		return Cursor.getDefaultCursor();
	}

	public Cursor getDefaultCursor() {
		return Cursor.getDefaultCursor();
	}
	

	@Override
	public int switchChat(String title, String prompt, String... options) {
   		return switchChat(title, prompt, COLOR_BOLD, Color.WHITE, options);
	}

	@Override
	public String inputBox(String prompt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void processHelp() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onMusicOn() {
		// TODO Auto-generated method stub
	}

	public void setFlipEnabled(boolean flipEnabled) {
		this.flipEnabled = flipEnabled;
	}
	
	@Override
	public void reset() {
		messageBox.setText("");
		messageHistory.clear();
	}

	
	public static Cursor createCursor (String cursorsFile, int x, int y, int hotX, int hotY){
		Toolkit tk = Toolkit.getDefaultToolkit();
		try {
			Image cursorImage = ImageUtils.crearImagen(cursorsFile , x*24, y*24, 24, 24);
			Cursor c = tk.createCustomCursor(cursorImage, new Point(hotX, hotY), "gfxui-"+x+"-"+y);
			return c;
		} catch (IOException e) {
			SworeGame.crash("Error loading cursors", e);
			return null;
		}
	}
	
	/**
	 * Saves the contents of the map and UI Layer in order to reset 
	 * them after using the UILayer  
	 */
	protected void saveMapLayer(){
		si.saveLayer(getUILayer());
		si.saveLayer(getMapLayer());
	}
	
	protected void resetMapLayer(){
		si.loadLayer(getMapLayer());
		si.loadLayer(getUILayer());
	}
	
	protected void resetAndDrawMapLayer(){
		si.loadAndDrawLayer(getMapLayer());
		si.loadAndDrawLayer(getUILayer());
	}
	
	protected void hideStandardMessageBox(){
		messageBox.setVisible(false);
	}
	
	protected void showStandardMessageBox(){
	}
}



