package net.slashie.serf.ui.oryxUI;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.Serializable;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Timer;

import net.slashie.libjcsi.CharKey;
import net.slashie.serf.action.Action;
import net.slashie.serf.action.ActionSelector;
import net.slashie.serf.action.Actor;
import net.slashie.serf.ui.ActionCancelException;
import net.slashie.serf.ui.UISelector;
import net.slashie.serf.ui.UserAction;
import net.slashie.utils.Position;
import net.slashie.utils.PropertyFilters;

public class ContinuousGFXUISelector extends GFXUISelector implements ActionSelector, Serializable{
	private static final long serialVersionUID = 1L;
	protected transient SwingSystemInterface si;
	private boolean useMouse = false;
	
	protected static final int DONOTHING1_KEY = CharKey.DOT;
	protected static final int DONOTHING2_KEY = CharKey.DOT;
	
	public final int[] QDIRECTIONS = new int[]{
			Action.UPLEFT,
			Action.UP,
			Action.UPRIGHT,
			Action.LEFT,
			Action.SELF,
			Action.RIGHT,
			Action.DOWNLEFT,
			Action.DOWN,
			Action.DOWNRIGHT
		};
	private Cursor[] QCURSORS;
	
	private void initializeCursors (Assets assets){
		QCURSORS = new Cursor[]{
			assets.getCursorAsset("QUADRANT_0_CURSOR"),
			assets.getCursorAsset("QUADRANT_1_CURSOR"),
			assets.getCursorAsset("QUADRANT_2_CURSOR"),
			assets.getCursorAsset("QUADRANT_3_CURSOR"),
			assets.getCursorAsset("QUADRANT_4_CURSOR"),
			assets.getCursorAsset("QUADRANT_5_CURSOR"),
			assets.getCursorAsset("QUADRANT_6_CURSOR"),
			assets.getCursorAsset("QUADRANT_7_CURSOR"),
			assets.getCursorAsset("QUADRANT_8_CURSOR"),
		};
	}
	
	protected int mouseDirection = -1;
	protected Point mousePosition;
	
	// These define the mouse cuadrant
	private int x1;
	private int x2;
	private int y1;
	private int y2;
	
	protected boolean selectionActive = false;
	
