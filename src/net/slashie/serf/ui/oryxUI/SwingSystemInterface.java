package net.slashie.serf.ui.oryxUI;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.LineBorder;

import net.slashie.libjcsi.CharKey;
import net.slashie.serf.game.SworeGame;
import net.slashie.utils.ImageUtils;
import net.slashie.utils.Position;
import net.slashie.utils.swing.CallbackKeyListener;
import net.slashie.utils.swing.CallbackMouseListener;

public class SwingSystemInterface implements Runnable{ 
	public void run(){
	}
	private SwingInterfacePanel sip;
	private JPanel componentsPanel;
	private int screenWidth, screenHeight;

	private StrokeNClickInformer aStrokeInformer;
	private Position caretPosition = new Position(0,0);
	private Hashtable<String, Image> images = new Hashtable<String, Image>();
	private BlockingQueue <Integer> inputQueue = new LinkedBlockingQueue<Integer>();
	
	private JFrame frameMain;
	private Point posClic;
	//private int frameRate;

	private CallbackKeyListener<Integer> inputQueueKeyListener;

	public void addMouseListener(MouseListener listener){
		frameMain.removeMouseListener(listener);
		frameMain.addMouseListener(listener);
	}
	
	public void addMouseMotionListener(MouseMotionListener listener){
		frameMain.removeMouseMotionListener(listener);
		frameMain.addMouseMotionListener(listener);
	}
	
	public void setCursor(Cursor c){
		frameMain.setCursor(c);
	}
	
	public void setIcon(Image icon){
		frameMain.setIconImage(icon);
	}
	
	public void setTitle(String title){
		frameMain.setTitle(title);
	}
	
	public void setVisible(boolean bal){
		frameMain.setVisible(bal);
	}
	
	// Initialization
	
	public SwingSystemInterface (){
		this(false, 800, 600);
	}
	
	public SwingSystemInterface (boolean fullScreen, int screenWidth, int screenHeight){
		this(1, fullScreen, screenWidth, screenHeight);
	}
	
	public SwingSystemInterface (int layers, boolean fullScreen, int screenWidth, int screenHeight){
		this (layers, fullScreen, screenWidth, screenHeight, 20);
	}
	
	public SwingSystemInterface (int layers, boolean fullScreen, final int screenWidth, int screenHeight, int fps){
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		if (fullScreen)
			initFullScreen(screenWidth, screenHeight);
		else {
			Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
    		frameMain = new JFrame();
    		frameMain.setBounds((size.width - screenWidth)/2,(size.height-screenHeight)/2,screenWidth,screenHeight);
    		frameMain.getContentPane().setLayout(new GridLayout(1,1));
    		frameMain.setUndecorated(true);
    		frameMain.setVisible(true);
    		frameMain.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    		frameMain.setBackground(Color.BLACK);
    		frameMain.setFocusable(true);
    		frameMain.getContentPane().setLayout(null);
		}
		
		JLayeredPane layeredPane = new JLayeredPane();
		frameMain.getContentPane().add(layeredPane);
		layeredPane.setBounds(0,0,screenWidth,screenHeight);
		
		componentsPanel = new JPanel();
		componentsPanel.setBounds(0,0,screenWidth,screenHeight);
		componentsPanel.setOpaque(false);
		componentsPanel.setLayout(null);
		layeredPane.add(componentsPanel);
		
		sip = new SwingInterfacePanel();
		sip.setBounds(0,0,screenWidth,screenHeight);
		layeredPane.add(sip);
		
			
		aStrokeInformer = new StrokeNClickInformer();
		frameMain.addKeyListener(aStrokeInformer);
		frameMain.addMouseListener(aStrokeInformer);
		sip.init(layers);
	
		inputQueueKeyListener = new CallbackKeyListener<Integer>(inputQueue){
			
			@Override
			public void keyPressed(KeyEvent e) {
				try {
					handler.put(SwingSystemInterface.charCode(e));
				} catch (InterruptedException e1) {}
			}
		};
		
		frameMain.addMouseMotionListener(new MouseMotionListener(){
			public void mouseDragged(MouseEvent e) {
				if (isFullScreen)
					return;
				if (posClic.getY() > 26 || posClic.getX() < screenWidth - 26)
					return;
		        frameMain.setLocation(e.getX()-posClic.x+frameMain.getLocation().x, e.getY()-posClic.y+frameMain.getLocation().y);
			}
			
			public void mouseMoved(MouseEvent e) {}
        	
        });
		frameMain.addMouseListener(new MouseListener(){

			public void mouseClicked(MouseEvent e) {}

			public void mouseEntered(MouseEvent e) {}

			public void mouseExited(MouseEvent e) {}

			public void mousePressed(MouseEvent e) {
				posClic = e.getPoint();
			}

			public void mouseReleased(MouseEvent e) {}
        });
		
		final SwingSystemInterface _si = this;
		/*frameRate = (int)Math.round(1000.0d / (double)fps);
        Timer t = new Timer(frameRate, new ActionListener(){
        	@Override
        	public void actionPerformed(ActionEvent e) {
        		if (sip.isUpdated()){
        			sip.outdate();
        			sip.repaint();
        		}
        	}
        });
        t.start();*/
	}
	
