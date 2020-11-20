package com.nyrds.pixeldungeon.mobs.spiders;

import com.nyrds.pixeldungeon.ai.MobAi;
import com.nyrds.pixeldungeon.ai.Sleeping;
import com.nyrds.pixeldungeon.mobs.common.MobSpawner;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.mobs.Mob;
import com.watabou.pixeldungeon.items.potions.PotionOfHealing;

public class SpiderNest extends Mob {

	public SpiderNest() {
		
		hp(ht(10));
		baseDefenseSkill = 1;
		baseSpeed = 0f;

		exp = 0;
		maxLvl = 9;
		
		postpone(20);
		
		loot(new PotionOfHealing(), 0.2f);

		movable = false;
	}
	
	@Override
	public int damageRoll() {
		return 0;
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 0;
	}
	
	@Override
    public boolean act(){
		super.act();

		Mob newSpider = MobSpawner.spawnRandomMob(Dungeon.level, getPos());
		
		if(isPet()) {
			Mob.makePet(newSpider, getOwnerId());
		}
		setState(MobAi.getStateByClass(Sleeping.class));

		postpone(20);
		
		return true;
	}
	
	@Override
	public int dr() {
		return 0;
	}

	@Override
	public boolean canBePet() {
		return false;
	}
}
