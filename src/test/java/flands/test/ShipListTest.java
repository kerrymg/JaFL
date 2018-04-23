package flands.test;

import flands.Ship;
import flands.ShipList;

import javax.swing.JOptionPane;
import javax.swing.JTable;

public class ShipListTest {
    public static void main(String args[]) {
        final ShipList sl = new ShipList();
        String[] docks = new String[]{"Aku", "Kunrir", null};
        for (int s = 0; s < 10; s++) {
            Ship ship = new Ship((int)(Math.random() * (Ship.MAX_TYPE+1)), null,
                    (int)(Math.random() * (Ship.MAX_CREW+2)) - 1);
            ship.setDocked(docks[(int)(Math.random() * docks.length)]);
            for (int c = 0; c < ship.getCapacity(); c++) {
                int cargoType = (int)(Math.random() * (Ship.MAX_CARGO+1));
                if (cargoType != Ship.NO_CARGO)
                    ship.addCargo(cargoType);
            }
            sl.addShip(ship);
        }
        String dock = docks[(int)(Math.random() * docks.length)];
        sl.setAtDock(dock);

        final javax.swing.JComboBox<String> cargoBox = new javax.swing.JComboBox<>();
        for (int c = 0; c <= Ship.MAX_CARGO; c++)
            cargoBox.addItem(Ship.getCargoName(c));
        cargoBox.setEditable(false);
        javax.swing.JButton buyCargoButton = new javax.swing.JButton("Buy");
        buyCargoButton.addActionListener(evt -> {
            int[] ships = sl.findShipsWithSpace();
            if (ships.length == 0)
                JOptionPane.showMessageDialog(cargoBox, "No ships here with space!", "No space", JOptionPane.INFORMATION_MESSAGE);
            else if (ships.length > 1)
                JOptionPane.showMessageDialog(cargoBox, new Object[] {"Which ship should take the cargo?", "Please select one."}, "Can't decide", JOptionPane.INFORMATION_MESSAGE);
            else
                sl.addCargoTo(ships[0], cargoBox.getSelectedIndex());
        });
        javax.swing.JButton sellCargoButton = new javax.swing.JButton("Sell");
        sellCargoButton.addActionListener(evt -> {
            int[] ships = sl.findShipsWithCargo(cargoBox.getSelectedIndex());
            if (ships.length == 0)
                JOptionPane.showMessageDialog(cargoBox, "No ships here with this cargo!", "No cargo", JOptionPane.INFORMATION_MESSAGE);
            else if (ships.length > 1)
                JOptionPane.showMessageDialog(cargoBox, new Object[] {"Multiple ships have this cargo!", "Please select one."}, "Can't decide", JOptionPane.INFORMATION_MESSAGE);
            else
                sl.removeCargoFrom(ships[0], cargoBox.getSelectedIndex());
        });
        JTable table = sl.getTable();
        javax.swing.JFrame jf = new javax.swing.JFrame("Ship list - " + dock);
        jf.getContentPane().add(new javax.swing.JScrollPane(table));
        javax.swing.JPanel lowerPanel = new javax.swing.JPanel();
        lowerPanel.add(cargoBox);
        lowerPanel.add(buyCargoButton);
        lowerPanel.add(sellCargoButton);
        jf.getContentPane().add(lowerPanel, java.awt.BorderLayout.SOUTH);
        jf.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        jf.pack();
        jf.setLocationRelativeTo(null);
        jf.setVisible(true);
    }
}
