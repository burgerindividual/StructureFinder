package main;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class GuiTweaks {

	static class LimitDocumentFilter extends DocumentFilter {

		private int limit;

		public LimitDocumentFilter(int limit) {
			if (limit <= 0) {
				throw new IllegalArgumentException("Limit can not be <= 0");
			}
			this.limit = limit;
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
				throws BadLocationException {
			int currentLength = fb.getDocument().getLength();
			int overLimit = (currentLength + text.length()) - limit - length;
			if (overLimit > 0) {
				text = text.substring(0, text.length() - overLimit);
			}
			if (text.length() > 0) {
				super.replace(fb, offset, length, text, attrs);
			}
		}
	}

	static class ConditionalComboBoxRenderer extends BasicComboBoxRenderer implements ListCellRenderer {
		private static final long serialVersionUID = -1384299159610756056L;
		private final BasicComboBoxRenderer defaultComboBoxRenderer = (BasicComboBoxRenderer) new JComboBox<>()
				.getRenderer();

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			boolean itemEnabled = ((Conditionable) value).isEnabled();

			Component c = defaultComboBoxRenderer.getListCellRendererComponent(list, value, index,
					isSelected && itemEnabled, cellHasFocus);

			if (!itemEnabled) {
				c.setBackground(list.getBackground());
				c.setForeground(UIManager.getColor("Label.disabledForeground"));
			}

			return c;
		}
	}

	static class ConditionalComboBoxListener implements ActionListener {
		JComboBox<ConditionalString> combobox;
		Object oldItem;

		ConditionalComboBoxListener(JComboBox<ConditionalString> combobox) {
			this.combobox = combobox;
			combobox.setSelectedIndex(0);
			oldItem = combobox.getSelectedItem();
		}

		public void actionPerformed(ActionEvent e) {
			Object selectedItem = combobox.getSelectedItem();
			if (!((Conditionable) selectedItem).isEnabled()) {
				combobox.setSelectedItem(oldItem);
			} else {
				oldItem = selectedItem;
			}
		}
	}

	static class ConditionalString implements Conditionable {
		String string;
		boolean isEnabled;

		ConditionalString(String string, boolean isEnabled) {
			this.string = string;
			this.isEnabled = isEnabled;
		}

		ConditionalString(String string) {
			this(string, true);
		}

		public boolean isEnabled() {
			return isEnabled;
		}

		public void setEnabled(boolean isEnabled) {
			this.isEnabled = isEnabled;
		}

		public String toString() {
			return string;
		}
	}

	interface Conditionable {
		public boolean isEnabled();

		public void setEnabled(boolean enabled);

		public String toString();
	}

}