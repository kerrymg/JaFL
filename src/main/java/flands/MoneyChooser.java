package flands;


import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Pop-up window allowing the player to withdraw or deposit an amount of money.
 * Could do with more functionality (min and max buttons, for one).
 * 
 * @author Jonathan Mann
 */
public class MoneyChooser extends JDialog implements ActionListener, ChangeListener {
	private SpinnerNumberModel spinnerModel;
	private JTextField withdrawalField = null;
	private float withdrawalCharge = 0f;
	private int multiples = 1;
	private int minimum = 0;
	private boolean successful = false;

	MoneyChooser(Frame f, String title, String text, int value, int max) {
		super(f, title, true);
		init(f, text, value, max);
	}

	MoneyChooser(Frame f, String title, String text, int value, int min, int max, int multiples) {
		super(f, title, true);
		this.multiples = multiples;
		this.minimum = min;
		init(f, text, value, max);
	}

	public MoneyChooser(Frame f, String title, String text, int value, int max, float withdrawalCharge, int multiples) {
		super(f, title, true);
		this.withdrawalCharge = withdrawalCharge;
		this.multiples = multiples;
		init(f, text, value, max);
	}

	private void init(Window owner, String text, int value, int max) {
		spinnerModel = new SpinnerNumberModel(value, minimum, max, multiples) {
			@Override
			public void setValue(Object val) {
				if (multiples != 1) {
					int newVal = ((Number)val).intValue();
					if (((newVal - minimum) % multiples) != 0) {
						getToolkit().beep();
						fireStateChanged();
						return;
					}
				}
				super.setValue(val);
			}
		};
		JSpinner spinner = new JSpinner(spinnerModel);

		GridBagLayout gbl = new GridBagLayout();
		getContentPane().setLayout(gbl);

		new GBC(0, 0)
			.setWeight(0, 1)
			.setAnchor(GBC.WEST)
			.setInsets(12, 12, 0, 6)
			.addComp(getContentPane(), new JLabel(text), gbl);
		new GBC(1, 0)
			.setWeight(1, 1)
			.setBothFill()
			.setInsets(12, 0, 0, 11)
			.addComp(getContentPane(), spinner, gbl);

		if (withdrawalCharge > 0) {
			withdrawalField = new JTextField();
			withdrawalField.setHorizontalAlignment(JTextField.RIGHT);
			withdrawalField.setEditable(false);
			spinnerModel.addChangeListener(this);
			stateChanged(null); // to get initial value right

			new GBC(0, 1)
				.setWeight(0, 1)
				.setAnchor(GBC.WEST)
				.setInsets(0, 12, 0, 6)
				.addComp(getContentPane(), new JLabel("Withdrawal charge:"), gbl);
			new GBC(1, 1)
				.setWeight(1, 1)
				.setBothFill()
				.setInsets(0, 0, 0, 11)
				.addComp(getContentPane(), withdrawalField, gbl);
		}

		CommandButtons buttons = CommandButtons.createRow(CommandButtons.OK_CANCEL, this);
		new GBC(0, 2)
			.setSpan(2, 1)
			.setWeight(1, 0)
			.setBothFill()
			.addComp(getContentPane(), buttons, gbl);
		getRootPane().setDefaultButton(buttons.getButton(CommandButtons.OK_BUTTON));

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(owner);
	}

	private int getValue() { return spinnerModel.getNumber().intValue(); }
	public int getResult() {
		return (successful ? getValue() : 0);
	}
	public int getResultLessCharge() {
		int result = getResult();
		return (result - (int)Math.ceil(result*withdrawalCharge));
	}

	@Override
	public void stateChanged(ChangeEvent evt) {
		withdrawalField.setText(Integer.toString((int)Math.ceil(getValue() * withdrawalCharge)));
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		if (evt.getActionCommand().equals(CommandButtons.okCommand))
			successful = true;
		setVisible(false);
		dispose();
	}
}
