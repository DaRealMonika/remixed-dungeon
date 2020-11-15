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
package com.watabou.pixeldungeon.actors;

import com.nyrds.LuaInterface;
import com.nyrds.Packable;
import com.nyrds.android.util.Scrambler;
import com.nyrds.android.util.TrackedRuntimeException;
import com.nyrds.pixeldungeon.items.ItemOwner;
import com.nyrds.pixeldungeon.levels.objects.LevelObject;
import com.nyrds.pixeldungeon.levels.objects.Presser;
import com.nyrds.pixeldungeon.mechanics.HasPositionOnLevel;
import com.nyrds.pixeldungeon.mechanics.LevelHelpers;
import com.nyrds.pixeldungeon.mechanics.NamedEntityKind;
import com.nyrds.pixeldungeon.mechanics.buffs.RageBuff;
import com.nyrds.pixeldungeon.ml.BuildConfig;
import com.nyrds.pixeldungeon.ml.EventCollector;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.mobs.common.CustomMob;
import com.nyrds.pixeldungeon.utils.CharsList;
import com.nyrds.pixeldungeon.utils.EntityIdSource;
import com.nyrds.pixeldungeon.utils.Position;
import com.watabou.noosa.Game;
import com.watabou.noosa.StringsManager;
import com.watabou.noosa.audio.Sample;
import com.watabou.pixeldungeon.Assets;
import com.watabou.pixeldungeon.CommonActions;
import com.watabou.pixeldungeon.Dungeon;
import com.watabou.pixeldungeon.ResultDescriptions;
import com.watabou.pixeldungeon.actors.buffs.Buff;
import com.watabou.pixeldungeon.actors.buffs.BuffCallback;
import com.watabou.pixeldungeon.actors.buffs.CharModifier;
import com.watabou.pixeldungeon.actors.buffs.Frost;
import com.watabou.pixeldungeon.actors.buffs.Fury;
import com.watabou.pixeldungeon.actors.buffs.Hunger;
import com.watabou.pixeldungeon.actors.buffs.Levitation;
import com.watabou.pixeldungeon.actors.buffs.Light;
import com.watabou.pixeldungeon.actors.buffs.Paralysis;
import com.watabou.pixeldungeon.actors.buffs.Roots;
import com.watabou.pixeldungeon.actors.buffs.Slow;
import com.watabou.pixeldungeon.actors.buffs.Speed;
import com.watabou.pixeldungeon.actors.buffs.Vertigo;
import com.watabou.pixeldungeon.actors.hero.Belongings;
import com.watabou.pixeldungeon.actors.hero.CharAction;
import com.watabou.pixeldungeon.actors.hero.Hero;
import com.watabou.pixeldungeon.actors.hero.HeroClass;
import com.watabou.pixeldungeon.actors.hero.HeroSubClass;
import com.watabou.pixeldungeon.actors.mobs.Boss;
import com.watabou.pixeldungeon.actors.mobs.Fraction;
import com.watabou.pixeldungeon.actors.mobs.Mob;
import com.watabou.pixeldungeon.actors.mobs.WalkingType;
import com.watabou.pixeldungeon.effects.Speck;
import com.watabou.pixeldungeon.items.EquipableItem;
import com.watabou.pixeldungeon.items.Gold;
import com.watabou.pixeldungeon.items.Item;
import com.watabou.pixeldungeon.items.weapon.melee.KindOfBow;
import com.watabou.pixeldungeon.items.weapon.missiles.Arrow;
import com.watabou.pixeldungeon.items.weapon.missiles.MissileWeapon;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.levels.Terrain;
import com.watabou.pixeldungeon.levels.features.Door;
import com.watabou.pixeldungeon.mechanics.ShadowCaster;
import com.watabou.pixeldungeon.scenes.CellSelector;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.scenes.InterlevelScene;
import com.watabou.pixeldungeon.sprites.CharSprite;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;

import static com.watabou.pixeldungeon.Dungeon.level;

public abstract class Char extends Actor implements HasPositionOnLevel, Presser, ItemOwner, NamedEntityKind {

    public static final String IMMUNITIES        = "immunities";
	public static final String RESISTANCES       = "resistances";
    public MissileWeapon rangedWeapon = null;

	public Char enemy;

	@NotNull
	protected ArrayList<Char> visibleEnemies = new ArrayList<>();

	private Belongings belongings;


	@Packable(defaultValue = "-1")//EntityIdSource.INVALID_ID
	private int owner = EntityIdSource.INVALID_ID;

