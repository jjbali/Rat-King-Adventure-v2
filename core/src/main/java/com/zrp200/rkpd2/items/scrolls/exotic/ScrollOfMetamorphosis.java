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

package com.zrp200.rkpd2.items.scrolls.exotic;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;
import com.zrp200.rkpd2.Assets;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.hero.HeroClass;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.effects.Speck;
import com.zrp200.rkpd2.effects.Transmuting;
import com.zrp200.rkpd2.items.scrolls.InventoryScroll;
import com.zrp200.rkpd2.items.scrolls.Scroll;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.GameScene;
import com.zrp200.rkpd2.scenes.PixelScene;
import com.zrp200.rkpd2.sprites.ItemSprite;
import com.zrp200.rkpd2.sprites.ItemSpriteSheet;
import com.zrp200.rkpd2.ui.RenderedTextBlock;
import com.zrp200.rkpd2.ui.TalentButton;
import com.zrp200.rkpd2.ui.TalentsPane;
import com.zrp200.rkpd2.ui.Window;
import com.zrp200.rkpd2.windows.IconTitle;
import com.zrp200.rkpd2.windows.WndOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import static com.zrp200.rkpd2.actors.hero.HeroClass.*;
import static com.zrp200.rkpd2.actors.hero.Talent.*;

public class ScrollOfMetamorphosis extends ExoticScroll {
	
	{
		icon = ItemSpriteSheet.Icons.SCROLL_METAMORPH;
	}

	protected static boolean identifiedByUse = false;
	
	@Override
	public void doRead() {
		if (!isKnown()) {
			identify();
			identifiedByUse = true;
		} else {
			identifiedByUse = false;
		}
		GameScene.show(new WndMetamorphChoose());
	}

	public static void onMetamorph( Talent oldTalent, Talent newTalent ){
		((Scroll) curItem).readAnimation();
		Sample.INSTANCE.play( Assets.Sounds.READ );
		curUser.sprite.emitter().start(Speck.factory(Speck.CHANGE), 0.2f, 10);
		Transmuting.show(curUser, oldTalent, newTalent);

		if (Dungeon.hero.hasTalent(newTalent)) {
			Talent.onTalentUpgraded(Dungeon.hero, newTalent);
		}
	}

	private void confirmCancelation( Window chooseWindow ) {
		GameScene.show( new WndOptions(new ItemSprite(this),
				Messages.titleCase(name()),
				Messages.get(InventoryScroll.class, "warning"),
				Messages.get(InventoryScroll.class, "yes"),
				Messages.get(InventoryScroll.class, "no") ) {
			@Override
			protected void onSelect( int index ) {
				switch (index) {
					case 0:
						curUser.spendAndNext( TIME_TO_READ );
						identifiedByUse = false;
						chooseWindow.hide();
						break;
					case 1:
						//do nothing
						break;
				}
			}
			public void onBackPressed() {}
		} );
	}

	public static class WndMetamorphChoose extends Window {

		public static WndMetamorphChoose INSTANCE;

		TalentsPane pane;

		public WndMetamorphChoose(){
			super();

			INSTANCE = this;

			float top = 0;

			IconTitle title = new IconTitle( curItem );
			title.color( TITLE_COLOR );
			title.setRect(0, 0, 120, 0);
			add(title);

			top = title.bottom() + 2;

			RenderedTextBlock text = PixelScene.renderTextBlock(Messages.get(ScrollOfMetamorphosis.class, "choose_desc"), 6);
			text.maxWidth(120);
			text.setPos(0, top);
			add(text);

			top = text.bottom() + 2;

			ArrayList<LinkedHashMap<Talent, Integer>> talents = new ArrayList<>();
			initClassTalents(Dungeon.hero.heroClass, talents, Dungeon.hero.metamorphedTalents);

			for (LinkedHashMap<Talent, Integer> tier : talents){
				for (Talent talent : tier.keySet()){
					tier.put(talent, Dungeon.hero.pointsInTalent(talent));
				}
			}

			for (int i = 0; i < talents.size(); i++){
				LinkedHashMap<Talent, Integer> heroTalents = Dungeon.hero.talents.get(i);
				LinkedHashMap<Talent, Integer> lolTalents = talents.get(i);
				for (Talent talent : heroTalents.keySet()){
					if (!lolTalents.containsKey(talent)){
						if ((i == 3 &&
								!(talent == HEROIC_RATINESS || talent == HEROIC_ARCHERY ||
										talent == HEROIC_ENDURANCE || talent == HEROIC_STAMINA || talent == HEROIC_WIZARDRY))){
							continue;
						}
						if (i == 2) continue;
						lolTalents.put(talent, Dungeon.hero.pointsInTalent(talent));
					}
				}
			}

			pane = new TalentsPane(TalentButton.Mode.METAMORPH_CHOOSE, talents);
			add(pane);
			pane.setPos(0, top);
			pane.setSize(120, pane.content().height());
			resize((int)pane.width(), (int)pane.bottom());
			pane.setPos(0, top);
		}

		@Override
		public void hide() {
			super.hide();
			INSTANCE = null;
		}

		@Override
		public void onBackPressed() {

			if (identifiedByUse){
				((ScrollOfMetamorphosis)curItem).confirmCancelation(this);
			} else {
				super.onBackPressed();
				curItem.collect();
			}
		}

