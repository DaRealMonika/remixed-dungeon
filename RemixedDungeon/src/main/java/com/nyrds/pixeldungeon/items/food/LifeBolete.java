package com.nyrds.pixeldungeon.items.food;

import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.platform.game.Game;
import com.watabou.pixeldungeon.actors.Char;

abstract public class LifeBolete extends Mushroom {
	{
		image = 0;
		message = Game.getVar(R.string.Mushroom_Eat_Message);
	}

	@Override
	protected void applyEffect(Char hero){
		//TODO: + 1 max hp.  А называется он Жизневик
	}
}
