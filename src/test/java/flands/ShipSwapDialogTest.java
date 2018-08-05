package flands;

import javax.swing.JDialog;
import javax.swing.JFrame;

public class ShipSwapDialogTest {
	public static void main(String args[]) {
		ShipList ships = new ShipList();
		Ship ship1 = new Ship(Ship.BARQ_TYPE, "Tubby", Ship.POOR_CREW);
		ship1.addCargo(Ship.TIMBER_CARGO);
		ships.addShip(ship1);
		Ship ship2 = new Ship(Ship.BRIG_TYPE, "Cruiser", Ship.GOOD_CREW);
		ship2.addCargo(Ship.METAL_CARGO);
		ships.addShip(ship2);
		Ship ship3 = new Ship(Ship.GALL_TYPE, "Brutus", Ship.EX_CREW);
		ship3.addCargo(Ship.MINERAL_CARGO);
		ship3.addCargo(Ship.SPICE_CARGO);
		ships.addShip(ship3);

		JFrame jf = new JFrame("Test");
		jf.setBounds(0, 0, 200, 200);
		jf.setVisible(true);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JDialog jd = new JDialog(jf, "Test2");
		jd.setBounds(10, 10, 200, 200);
		jd.setVisible(true);

		ShipSwapDialog ssd = new ShipSwapDialog(jd, ships);
		ssd.setVisible(true);
	}
}
