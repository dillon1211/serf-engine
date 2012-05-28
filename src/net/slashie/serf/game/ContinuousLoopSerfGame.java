package net.slashie.serf.game;

import net.slashie.serf.action.Actor;
import net.slashie.serf.ui.UserInterface;


public abstract class ContinuousLoopSerfGame extends SworeGame{
	@Override
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
            boolean acted = actor.act();
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
	
}