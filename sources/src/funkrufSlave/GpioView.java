package funkrufSlave;

import java.awt.BorderLayout;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.ImageIcon;

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