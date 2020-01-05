package main;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class CoordTableModel extends AbstractTableModel {
	private static final long serialVersionUID = -6275385179694526201L;
	
	private final List<CoordData> data = new ArrayList<>();
	
	@Override
	public int getRowCount() {
		return data.size();
	}
	
	@Override
	public int getColumnCount() {
		return 5;
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		CoordData row = data.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return row.getX();
		case 1:
			return row.getZ();
		case 2:
			return (int) row.getDistance();
		case 3:
			return row.getAngle();
		case 4:
			return AngleHelper.getAngleId(row.getAngle());
		default:
			return null;
		}
	}
	
	@Override
	public String getColumnName(int index) {
		switch (index) {
		case 0:
			return "X";
		case 1:
			return "Z";
		case 2:
			return "Distance";
		case 3:
			return "Angle";
		case 4:
			return "Direction";
		default:
			return null;
		}
	}
	
	public void addRow(CoordData cd) {
		data.add(cd);
		fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
	}
	
	public void clearRows() {
		data.clear();
		fireTableDataChanged();
	}
	
	public CoordData getRow(int index) {
		return data.get(index);
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
		case 1:
			return Long.class;
		case 2:
			return Integer.class;
		case 3:
			return Float.class;
		case 4:
			return Byte.class;
		default:
			return Object.class;
		}
		
	}
	
	public static class AngleRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 8768622706563933385L;
		private static final DecimalFormat FORMATTER = new DecimalFormat("###.#");
		
		@Override
		public void setValue(Object value) {
			if (value instanceof Float) {
				setText(FORMATTER.format((float) value) + (char) 176);
			}
		}
	}
	
	public static class DirectionRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 3056798506488752387L;
		
		@Override
		public void setValue(Object value) {
			if (value instanceof Byte) {
				setText(AngleHelper.getIdFromAbbreviation((byte) value));
			}
		}
		
	}
	
}
