5430ca.crt
  The CA Public Key (key of CS 5430) from
    http://www.cs.cornell.edu/courses/CS5430/2015sp/5430ca.crt
asw275.crt
  Our public key (our certificate)
    signed by the CS 5430 CA's private key
  Generated as a cert that expires in 120 days, and uses RSA 4096

clientside/5430ts.jks
  this is our client truststore that contains only the CA's public key
serverside/sandsotrcpistore.jks
  keystore with CA public key installed and CA's certificate on our key
    (asw75.crt) installed
-
