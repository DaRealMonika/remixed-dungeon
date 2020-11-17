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
package com.watabou.pixeldungeon.actors.hero;

import com.nyrds.LuaInterface;
import com.nyrds.Packable;
import com.nyrds.android.util.ModdingMode;
import com.nyrds.generated.BundleHelper;
import com.nyrds.pixeldungeon.items.ItemUtils;
import com.nyrds.pixeldungeon.items.common.ItemFactory;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.utils.CharsList;
import com.nyrds.pixeldungeon.utils.DungeonGenerator;
import com.watabou.noosa.Game;
import com.watabou.pixeldungeon.Badges;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.items.Amulet;
import com.watabou.pixeldungeon.items.EquipableItem;
import com.watabou.pixeldungeon.items.Gold;
import com.watabou.pixeldungeon.items.Item;
import com.watabou.pixeldungeon.items.bags.Bag;
import com.watabou.pixeldungeon.items.food.Food;
import com.watabou.pixeldungeon.items.keys.IronKey;
import com.watabou.pixeldungeon.items.keys.Key;
import com.watabou.pixeldungeon.items.scrolls.ScrollOfCurse;
import com.watabou.pixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.watabou.pixeldungeon.items.wands.Wand;
import com.watabou.pixeldungeon.ui.QuickSlot;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import lombok.var;

public class Belongings implements Iterable<Item>, Bundlable {

	public static final int BACKPACK_SIZE	= 18;

	private Item selectedItem = CharsList.DUMMY_ITEM;

    private final Char owner;
	
	public Bag backpack;

	public Item getSelectedItem() {
		return selectedItem;
	}

	public void setSelectedItem(@NotNull Item selectedItem) {
		this.selectedItem = selectedItem;
	}

	public enum Slot{
		NONE,
		WEAPON,
		LEFT_HAND,
		ARMOR,
		ARTIFACT,
		LEFT_ARTIFACT
	}

	private Map<Slot, EquipableItem> blockedSlots = new HashMap<>();
	public Map<EquipableItem, Slot> usedSlots    = new HashMap<>();

	private Set<EquipableItem> activatedItems = new HashSet<>();

	@Packable(defaultValue = "DUMMY_ITEM")
	@NotNull
	public EquipableItem weapon = CharsList.DUMMY_ITEM;
	@Packable(defaultValue = "DUMMY_ITEM")
	@NotNull
	public EquipableItem leftHand = CharsList.DUMMY_ITEM;
	@Packable(defaultValue = "DUMMY_ITEM")
	@NotNull
	public EquipableItem armor  = CharsList.DUMMY_ITEM;
	@Packable(defaultValue = "DUMMY_ITEM")
	@NotNull
	public EquipableItem ring1  = CharsList.DUMMY_ITEM;
	@Packable(defaultValue = "DUMMY_ITEM")
	@NotNull
	public EquipableItem ring2  = CharsList.DUMMY_ITEM;

	public Belongings( Char owner ) {
		this.owner = owner;
		
		backpack = new Backpack();
		backpack.setOwner(owner);

		collect(new Gold(0));
	}

	public void storeInBundle( Bundle bundle ) {
		backpack.storeInBundle(bundle);
		BundleHelper.Pack(this,bundle);
	}

	@Override
	public boolean dontPack() {
		return false;
	}

	public void restoreFromBundle( Bundle bundle ) {
		
		backpack.clear();
		backpack.restoreFromBundle(bundle);
		BundleHelper.UnPack(this,bundle);

		activateEquippedItems();
	}

	@LuaInterface
	public boolean slotBlocked(String slot) {
		return slotBlocked(Slot.valueOf(slot));
	}

	public boolean slotBlocked(Slot slot) {
		return itemBySlot(slot) != CharsList.DUMMY_ITEM || blockedSlots.containsKey(slot);
	}

	private void blockSlots() {
		blockedSlots.clear();
		var itemIterator = iterator();

		while (itemIterator.hasNextEquipped()) {
			EquipableItem item = (EquipableItem) itemIterator.next();

			blockedSlots.put(item.blockSlot(), item);
		}
	}

	private void activateEquippedItems() {
		var itemIterator = iterator();

		while (itemIterator.hasNextEquipped()) {
			EquipableItem item = (EquipableItem) itemIterator.next();
			if(item!=null) {
				item.setOwner(owner);
				if (!activatedItems.contains(item)) {
					item.activate(owner);
					activatedItems.add(item);
				}
			}
		}
		blockSlots();
	}

	@LuaInterface
	public String itemSlotName(EquipableItem item) {
		if (usedSlots.containsKey(item)) {
			return usedSlots.get(item).name();
		}
		return Slot.NONE.name();
	}

	public boolean isEquipped(@NotNull Item item) {
		return item.equals(weapon) || item.equals(armor) || item.equals(leftHand) || item.equals(ring1) || item.equals(ring2);
	}

	@LuaInterface
	public Item checkItem( Item src ) {
		for (Item item : this) {
			if (item == src ) {
				return item;
			}
		}

		return CharsList.DUMMY_ITEM;
	}

