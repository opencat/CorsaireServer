@echo off
@title Login Server
set CLASSPATH=.;dist\v88.jar;dist\mina-core.jar;dist\slf4j-api.jar;dist\slf4j-jdk14.jar;dist\mysql-connector-java-bin.jar
"C:\Program Files\Java\jdk1.6.0_24\bin\java.exe" -Xmx300m -Dwzpath=wz\ -Djavax.net.ssl.keyStore=filename.keystore -Djavax.net.ssl.keyStorePassword=passwd -Djavax.net.ssl.trustStore=filename.keystore -Djavax.net.ssl.trustStorePassword=passwd -Drecvops=recvops.properties -Dsendops=sendops.properties net.login.LoginServer
pause
