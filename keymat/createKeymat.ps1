# This script creates all required key material.

# Change to the folder of this script
$scriptpath = $MyInvocation.MyCommand.Path
$dir = Split-Path $scriptpath
Set-Location $dir

# Remove all previously generated files
Remove-Item -Force *.crt -ErrorAction Ignore
Remove-Item -Force *.csr -ErrorAction Ignore
Remove-Item -Force *.key -ErrorAction Ignore
Remove-Item -Force *.p12 -ErrorAction Ignore
Remove-Item -Force *.pem -ErrorAction Ignore
Remove-Item -Force *.pfx -ErrorAction Ignore
Remove-Item -Force *.srl -ErrorAction Ignore

# Generate Root Certificate Authority for the server
openssl genrsa -des3 -out rootCA.key -passout pass:testrootCA 2048
openssl req -x509 -new -nodes -key rootCA.key -passin pass:testrootCA -sha256 -days 4096 -out rootCA.pem -subj "/C=UK/ST=Berkshire/L=Bracknell/O=Fujitsu/OU=MutualTLSIssueReproduction/CN=mutualtlsserverCA"
openssl x509 -outform der -in rootCA.pem -out rootCA.crt 

# Generate Root Certificate Authority for clients (separate from main root CA)
openssl genrsa -des3 -out clientRootCA.key -passout pass:testrootCA 2048
openssl req -x509 -new -nodes -key clientRootCA.key -passin pass:testrootCA -sha256 -days 4096 -out clientRootCA.pem -subj "/C=UK/ST=Berkshire/L=Bracknell/O=Fujitsu/OU=MutualTLSIssueReproduction/CN=mutualtlsclientCA"
openssl x509 -outform der -in clientRootCA.pem -out clientRootCA.crt 

# Create (valid) test server certificate
openssl req -out server.csr -newkey rsa:2048 -nodes -keyout server.key -subj "/C=UK/ST=Berkshire/L=Bracknell/O=Fujitsu/OU=MutualTLSIssueReproduction/CN=mutualtls.devtest" -config testserver-csr.cnf
openssl x509 -req -in server.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out server.crt -sha256 -days 3650 -extfile .\testserver-csr.cnf -extensions req_ext -passin pass:testrootCA
openssl pkcs12 -export -out server.pfx -inkey server.key -in server.crt -name server.mutualtls.devtest -passout pass:mutualtls

# Create (valid) test client certificate - NOTE: signed by the client root CA, i.e. different chain of trust than the server
openssl req -out client.csr -newkey rsa:2048 -nodes -keyout client.key -subj "/C=UK/ST=Berkshire/L=Bracknell/O=Fujitsu/OU=MutualTLSIssueReproduction/CN=client" -config testclient-csr.cnf
openssl x509 -req -in client.csr -CA clientRootCA.pem -CAkey clientRootCA.key -CAcreateserial -out client.crt -sha256 -days 3650 -extfile .\testclient-csr.cnf -extensions req_ext -passin pass:testrootCA
openssl pkcs12 -export -out client.pfx -inkey client.key -in client.crt -name mutualtlsclient -passout pass:mutualtls

####
# Keystore and Truststore for the server

# Create (valid) server keystore
keytool -importkeystore -destkeystore server-keystore.p12 -srckeystore server.pfx -srcstoretype PKCS12 -alias server.mutualtls.devtest -srcstorepass mutualtls -deststorepass mutualtls -noprompt
# Confirm that the private key is in the store 
keytool -list -v -keystore server-keystore.p12 -storepass mutualtls

# Create (valid) server truststore
keytool -import -trustcacerts -alias rootca -file rootCA.pem -keystore server-truststore.p12 -storepass mutualtls -noprompt
keytool -import -alias mutualtlsclient -file client.crt -keystore server-truststore.p12 -storepass mutualtls -noprompt
# Confirm that the correct certs are in the store 
keytool -list -v -keystore server-truststore.p12 -storepass mutualtls

####
# Keystore and Truststore for the client

# Create Guardpost trust store for test

keytool -import -trustcacerts -alias rootca -file rootCA.pem -keystore client-truststore.p12 -storepass mutualtls -noprompt
keytool -list -v -keystore client-truststore.p12 -storepass mutualtls

# Create Guardpost key store for test
keytool -importkeystore -destkeystore client-keystore.p12 -srckeystore client.pfx -srcstoretype PKCS12 -alias mutualtlsclient -srcstorepass mutualtls -deststorepass mutualtls -noprompt
keytool -list -v -keystore client-keystore.p12 -storepass mutualtls
