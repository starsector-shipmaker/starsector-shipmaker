package shipeditor.utility.components.dialog;

import shipeditor.PrimaryWindow;
import shipeditor.components.instrument.ship.slots.SlotCreationPane;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;

public class SlotCreationDialog extends JDialog {

    public SlotCreationDialog(Frame owner) {
        super(owner, "Weapon Slot Creation Defaults", true);
        this.initUI();
    }

    private void initUI() {
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setMinimumSize(new Dimension(360, 480));
        this.setSize(380, 560);
        this.setLocationRelativeTo(this.getOwner());

        SlotCreationPane contentPane = new SlotCreationPane();
        this.add(contentPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(new EmptyBorder(4, 8, 8, 8));

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> this.dispose());
        bottomPanel.add(closeButton);

        this.add(bottomPanel, BorderLayout.SOUTH);
    }
}
