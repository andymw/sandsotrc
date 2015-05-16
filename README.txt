SANDSOTRC password manager and chat system

Pre-requisites to compiling:
  - ant
  - java 7
  - jce installed at $JAVA_HOME/jre/lib/security/

Execution:
  1. ant
  2. cd bin/
  To run the SAND password manager:
    java sand.client.SandClientCLI [host]
    OR
    java sand.client.gui.SandClientGUI [host]
  To run the SAND server:
    java sand.server.SandServer
    You will need the password to unlock the server keystore.

  To set up Two-Factor authentication, download the Google authenticator app:
    iOS:
      https://itunes.apple.com/us/app/google-authenticator/id388497605?mt=8
    Android:
      https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2&hl=en
-
