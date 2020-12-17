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
package com.watabou.pixeldungeon.actors.mobs.npcs;

import com.nyrds.android.util.ModdingMode;
import com.nyrds.pixeldungeon.items.ItemUtils;
import com.nyrds.pixeldungeon.items.Treasury;
import com.nyrds.pixeldungeon.mechanics.NamedEntityKind;
import com.nyrds.pixeldungeon.ml.R;
import com.watabou.noosa.Game;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.buffs.Regeneration;
import com.watabou.pixeldungeon.effects.CellEmitter;
import com.watabou.pixeldungeon.effects.particles.ElmoParticle;
import com.watabou.pixeldungeon.items.Gold;
import com.watabou.pixeldungeon.items.Item;
import com.watabou.pixeldungeon.items.bags.Bag;
import com.watabou.pixeldungeon.items.food.Food;
import com.watabou.pixeldungeon.items.food.OverpricedRation;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.sprites.ShopkeeperSprite;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.pixeldungeon.windows.WndBag;
import com.watabou.pixeldungeon.windows.WndOptions;
import com.watabou.pixeldungeon.windows.WndTradeItem;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import lombok.var;

public class Shopkeeper extends NPC {

	{
		spriteClass = ShopkeeperSprite.class;
		movable = false;
		addImmunity(Regeneration.class);
	}

    public static int countFood(Bag backpack) {
        int ret = 0;

        for (Item item : backpack) {
            if (item instanceof Food) {
                ret+=item.quantity();
            }
        }
        return ret;
    }

    @Override
    public boolean act() {

		ItemUtils.throwItemAway(getPos());

		getSprite().turnTo( getPos(), Dungeon.hero.getPos() );
		spend( TICK );
		return true;
	}
	
	@Override
	public void damage(int dmg, @NotNull NamedEntityKind src ) {
		flee();
	}

	private void flee() {
		destroy();
		
		getSprite().killAndErase();
		CellEmitter.get( getPos() ).burst( ElmoParticle.FACTORY, 6 );
	}
	
	@Override
	public boolean reset() {
		return true;
	}

	private WndBag.Listener sellItemSelector = new WndBag.Listener() {
		@Override
		public void onSelect(Item item, Char selector) {
			if (item != null) {

				if(item instanceof Bag && !((Bag)item).items.isEmpty()) {
					GameScene.selectItemFromBag(sellItemSelector, (Bag)item , WndBag.Mode.FOR_SALE, Game.getVar(R.string.Shopkeeper_Sell));
					return;
				}

				GameScene.show( new WndTradeItem( item, Shopkeeper.this, false) );
			}
		}
	};

	private WndBag.Listener buyItemSelector = (item, selector) -> {
		if (item != null) {
			GameScene.show( new WndTradeItem( item, Shopkeeper.this, true) );
		}
	};

	@Override
	public boolean interact(final Char hero) {

		int attempts = 0;

		if(!ModdingMode.inMod() && Game.getDifficulty() < 2) {
			if (countFood(getBelongings().backpack) < 3) {
				var foodSupply = new OverpricedRation();
				foodSupply.quantity(5);
				addItem(foodSupply);
			}
		}

		while(getBelongings().backpack.items.size() < getBelongings().backpack.getSize() + 2 && attempts < 100) {
			generateNewItem();
			attempts++;
		}

		Collections.shuffle(getBelongings().backpack.items);

		GameScene.show(new WndOptions(Utils.capitalize(getName()),
								Game.getVar(R.string.Shopkeeper_text),
								Game.getVar(R.string.Shopkeeper_SellPrompt),
								Game.getVar(R.string.Shopkeeper_BuyPrompt)){
			@Override
			public void onSelect(int index) {
				WndBag wndBag = null;

				switch (index) {
					case 0:
						wndBag = new WndBag(hero.getBelongings(),hero.getBelongings().backpack,sellItemSelector,WndBag.Mode.FOR_SALE, Game.getVar(R.string.Shopkeeper_Sell));
						break;
					case 1:
						wndBag = new WndBag(getBelongings(), getBelongings().backpack,buyItemSelector,WndBag.Mode.FOR_BUY, Game.getVar(R.string.Shopkeeper_Buy));
						break;
				}

				if(wndBag!=null) {
					GameScene.show(wndBag);
				}
			}
		});
		return true;
	}

	public void generateNewItem()
	{
		Item newItem = Treasury.getLevelTreasury().random();

		if(newItem instanceof Gold) {
			return;
		}

		if(newItem.isCursed()) {
			return;
		}

		var supply = getBelongings().getItem(newItem.getEntityKind());

		if(!newItem.stackable && supply.valid()) {
			return;
		}

		if(newItem.stackable && supply.valid() && supply.price() > 100) {
			return;
		}

		addItem(newItem);
	}

	public void addItem(Item item) {
		if(item instanceof Bag && Dungeon.hero != null) {
			if(Dungeon.hero.getBelongings().getItem(item.getClassName()).valid()) {
				return;
			}
		}

		if(level()!=null) {
			item = Treasury.getLevelTreasury().check(item);
		} else {
			item = Treasury.get().check(item);
		}
		item.collect(this);
	}

	@Override
	public boolean useBags() {
		return false;
	}
}
