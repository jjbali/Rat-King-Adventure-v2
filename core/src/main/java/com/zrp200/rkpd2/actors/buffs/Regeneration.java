/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2022 Evan Debenham
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

package com.zrp200.rkpd2.actors.buffs;

import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.items.rings.RingOfEnergy;
import com.zrp200.rkpd2.levels.Terrain;

public class Regeneration extends Buff {
	
	{
		//unlike other buffs, this one acts after the hero and takes priority against other effects
		//healing is much more useful if you get some of it off before taking damage
		actPriority = HERO_PRIO - 1;
	}
	
	private static final float REGENERATION_DELAY = 10;
	
	@Override
	public boolean act() {
		if (target.isAlive()) {

			if (target.HP < regencap() && !((Hero)target).isStarving()) {
				LockedFloor lock = target.buff(LockedFloor.class);
				Hunger hunger = target.buff(Hunger.class);
				if (hunger != null && hunger.accumulatingDamage > 0){
					hunger.accumulatingDamage = Math.max(0, hunger.accumulatingDamage-2);
				} else if (target.HP > 0 && ((lock == null || lock.regenOn()))) {
					target.HP += 1;
					if (target.HP == regencap()) {
						((Hero) target).resting = false;
					}
				}
			}

			RegenerationBuff regenBuff = Dungeon.hero.buff( RegenerationBuff.class);

			float delay = REGENERATION_DELAY;
			if (regenBuff != null) {
				if (regenBuff.isCursed()) {
					delay *= 1.5f;
				} else {
					delay -= regenBuff.itemLevel()*0.9f;
					delay /= RingOfEnergy.artifactChargeMultiplier(target);
				}
			}
			if (Dungeon.hero.hasTalent(Talent.NATURE_AID_2) && Dungeon.level.map[Dungeon.hero.pos] == Terrain.FURROWED_GRASS){
				delay *= 1 - Dungeon.hero.pointsInTalent(Talent.NATURE_AID_2)*0.35f;
			}
			spend( delay );
			
		} else {
			
			diactivate();
			
		}
		
		return true;
	}
	
	public int regencap(){
		return target.HT;
	}
}
