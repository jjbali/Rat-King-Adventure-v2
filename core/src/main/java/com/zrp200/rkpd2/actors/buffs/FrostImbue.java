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

import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.blobs.FrostFire;
import com.zrp200.rkpd2.effects.particles.SnowParticle;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class FrostImbue extends FlavourBuff {
	
	{
		type = buffType.POSITIVE;
		announced = true;
	}
	
	public static final float DURATION	= 50f;
	
	public void proc(Char enemy){
		Buff.affect(enemy, Chill.class, 2f);
		enemy.sprite.emitter().burst( SnowParticle.FACTORY, 2 );
	}
	
	@Override
	public int icon() {
		return BuffIndicator.IMBUE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0, 2f, 3f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}
	
	@Override
	public String toString() {
		return Messages.get(this, "name");
	}
	
	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns());
	}
	
	{
		immunities.add( Frost.class );
		immunities.add( Chill.class );
		immunities.add(FrostFire.class);
	}
}
