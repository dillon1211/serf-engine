package net.slashie.serf.ui.oryxUI;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
import net.slashie.utils.swing.CallbackKeyListener;
import net.slashie.utils.swing.CallbackMouseListener;

public class SynchronizedGFXUISelector extends GFXUISelector implements ActionSelector, Serializable{
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
	
	protected BlockingQueue<String> selectionHandler;
	
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
		selectionHandler = new LinkedBlockingQueue<String>();
		//selectionHandler = new ArrayBlockingQueue<String>(1);
		if (UIProperties.getProperty("useMouse").equals("true")){
			useMouse = true;
			psi.addMouseListener(getMouseClickListener(selectionHandler));
			psi.addMouseMotionListener(getCursorListener());
		}
		
		si.addKeyListener(new CallbackKeyListener<String>(selectionHandler){
			@Override
			public void keyPressed(KeyEvent e) {
				if (!selectionActive)
					return;
				if (!handler.isEmpty())
					return;
				int charcode = SwingSystemInterface.charCode(e);
				try {
					handler.put("KEY:"+charcode);
				} catch (InterruptedException e1) {}
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

	protected MouseListener getMouseClickListener(BlockingQueue<String> selectionHandler_) {
		javax.swing.Action gotoDirectionAction = new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!selectionActive)
					return;
				int quadrant = defineQuadrant(mousePosition.x, mousePosition.y);
				mouseDirection = QDIRECTIONS[quadrant-1];
				try {
					selectionHandler.put("MOUSE_MOVE:"+mouseDirection);
					
				} catch (InterruptedException e1) {}
			}
		};
		final Timer gotoDirectionTimer = new Timer(50, gotoDirectionAction);
		return new CallbackMouseListener<String>(selectionHandler_){
			public void mousePressed(final MouseEvent e) {
				if (!selectionActive)
					return;
				if (e.getButton() == MouseEvent.BUTTON1){
					mousePosition = e.getPoint();
					int quadrant = defineQuadrant(mousePosition.x, mousePosition.y);
					mouseDirection = QDIRECTIONS[quadrant-1];
					try {
						handler.put("MOUSE_MOVE:"+mouseDirection);
						
					} catch (InterruptedException e1) {}
					gotoDirectionTimer.start();
				} else if (e.getButton() == MouseEvent.BUTTON3){
					Position p = translatePosition(e.getPoint().x, e.getPoint().y);
					try {
						handler.put("MOUSE:"+p.x+":"+p.y);
					} catch (InterruptedException e1) {}
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
	
	public Action selectAction(Actor who) {
	    while (true){
	    	activate();
	    	if (ui().gameOver()){
	    		shutdown();
	    		return null;
	    	}
	    	selectionHandler.clear();
	    	String selection = null;
	    	while (selection == null){
	    		try {
					selection = selectionHandler.take();
				} catch (InterruptedException e) {}
	    	}
	    	deactivate();
	    	// System.out.println("Selection "+selection);
	    	String[] commands = selection.split(":");
	    	if (commands[0].equals("KEY")){
	    		int key = Integer.parseInt(commands[1]);
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
				if (SynchronizedGFXUISelector.isArrow(input)){
					return advanceInDirection(toIntDirection(input));
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
	    	} else if (commands[0].equals("MOUSE")){
	    		if (!useMouse){
	    			continue;
	    		}
	    		if (target == null){
	    			// No action set up for direct action
	    			continue;
	    		}
	    		Position mousePosition = new Position(Integer.parseInt(commands[1]),Integer.parseInt(commands[2]));
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
	    	} else if (commands[0].equals("MOUSE_MOVE")){
	    		if (!useMouse){
	    			continue;
	    		}
	    		int direction = Integer.parseInt(commands[1]);
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
		// Empty the selection queue
		selectionHandler.clear();
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