package flands;

public class CurseTest {
	public static void main(String args[]) {
		Curse c = new Curse(Curse.CURSE_TYPE, "Curse of Donkey's Ears");
		c.addEffect(AbilityEffect.createAbilityBonus(Adventurer.ABILITY_CHARISMA, -2));
	}
}