	@Packable(defaultValue = "-1")//Level.INVALID_CELL
	@Getter
	private int pos     = Level.INVALID_CELL;

	private int prevPos = Level.INVALID_CELL;

	@Packable(defaultValue = "-1")//EntityIdSource.INVALID_ID
	@Getter
	private int id = EntityIdSource.INVALID_ID;

	public  Fraction fraction = Fraction.DUNGEON;

	protected CharSprite sprite;

	@Getter
	protected String name           = Game.getVar(R.string.Char_Name);
	@Getter
	protected String name_objective = Game.getVar(R.string.Char_Name_Objective);

	@Getter
	protected String description = Game.getVar(R.string.Mob_Desc);
	@Getter
	protected String defenceVerb = null;
	@Getter
	protected int gender = Utils.NEUTER;

	protected WalkingType walkingType = WalkingType.NORMAL;

	private int HT;
	private int HP;

	protected float baseSpeed = 1;
	protected boolean movable = true;

	public    boolean paralysed = false;
	public    boolean pacified  = false;
	protected boolean flying    = false;
	public    int     invisible = 0;

	private int viewDistance = 8;

	protected Set<String> immunities = new HashSet<>();
	protected Set<String> resistances = new HashSet<>();

	protected Set<Buff> buffs = new HashSet<>();

	private Map<String, Number> spellsUsage = new HashMap<>();

	public CharAction curAction = null;

	//TODO store&restore it for all chars
	private int lvl = Scrambler.scramble(1);

	public Char() {
	}

	public boolean canSpawnAt(Level level,int cell) {
		return walkingType.canSpawnAt(level, cell) && level.getTopLevelObject(cell) == null && level.map[cell] != Terrain.ENTRANCE;
	}

	public int respawnCell(Level level) {
		return walkingType.respawnCell(level);
	}

	public void spendAndNext(float time) {
		spend(time);
		next();
	}

	@Override
	public boolean act() {
		level().updateFieldOfView(this);

		forEachBuff(CharModifier::charAct);

		for(Item item: getBelongings()) {
			item.charAct();
		}

		return false;
	}