	private boolean isFullScreen = false;
	private Component initFullScreen(int screenWidth, int screenHeight) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gs = ge.getDefaultScreenDevice();
		
		if (gs.isFullScreenSupported()){
			System.out.println("Fullscreen supported");
			
			// frameMain = new JFrame(gs.getDefaultConfiguration());
			frameMain = new JFrame(gs.getDefaultConfiguration());
			frameMain.setBounds(0,0,screenWidth,screenHeight);
			frameMain.getContentPane().setLayout(new GridLayout(1,1));
			frameMain.setUndecorated(true);
			frameMain.setVisible(true);
			frameMain.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frameMain.setBackground(Color.BLACK);
			frameMain.setFocusable(true);
			frameMain.getContentPane().setLayout(null);
			
			//Window win = new Window(frameMain);
			//gs.setFullScreenWindow(win);
			//win.validate();
			gs.setFullScreenWindow(frameMain);
			frameMain.validate();
    		boolean canChg = gs.isDisplayChangeSupported();
    		if (canChg) {
    			System.out.println("Can change screen size");
    	        // Change the screen size and number of colors
    	        DisplayMode displayMode = gs.getDisplayMode();
    	        int bitDepth = 16;
    	        displayMode = new DisplayMode(screenWidth, screenHeight, bitDepth, displayMode.getRefreshRate());
    	        try {
    	            gs.setDisplayMode(displayMode);
    	            isFullScreen = true;
    	        } catch (Throwable e) {
    	        	System.out.println("Desired display mode is not supported; leave full-screen mode");
    	            gs.setFullScreenWindow(null);
    	        }
    	    }
    		try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		return frameMain;
    	} else {
    		Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
    		frameMain = new JFrame();
    		frameMain.setBounds((size.width - screenWidth)/2,(size.height-screenHeight)/2,screenWidth,screenHeight);
    		frameMain.getContentPane().setLayout(new GridLayout(1,1));
    		frameMain.setUndecorated(true);
    		frameMain.setVisible(true);
    		frameMain.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    		frameMain.setBackground(Color.BLACK);
    		frameMain.setFocusable(true);
    		frameMain.getContentPane().setLayout(null);
    		return frameMain;
    	}
	}

	// Drawing Methods
	
	public void cls(int layer){
		sip.cls(layer);
	}
	
	public void drawImage(int layer, String filename){
		Image im = (Image)images.get(filename);
		if (im == null){
			try {
				im = ImageUtils.createImage(filename);
			} catch (Exception e){
				SworeGame.crash("Exception trying to create image "+filename, e);
			}
			images.put(filename, im);
		}
		sip.drawImage(layer, im);
		//sip.repaint();
	}
	
	public void drawImage(int layer, Image image){
		sip.drawImage(layer, image);
	}

	public void printAtPixel(int layer, int x, int y, String text, Color color){
		printAtPixel(layer, x, y, text, color, false);
	}
	
	public void printAtPixel(int layer, int x, int y, String text, Color color, boolean alignRight){
		if (alignRight){
			x -= getTextWidth(layer, text);
		}
		sip.print(layer, x, y, text, color);
	}
	
	public void printAtPixel(int layer, int x, int y, String text){
		sip.print(layer, x, y, text);
	}
	
	
	public void printCentered(int layer, int y, String text, Color color){
		FontMetrics metrics = getDrawingGraphics(layer).getFontMetrics(sip.getFont());
		printAtPixel(layer, (int)(sip.getWidth()/2.0d)-(int)(metrics.stringWidth(text)), y, text, color);
	}

	
	public void print(int layer, int x, int y, String text, Color color){
		sip.print(layer, x*10, y*24, text, color);
	}
	
	public void print(int layer, int x, int y, String text){
		sip.print(layer, x*10, y*24, text);
	}
	
	public /*synchronized*/ void drawImage(int layer, int scrX, int scrY, Image img){
		sip.drawImage(layer, scrX, scrY, img);
	}
	
	public void drawImage(int layer, int scrX, int scrY, String filename){
		//System.out.println("Inadequate drawImage "+filename);
		Image im = (Image)images.get(filename);
		if (im == null){
			try {
				im = ImageUtils.createImage(filename);
			} catch (Exception e){
				SworeGame.crash("Exception trying to create image "+filename, e);
			}
			images.put(filename, im);
		}
		sip.drawImage(layer, scrX, scrY, im);
	}

	public void drawImageCC(int layer, int consoleX, int consoleY, Image img){
		drawImage(layer, consoleX*10, consoleY*24, img);
	}

	public void drawImageCC(int layer, int consoleX, int consoleY, String img){
		drawImage(layer, consoleX*10, consoleY*24, img);
	}
	
	public Graphics2D getDrawingGraphics(int layer){
		return sip.getDrawingGraphics(layer);
	}
	
	public void setFont(int layer, Font fnt){
		sip.setFontFace(fnt, layer);
	}
	
	public void setColor(int layer, Color color){
		sip.setColor(layer, color);
	}
	
	public /*synchronized */ void cleanLayer(int layer){
		sip.cleanLayer(layer);
	}
	
	public void flash(Color c){
		sip.flash(c);
	}
	
	// Board Operations
	public void saveLayer(int layer){
		sip.save(layer);
	}
	
	public void loadLayer(int layer){
		// sip.load(layer);
		loadAndDrawLayer(layer);
	}
	
	public void loadAndDrawLayer(int layer){
		sip.loadAndDraw(layer);
		sip.commit(layer);
	}
	
	
	
	public void backupInBuffer(int buffer, int layer){
		sip.backup(buffer, layer);
	}
	
	public void restoreFromBuffer(int buffer, int layer){
		sip.restoreAndDraw(buffer, layer);
	}
	
	/**
	 * 
	 * @param layer
	 * @param setUpdated Determines if the panel should be redrawn after this commit
	 */
	public void commitLayer(int layer, boolean setUpdated){
		sip.commit(layer, setUpdated);
	}
	
	public void commitLayer(int layer){
		sip.commit(layer, true);
	}
	
	// Input methods
	
	public void waitKey (int keyCode){
		CharKey x = new CharKey(CharKey.NONE);
		while (x.code != keyCode)
			x = inkey();
	}
	
	public void waitKeys (int... keyCodes){
		CharKey x = new CharKey(CharKey.NONE);
		while (true){
			x = inkey();
			for (int keyCode: keyCodes){
				if (x.code == keyCode)
					return;
			}
			
		}
	}
	
	public synchronized CharKey inkey(){
	    /*aStrokeInformer.informKey(Thread.currentThread());
	    try {
			this.wait();
		} catch (InterruptedException ie) {}
		CharKey ret = new CharKey(aStrokeInformer.getInkeyBuffer());
		return ret;*/
		frameMain.addKeyListener(inputQueueKeyListener);
		Integer code = null;
		while (code == null){
			try {
				code = inputQueue.take();
			} catch (InterruptedException e) {}
		}
		frameMain.removeKeyListener(inputQueueKeyListener);
		return new CharKey(code);
	}
	private static final CharKey NULL_CHARKEY = new CharKey(CharKey.NONE);
	public synchronized CharKey inkey(long wait){
	    aStrokeInformer.informKey(Thread.currentThread());
	    try {
			this.wait(wait);
			return NULL_CHARKEY;
		} catch (InterruptedException ie) {}
		CharKey ret = new CharKey(aStrokeInformer.getInkeyBuffer());
		return ret;
	}
	
	public String input(int layer, int xpos,int ypos, Color textColor, int maxLength){
		return input(layer, xpos, ypos, textColor, maxLength, null);
	}
	
	
	public String input(int layer, int xpos,int ypos, Color textColor, int maxLength, String preselectedWord){
		frameMain.addKeyListener(inputQueueKeyListener);
		String ret = "";
		if (preselectedWord != null)
			ret = preselectedWord;
		
		CharKey read = new CharKey(CharKey.NONE);
		saveLayer(layer);
		while (true){
			sip.loadAndDraw(layer);
			printAtPixel(layer, xpos, ypos, ret+"_", textColor);
			commitLayer(layer, true);
			Integer code = null;
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {}
			while (code == null){
				try {
					code = inputQueue.take();
				} catch (InterruptedException e) {}
			}
			read.code = code;
			if (read.code == CharKey.ENTER)
				break;
			if (read.code == CharKey.BACKSPACE){
				if (ret.equals("")){
					read.code = CharKey.NONE;
					continue;
				}
				if (ret.length() > 1)
					ret = ret.substring(0, ret.length() -1);
				else
					ret = "";
                caretPosition.x--;
            }
			else {
				if (ret.length() >= maxLength){
					read.code = CharKey.NONE;
					continue;
				}
				if (!read.isAlphaNumeric() && read.code != CharKey.SPACE){
					read.code = CharKey.NONE;
					continue;
				}
					
				String new_ = read.toString();
				ret += new_;
				caretPosition.x++;
			}
			read.code = CharKey.NONE;
		}
		frameMain.removeKeyListener(inputQueueKeyListener);
		return ret;
	}
	
	public void add(final Component c){
		if (SwingUtilities.isEventDispatchThread()){
			componentsPanel.add(c);
			componentsPanel.validate();
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable(){
					@Override
					public void run() {
						componentsPanel.add(c);
						componentsPanel.validate();
					}
				});
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
		
	public void changeZOrder(final Component c, final int zOrder){
		if (SwingUtilities.isEventDispatchThread()){
			componentsPanel.setComponentZOrder(c, zOrder);
			componentsPanel.validate();
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable(){
					@Override
					public void run() {
						componentsPanel.setComponentZOrder(c, zOrder);
						componentsPanel.validate();
					}
				});
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void remove(final Component c){
		if (SwingUtilities.isEventDispatchThread()){
			componentsPanel.remove(c);
			componentsPanel.validate();
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable(){
					@Override
					public void run() {
						componentsPanel.remove(c);
						componentsPanel.validate();
					}
				});
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void recoverFocus(){
		frameMain.requestFocus();
	}

	public void removeMouseMotionListener(MouseMotionListener listener) {
		frameMain.removeMouseMotionListener(listener);
	}
	
	public void removeMouseListener(MouseListener listener){
		frameMain.removeMouseListener(listener);
	}

	public StrokeNClickInformer getStrokeInformer() {
		return aStrokeInformer;
	}
	
	public static int charCode(KeyEvent x){
    	int code = x.getKeyCode();
    	if(x.isControlDown()) {
			return CharKey.CTRL;
		}
    	if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z){
    		if (x.getKeyChar() >= 'a'){
	    		int diff = KeyEvent.VK_A - CharKey.a;
   		 		return code-diff;
   			} else {
	   			int diff = KeyEvent.VK_A - CharKey.A;
   		 		return code-diff;
   			}
    	}

		switch (x.getKeyCode()){
			case KeyEvent.VK_SPACE:
				return CharKey.SPACE;
			case KeyEvent.VK_COMMA:
				return CharKey.COMMA;
			case KeyEvent.VK_PERIOD: 
				return CharKey.DOT;
			case KeyEvent.VK_DELETE:
				return CharKey.DELETE;
			case KeyEvent.VK_NUMPAD0:
				return CharKey.N0;
			case KeyEvent.VK_NUMPAD1:
				return CharKey.N1;
			case KeyEvent.VK_NUMPAD2:
				return CharKey.N2;
			case KeyEvent.VK_NUMPAD3:
				return CharKey.N3;
			case KeyEvent.VK_NUMPAD4:
				return CharKey.N4;
			case KeyEvent.VK_NUMPAD5:
				return CharKey.N5;
			case KeyEvent.VK_NUMPAD6:
				return CharKey.N6;
			case KeyEvent.VK_NUMPAD7:
				return CharKey.N7;
			case KeyEvent.VK_NUMPAD8:
				return CharKey.N8;
			case KeyEvent.VK_NUMPAD9:
				return CharKey.N9;
			case KeyEvent.VK_0:
				return CharKey.N0;
			case KeyEvent.VK_1:
				return CharKey.N1;
			case KeyEvent.VK_2:
				return CharKey.N2;
			case KeyEvent.VK_3:
				return CharKey.N3;
			case KeyEvent.VK_4:
				return CharKey.N4;
			case KeyEvent.VK_5:
				return CharKey.N5;
			case KeyEvent.VK_6:
				return CharKey.N6;
			case KeyEvent.VK_7:
				return CharKey.N7;
			case KeyEvent.VK_8:
				return CharKey.N8;
			case KeyEvent.VK_9:
				return CharKey.N9;
			case KeyEvent.VK_F1:
				return CharKey.F1;
			case KeyEvent.VK_F2:
				return CharKey.F2;
			case KeyEvent.VK_F3:
				return CharKey.F3;
			case KeyEvent.VK_F4:
				return CharKey.F4;
			case KeyEvent.VK_F5:
				return CharKey.F5;
			case KeyEvent.VK_F6:
				return CharKey.F6;
			case KeyEvent.VK_F7:
				return CharKey.F7;
			case KeyEvent.VK_F8:
				return CharKey.F8;
			case KeyEvent.VK_F9:
				return CharKey.F9;
			case KeyEvent.VK_F10:
				return CharKey.F10;
			case KeyEvent.VK_F11:
				return CharKey.F11;
			case KeyEvent.VK_F12:
				return CharKey.F12;
			case KeyEvent.VK_ENTER:
				return CharKey.ENTER;
			case KeyEvent.VK_BACK_SPACE:
				return CharKey.BACKSPACE;
			case KeyEvent.VK_ESCAPE:
				return CharKey.ESC;
			case KeyEvent.VK_UP:
				return CharKey.UARROW;
			case KeyEvent.VK_DOWN:
				return CharKey.DARROW;
			case KeyEvent.VK_LEFT:
				return CharKey.LARROW;
			case KeyEvent.VK_RIGHT:
				return CharKey.RARROW;
			

		}
		if (x.getKeyChar() == '.')
    		return CharKey.DOT;
    	if (x.getKeyChar() == '?')
    		return CharKey.QUESTION;
		return -1;
	}
	
	public class StrokeNClickInformer extends StrokeInformer implements MouseListener{
		public void mousePressed(MouseEvent e) {
			if (keyListener != null){
				bufferCode = CharKey.NONE;
			    keyListener.interrupt();
			}
		}
		
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
	}

	public void addKeyListener(KeyListener keyListener) {
		frameMain.removeKeyListener(keyListener);
		frameMain.addKeyListener(keyListener);	
	}

	public void removeKeyListener(KeyListener keyListener) {
		frameMain.removeKeyListener(keyListener);
	}

	public Font getFont(int layer) {
		return sip.getGraphicsFont(layer);
	}

	public Cursor getCursor() {
		return frameMain.getCursor();
	}
	
	public BlockingQueue<Integer> getInputQueue(){
		return inputQueue;
	}

	public static Integer charCode(char c) {
		if (c >= 'A' && c <= 'Z'){
			return CharKey.A + c - 'A';
		} else if (c >= 'a' && c <= 'z'){
			return CharKey.a + c - 'a';
		}
		if (c == ' ')
			return CharKey.SPACE;
		return null;
	}

	public void waitKeysOrClick(final int... keyCodes) {
		BlockingQueue<String> waitKeyOrClickHandler = new LinkedBlockingQueue<String>();
		CallbackMouseListener<String> cbml = new CallbackMouseListener<String>(waitKeyOrClickHandler){
			@Override
			public void mousePressed(MouseEvent e) {
				try {
					handler.put("OK");
				} catch (InterruptedException e1) {}
			}
		};
		addMouseListener(cbml);
		CallbackKeyListener<String> cbkl = new CallbackKeyListener<String>(waitKeyOrClickHandler){
			@Override
			public void keyPressed(KeyEvent e) {
				try {
					int pressed = SwingSystemInterface.charCode(e);
					for (int keyCode: keyCodes){
						if (pressed == keyCode){
							handler.put("OK");
							return;
						}
					}
				} catch (InterruptedException e1) {}
			}
		};
		addKeyListener(cbkl);
		String take = null;
		while (take == null){
			try {
				take = waitKeyOrClickHandler.take();
			} catch (InterruptedException e1) {}
		};
		removeMouseListener(cbml);
		removeKeyListener(cbkl);
		
	}

	
	public void revalidate() {
		if (SwingUtilities.isEventDispatchThread()){
			sip.revalidate();
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable(){
					@Override
					public void run() {
						sip.revalidate();
					}
				});
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		
	}

	
	public Point getScreenPosition() {
		return frameMain.getLocationOnScreen();
	}

	
	/*public int getFrameRate() {
		return frameRate;
	}*/

	
	public int getTextWidth(int layer, String text) {
		FontMetrics metrics = getDrawingGraphics(layer).getFontMetrics(getDrawingGraphics(layer).getFont());
		return (int)(metrics.stringWidth(text));
	}

	
	public int getScreenWidth() {
		return screenWidth;
	}
	
	public int getScreenHeight(){
		return screenHeight;
	}
}

class SwingInterfacePanel extends JPanel{
	/**
	 * 
 	 *      Drawing         Layer       Composite 
	 *       Boards          Boards      Board
	 *        ____            ____         ____
	 * draw   \   \  commit   \   \  paint \   \
	 *  --->  \\   \ ------>  \\   \  ----> \   \
	 *         \\___\          \\___\        \___\
	 *          \___\           \___\
	 *                            ^
	 *                            |    
	 *                            |
	 *                            ^
	 *              save - load  / \  backup - restore
	 *                          /   \ 
	 *                    ____ /     \ ____ 
	 *                    \   \        \   \
	 *                    \\   \       \\   \
	 *                     \\___\      \\\___\
	 *                      \___\       \\___\
	 *                                   \___\
	 *              
	 *                    Saved         Backup
	 *                     Boards        Boards
	 */
	
	/**
	 * Composite board: All layers are added here
	 */
	private Image compositeImage;
	private Graphics compositeGraphics;
	
	/**
	 * Drawing boards, this is where all operations are done. They won't be shown until
	 * a commit operation is made over the board
	 */
	private Image[] drawingImages; private Graphics[] drawingGraphics;
	
	/**
	 * Layers boards, which are added up when refreshing to create and print the composite board
	 */
	private Image[] layerImages; private Graphics[] layerGraphics;
	
	/**
	 * Saved Boards: save the status of a layer in order to restore it if needed 
	 */
	private Image[] savedImages; private Graphics[] savedGraphics;

	private boolean updated = true;
	
	public void outdate() {
		updated = false;
	}
	
	private void setUpdated() {
		updated = true;
	}
	
	public boolean isUpdated() {
		return updated;
	}

	/**
	 * Backup boards, they can contain copies of any layer
	 */
	private Image[] backupImages; private Graphics[] backupGraphics;
	
	// Initialization
	
	public SwingInterfacePanel(){
		setLayout(null);
		setBorder(new LineBorder(Color.GRAY));
	}

	public void init(int layers){
		init(layers, 2);
	}

		
	public void init(int layers, int backupBoards){
		layerImages = new Image[layers];
		layerGraphics = new Graphics[layers];
		drawingImages = new Image[layers];
		drawingGraphics = new Graphics[layers];
		savedImages = new Image[layers];
		savedGraphics = new Graphics[layers];
		backupImages = new Image[layers];
		backupGraphics = new Graphics[layers];
		
		compositeImage = createTransparentImage();
		compositeGraphics = compositeImage.getGraphics();
		
		for (int i = 0; i < layers; i++){
			layerImages[i] = createTransparentImage();
			layerGraphics[i] = layerImages[i].getGraphics();
			layerGraphics[i].setColor(Color.WHITE);
			
			drawingImages[i] = createTransparentImage();
			drawingGraphics[i] = drawingImages[i].getGraphics();
			drawingGraphics[i].setColor(Color.WHITE);
			
			savedImages[i] = createTransparentImage();
			savedGraphics[i] = savedImages[i].getGraphics();
			savedGraphics[i].setColor(Color.WHITE);
		}
        
		
        backupImages = new Image[backupBoards];
        backupGraphics = new Graphics[backupBoards];
        for (int i = 0 ; i < backupBoards; i++){
        	backupImages[i] = createTransparentImage();
        	backupGraphics[i] = backupImages[i].getGraphics();
        }
        
        setOpaque(false);
	}
	
	
	// Drawing methods
	public void cls(int layer){
		Color oldColor = drawingGraphics[layer].getColor();
		drawingGraphics[layer].setColor(Color.BLACK);
		drawingGraphics[layer].fillRect(0,0,getWidth(),getHeight());
		drawingGraphics[layer].setColor(oldColor);
	}
	
	public void setColor(int layer, Color color){
		drawingGraphics[layer].setColor(color);
	}
	
	public void setFontFace(Font f, int layer){
		drawingGraphics[layer].setFont(f);
	}
	
	public Font getGraphicsFont(int layer){
		return drawingGraphics[layer].getFont();
	}
	
	public Graphics2D getDrawingGraphics(int layer){
		return (Graphics2D)drawingGraphics[layer];
	}
		
	public void drawImage(int layer, Image img){
		drawImage(layer, 0, 0, img);
	}
	
	public void drawImage(int layer, int scrX, int scrY, Image img){
		drawingGraphics[layer].drawImage(img, scrX, scrY,this);
	}
	
	public void print(int layer, int x, int y, String text){
		print(layer, x, y, text, null);
	}

	public void print(int layer, int x, int y, String text, Color c) {
		Color old = null;
		if (c != null){
			old = drawingGraphics[layer].getColor();
			drawingGraphics[layer].setColor(c);
		}
		if (false){ // TODO
			((Graphics2D)drawingGraphics[layer]).setRenderingHint(
			        RenderingHints.KEY_TEXT_ANTIALIASING,
			        RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
		}
		drawingGraphics[layer].drawString(text, x,y);
		if (c!= null){
			drawingGraphics[layer].setColor(old);
		}
	}
	
	public void cleanLayer(int layer){
		clearImage(drawingGraphics[layer]);
/*
		drawingImages[layer] =  getTransparentImage();
		Font f =drawingGraphics[layer].getFont(); 
		drawingGraphics[layer] = drawingImages[layer].getGraphics();
		drawingGraphics[layer].setFont(f);*/
	}
	
	
	GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	GraphicsDevice gs = ge.getDefaultScreenDevice();
	GraphicsConfiguration gc = gs.getDefaultConfiguration();
	
	private Image createTransparentImage(){
		return gc.createCompatibleImage(getWidth(), getHeight(), Transparency.BITMASK);
	}
	
	Color transparent = new Color(255,255,255,0);
	private void clearImage(Graphics g){
		((Graphics2D)g).setBackground(transparent);
		g.clearRect(0, 0, getWidth(), getHeight());
	}
	
	public void flash(Color c){
		
	}
	
	// Board operations
	
	/*public void save(){
		save(0);
	}*/
	
	public void save(int layer){
		clearImage(savedGraphics[layer]);

		/*savedImages[layer] =  getTransparentImage();
		savedGraphics[layer] = savedImages[layer].getGraphics();*/
		
		savedGraphics[layer].drawImage(layerImages[layer],0,0,this);
	}
	
	/*public void backup (int buffer){
		backup(buffer, 0);
	}*/
	
	public void backup(int buffer, int layer){
		clearImage(backupGraphics[buffer]);
		/*backupImages[buffer] = getTransparentImage();
		backupGraphics[buffer] = backupImages[buffer].getGraphics();*/
		backupGraphics[buffer].drawImage(layerImages[layer],0,0,this);
	}
	
	
	/*public void load(){
		load(0);
	}*/
	
	public synchronized void load(int layer){
		clearImage(layerGraphics[layer]);

		/*layerImages[layer] =  getTransparentImage();
		layerGraphics[layer] = layerImages[layer].getGraphics();*/
		
		layerGraphics[layer].drawImage(savedImages[layer], 0,0,this);
		setUpdated();
	}
	
	

	public void loadAndDraw(int layer){
		clearImage(drawingGraphics[layer]);

		/*drawingImages[layer] =  getTransparentImage();
		Font f = drawingGraphics[layer].getFont(); 
		drawingGraphics[layer] = drawingImages[layer].getGraphics();
		drawingGraphics[layer].setFont(f);*/
		drawingGraphics[layer].drawImage(savedImages[layer], 0,0,this);
	}
	

	/*public void restore(int buffer){
		restore(buffer, 0);
	}*/
	
	public synchronized void restore(int buffer, int layer){
		layerGraphics[layer].drawImage(backupImages[buffer], 0,0,this);
		setUpdated();
	}
	
	public synchronized void restoreAndDraw(int buffer, int layer){
		clearImage(drawingGraphics[layer]);
		/*drawingImages[layer] =  getTransparentImage();
		Font f = drawingGraphics[layer].getFont(); 
		drawingGraphics[layer] = drawingImages[layer].getGraphics();
		drawingGraphics[layer].setFont(f);*/
		drawingGraphics[layer].drawImage(backupImages[buffer], 0,0,this);
	}
	
	/**
	 * NOTE: It's very important for this method to be synchronized to avoid flickering
	 * @param layer
	 */
	public synchronized void commit(int layer){
		commit(layer, true);
	}
	
	/**
	 * NOTE: It's very important for this method to be synchronized to avoid flickering
	 * @param layer
	 */
	public synchronized void commit(int layer, boolean setUpdated){
		clearImage(layerGraphics[layer]);

		// Clean the layer 
		/*layerImages[layer] =  getTransparentImage();
		layerGraphics[layer] = layerImages[layer].getGraphics();*/
		layerGraphics[layer].drawImage(drawingImages[layer], 0,0,this);

		repaint(); // added

		if (setUpdated)
			setUpdated();
	}

	/**
	 * NOTE: It's very important for this method to be synchronized to avoid flickering
	 * @param layer
	 */
	public synchronized void paintComponent(Graphics g){
		if (layerImages != null && compositeGraphics != null){
			/*super.paintComponent(compositeGraphics);
			compositeGraphics.setColor(Color.BLACK);
			compositeGraphics.fillRect(0,0,getWidth(),getHeight());
			for (int i = 0; i < layerImages.length; i++){
				compositeGraphics.drawImage(layerImages[i], 0,0,null);
				if (!((BufferedImage)layerImages[i]).getColorModel().hasAlpha()){
					System.out.println("Woops");
				}
			}
			g.drawImage(compositeImage, 0, 0, null);*/
			super.paintComponent(g);
			g.setColor(Color.BLACK);
			g.fillRect(0,0,getWidth(),getHeight());
			for (int i = 0; i < layerImages.length; i++){
				g.drawImage(layerImages[i], 0,0,null);
			}
		}
	}
}

class StrokeInformer implements KeyListener{
	protected int bufferCode;
	protected transient Thread keyListener;

	public StrokeInformer(){
		bufferCode = -1;
	}

	public void informKey (Thread toWho){
		keyListener = toWho;
	}

	public int getInkeyBuffer(){
		return bufferCode;
	}

	public void keyPressed(KeyEvent e) {
	    bufferCode = SwingSystemInterface.charCode(e);
	    //if (!e.isShiftDown())
	    if (keyListener != null)
		    keyListener.interrupt();
    }

    public void keyReleased(KeyEvent e) {}
    
    public void keyTyped(KeyEvent e) {}
}

