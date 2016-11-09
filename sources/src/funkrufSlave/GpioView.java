package funkrufSlave;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class GpioView extends JFrame {
	private JPanel contentPane;

	public GpioView() {
		setTitle("Raspberry Pi: GPIO");
		setBounds(100, 100, 500, 907);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JLabel label = new JLabel("");
		label.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/pi_gpio.png"))));
		contentPane.add(label, BorderLayout.CENTER);
	}
}