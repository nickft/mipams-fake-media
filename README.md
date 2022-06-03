# Generating key for example

keytool -genkeypair -ext san=dns:localhost -storetype PKCS12 -keystore server.p12 -storepass password -alias myalias -keyalg RSA -keysize 4096 -dname "C=,S=,L=,O=,OU=,CN=localhost"

keytool -exportcert -rfc -keystore server.p12 -storepass password -alias myalias -file server.public.crt

keytool -list -v -storetype PKCS12 -storepass password -keystore server.p12

openssl x509 -pubkey -in server.public.crt -noout > mypubkey.pem

openssl pkcs12 -in server.p12 -nocerts -nodes -passin pass:password | openssl rsa -outform PEM -out server.private.pem

openssl pkcs8 -topk8 -inform PEM -outform DER -in server.private.pem -out server.private.key -nocrypt
