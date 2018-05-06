package flands;


import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyledDocument;

import org.xml.sax.Attributes;

/**
 * Action node that handles difficulty rolls (of the form, "make a X roll at Difficulty Y").
 * The result is stored in a variable, usually to be used by DifficultyResultNode; the
 * variable can be specified, to be read by some other node, and the ability to be tested
 * may be chosen by the player.
 * @author Jonathan Mann
 */
public class DifficultyNode extends ActionNode implements Executable, Roller.Listener, UndoManager.Creator, Flag.Listener {
	private int[] abilities;
	private int abilityModifier;
	private int abilityChosen = -1;
	private int level;
	private int result = -1;
	private String var;
	private boolean force = true;
	private List<AdjustNode> adjustments = null;
	private boolean usingAbilityBonus = false;
	private String flag;

	public static final String ElementName = "difficulty";
	static final String AbilityTypeVar = "*ability*";
	DifficultyNode(Node parent) {
		super(ElementName, parent);
		setEnabled(false);
	}

	@Override
	public void init(Attributes atts) {
		level = getIntValue(atts, "level", -1);
		if (level < 0)
			System.out.println("Bad value for difficulty:level attribute: " + atts.getValue("level"));
		var = atts.getValue("var");
		abilities = Adventurer.getAbilityTypes(atts.getValue("ability"));
		if (abilities.length == 1)
			// Store the ability type as a variable for the outcomes to access
			setVariableValue(AbilityTypeVar, abilities[0]);
		abilityModifier = Adventurer.getAbilityModifier(atts.getValue("modifier"));
		force = getBooleanValue(atts, "force", true);
		flag = atts.getValue("flag");
	}

