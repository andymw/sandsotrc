package sand.client.gui;

import java.util.logging.*;

import javax.swing.*;

import common.*;
import java.io.IOException;
import sand.client.*;

/**
 *
 * @author spencer
 */
public class LoginGUI extends JFrame {
    private SandClientManager manager;
    private SandClientGUI parentWindow;
    private boolean offlineMode;
    /**
     * Sets up the login GUI
     * @param man - manager being passed
     * @param g - reference to the SandClientGUI
     * @param offline - online or offline login status
     */
    public LoginGUI(SandClientManager man, SandClientGUI g, boolean offline) {
        manager = man;
        parentWindow = g;
        offlineMode = offline;
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
        lblusername = new javax.swing.JLabel();
        lblpassword = new javax.swing.JLabel();
        password = new javax.swing.JPasswordField();
        loginButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        Title.setFont(new java.awt.Font("Noto Sans", 0, 18)); // NOI18N
        Title.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Title.setText("S.A.N.D. Password Manager ");

        lblusername.setText("Username");

        lblpassword.setText("Password");

        loginButton.setText("Login");
        loginButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(Title)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblusername)
                            .addComponent(lblpassword))
                        .addGap(28, 28, 28)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(username)
                            .addComponent(password, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(loginButton, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(31, Short.MAX_VALUE))
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
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblpassword)
                    .addComponent(password, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(loginButton, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void loginButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginButtonActionPerformed
        String usernameString = username.getText();
        char[] pass = password.getPassword();

        if(!LoginUtilities.passwordChecker(usernameString, String.valueOf(pass))) {
                System.out.println("Password does not meet requirements");
                JOptionPane.showMessageDialog(this, "Password does not meet requirements", SandClientGUI.popUpTitle, JOptionPane.ERROR_MESSAGE);
        } else {
                boolean loggedIn = false;
                System.out.println("Good password and username");

                if(!offlineMode) {
                    if(manager.login(usernameString, pass)) {
                        loggedIn = true;
                    } else {
                        loggedIn = false;
                    }
                } else {
                    if(manager.loginOffline(usernameString, pass)) {
                        loggedIn = true;
                    } else {
                        loggedIn = false;
                    }
                }

                if(loggedIn) {
                    // Two Factor Authentication
                    if(manager.needsTwoFactorAuthentication()) {
                        long code = 0;
                        try {
                            String s = (String)JOptionPane.showInputDialog(this,"Please enter generated two-factor authentication code", "2-Factor Authentication",
                                    JOptionPane.PLAIN_MESSAGE, null, null,"");

                            code = Long.parseLong(s);
                            if(!manager.twoFactorAuthenticate(code)) {

                                        JOptionPane.showMessageDialog(this, "Login Failed", SandClientGUI.popUpTitle, JOptionPane.ERROR_MESSAGE);
                                        this.dispose();
                                        parentWindow.setVisible(true);
                                        return;
                            }
                        } catch (NumberFormatException | IOException e) {
                            JOptionPane.showMessageDialog(this, "Login Failed. Try loggin in again.", SandClientGUI.popUpTitle, JOptionPane.ERROR_MESSAGE);
                            try{
                                manager.logout();
                                this.dispose();
                                parentWindow.setVisible(true);
                                return;
                            } catch(IOException ioe) {}
                        }
                    }

                    if(manager.checkForAbnormalLogin()) {
                            JOptionPane.showMessageDialog(this, "Abnormal login was detected!", SandClientGUI.popUpTitle, JOptionPane.WARNING_MESSAGE);
                    }

                    System.out.println("Logging in");

                    // Start up the program
                    SandActionsGUI gui = new SandActionsGUI(manager, parentWindow, offlineMode);
                    gui.setTitle("S.A.N.D.");
                    gui.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    gui.pack();
                    gui.setVisible(true);
                    this.dispose();
                } else {
                    System.out.println("Login failed");
                    JOptionPane.showMessageDialog(this, "Login failed", SandClientGUI.popUpTitle, JOptionPane.ERROR_MESSAGE);
                    this.dispose();
                    parentWindow.setVisible(true);
                }
        }
    }//GEN-LAST:event_loginButtonActionPerformed

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
            Logger.getLogger(LoginGUI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(LoginGUI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(LoginGUI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(LoginGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new LoginGUI(null, null,true).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Title;
    private javax.swing.JLabel lblpassword;
    private javax.swing.JLabel lblusername;
    private javax.swing.JButton loginButton;
    private javax.swing.JPasswordField password;
    private javax.swing.JTextField username;
    // End of variables declaration//GEN-END:variables
}
