package sand.client.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import common.*;
import sand.client.*;

/**
 *
 * @author spencer
 */
public class AccountCreateGUI extends JFrame {
	private SandClientManager manager;
	/**
	 * Creates new form AccountCreateUpdatedGUI
	 */
	public AccountCreateGUI(SandClientManager man, SandClientGUI g) {
		manager = man;
		initComponents();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		Title = new javax.swing.JLabel();
		username = new javax.swing.JTextField();
		password = new javax.swing.JPasswordField();
		confirmPassword = new javax.swing.JPasswordField();
		lblusername = new javax.swing.JLabel();
		lblpassword = new javax.swing.JLabel();
		lblconfirmPassword = new javax.swing.JLabel();
		jLabel1 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jLabel3 = new javax.swing.JLabel();
		jLabel4 = new javax.swing.JLabel();
		submitButton = new javax.swing.JButton();
		generatePasswordButton = new javax.swing.JButton();
		jLabel5 = new javax.swing.JLabel();
		txtpassLength = new javax.swing.JTextField();
		jLabel6 = new javax.swing.JLabel();
		txtgeneratedPass = new javax.swing.JTextField();
		checkboxTwoFactor = new javax.swing.JCheckBox();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

		Title.setFont(new java.awt.Font("Noto Sans", 0, 18)); // NOI18N
		Title.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		Title.setText("S.A.N.D. Password Manager ");

		lblusername.setText("Username");

		lblpassword.setText("Password");

		lblconfirmPassword.setText("Confirm Password");

		jLabel1.setText("Passwords must:");

		jLabel2.setText("- be at least 8 characters long");

		jLabel3.setText("- contain at least one alpha character");

		jLabel4.setText("- contain only alpha-numeric characters (0-9,a-z,A-Z)");

		submitButton.setText("Submit");
		submitButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				submitButtonActionPerformed(evt);
			}
		});

		generatePasswordButton.setText("Auto-generate password");
		generatePasswordButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				generatePasswordButtonActionPerformed(evt);
			}
		});

		jLabel5.setText("of length ");

		jLabel6.setText("Generated Password:");

		checkboxTwoFactor.setText("Enable 2-Factor Authentication");

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
							.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
								.addComponent(generatePasswordButton, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(jLabel5)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(txtpassLength, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
							.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(jLabel1)
								.addComponent(jLabel2)
								.addComponent(jLabel4)
								.addComponent(jLabel3)
								.addGroup(layout.createSequentialGroup()
									.addComponent(jLabel6)
									.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(txtgeneratedPass, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)))))
					.addGroup(layout.createSequentialGroup()
						.addGap(73, 73, 73)
						.addComponent(Title))
					.addGroup(layout.createSequentialGroup()
						.addGap(34, 34, 34)
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
							.addGroup(layout.createSequentialGroup()
								.addComponent(lblusername)
								.addGap(271, 271, 271))
							.addGroup(layout.createSequentialGroup()
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
									.addComponent(lblpassword)
									.addComponent(lblconfirmPassword))
								.addGap(26, 26, 26)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
									.addComponent(checkboxTwoFactor)
									.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
										.addComponent(submitButton, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
											.addComponent(password)
											.addComponent(confirmPassword, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE))
										.addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)))))))
				.addContainerGap(30, Short.MAX_VALUE))
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(Title, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
					.addComponent(lblusername))
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(password, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
					.addComponent(lblpassword))
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(confirmPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
					.addComponent(lblconfirmPassword))
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
				.addComponent(checkboxTwoFactor)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(submitButton)
				.addGap(18, 18, 18)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(generatePasswordButton)
					.addComponent(jLabel5)
					.addComponent(txtpassLength, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(jLabel6)
					.addComponent(txtgeneratedPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
				.addGap(18, 18, 18)
				.addComponent(jLabel1)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(jLabel2)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(jLabel3)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(jLabel4)
				.addGap(27, 27, 27))
		);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void submitButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_submitButtonActionPerformed
		String usernameString = username.getText();
		char[] pass = password.getPassword();
		char[] cpass = confirmPassword.getPassword();
				boolean enableTFA = checkboxTwoFactor.isSelected();

		if(!Arrays.equals(pass, cpass)) {
				System.out.println("Passwords do not match!");
				JOptionPane.showMessageDialog(this, "Passwords do not match!", SandClientGUI.popUpTitle, JOptionPane.ERROR_MESSAGE);
		} else if(usernameString.length()<1 || usernameString.contains(" ")) {
				System.out.println("Username does not meet requirements");
				JOptionPane.showMessageDialog(this, "Username be at least length 1, no spaces", SandClientGUI.popUpTitle, JOptionPane.ERROR_MESSAGE);
		} else if(!LoginUtilities.passwordChecker(usernameString, String.valueOf(pass))) {
				System.out.println("Password does not meet requirements");
				JOptionPane.showMessageDialog(this, "Password does not meet requirements", SandClientGUI.popUpTitle, JOptionPane.ERROR_MESSAGE);
		} else {
				System.out.println("Valid password and username");
				String qrLocation = manager.createAccount(usernameString, pass, enableTFA);
				if (qrLocation != null) {
					System.out.println("Account Created Successfully");
					if(enableTFA) {
						System.out.println(qrLocation);
						QRCodeGUI dialog = new QRCodeGUI(new javax.swing.JFrame(), true, qrLocation);
						dialog.setVisible(true);
					}
					JOptionPane.showMessageDialog(this, "Account Created. Proceed to login.", SandClientGUI.popUpTitle, JOptionPane.INFORMATION_MESSAGE);
					this.dispose();
				} else {
					System.out.println("Account not created. Try again later");
					JOptionPane.showMessageDialog(this, "Account not created. Try again later", SandClientGUI.popUpTitle, JOptionPane.ERROR_MESSAGE);
				}
		}
	}//GEN-LAST:event_submitButtonActionPerformed

	private void generatePasswordButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_generatePasswordButtonActionPerformed
		boolean validNumber = false;
		int passLength = 12;
		String spassLength = txtpassLength.getText();
		if(spassLength.equals("")) {
			validNumber = false;
		} else {
			try {
				passLength = Integer.parseInt(spassLength);
				if(passLength >= 8){
					validNumber = true;
				} else {
					validNumber = false;
				}
			} catch (NumberFormatException e) {
				validNumber = false;
			}
		}
		if(!validNumber) {
			passLength = 12;
			JOptionPane.showMessageDialog(this, "Please enter an integer value greater than or equal to 8. Setting length to 12", SandClientGUI.popUpTitle, JOptionPane.INFORMATION_MESSAGE);
		}

		String strongPass = LoginUtilities.getRandomSecurePassword(passLength);

		txtgeneratedPass.setText(strongPass);
	}//GEN-LAST:event_generatePasswordButtonActionPerformed

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		/* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
		 * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
		 */
		try {
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(AccountCreateGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(AccountCreateGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(AccountCreateGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(AccountCreateGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		//</editor-fold>
		//</editor-fold>

		/* Create and display the form */
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				new AccountCreateGUI(null,null).setVisible(true);
			}
		});
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel Title;
	private javax.swing.JCheckBox checkboxTwoFactor;
	private javax.swing.JPasswordField confirmPassword;
	private javax.swing.JButton generatePasswordButton;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel lblconfirmPassword;
	private javax.swing.JLabel lblpassword;
	private javax.swing.JLabel lblusername;
	private javax.swing.JPasswordField password;
	private javax.swing.JButton submitButton;
	private javax.swing.JTextField txtgeneratedPass;
	private javax.swing.JTextField txtpassLength;
	private javax.swing.JTextField username;
	// End of variables declaration//GEN-END:variables
}