	@LuaInterface
	public Item getItem( String itemClass ) {
		for (Item item : this) {
			if (itemClass.equals( item.getClassName() )) {
				return item;
			}
		}
		return CharsList.DUMMY_ITEM;
	}

	@Deprecated
	@SuppressWarnings("unchecked")
	public<T extends Item> T getItem( Class<T> itemClass ) {

		for (Item item : this) {
			if (itemClass.isInstance( item )) {
				return (T)item;
			}
		}
		return null;
	}


	@SuppressWarnings("unchecked")
	@Nullable
	public <T extends Key> T getKey(Class<T> kind, int depth, @NotNull String levelId) {
		for (Item item : backpack) {
			if (item instanceof Key && item.getClass() == kind) {
				Key key = (Key) item;
				if (levelId.equals(key.levelId)
						|| (Utils.UNKNOWN.equals(key.levelId) && key.getDepth() == depth)) {
					return (T) key;
				}
			}
		}
		return null;
	}

	public int countFood() {
		int ret = 0;

		for (Item item : backpack) {
			if (item instanceof Food) {
				ret+=item.quantity();
			}
		}
		return ret;
	}

	public void countIronKeys() {
		if (getOwner() != Dungeon.hero) {
			return;
		}

		IronKey.curDepthQuantity = 0;

		var levelId = DungeonGenerator.getCurrentLevelId();

		for (Item item : backpack) {
			if (item instanceof IronKey && ((IronKey)item).levelId.equals(levelId)) {
				IronKey.curDepthQuantity++;
			}
		}
	}
	
	public void identify() {
		for (Item item : this) {
			item.identify();
		}
	}
	
	public void observe() {
		var itemIterator = iterator();

		while (itemIterator.hasNextEquipped()) {
			EquipableItem item = (EquipableItem) itemIterator.next();
			item.identify();
			Badges.validateItemLevelAcquired(item);
		}

		for (Item item : backpack) {
			item.setCursedKnown(true);
		}
	}
	
	public void uncurseEquipped() {
		ScrollOfRemoveCurse.uncurse( owner, armor, weapon, leftHand, ring1, ring2 );
	}

	public void curseEquipped() {
		ScrollOfCurse.curse( owner, armor, weapon, leftHand, ring1, ring2 );
	}

	@NotNull
	public Item randomUnequipped() {
		Item ret = Random.element( backpack.items );
		if (ret == null) {
			return CharsList.DUMMY_ITEM;
		}
		return ret;
	}

	public boolean removeItem(Item itemToRemove) {

		if(itemToRemove instanceof EquipableItem && isEquipped(itemToRemove)) {
			var eItem = (EquipableItem) itemToRemove;
			eItem.deactivate(owner);
			usedSlots.remove(eItem);
		}

		itemToRemove.setOwner(CharsList.DUMMY);

		if(itemToRemove.equals(weapon)) {
			weapon = CharsList.DUMMY_ITEM;
			return true;
		}

		if(itemToRemove.equals(armor)) {
			armor = CharsList.DUMMY_ITEM;
			return true;
		}

		if(itemToRemove.equals(leftHand)) {
			leftHand = CharsList.DUMMY_ITEM;
			return true;
		}

		if(itemToRemove.equals(ring1)) {
			ring1 = CharsList.DUMMY_ITEM;
			return true;
		}

		if(itemToRemove.equals(ring2)) {
			ring2 = CharsList.DUMMY_ITEM;
			return true;
		}

		return backpack.remove(itemToRemove);
	}

	public void resurrect( int depth ) {

		for (Item item : backpack.items.toArray(new Item[0])) {
			if (item instanceof Key) {
				if (((Key) item).getDepth() == depth) {
					item.detachAll(backpack);
				}
			} else if (item instanceof Amulet) {

			} else if (!item.isEquipped(owner)) {
				item.detachAll(backpack);
			}
		}

		uncurseEquipped();
		activateEquippedItems();
	}
	
	public int charge( boolean full) {
		
		int count = 0;
		
		for (Item item : this) {
			if (item instanceof Wand) {
				Wand wand = (Wand)item;
				if (wand.curCharges() < wand.maxCharges()) {
					wand.curCharges(full ? wand.maxCharges() : wand.curCharges() + 1);
					count++;

					QuickSlot.refresh(owner);
				}
			}
		}
		
		return count;
	}
	
	public void discharge() {
		for (Item item : this) {
			if (item instanceof Wand) {
				Wand wand = (Wand)item;
				if (wand.curCharges() > 0) {
					wand.curCharges(wand.curCharges() - 1);
					QuickSlot.refresh(owner);
				}
			}
		}
	}

