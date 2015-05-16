/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sotrc.client.gui;


import java.util.Arrays;
import javax.swing.JOptionPane;

import common.*;
import sotrc.client.ClientManager;

/**
 *
 * @author spencer
 */
public class EditUserInfoGUI extends javax.swing.JFrame {
	ClientManager manager;
	/**
	 * Creates new form EditUserInfoGUI
	 */
	public EditUserInfoGUI(ClientManager man) {
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

		jTextField1 = new javax.swing.JTextField();
		UpdatePasswordPanel = new javax.swing.JPanel();
		jLabel3 = new javax.swing.JLabel();
		jLabel4 = new javax.swing.JLabel();
		jLabel5 = new javax.swing.JLabel();
		pswdConfirm = new javax.swing.JPasswordField();
		pswdNew = new javax.swing.JPasswordField();
		pswdOld = new javax.swing.JPasswordField();
		updatePasswordButton = new javax.swing.JButton();

		jTextField1.setText("jTextField1");

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

		UpdatePasswordPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Update Password"));

		jLabel3.setText("Old Password");

		jLabel4.setText("New Password");

		jLabel5.setText("Confirm Pasword");

		updatePasswordButton.setText("Submit");
		updatePasswordButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				updatePasswordButtonActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout UpdatePasswordPanelLayout = new javax.swing.GroupLayout(UpdatePasswordPanel);
		UpdatePasswordPanel.setLayout(UpdatePasswordPanelLayout);
		UpdatePasswordPanelLayout.setHorizontalGroup(
			UpdatePasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, UpdatePasswordPanelLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(UpdatePasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
					.addGroup(UpdatePasswordPanelLayout.createSequentialGroup()
						.addGap(0, 0, Short.MAX_VALUE)
						.addComponent(updatePasswordButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
					.addGroup(UpdatePasswordPanelLayout.createSequentialGroup()
						.addGroup(UpdatePasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
							.addComponent(jLabel5)
							.addComponent(jLabel4)
							.addComponent(jLabel3))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(UpdatePasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
							.addComponent(pswdConfirm, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
							.addComponent(pswdNew)
							.addComponent(pswdOld))))
				.addContainerGap())
		);
		UpdatePasswordPanelLayout.setVerticalGroup(
			UpdatePasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(UpdatePasswordPanelLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(UpdatePasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(jLabel3)
					.addComponent(pswdOld, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
				.addGap(5, 5, 5)
				.addGroup(UpdatePasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(jLabel4)
					.addComponent(pswdNew, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
				.addGap(6, 6, 6)
				.addGroup(UpdatePasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
					.addComponent(jLabel5)
					.addComponent(pswdConfirm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
				.addComponent(updatePasswordButton)
				.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
		);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(UpdatePasswordPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addContainerGap())
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(UpdatePasswordPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
				.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
		);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void updatePasswordButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updatePasswordButtonActionPerformed
		char[] oldPass = pswdOld.getPassword();
		char[] newPass = pswdNew.getPassword();
		char[] confirmPass = pswdConfirm.getPassword();

		if(!Arrays.equals(newPass, confirmPass)) {
				System.out.println("Passwords do not match!");
				JOptionPane.showMessageDialog(this, "Passwords do not match!", "SOTRC", JOptionPane.ERROR_MESSAGE);
		} else if(!LoginUtilities.guidelineChecker(String.valueOf(newPass))) {
				System.out.println("Password does not meet requirements");
				JOptionPane.showMessageDialog(this, "Password does not meet requirements", "SOTRC", JOptionPane.ERROR_MESSAGE);
		} else {
			int selectedOption = JOptionPane.showConfirmDialog(this,
					"Are you sure you want to change your password?\n",
					"Change Password?",
					JOptionPane.YES_NO_OPTION);
			if (selectedOption == JOptionPane.YES_OPTION) {
				if(manager.changePassword(oldPass, newPass)){
					pswdOld.setText("");
					pswdNew.setText("");
					pswdConfirm.setText("");
								JOptionPane.showMessageDialog(this, "Password changed", "SOTRC", JOptionPane.INFORMATION_MESSAGE);
				} else {
					System.out.println("Password not changed");
					JOptionPane.showMessageDialog(this, "Password not changed", "SOTRC", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}//GEN-LAST:event_updatePasswordButtonActionPerformed

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
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(EditUserInfoGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(EditUserInfoGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(EditUserInfoGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(EditUserInfoGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		//</editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new EditUserInfoGUI(new ClientManager()).setVisible(true);
			}
		});
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JPanel UpdatePasswordPanel;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JTextField jTextField1;
	private javax.swing.JPasswordField pswdConfirm;
	private javax.swing.JPasswordField pswdNew;
	private javax.swing.JPasswordField pswdOld;
	private javax.swing.JButton updatePasswordButton;
	// End of variables declaration//GEN-END:variables
}
