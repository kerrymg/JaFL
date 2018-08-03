package flands;

public class EffectSetTest {
    public static void main(String args[]) {
        Item tool1 = new Item.Tool("compass", Adventurer.ABILITY_SCOUTING, 1);
        Item rod = new Item.Weapon("rod of woe", 4);
        AbilityEffect magicToolEffect = AbilityEffect.createAbilityBonus(Adventurer.ABILITY_SCOUTING, 4);
        magicToolEffect.setType(Effect.TYPE_TOOL);
        rod.addEffect(magicToolEffect);
        Item tool2 = new Item.Tool("sextant", Adventurer.ABILITY_SCOUTING, 3);
        EffectSet effects = new EffectSet(new Adventurer());
        effects.addStatRelated(Adventurer.ABILITY_SCOUTING, tool1);
        effects.addStatRelated(Adventurer.ABILITY_SCOUTING, tool2);
        effects.addStatRelated(Adventurer.ABILITY_COMBAT, rod);
        effects.addStatRelated(Adventurer.ABILITY_SCOUTING, rod, magicToolEffect);
        System.out.println("3 -> " + effects.adjustAbility(Adventurer.ABILITY_SCOUTING, 3));
    }
}