	public void setupFromJson(@NotNull JSONObject desc) throws JSONException {
		try {
			if (desc.has("armor")) {
				armor = (EquipableItem) ItemFactory.createItemFromDesc(desc.getJSONObject("armor"));
			}

			if (desc.has("weapon")) {
				weapon = (EquipableItem) ItemFactory.createItemFromDesc(desc.getJSONObject("weapon"));
			}

			if (desc.has("left_hand")) {
				leftHand = (EquipableItem) ItemFactory.createItemFromDesc(desc.getJSONObject("left_hand"));
			}

			if (desc.has("ring1")) {
				ring1 = (EquipableItem) ItemFactory.createItemFromDesc(desc.getJSONObject("ring1"));
			}

			if (desc.has("ring2")) {
				ring2 = (EquipableItem) ItemFactory.createItemFromDesc(desc.getJSONObject("ring2"));
			}
		} catch (ClassCastException e) {
			throw ModdingMode.modException(e);
		}

		if (desc.has("items")) {
			JSONArray items = desc.getJSONArray("items");
			for (int i = 0; i < items.length(); ++i) {
				Item item = ItemFactory.createItemFromDesc(items.getJSONObject(i));
				collect(item);
			}
		}

		activateEquippedItems();
	}

	@Override
	@NotNull
	public ItemIterator iterator() {
		return new ItemIterator(); 
	}

	@LuaInterface
	@NotNull
	public Char getOwner() {
		return owner;
	}

	private class ItemIterator implements Iterator<Item> {

		private int index = 0;
		
		private Iterator<Item> backpackIterator = backpack.iterator();
		
		private Item[] equipped = {weapon, armor, leftHand, ring1, ring2};

		public boolean hasNextEquipped(){
			for (int i = index; i < equipped.length; i++) {
				if (equipped[i] != CharsList.DUMMY_ITEM && equipped[i] != null) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean hasNext() {
			return hasNextEquipped() || backpackIterator.hasNext();
		}

		@Override
		public Item next() {
			while (index < equipped.length) {
				Item item = equipped[index++];
				if (item != CharsList.DUMMY_ITEM && item != null) {
					return item;
				}
			}
			index++;
			return backpackIterator.next();
		}
	}

	public boolean collect(@NotNull Item newItem) {
		return newItem.collect(backpack);
	}
	
	@Contract(pure = true)
	public static int getBackpackSize(){
		return BACKPACK_SIZE;
	}

	public boolean unequip(EquipableItem item) {
		if(checkItem(item).valid()) {
			removeItem(item);
			activatedItems.remove(item);
			blockSlots();
			owner.updateSprite();
			return true;
		}

		return false;
	}

	public boolean drop(EquipableItem item) {
		if(unequip(item)) {
			item.doDrop(owner);
			return true;
		}
		return false;
	}

	public void dropAll() {
		var itemsToDrop = new ArrayList<Item>();

		for (Item item : this) {
			if (item.quantity()>0) {
				itemsToDrop.add(item);
			}
		}

		for (Item item: itemsToDrop) {
			item.doDrop(owner);
		}
	}

	public boolean isBackpackEmpty() {
		for (Item item : backpack) {
			if (item.quantity()>0) {
				return false;
			}
		}
		return true;
	}


	@NotNull
	public Item itemBySlot(Belongings.Slot slot) {
		switch (slot) {
			case WEAPON:
				return weapon;
			case LEFT_HAND:
				return leftHand;
			case ARMOR:
				return armor;
			case ARTIFACT:
				return ring1;
			case LEFT_ARTIFACT:
				return ring2;
		}
		return CharsList.DUMMY_ITEM;
	}

	public boolean equip(@NotNull EquipableItem item, Slot slot) {
		if(slot==Slot.NONE) {
			return false;
		}

		Item blockingItem = blockedSlots.get(slot);

		if(blockingItem==null) {
			blockingItem = itemBySlot(item.blockSlot());
		}

		if(blockingItem!=CharsList.DUMMY_ITEM) {
			GLog.w(Game.getVar(R.string.Belongings_CantWearBoth),
					item.name(),
					blockingItem.name());
			return false;
		}

		if(slot==Slot.WEAPON) {
			if (weapon.doUnequip( owner, true )) {
				weapon = (EquipableItem) item.detach(backpack);
			} else {
				return false;
			}
		}

		if(slot==Slot.ARMOR) {
			if (armor.doUnequip( owner, true)) {
				armor = (EquipableItem) item.detach(backpack);
			} else {
				return false;
			}
		}

		if(slot==Slot.LEFT_HAND) {
			if (leftHand.doUnequip( owner, true)) {
				leftHand = (EquipableItem) item.detach(backpack);
			} else {
				return false;
			}
		}

		if(slot==Slot.ARTIFACT || slot==Slot.LEFT_ARTIFACT) {
			if (ring1 != CharsList.DUMMY_ITEM && ring2 != CharsList.DUMMY_ITEM) {
				GLog.w(Game.getVar(R.string.Artifact_Limit));
				return false;
			} else {
				if (ring1 == CharsList.DUMMY_ITEM) {
					ring1 = (EquipableItem) item.detach(backpack);;
				} else {
					ring2 = (EquipableItem) item.detach(backpack);;
				}
			}
		}

		item.setCursedKnown(true);
		if(item.isCursed()) {
			ItemUtils.equipCursed( owner );
			item.equippedCursed();
		}

		usedSlots.put(item, slot);

		activateEquippedItems();
		blockSlots();
		QuickSlot.refresh(owner);
		owner.updateSprite();

		owner.spendAndNext(item.time2equip(owner));

		return true;
	}

}
