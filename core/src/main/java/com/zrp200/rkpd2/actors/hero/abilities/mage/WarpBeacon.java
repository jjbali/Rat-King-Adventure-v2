/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2021 Evan Debenham
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

package com.zrp200.rkpd2.actors.hero.abilities.mage;

import static com.zrp200.rkpd2.actors.hero.abilities.rat_king.OmniAbility.markAbilityUsed;

import com.zrp200.rkpd2.Assets;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.Actor;
import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.buffs.Buff;
import com.zrp200.rkpd2.actors.buffs.Invisibility;
import com.zrp200.rkpd2.actors.buffs.LockedFloor;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.actors.hero.abilities.ArmorAbility;
import com.zrp200.rkpd2.effects.CellEmitter;
import com.zrp200.rkpd2.effects.MagicMissile;
import com.zrp200.rkpd2.effects.Pushing;
import com.zrp200.rkpd2.items.armor.ClassArmor;
import com.zrp200.rkpd2.items.artifacts.TimekeepersHourglass;
import com.zrp200.rkpd2.items.scrolls.ScrollOfTeleportation;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.plants.Swiftthistle;
import com.zrp200.rkpd2.scenes.GameScene;
import com.zrp200.rkpd2.scenes.InterlevelScene;
import com.zrp200.rkpd2.ui.HeroIcon;
import com.zrp200.rkpd2.utils.BArray;
import com.zrp200.rkpd2.utils.GLog;
import com.zrp200.rkpd2.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WarpBeacon extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	@Override
	public String targetingPrompt() {
		if (Dungeon.hero.buff(WarpBeaconTracker.class) == null
				&& Dungeon.hero.hasTalent(Talent.REMOTE_BEACON)){
			return Messages.get(this, "prompt");
		}
		return super.targetingPrompt();
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		if (target == null){
			return;
		}

		if (hero.buff(WarpBeaconTracker.class) != null){
			final WarpBeaconTracker tracker = hero.buff(WarpBeaconTracker.class);

			GameScene.show( new WndOptions(
					new Image(hero.sprite),
					Messages.titleCase(name()),
					Messages.get(WarpBeacon.class, "window_desc", tracker.depth),
					Messages.get(WarpBeacon.class, "window_tele"),
					Messages.get(WarpBeacon.class, "window_clear"),
					Messages.get(WarpBeacon.class, "window_cancel")){

				@Override
				protected void onSelect(int index) {
					if (index == 0){

						/*if (tracker.depth != Dungeon.depth && !hero.hasTalent(Talent.LONGRANGE_WARP)){
							GLog.w( Messages.get(WarpBeacon.class, "depths") );
							return;
						}*/

						float chargeNeeded = chargeUse(hero);

						if (tracker.depth != Dungeon.depth){
							// changed from shattered
							chargeNeeded *= 1.75f - 0.25*Dungeon.hero.pointsInTalent(Talent.LONGRANGE_WARP);
						}

						// TODO fix for supercharge
						if (armor.charge < chargeNeeded){
							GLog.w( Messages.get(ClassArmor.class, "low_charge") );
							return;
						}

						armor.charge -= chargeNeeded;
						armor.updateQuickslot();
						//  todo should I clear the beacon at this point? Bottom implementation wouldn't work, would need to manually clear the thing.
						// could also force you to teleport once before switching (this would be in line with standard mechanics but would be annoying af)
						// markAbilityUsed(WarpBeacon.this); // won't let you repeat.

						if (tracker.depth == Dungeon.depth){
							Char existing = Actor.findChar(tracker.pos);

							ScrollOfTeleportation.appear(hero, tracker.pos);

							if (existing != null && existing != hero){
								//if (hero.hasTalent(Talent.TELEFRAG)){
									int heroHP = hero.HP + hero.shielding();
									int heroDmg = 5 * hero.shiftedPoints(Talent.TELEFRAG);
									hero.damage(Math.min(heroDmg, heroHP-1), WarpBeacon.this);

									int damage = Random.NormalIntRange(10*hero.shiftedPoints(Talent.TELEFRAG), 15*hero.shiftedPoints(Talent.TELEFRAG));
									existing.sprite.flash();
									existing.sprite.bloodBurstA(existing.sprite.center(), damage);
									existing.damage(damage, WarpBeacon.this);

									Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
									Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
								//}

								if (existing.isAlive()){
									Char toPush = Char.hasProp(existing, Char.Property.IMMOVABLE) ? hero : existing;

									ArrayList<Integer> candidates = new ArrayList<>();
									for (int n : PathFinder.NEIGHBOURS8) {
										int cell = tracker.pos + n;
										if (!Dungeon.level.solid[cell] && Actor.findChar( cell ) == null
												&& (!Char.hasProp(toPush, Char.Property.LARGE) || Dungeon.level.openSpace[cell])) {
											candidates.add( cell );
										}
									}
									Random.shuffle(candidates);

									if (!candidates.isEmpty()){
										Actor.addDelayed( new Pushing( toPush, toPush.pos, candidates.get(0) ), -1 );

										toPush.pos = candidates.get(0);
										Dungeon.level.occupyCell(toPush);
										hero.next();
									}
								}
							}

							Invisibility.dispel();
							Dungeon.observe();

						} else {

							if (hero.buff(LockedFloor.class) != null){
								GLog.w( Messages.get(WarpBeacon.class, "locked_floor") );
								return;
							}

							TimekeepersHourglass.timeFreeze timeFreeze = hero.buff(TimekeepersHourglass.timeFreeze.class);
							if (timeFreeze != null) timeFreeze.disarmPressedTraps();
							Swiftthistle.TimeBubble timeBubble = hero.buff(Swiftthistle.TimeBubble.class);
							if (timeBubble != null) timeBubble.disarmPressedTraps();
							Invisibility.dispel();

							InterlevelScene.mode = InterlevelScene.Mode.RETURN;
							InterlevelScene.returnDepth = tracker.depth;
							InterlevelScene.returnPos = tracker.pos;
							Game.switchScene( InterlevelScene.class );
						}

					} else if (index == 1){
						hero.buff(WarpBeaconTracker.class).detach();
					}
				}
			} );

		} else {
			if (!Dungeon.level.mapped[target] && !Dungeon.level.visited[target]){
				return;
			}

			if (Dungeon.level.distance(hero.pos, target) > 4*hero.pointsInTalent(Talent.REMOTE_BEACON)){
				GLog.w( Messages.get(WarpBeacon.class, "too_far") );
				return;
			}

			PathFinder.buildDistanceMap(target, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
			if (Dungeon.level.pit[target] ||
					(Dungeon.level.solid[target] && !Dungeon.level.passable[target]) ||
					PathFinder.distance[hero.pos] == Integer.MAX_VALUE){
				GLog.w( Messages.get(WarpBeacon.class, "invalid_beacon") );
				return;
			}

			WarpBeaconTracker tracker = new WarpBeaconTracker();
			tracker.pos = target;
			tracker.depth = Dungeon.depth;
			tracker.attachTo(hero);
			markAbilityUsed(this);

			hero.sprite.operate(target);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			Invisibility.dispel();
			hero.spendAndNext(Actor.TICK);
		}
	}

	public static class WarpBeaconTracker extends Buff {

		{
			revivePersists = true;
		}

		int pos;
		int depth;

		Emitter e;

		@Override
		public void fx(boolean on) {
			if (on && depth == Dungeon.depth) {
				e = CellEmitter.center(pos);
				e.pour(MagicMissile.WardParticle.UP, 0.05f);
			}
			else if (e != null) e.on = false;
		}

		public static final String POS = "pos";
		public static final String DEPTH = "depth";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(POS, pos);
			bundle.put(DEPTH, depth);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			pos = bundle.getInt(POS);
			depth = bundle.getInt(DEPTH);
		}
	}

	@Override public boolean isActive() {
		return Actor.containsClass(WarpBeaconTracker.class);
	}

	@Override
	public int icon() {
		return HeroIcon.WARP_BEACON;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.TELEFRAG, Talent.REMOTE_BEACON, Talent.LONGRANGE_WARP, Talent.HEROIC_ENERGY};
	}
}
