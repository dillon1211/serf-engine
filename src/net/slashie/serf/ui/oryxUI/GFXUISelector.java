package net.slashie.serf.ui.oryxUI;

import java.util.Properties;

import net.slashie.libjcsi.CharKey;
import net.slashie.serf.action.Action;
import net.slashie.serf.action.ActionSelector;
import net.slashie.serf.action.Actor;
import net.slashie.serf.ui.UISelector;
import net.slashie.serf.ui.UserAction;
import net.slashie.utils.Position;

public abstract class GFXUISelector extends UISelector{
	public abstract void init(SwingSystemInterface psi,
			UserAction[] gameActions, Properties UIProperties, Action advance,
			Action target, Action attack, GFXUserInterface ui,
			Properties keyBindings, Assets assets);

	public abstract GFXUserInterface ui();

	public abstract Action selectAction(Actor who);

	public abstract void activate();

	public abstract void deactivate();

	public abstract String getID();

	public abstract ActionSelector derive();

	public abstract void shutdown();

	public static int toIntDirection(Position what){
		switch (what.x()){
			case 1:
				switch (what.y()){
					case 1:
						return Action.DOWNRIGHT;
					case 0:
						return Action.RIGHT;
					case -1:
						return Action.UPRIGHT;
				}
			case 0:
				switch (what.y()){
					case 1:
						return Action.DOWN;
					case -1:
						return Action.UP;
				}
			case -1:
				switch (what.y()){
					case 1:
						return Action.DOWNLEFT;
					case 0:
						return Action.LEFT;
					case -1:
						return Action.UPLEFT;
				}
		}

		return -1;
	}

}