/*
 * Pixel Dungeon
 * Copyright (C) 2012-2014  Oleg Dolya
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.watabou.pixeldungeon.items.rings;

import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.platform.game.Game;
import com.watabou.pixeldungeon.Badges;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.items.Item;
import com.watabou.utils.Random;

import org.jetbrains.annotations.NotNull;

public class RingOfThorns extends Ring {
	
	@Override
	protected ArtifactBuff buff( ) {
		return new Thorns();
	}
	
	@Override
	public Item random() {
		level(+1);
		return this;
	}
	
	@Override
	public boolean doPickUp(@NotNull Char hero ) {
		identify();
		Badges.validateRingOfThorns();
		Badges.validateItemLevelAcquired( this );
		return super.doPickUp(hero);
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	
	@Override
	public String desc() {
		return isKnown() ? Game.getVar(R.string.RingOfThorns_Info) : super.desc();
	}
	
	public class Thorns extends RingBuff {
		@Override
		public int defenceProc(Char defender, Char enemy, int damage) {
			int dmg = Random.IntRange(0, damage);
			if (dmg > 0) {
				enemy.damage(dmg, this);
			}
			return damage;
		}
	}
}