	private static final String TAG_HP        = "HP";
	private static final String TAG_HT        = "HT";
	private static final String BUFFS         = "buffs";
	private static final String SPELLS_USAGE  = "spells_usage";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);

		bundle.put(TAG_HP, hp());
		bundle.put(TAG_HT, ht());
		bundle.put(BUFFS, buffs);
		bundle.put(SPELLS_USAGE, spellsUsage);

		getBelongings().storeInBundle(bundle);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {

		super.restoreFromBundle(bundle);

		if(id!=EntityIdSource.INVALID_ID) {
            CharsList.add(this, id);
        }

		hp(bundle.getInt(TAG_HP));
		ht(bundle.getInt(TAG_HT));

		for (Buff b : bundle.getCollection(BUFFS, Buff.class)) {
			b.attachTo(this);
		}

		spellsUsage = bundle.getMap(SPELLS_USAGE);

		setupCharData();

		getBelongings().restoreFromBundle(bundle);
	}

	private String getClassParam(String paramName, String defaultValue, boolean warnIfAbsent) {
		return Utils.getClassParam(this.getClass().getSimpleName(), paramName, defaultValue, warnIfAbsent);
	}

	protected void setupCharData() {
        ///freshly created char or pre 28.6 save
        if(id==EntityIdSource.INVALID_ID) {
            id = EntityIdSource.getNextId();
            CharsList.add(this,id);
        }

		if(getOwnerId() < 0) { //fix pre 29.4.fix.6 saves
			setOwnerId(id);
		}

		setBelongings(new Belongings(this));

		if(this instanceof CustomMob) {
			return;
		}

		name = getClassParam("Name", name, true);
		name_objective = getClassParam("Name_Objective", name, true);

		if(this instanceof Hero) {
			return;
		}

		description = getClassParam("Desc", description, true);
		gender = Utils.genderFromString(getClassParam("Gender", "masculine", true));
		defenceVerb = getClassParam("Defense", null, false);
	}

	@LuaInterface
	public void yell(String str) {
        GLog.n(Game.getVar(R.string.Mob_Yell), getName(), StringsManager.maybeId(str));
    }

    @LuaInterface
    public void say(String str) {
        GLog.i(Game.getVar(R.string.Mob_Yell), getName(), StringsManager.maybeId(str));
    }

    @LuaInterface
    public void yell(String str, int index) {
        GLog.n(Game.getVar(R.string.Mob_Yell), getName(), StringsManager.maybeId(str,index));
    }

    @LuaInterface
    public void say(String str, int index) {
        GLog.i(Game.getVar(R.string.Mob_Yell), getName(), StringsManager.maybeId(str,index));
    }

    public boolean ignoreDr() {
		return false;
	}

    public boolean attack(@NotNull Char enemy) {

		if (enemy.invalid()) {
			EventCollector.logException(getEntityKind() + " attacking dummy enemy");
			return false;
		}

		if(!level().cellValid(enemy.getPos())) {
			EventCollector.logException(getEntityKind() + " attacking " +enemy.getEntityKind() + "on invalid cell" );
			return false;
		}

		boolean visibleFight = CharUtils.isVisible(this) || CharUtils.isVisible(enemy);

		if (hit(this, enemy, false)) {

			if (visibleFight) {
				GLog.i(Game.getVars(R.array.Char_Hit)[gender], name, enemy.getName_objective());
			}

			int dmg = damageRoll();

			if(inFury()) {
				dmg *= 1.5f;
			}

			int effectiveDamage = Math.max(dmg, 0);

			effectiveDamage = attackProc(enemy, effectiveDamage);
			effectiveDamage = enemy.defenseProc(this, effectiveDamage);
			enemy.damage(effectiveDamage, this);

			if (visibleFight) {
                Sample.INSTANCE.play(Assets.SND_HIT, 1, 1, Random.Float(0.8f, 1.25f));

                enemy.getSprite().bloodBurstA(
                        getSprite().center(), effectiveDamage);
                enemy.getSprite().flash();
            }

			if (!enemy.isAlive() && visibleFight) {
				if (enemy == Dungeon.hero) {

					if (Dungeon.hero.killerGlyph != null) {

						Dungeon.fail(Utils.format(ResultDescriptions.getDescription(ResultDescriptions.Reason.GLYPH), Dungeon.hero.killerGlyph.name(), Dungeon.depth));
						GLog.n(Game.getVars(R.array.Char_Kill)[Dungeon.hero.gender], Dungeon.hero.killerGlyph.name());

					} else {
						if (this instanceof Boss) {
							Dungeon.fail(Utils.format(ResultDescriptions.getDescription(ResultDescriptions.Reason.BOSS), name, Dungeon.depth));
						} else {
							Dungeon.fail(Utils.format(ResultDescriptions.getDescription(ResultDescriptions.Reason.MOB),
									Utils.indefinite(name), Dungeon.depth));
						}

						GLog.n(Game.getVars(R.array.Char_Kill)[gender], name);
					}

				} else {
					GLog.i(Game.getVars(R.array.Char_Defeat)[gender], name, enemy.getName_objective());
				}
			}

			return true;

		} else {

			if (visibleFight) {
				String defense = enemy.defenseVerb();
				enemy.getSprite().showStatus(CharSprite.NEUTRAL, defense);
				if (this == Dungeon.hero) {
					GLog.i(Game.getVar(R.string.Char_YouMissed), enemy.name, defense);
				} else {
					GLog.i(Game.getVar(R.string.Char_SmbMissed), enemy.name, defense, name);
				}

				Sample.INSTANCE.play(Assets.SND_MISS);
			}

			return false;

		}
	}

	public static boolean hit(@NotNull Char attacker, Char defender, boolean magic) {
		if(attacker.invisible>0) {
			return true;
		}

		float acuRoll = Random.Float(attacker.attackSkill(defender));
		float defRoll = Random.Float(defender.defenseSkill(attacker));
		return (magic ? acuRoll * 2 : acuRoll) >= defRoll;
	}

    public boolean shoot(Char enemy, MissileWeapon wep) {

        rangedWeapon = wep;
        boolean result = attack(enemy);
        rangedWeapon = null;

        return result;
    }

	public boolean bowEquipped() {
		return getBelongings().weapon instanceof KindOfBow;
	}

	public int attackSkill(Char target) {
		return 0;
	}

	public int defenseSkill(Char enemy) {
		return 0;
	}

	public String defenseVerb() {
		if (defenceVerb != null) {
			return defenceVerb;
		}
		return Game.getVars(R.array.Char_StaDodged)[gender];
	}


	public int defenceRoll(Char enemy) {
		if(enemy.ignoreDr()) {
			return 0;
		}

		final int[] dr = {dr()};
		forEachBuff(b-> dr[0] +=b.drBonus());
		return  Random.IntRange(0, dr[0]);
	}

	public int dr() {
		return getBelongings().armor.effectiveDr();
	}

	protected boolean inFury() {
		return (hasBuff(Fury.class) || hasBuff(RageBuff.class) );
	}

	public boolean actMeleeAttack(Char enemy) {

		if (canAttack(enemy)) {
			spend(attackDelay());
			getSprite().attack(enemy.getPos());

			return false;
		}
		return getCloserIfVisible(enemy.getPos());
	}

	public boolean canAttack(@NotNull Char enemy) {
		return false;
	}

	public boolean actBowAttack(Char enemy) {

		KindOfBow kindOfBow = (KindOfBow) getBelongings().weapon;

		Class<? extends Arrow> arrowType = kindOfBow.arrowType();

		Arrow arrow = getBelongings().getItem(arrowType);
		if(arrow==null || arrow.quantity() == 0) {
			arrow = getBelongings().getItem(Arrow.class);
		}

		if (arrow != null && arrow.quantity() > 0) { // We have arrows!
			arrow.cast(this, enemy.getPos());
			ready();
			return false;
		} // no arrows? Go Melee

		return actMeleeAttack(enemy);
	}

	private void ready() {
	}

	public boolean getCloserIfVisible(int pos) {
		if (level.fieldOfView[pos] && getCloser(pos)) {
			return true;
		} else {
			readyAndIdle();
			return false;
		}
	}

	public int attackProc(@NotNull Char enemy, int damage) {
		final int[] dmg = {damage};
		forEachBuff(b->dmg[0] = b.attackProc(this, enemy, dmg[0]));
		return dmg[0];
	}

	public int defenseProc(Char enemy, int baseDamage) {
		int dr = defenceRoll(enemy);
		final int[] damage = {baseDamage - dr};
		forEachBuff(b->damage[0] = b.defenceProc(this, enemy, damage[0]));
		return getBelongings().armor.defenceProc(enemy, this, damage[0]);
	}

	@NotNull
	public EquipableItem getActiveWeapon() {
		if(rangedWeapon!=null) {
			return rangedWeapon;
		}

		return getBelongings().weapon;
	}

	@NotNull
	public EquipableItem getSecondaryWeapon() {
	    EquipableItem leftItem = getBelongings().leftHand;

	    if(leftItem.goodForMelee()) {
	        return leftItem;
        }

	    return CharsList.DUMMY_ITEM;
    }

	public int damageRoll() {
		int dmg = effectiveSTR() > 10 ? Random.IntRange(1, effectiveSTR() - 9) : 1;

		dmg += getActiveWeapon().damageRoll(this);

		if(rangedWeapon!=null) {
			dmg += getSecondaryWeapon().damageRoll(this);
		}

		return dmg;
	}

	public float speed() {
		final float[] speed = {baseSpeed};
		forEachBuff(b-> speed[0] *=b.speedMultiplier());

		return speed[0];
	}


	public void heal(int heal, NamedEntityKind src) {
		heal(heal, src, false);
	}

	public void heal(int heal, NamedEntityKind src, boolean noAnim) {
        if (!isAlive()) {
            return;
        }

        heal = resist(heal, src);
        heal = Math.min(ht()-hp(),heal);

        if(heal<=0) {
        	return;
		}

        if(BuildConfig.DEBUG) {
        	GLog.i("%s <- heal %d (%s)", getEntityKind(), heal, src.getEntityKind());
		}

        hp(hp() + heal);

        if(!noAnim && heal > 0) {
			getSprite().emitter().burst(Speck.factory(Speck.HEALING), Math.max(1, heal * 5 / ht()));
		}
    }

	public void damage(int dmg, @NotNull NamedEntityKind src) {

		if(BuildConfig.DEBUG){
			GLog.i("%s: <- %d dmg from %s", getEntityKind(), dmg, src.getEntityKind());
		}

		if (!isAlive()) {
			return;
		}

		Buff.detach(this, Frost.class);

        dmg = resist(dmg, src);

        if (hasBuff(Paralysis.class)) {
			if (Random.Int(dmg) >= Random.Int(hp())) {
				Buff.detach(this, Paralysis.class);
				if (CharUtils.isVisible(this)) {
					GLog.i(Game.getVar(R.string.Char_OutParalysis), getName_objective());
				}
			}
		}

        if(dmg<0) {
        	return;
		}

		hp(hp() - dmg);
		if (dmg > 0 || src instanceof Char) {
			getSprite().showStatus(hp() > ht() / 2 ?
							CharSprite.WARNING :
							CharSprite.NEGATIVE,
					Integer.toString(dmg));
		}
		if (hp()<=0) {
			die(src);
		}
	}

    private int resist(int dmg, @NotNull NamedEntityKind src) {
        String srcName = src.getEntityKind();
        if (immunities().contains(srcName)) {
            dmg = 0;
        } else if (resistances().contains(srcName)) {
            dmg = Random.IntRange(0, dmg);
        }
        return dmg;
    }

    public void destroy() {
		hp(0);
		Actor.remove(this);

        for (Buff buff : buffs.toArray(new Buff[0])) {
            buff.detach();
        }

		Actor.freeCell(getPos());
	}

	public void die(NamedEntityKind src) {
		destroy();
		if(level().pit[getPos()]) {
			getSprite().fall();
		} else {
			getSprite().die();
		}
	}

	public boolean isAlive() {
		return hp() > 0;
	}

	abstract protected float _attackDelay();

	public float attackDelay() {
		float delayFactor = 1;

		delayFactor = getActiveWeapon().impactDelayFactor(this,delayFactor);
		delayFactor = getSecondaryWeapon().impactDelayFactor(this, delayFactor);

		return _attackDelay() * delayFactor;
	}

	@Override
	public void spend(float time) {

		float timeScale = 1f;
		timeScale *= 1.f/(1+buffLevel(Slow.class));
		timeScale *= (1 + buffLevel(Speed.class));

		float scaledTime = time / timeScale;

		for(Map.Entry<String,Number> spell:spellsUsage.entrySet()) {
			spell.setValue(spell.getValue().floatValue()+scaledTime);
		}

		super.spend(scaledTime);
	}

	public Set<Buff> buffs() {
		return buffs;
	}

	@SuppressWarnings("unchecked")
	public <T extends Buff> HashSet<T> buffs(Class<T> c) {
		HashSet<T> filtered = new HashSet<>();
		for (Buff b : buffs) {
			if (c.isInstance(b)) {
				filtered.add((T) b);
			}
		}
		return filtered;
	}

	@SuppressWarnings("unchecked")
	public <T extends Buff> T buff(Class<T> c) {
		for (Buff b : buffs) {
			if (c.isInstance(b)) {
				return (T) b;
			}
		}
		return null;
	}


	@LuaInterface
	public Buff buff(String buffName) {
		for (Buff b : buffs) {
			if (buffName.equals(b.getEntityKind())) {
				return b;
			}
		}
		return null;
	}

	@LuaInterface
	public int buffLevel(String buffName) {
		int level = 0;
		for (Buff b : buffs) {
			if (buffName.equals(b.getEntityKind())) {
				level += b.level();
			}
		}
		return level;
	}

	@Deprecated
	public int buffLevel(Class<? extends Buff> c) {
		int level = 0;
		for (Buff b : buffs) {
			if (c.isInstance(b)) {
				level += b.level();
			}
		}
		return level;
	}

	@Deprecated
	public boolean hasBuff(Class<? extends Buff> c) {
		for (Buff b : buffs) {
			if (c.isInstance(b)) {
				return true;
			}
		}
		return false;
	}

	public void add(Buff buff) {
		if(!isAlive()) {
			return;
		}

		GLog.debug("%s added to %s", buff.getEntityKind(), getEntityKind());

		buffs.add(buff);
		Actor.add(buff);

		if (!GameScene.isSceneReady()) {
			return;
		}

		buff.attachVisual();
	}

	public void remove(@Nullable Buff buff) {
		buffs.remove(buff);
		Actor.remove(buff);

		if(buff!=null) {
			GLog.debug("%s removed from %s", buff.getEntityKind(), getEntityKind());
		}

		if(buff!=null && sprite != null) {
			sprite.remove(buff.charSpriteStatus());
		}
	}

	@NotNull
	public Hunger hunger() {
		if(!(this instanceof Hero)) { //fix it later
			return new Hunger();
		}

		if(!isAlive()) {
			return new Hunger();
		}

		Hunger hunger = buff(Hunger.class);

		if(hunger == null) {
			EventCollector.logEvent("null hunger on alive Char!");
			hunger = new Hunger();
			hunger.attachTo(this);
		}

		return hunger;
	}

    public boolean isStarving() {
	    Hunger hunger = hunger();

        return hunger.isStarving();
    }

	public int stealth() {
		final int[] bonus = {0};

		forEachBuff(b-> bonus[0] += b.stealthBonus());

		return bonus[0];
	}

	public void placeTo(int cell) {
		if (level().map[getPos()] == Terrain.OPEN_DOOR) {
			Door.leave(getPos());
		}

		setPos(cell);

		if (!isFlying()) {
			level().press(getPos(),this);
		}

		if (isFlying() && level().map[getPos()] == Terrain.DOOR) {
			Door.enter(getPos());
		}

		if (this != Dungeon.hero) {
			getSprite().setVisible(Dungeon.visible[getPos()] && invisible >= 0);
		}
	}

	public void move(int step) {
		if(!isMovable()) {
			return;
		}

		if (hasBuff(Vertigo.class) && level().adjacent(getPos(), step)) { //ignore vertigo when blinking or teleporting

			List<Integer> candidates = new ArrayList<>();
			for (int dir : Level.NEIGHBOURS8) {
				int p = getPos() + dir;
				if (level().cellValid(p)) {
					if ((level().passable[p] || level().avoid[p]) && Actor.findChar(p) == null) {
						candidates.add(p);
					}
				}
			}

			if (candidates.isEmpty()) { // Nowhere to move? just stay then
				return;
			}

			step = Random.element(candidates);
		}

		placeTo(step);
	}

	public int distance(@NotNull Char other) {
		return level().distance(getPos(), other.getPos());
	}

	public void onMotionComplete() {
		next();
	}

	public void onAttackComplete() {
		next();
	}

	public  void onZapComplete() {
		next();
	}

	public void onOperateComplete() {
		next();
	}

	public void spendGold(int spend) {
        Belongings belongings = getBelongings();

		Gold gold = belongings.getItem(Gold.class);
		if(gold!=null) {
			gold.quantity(gold.quantity()-spend);
		}
    }

	public Set<String> resistances() {
		HashSet<String> ret = new HashSet<>(resistances);

		forEachBuff(b->ret.addAll(b.resistances()));

		return ret;
	}

	public Set<String> immunities() {
		HashSet<String> ret = new HashSet<>(immunities);

		forEachBuff(b->ret.addAll(b.immunities()));

		return ret;
	}

	public void updateSprite(){
		updateSprite(getSprite());
	}

	private void updateSprite(CharSprite sprite){
		if(level().cellValid(getPos())) {
			sprite.setVisible(Dungeon.visible[getPos()] && invisible >= 0);
		} else {
			EventCollector.logException("invalid pos for:"+toString()+":"+getClass().getCanonicalName());
		}
		GameScene.addMobSpriteDirect(sprite);
		sprite.link(this);
	}

	public void regenSprite() {
		sprite = null;
	}

	@LuaInterface
	public CharSprite getSprite() {
		if (sprite == null) {

			if(!GameScene.mayCreateSprites()) {
				throw new TrackedRuntimeException("scene not ready for "+ this.getClass().getSimpleName());
			}
			sprite = newSprite();
		}
		if(sprite == null) {
			throw new TrackedRuntimeException("Sprite creation for: "+getClass().getSimpleName()+" failed");
		}

		if(sprite.getParent()==null) {
			updateSprite(sprite);
		}

		return sprite;
	}

	public Fraction fraction() {
		return fraction;
	}

	public int getOwnerId() {
		if(owner<0) {
			setOwnerId(getId());
		}
		return owner;
	}

	@NotNull
	@LuaInterface
	public Char getOwner() {
		return CharsList.getById(getOwnerId());
	}

	public boolean followOnLevelChanged(InterlevelScene.Mode changeMode) {
		return false;
	}

	public abstract CharSprite newSprite();

	public int ht() {
		return Scrambler.descramble(HT);
	}

	public int ht(int hT) {
		HT = Scrambler.scramble(hT);
		return hT;
	}

	public int hp() {
		return Scrambler.descramble(HP);
	}

	public void hp(int hP) {
		HP = Scrambler.scramble(hP);
	}

	public void _stepBack() {
		if(level().cellValid(prevPos)){
			setPos(prevPos);
		}
	}

	public void setPos(int pos) {
		if(pos == Level.INVALID_CELL) { // level may ve not yet available here
			throw new TrackedRuntimeException("Trying to set invalid pos "+pos+" for "+getEntityKind());
		}
		prevPos = this.pos;
		freeCell(this.pos);
		this.pos = pos;
		occupyCell(this);
	}

	public boolean isMovable() {
		return movable;
	}

	public boolean collect(@NotNull Item item) {
		if (!item.collect(this)) {
			if (level() != null && level().cellValid(getPos())) {
				level().animatedDrop(item, getPos());
			}
			return false;
		}
		return true;
	}

	public int skillLevel() {
		return 10;
	}

	//backward compatibility with mods
	@LuaInterface
	public int magicLvl() {
		return skillLevel();
	}

	@Override
	public boolean affectLevelObjects() {
		return true;
	}

	public boolean isFlying() {
		return !paralysed && (flying || hasBuff(Levitation.class));
	}

	@LuaInterface
	public boolean isParalysed() {
		return paralysed;
	}

	public void paralyse(boolean paralysed) {
		this.paralysed = paralysed;
		if(paralysed && GameScene.isSceneReady()) {
			level().press(getPos(),this);
		}
	}

	public boolean friendly(@NotNull Char chr){
		return !fraction.isEnemy(chr.fraction);
	}

	public Level level(){
		return level;
	}

	@LuaInterface
	public boolean valid(){
	    return !(this instanceof DummyChar);
    }

    @LuaInterface
	public boolean doStepTo(final int target) {
		int oldPos = getPos();
		spend(1 / speed());
		if (level().cellValid(target) && getCloser(target)) {

			moveSprite(oldPos, getPos());
			return true;
		}
		return false;
	}

	@LuaInterface
	public boolean doStepFrom(final int target) {
		int oldPos = getPos();
		spend(1 / speed());
		if (level().cellValid(target) && getFurther(target)) {

			moveSprite(oldPos, getPos());
			return true;
		}
		return false;
	}

	protected abstract void moveSprite(int oldPos, int pos);

	public int visibleEnemies() {
		return visibleEnemies.size();
	}

	public Char visibleEnemy(int index) {
		if (index >= visibleEnemies.size()) {
			return CharsList.DUMMY;
		}
		return visibleEnemies.get(index);
	}

	@LuaInterface
	public Char randomEnemy() {
		return Random.element(visibleEnemies);
	}

	public void checkIfFurious() {
		if (getSubClass() == HeroSubClass.BERSERKER && 0 < hp() && hp() <= ht() * Fury.LEVEL) {
			if (!hasBuff(Fury.class)) {
				Buff.affect(this, Fury.class);
			}
		}
	}

	public abstract boolean getCloser(final int cell);
	protected abstract boolean getFurther(final int cell);

	@NotNull
	@Override
	public Belongings getBelongings() {
		if(belongings == null) {
			belongings = new Belongings(this);
		}
		return belongings;
	}

    public int gold() {
		Belongings belongings = getBelongings();
		Gold gold = belongings.getItem(Gold.class);
		if(gold!=null) {
			return gold.quantity();
		}
		return 0;
    }

	public void spellCasted(String spellName) {
		spellsUsage.put(spellName, 0.f);
	}

    public float spellCooldown(String spellName) {
		if(spellsUsage.containsKey(spellName)) {
			return spellsUsage.get(spellName).floatValue();
		}
		return Float.MAX_VALUE;
    }

	public void addImmunity(String namedEntity){
		immunities.add(namedEntity);
	}

    public void addImmunity(Class<?> buffClass){
		immunities.add(buffClass.getSimpleName());
	}

	public void addResistance(Class<?> buffClass){
		resistances.add(buffClass.getSimpleName());
	}

	public void removeImmunity(Class<?> buffClass){
		immunities.remove(buffClass.getSimpleName());
	}

	public void removeResistance(Class<?> buffClass){
		resistances.remove(buffClass.getSimpleName());
	}

	@Override
	public String getEntityKind() {
		return getClass().getSimpleName();
	}

	@Override
	public String name() {
		return getName_objective();
	}

	@LuaInterface
	public boolean push(Char chr) {

		if(!isMovable()) {
			return false;
		}

		int nextCell = LevelHelpers.pushDst(chr, this, false);

		if (!level().cellValid(nextCell)) {
			return false;
		}

		LevelObject lo = level().getLevelObject(nextCell);

		if (lo != null && !lo.push(this)) {
			return false;
		}

		Char ch = Actor.findChar(nextCell);

		if(ch != null) {
			if(!ch.isMovable()) {
				return false;
			}

			if(!ch.push(this)) {
				return false;
			}
		}

		moveSprite(getPos(),nextCell);
		placeTo(nextCell);
		return true;
	}

	public void forEachBuff(BuffCallback cb) {
	    Buff [] copyOfBuffsSet = buffs.toArray(new Buff[0]);
		for(Buff b: copyOfBuffsSet){
			cb.onBuff(b);
		}

		cb.onBuff(getHeroClass());
		cb.onBuff(getSubClass());
	}

	@LuaInterface
	public boolean swapPosition(final Char chr) {
		if(!walkingType.canSpawnAt(level(),chr.getPos())) {
			return false;
		}

		if(hasBuff(Roots.class)) {
			return false;
		}

		int myPos = getPos(), chPos = chr.getPos();
        moveSprite(myPos, chPos);
		placeTo(chPos);
		ensureOpenDoor();

		chr.getSprite().move(chPos, myPos);
		chr.placeTo(myPos);
		chr.ensureOpenDoor();

		float timeToSwap = 1 / chr.speed();
		chr.spend(timeToSwap);
		spend(timeToSwap);

		return true;
	}

	protected void ensureOpenDoor() {
		if (level().map[getPos()] == Terrain.DOOR) {
			Door.enter(getPos());
		}
	}

	public boolean interact(Char chr) {
		if (friendly(chr)) {
			swapPosition(chr);
			return true;
		}

		return false;
	}

	public String className() {
		return name();
	}

	public int lvl() {
		return Scrambler.descramble(lvl);
	}

	protected void lvl(int lvl) {
		this.lvl = Scrambler.scramble(lvl);
	}

	public HeroClass getHeroClass() {
		return HeroClass.NONE;
	}

	public HeroSubClass getSubClass() {
		return HeroSubClass.NONE;
	}

	public int countPets() {
		int ret = 0;
		for(Mob mob : level().mobs) {
			if(mob.getOwnerId()==getId()) {
				ret++;
			}
		}
		return ret;
	}

	@NotNull
	public Collection<Integer> getPets() {
		ArrayList<Integer> pets = new ArrayList<>();
		for(Mob mob : level().mobs) {
			if(mob.getOwnerId()==getId()) {
				pets.add(mob.getId());
			}
		}
		return pets;
	}

	public void releasePets() {
		for(Mob mob : level().mobs) {
			if(mob.getOwnerId()==getId()) {
				mob.releasePet();
			}
		}
	}

	public int effectiveSTR() {
		return 10;
	}

	public void busy(){}

	public void interrupt() {
	}

	public void itemPickedUp(Item item) {
	}

	public abstract Char makeClone();

	public ArrayList<String> actions(Char hero) {
		ArrayList<String> actions = new ArrayList<>();
		actions.add(CommonActions.MAC_STEAL);
		actions.add(CommonActions.MAC_TAUNT);
		actions.add(CommonActions.MAC_PUSH);
		return actions;
	}

	public void execute(Char hero, String action) {
	}

	protected void setOwnerId(int owner) {
		this.owner = owner;
	}

	@TestOnly
	public void resetBelongings(Belongings belongings) {
		setBelongings(belongings);
		updateSprite();
	}

	public void selectCell(CellSelector.Listener listener) {
		GLog.w("select cell for %s niy.", getEntityKind());
	}

	@LuaInterface
	public int getSkillPointsMax() {
		return 10;
	}

	@LuaInterface
	public int getSkillPoints() {
		return 10;
	}

	@LuaInterface
	public void spendSkillPoints(int cost) {
	}

	@LuaInterface
	public void setSkillPoints(int i) {
	}

	@LuaInterface
	@Deprecated // keep it for old version of Remixed Additions
	public void setSoulPoints(int i) {
	}

	@LuaInterface
	public void eat(Item food, float energy, String message) {
	}

	@LuaInterface
	public void skillLevelUp() {
	}

	@LuaInterface
	public void teleportTo(Position target) {
	}

	private Map<String, String> layersOverrides =  new HashMap<>();

    public Map<String, String> getLayersOverrides() {
        return layersOverrides;
    }

	@LuaInterface
    public void overrideSpriteLayer(String layer, String texture) {
        layersOverrides.put(layer, texture);
    }

    public int getViewDistance() {
		int computedViewDistance = viewDistance;

		if(hasBuff(Light.class)) {
			computedViewDistance = Utils.max(computedViewDistance,Level.MIN_VIEW_DISTANCE + 1, level().getViewDistance());
		}

		return Math.min(computedViewDistance, ShadowCaster.MAX_DISTANCE);
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
    }

	@Override
	public boolean useBags() {
		return true;
	}

	@LuaInterface
	public boolean canStepOn() {
    	return walkingType.canSpawnAt(level(),getPos()) && (Actor.findChar(pos) == null);
	}

	public void setBelongings(Belongings belongings) {
		this.belongings = belongings;
	}

	public boolean invalid() {
    	return !valid();
	}

	public void readyAndIdle() {
	}

	public void clearActions() {
	}

	public void handle(int cell) {
	}

	public boolean adjacent(@NotNull Char chr) {
    	return level().adjacent(getPos(),chr.getPos());
	}
}
