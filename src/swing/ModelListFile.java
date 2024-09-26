package swing;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ModelListFile extends JTable {
    private Object[] columnsHeader = new String[] {"Имя файла", "Размер файла"};
    private DefaultTableModel tableModel;
    private JTable table;

    public ModelListFile (){
        table = new JTable();
        tableModel = new DefaultTableModel(null, columnsHeader) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(tableModel);
    }

    public void updateTable(String [][] data) {
        if (data == null) {
            System.out.println("null");
        } else {
            for (int i = 0; i < data.length; i++) {
                tableModel.addRow(data[i]);
                System.out.println(data[i][0] + "  " + data[i][1]);
            }
        }
    }

    public void removeFile (Integer idx) {
        tableModel.removeRow(idx);
    }

    public JTable getTable () {
        return this.table;
    }
}
