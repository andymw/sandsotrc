RSA keys created using ssh-keygen
Public key is admin.pub, encrypted private key is admin.aes256cbc

Private key encrypted using CryptoUtils default AES settings
  (aes256/cbc/pkcs5 padding)
  password is an admin password known to SAND members.
Decrypted private key (admin.dec) will be in DER format (binary),
  used directly with PKCS8EncodedKeySpec, and does not have a password.
Private key decrypted (and encrypted) using AdminPrivateKeyDecryptor

Public key is in openssl ssh-rsa format.
The equivalent public key admin.pub.der (in DER format, binary)
  was generated from a PEM format private key

Helpful conversions:
Private key (DER) -> PEM:
  openssl rsa -inform DER -in admin.dec -outform PEM -out admin.pem
Private key (PEM) -> DER:
  openssl pkcs8 -topk8 -inform PEM -outform DER -in admin.pem -out admin.der -nocrypt
Private key (PEM) -> Public key (openssl ssh-rsa format) (view):
  ssh-keygen -y -f admin.pem
Private key (PEM) -> Public key (DER):
  openssl rsa -in admin.pem -pubout -outform DER -out admin.pub.der
Public key (openssl ssh-rsa format) -> PEM:
  ssh-keygen -f admin.pub -e -m pem > admin.pub.pem
Public key (DER) -> PEM (view):
  openssl rsa -in admin.pub.der -inform DER -pubin
Public key (DER or PEM) -> openssl ssh format
  ??
-
