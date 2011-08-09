package net.slashie.serf.ui.oryxUI.effects;

import java.awt.image.BufferedImage;

import net.slashie.serf.action.Action;
import net.slashie.serf.ui.oryxUI.GFXUserInterface;
import net.slashie.serf.ui.oryxUI.SwingSystemInterface;
import net.slashie.utils.Position;

public class GFXAnimatedMeleeEffect extends GFXDirectionalEffect{
	private BufferedImage[][] frames;
	private Position[][] variations;

	public void drawEffect(GFXUserInterface ui, SwingSystemInterface si){
		BufferedImage[] sequence = null;
		Position[] vars = null;
		si.saveLayer();
		setAnimationDelay(animationDelay);
		
		switch (direction){
		case Action.RIGHT:
			sequence = frames[4];
			vars = variations[4];
			break;
		case Action.UP:
			sequence = frames[1];
			vars = variations[1];
			break;
		case Action.LEFT:
			sequence = frames[3];
			vars = variations[3];
			break;
		case Action.DOWN:
			sequence = frames[6];
			vars = variations[6];
			break;
		case Action.DOWNRIGHT:
			sequence = frames[7];
			vars = variations[7];
			break;
		case Action.DOWNLEFT:
			sequence = frames[5];
			vars = variations[5];
			break;
		case Action.UPLEFT:
			sequence = frames[0];
			vars = variations[0];
			break;
		case Action.UPRIGHT:
			sequence = frames[2];
			vars = variations[2];
			break;
		}
		if (sequence == null){
			si.loadLayer();
			si.refresh();
			return;
		}
			
		Position runner = getPosition();
		for (int i = 0; i < depth; i++){
			int height = 0;
			if (ui.getPlayer().getLevel().getMapCell(runner) != null)
				height = ui.getPlayer().getLevel().getMapCell(runner).getHeight();
			Position relative = Position.subs(runner, ui.getPlayer().getPosition());
			Position toPrint = Position.add(ui.PC_POS, relative);
			/*if (!ui.insideViewPort(toPrint))
				break;*/
			toPrint = new Position(toPrint.x()*32, toPrint.y()*32-4*height);
			toPrint = Position.add(toPrint, vars[i]);
			si.drawImage(toPrint.x(), toPrint.y(), sequence[i]);
			si.refresh();
			animationPause();
			si.loadLayer();
		}
		
	}

	public GFXAnimatedMeleeEffect(String ID, BufferedImage[][] frames, Position vars[][], int delay){
		super(ID, delay);
		setMissile(frames);
		variations = vars;
	}

	public void setMissile(BufferedImage[][] frames) {
		this.frames = frames;
	} 
}