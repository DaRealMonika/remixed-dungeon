package com.watabou.pixeldungeon.actors.mobs;

import com.nyrds.pixeldungeon.ai.MobAi;
import com.nyrds.pixeldungeon.ai.Wandering;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.utils.Random;

public class Shadow extends Mob {
	{
		hp(ht(20));
		baseDefenseSkill = 15;
		
		exp = 5;
		maxLvl = 10;

		walkingType = WalkingType.WALL;

		setState(MobAi.getStateByClass(Wandering.class));
	}

	@Override
	public float speed() {
		return 2;
	}
	
	@Override
	protected float _attackDelay() {
		return 0.5f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 5, 10 );
	}
	
	@Override
	public int attackSkill(Char target) {
		return 10;
	}
}