	public void init(SwingSystemInterface psi, UserAction[] gameActions, Properties UIProperties,
			Action advance, Action target, Action attack, GFXUserInterface ui, Properties keyBindings, Assets assets){
		super.init(gameActions, advance, target, attack, ui,keyBindings);
		this.si = psi;
		if (UIProperties.getProperty("useMouse").equals("true")){
			useMouse = true;
			psi.addMouseListener(getMouseClickListener());
			psi.addMouseMotionListener(getCursorListener());
		}
		
		si.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (!selectionActive)
					return;
				selectedCharCode = SwingSystemInterface.charCode(e);
			}
		});
		initializeCursors(assets);
		Rectangle r = PropertyFilters.getRectangle(UIProperties.getProperty("mouseQuadrant"));
		x1 = r.x;
		x2 = r.x + r.width;
		y1 = r.y;
		y2 = r.y + r.height;
		
		
	}
	
	protected MouseMotionListener getCursorListener() {
		return new MouseMotionListener(){
			public void mouseDragged(MouseEvent e) {
				int newQuadrant = defineQuadrant(e.getPoint().x, e.getPoint().y);
				if (mouseDirection != -1 && mouseDirection != QDIRECTIONS[newQuadrant-1]){
					mouseDirection = QDIRECTIONS[newQuadrant-1];
				}
				mouseMoved(e);
			}

			public void mouseMoved(MouseEvent e) {
				if (!selectionActive)
					return;
				mousePosition = e.getPoint();
				int newQuadrant = defineQuadrant(e.getPoint().x, e.getPoint().y);
				si.setCursor(QCURSORS[newQuadrant-1]);
			}
		};
	}
	


	protected MouseListener getMouseClickListener() {
		javax.swing.Action gotoDirectionAction = new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!selectionActive)
					return;
				int quadrant = defineQuadrant(mousePosition.x, mousePosition.y);
				selectedMouseDirection = QDIRECTIONS[quadrant-1];
			}
		};
		final Timer gotoDirectionTimer = new Timer(200, gotoDirectionAction);
		return new MouseAdapter() {
			public void mousePressed(final MouseEvent e) {
				if (!selectionActive)
					return;
				if (e.getButton() == MouseEvent.BUTTON1){
					mousePosition = e.getPoint();
					int quadrant = defineQuadrant(mousePosition.x, mousePosition.y);
					selectedMouseDirection = QDIRECTIONS[quadrant-1];
					gotoDirectionTimer.start();
				} else if (e.getButton() == MouseEvent.BUTTON3){
					selectedMousePosition = translatePosition(e.getPoint().x, e.getPoint().y);
				}
			}

			public void mouseReleased(MouseEvent e) {
				mouseDirection = -1;
				gotoDirectionTimer.stop();
			}
			
			private Position tempRel = new Position(0,0);
			private Position translatePosition(int x, int y){
				int bigx = (int)Math.ceil(x/32.0);
				int bigy = (int)Math.ceil(y/32.0);
				tempRel.x = bigx-ui().PC_POS.x-1;
				tempRel.y = bigy-ui().PC_POS.y-1;
				return Position.add(player.getPosition(), tempRel);
			}
		};
	}
	
	public GFXUserInterface ui(){
		return (GFXUserInterface) getUI();
	}
	
	// For polling
	protected int selectedMouseDirection = -1;
	protected Position selectedMousePosition = null;
	protected int selectedCharCode = -1;
	
	protected boolean hasPolling(){
		return selectedCharCode != -1 ||
				selectedMouseDirection != -1 ||
				selectedMousePosition != null;
	}
	public Action selectAction(Actor who) {
	    while (true){
	    	activate();
	    	if (ui().gameOver()){
	    		shutdown();
	    		return null;
	    	}
	    	if (!hasPolling()){
	    		Thread.yield();
	    		continue;
	    	}
	    	deactivate();
	    	if (selectedCharCode != -1){
	    		int key = selectedCharCode;
	    		selectedCharCode = -1;
	    		CharKey input = new CharKey(key);
	    		if (input.code == CharKey.NONE)
	    			continue;
	    		Action ret = ((GFXUserInterface)getUI()).selectCommand(input);
				if (ret != null){
					if (ret.canPerform(player))
	            		return ret;
	            	else 
	            		return null;
				}
				if (input.code == DONOTHING1_KEY) {
					return null;
				}
				if (input.code == DONOTHING2_KEY) {
					return null;
				}
				if (GFXUISelector.isArrow(input)){
					return advanceInDirection(GFXUISelector.toIntDirection(input));
				} else {
					ret = getRelatedAction(input.code);
	            	try {
		            	if (ret != null){
		            		ret.setPerformer(player);
		            		if (ret.canPerform(player))
		            			ui().setTargets(ret);
		            		else {
		            			level.addMessage(ret.getInvalidationMessage());
			            		throw new ActionCancelException();
		            		}
	                    	return ret;
						}
					}catch (ActionCancelException ace){
						//player.getLevel().addMessage("Cancelled Action");
						continue;
					}
				}
	    	} else if (selectedMousePosition != null){
	    		if (!useMouse){
	    			selectedMousePosition = null;
	    			continue;
	    		}
	    		if (target == null){
	    			// No action set up for direct action
	    			selectedMousePosition = null;
	    			continue;
	    		}
	    		Position mousePosition = new Position(selectedMousePosition.x(),selectedMousePosition.y());
	    		selectedMousePosition = null;
				if (level.isValidCoordinate(mousePosition)){
					Action ret = target;
					try {
						ret.setPerformer(player);
						if (ret.canPerform(player))
							ret.setPosition(mousePosition);
						else {
							level.addMessage(ret.getInvalidationMessage());
							throw new ActionCancelException();
						}
                     	mousePosition = null;
                    	return ret;
					} catch (ActionCancelException ace){
						//player.getLevel().addMessage("Cancelled Action");
						continue;
					}
				}
	    	} else if (selectedMouseDirection != -1){
	    		if (!useMouse){
	    			selectedMouseDirection = -1;
	    			continue;
	    		}
	    		int direction = selectedMouseDirection;
	    		selectedMouseDirection = -1;
	    		return advanceInDirection(direction);
	    	}
		}
	}

	public void shutdown() {
		deactivate();
	}

	protected Action advanceInDirection(int direction) {
		Actor vMonster = player.getLevel().getActorAt(Position.add(player.getPosition(), Action.directionToVariation(direction)));
		if (vMonster != null && vMonster.isHostile() && attack.canPerform(player)){
			attack.setDirection(direction);
			return attack;
		} else {
			advance.setDirection(direction);
			switch (direction){
			case Action.UPLEFT:
			case Action.LEFT:
			case Action.DOWNLEFT:
				ui().setFlipFacing(true);
				break;
			case Action.UPRIGHT:
			case Action.RIGHT:
			case Action.DOWNRIGHT:
				ui().setFlipFacing(false);
				break;
			}
			if (advance.canPerform(player)){
				return advance;
			} else {
				player.getLevel().addMessage(advance.getInvalidationMessage());
				return null;
			}
		}
	}
	
	public void activate() {
		selectionActive = true;
	}
	
	public void deactivate() {
		selectionActive = false;
	}

	public String getID(){
		return "UI";
	}
    
	public ActionSelector derive(){
 		return null;
 	}
	
	protected int defineQuadrant(int x, int y){
		if (x > x2)
			if (y > y2)
				return 9;
			else if (y>y1)
				return 6;
			else
				return 3;
		else if (x > x1)
			if (y > y2)
				return 8;
			else if (y>y1)
				return 5;
			else
				return 2;
		else
			if (y > y2)
				return 7;
			else if (y>y1)
				return 4;
			else
				return 1;
	}
	


}