	private boolean hadContent = false;
	@Override
	public void handleContent(String text) {
		if (text.trim().length() == 0)
			return;

		MutableAttributeSet atts = null;
		if (!hadContent)
			atts = createStandardAttributes();

		Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText(text, atts));
		addEnableElements(leaves);
		if (!hadContent) {
			setHighlightElements(leaves);
			hadContent = true;
		}
	}

	@Override
	public boolean handleEndTag() {
		if (!hadContent && !getParent().hideChildContent()) {
			String text;
			if (getDocument().isNewSentence(getDocument().getLength()))
				text = "Make a ";
			else
				text = "make a ";
			if (abilities.length == 1)
				text += Adventurer.getAbilityName(abilities[0]).toUpperCase();
			text += " roll at Difficulty " + level;

			Element[] leaves = getDocument().addLeavesTo(getElement(), new StyledText(text, createStandardAttributes()));
			addEnableElements(leaves);
			setHighlightElements(leaves);
			hadContent = true;
		}
		addExecutableNode(this);

		if (flag != null)
			getFlags().addListener(flag, this);

		return super.handleEndTag();
	}

	@Override
	protected Node createChild(String name) {
		Node n = null;
		if (name.equals(AdjustNode.ElementName)) {
			AdjustNode an = new AdjustNode(this);
			if (adjustments == null)
				adjustments = new LinkedList<>();
			adjustments.add(an);
			n = an;
		}

		if (n == null)
			return super.createChild(name);
		else {
			addChild(n);
			return n;
		}
	}

	@Override
	public boolean execute(ExecutableGrouper grouper) {
		if (result < 0 && (flag == null || getFlags().getFlag(flag).getState())) {
			// Set up for user to roll
			System.out.println("DifficultyNode: ready to roll!");
			setEnabled(true);
			return !force;
		}
		else {
			// Already rolled
			System.out.println("DifficultyNode: execute called after already rolled (or flag isn't set)!");
			return true;
		}
	}

	@Override
	public void resetExecute() {
		removeVariable(var);
		result = -1;
		setEnabled(false);
	}

	private Roller roller = null;
	@Override
	public void actionPerformed(ActionEvent evt) {
		if (roller != null) return;

		setEnabled(false);
		Adventurer adv = getAdventurer();
		int abilityScore = adv.getAbilityValue(abilities[0], abilityModifier, Adventurer.PURPOSE_TESTING);
		if (abilities.length == 1)
			abilityChosen = abilities[0];
		else if (abilityChosen >= 0)
			;// This is already a reroll - keep the same ability chosen!
		else {
			// Let the user choose
			// (we could just pick the ability with the highest score,
			//  but sometimes the ability chosen has importance).
			int[] scores = new int[abilities.length];
			scores[0] = abilityScore;
			for (int a = 1; a < abilities.length; a++)
				scores[a] = adv.getAbilityValue(abilities[a], abilityModifier, Adventurer.PURPOSE_VALUE);

			StyledDocument[] choices = Adventurer.getAbilityDocuments(abilities, scores);

			int[] selected = DocumentChooser.showChooser(FLApp.getSingle(), "Choose ability", choices, false);
			if (selected == null || selected.length == 0) {
				setEnabled(true);
				return;
			}
			abilityChosen = abilities[selected[0]];
			abilityScore = adv.getAbilityValue(abilityChosen, abilityModifier, Adventurer.PURPOSE_TESTING);
			setVariableValue(AbilityTypeVar, abilityChosen);
		}

		if (!usingAbilityBonus)
			// Grab the relevant potion effect, if there is one
			usingAbilityBonus = getAdventurer().getEffects().hasAbilityPotionBonus(abilityChosen);

		// Add any extra adjustments to the roll
		int delta = 0;
		if (adjustments != null) {
			for (AdjustNode adjustment : adjustments)
				delta += adjustment.getAdjustment();
			System.out.println("Adjustment for difficulty=" + delta);
		}

		roller = new Roller(adv.getDifficultyDice(), abilityScore + delta);
		roller.addListener(this);
		roller.startRolling();
		
		if (flag != null)
			getFlags().setState(flag, false);
	}

	@Override
	public void rollerFinished(Roller r) {
		if (roller == r) {
			setVariableValue(var, r.getResult() - level); // > 0 is success, <= 0 is failure
			roller = null;

			if (r.getResult() - level <= 0) {
				// Failure
				if (getBlessings().hasAbilityBlessing(abilityChosen)) {
					int useBlessing = JOptionPane.showConfirmDialog(FLApp.getSingle(), "Ability roll failed!\nDo you want to use your ability blessing?", "Use Blessing?", JOptionPane.YES_NO_OPTION);
					if (useBlessing == JOptionPane.YES_OPTION) {
						getBlessings().removeAbilityBlessing(abilityChosen);
						setEnabled(true);
						return;
					}
				}
			}
			r.appendTooltipText(r.getResult() - level > 0 ? " - Success" : " - Failure");
			UndoManager.createNew(this).add(this);

			// Remove relevant potion effect
			if (usingAbilityBonus) {
				EffectSet effects = getAdventurer().getEffects();
				effects.removeAbilityPotionBonus(abilityChosen);
				effects.notifyOwner();
			}

			continueNodeExecution(this, true);
		}
	}

	int getAbilityChosen() { return abilityChosen; }

	@Override
	public void undoOccurred(UndoManager undo) {
		if (usingAbilityBonus) {
			// Add ability effect back into the mix
			EffectSet effects = getAdventurer().getEffects();
			effects.addAbilityPotionBonus(abilityChosen);
			effects.notifyOwner();
		}

		if (flag != null && !getFlags().getState(flag))
			getFlags().setState(flag, true);

		// Pretend like we've just been called by the cached grouper...
		findExecutableGrouper().ifPresent(this::execute);
	}

	@Override
	public void flagChanged(String name, boolean state) {
		if (flag != null && flag.equals(name))
			setEnabled(state && result < 0);
	}

	@Override
	protected void saveProperties(Properties props) {
		super.saveProperties(props);
		props.setProperty("abilityChosen", Integer.toString(abilityChosen));
		props.setProperty("result", Integer.toString(result));
	}

	@Override
	protected void loadProperties(Attributes atts) {
		super.loadProperties(atts);
		abilityChosen = getIntValue(atts, "abilityChosen", -1);
		result = getIntValue(atts, "result", -1);
	}

	@Override
	protected String getTipText() {
		Adventurer adv = getAdventurer();
		StringBuilder sb = new StringBuilder("Roll " + getDiceText(adv.getDifficultyDice()));
		sb.append(", add your ");
		int singleAbility = abilityChosen;
		if (singleAbility < 0 && abilities.length == 1)
			singleAbility = abilities[0];
		if (singleAbility >= 0)
			sb.append(Adventurer.getAbilityName(singleAbility)).append(" score (").append(adv.getAbilityValue(singleAbility, abilityModifier, Adventurer.PURPOSE_TESTING)).append(")");
		else
			sb.append("ability score");
		int delta = 0;
		if (adjustments != null)
			for (AdjustNode adjustment : adjustments)
				delta += adjustment.getAdjustment();
		if (delta > 0)
			sb.append(", add ").append(delta);
		else if (delta < 0)
			sb.append(", subtract ").append(-delta);
		sb.append(", trying to beat a score of ").append(level);
		return sb.toString();
	}

	@Override
	public void dispose() {
		if (flag != null)
			getFlags().removeListener(flag, this);
	}
}