		@Override
		public void offset(int xOffset, int yOffset) {
			super.offset(xOffset, yOffset);
			pane.setPos(pane.left(), pane.top()); //triggers layout
		}
	}
	//talents that can only be used by one hero class
	public static final HashMap<Talent, HeroClass> restrictedTalents = new HashMap<>();
	static {

		// rework made metamorph effect unreasonable.
		restrictedTalents.put(IRON_WILL, WARRIOR);

		restrictedTalents.put(Talent.RUNIC_TRANSFERENCE, HeroClass.WARRIOR);
		restrictedTalents.put(Talent.WAND_PRESERVATION, HeroClass.MAGE);
		restrictedTalents.put(Talent.NATURES_AID, HeroClass.HUNTRESS);
		restrictedTalents.put(RESTORED_NATURE, HeroClass.HUNTRESS);
		restrictedTalents.put(SEER_SHOT, HeroClass.HUNTRESS);
		restrictedTalents.put(BIG_RUSH, WARRIOR);
		restrictedTalents.put(EFFICIENT_SHADOWS, ROGUE);
		restrictedTalents.put(NATURE_AID_2, HUNTRESS);

		// rat king talents that contain restricted talents are removed.
		restrictedTalents.put(POWER_WITHIN, RAT_KING);
	}

	public static class WndMetamorphReplace extends Window {

		public static WndMetamorphReplace INSTANCE;

		public Talent replacing;
		public int tier;
		LinkedHashMap<Talent, Integer> replaceOptions;

		//for window restoring
		public WndMetamorphReplace(){
			super();

			if (INSTANCE != null){
				replacing = INSTANCE.replacing;
				tier = INSTANCE.tier;
				replaceOptions = INSTANCE.replaceOptions;
				INSTANCE = this;
				setup(replacing, tier, replaceOptions);
			} else {
				hide();
			}
		}

		public WndMetamorphReplace(Talent replacing, int tier){
			super();

			INSTANCE = this;

			this.replacing = replacing;
			this.tier = tier;

			LinkedHashMap<Talent, Integer> options = new LinkedHashMap<>();
			Set<Talent> curTalentsAtTier = Dungeon.hero.talents.get(tier-1).keySet();

			HeroClass[] classes = new HeroClass[ HeroClass.values().length-1 ];
			if(tier == 3) {
				// copy all the base classes, as rat king doesn't have any t3s to transfer.
				System.arraycopy(HeroClass.values(), 0, classes, 0, classes.length);
			} else {
				// lower chance = more likely to be included.
				HashMap<HeroClass, Float> chances = new HashMap<HeroClass, Float>(){{
					for(HeroClass cls : HeroClass.values()) put(cls,
							cls == RAT_KING
									? 1f
									: cls == Dungeon.hero.heroClass
									? 2f
									: 3f);
				}};
				// omit one class to closer simulate shattered. RKPD2 adds one class.
				for(int i=0; i < classes.length; i++) {
					chances.remove( classes[i] = Random.chances(chances) );
				}
			}
			java.util.Arrays.sort(classes); // this retains some sort of order.

			// limited to three choices.
			for (HeroClass cls : classes){
				ArrayList<LinkedHashMap<Talent, Integer>> clsTalents = new ArrayList<>();
				initClassTalents(cls, clsTalents);

				Set<Talent> clsTalentsAtTier = clsTalents.get(tier-1).keySet();
				boolean replacingIsInSet = false;
				for (Talent talent : clsTalentsAtTier.toArray(new Talent[0])){
					if (talent == replacing){
						replacingIsInSet = true;
						break;
					} else {
						if (curTalentsAtTier.contains(talent)){
							clsTalentsAtTier.remove(talent);
						}
						if (restrictedTalents.containsKey(talent)
								&& restrictedTalents.get(talent) != curUser.heroClass){
							clsTalentsAtTier.remove(talent);
						}
					}
				}
				if (!replacingIsInSet && !clsTalentsAtTier.isEmpty()) {
					options.put(Random.element(clsTalentsAtTier), Dungeon.hero.pointsInTalent(replacing));
				}
			}

			replaceOptions = options;
			setup(replacing, tier, options);
		}

		private void setup(Talent replacing, int tier, LinkedHashMap<Talent, Integer> replaceOptions){
			float top = 0;

			IconTitle title = new IconTitle( curItem );
			title.color( TITLE_COLOR );
			title.setRect(0, 0, 120, 0);
			add(title);

			top = title.bottom() + 2;

			RenderedTextBlock text = PixelScene.renderTextBlock(Messages.get(ScrollOfMetamorphosis.class, "replace_desc"), 6);
			text.maxWidth(120);
			text.setPos(0, top);
			add(text);

			top = text.bottom() + 2;

			TalentsPane.TalentTierPane optionsPane = new TalentsPane.TalentTierPane(replaceOptions, tier, TalentButton.Mode.METAMORPH_REPLACE);
			add(optionsPane);
			optionsPane.title.text(" ");
			optionsPane.setPos(0, top);
			optionsPane.setSize(120, optionsPane.height());
			resize((int)optionsPane.width(), (int)optionsPane.bottom());

			resize(120, (int)optionsPane.bottom());
		}

		@Override
		public void hide() {
			super.hide();
			if (INSTANCE == this) {
				INSTANCE = null;
			}
		}

		@Override
		public void onBackPressed() {
			((ScrollOfMetamorphosis)curItem).confirmCancelation(this);
		}
	}
}
