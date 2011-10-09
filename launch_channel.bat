@echo off
@title Channel Server
set CLASSPATH=.;dist\odinms.jar;dist\mina-core.jar;dist\slf4j-api.jar;dist\slf4j-jdk14.jar;dist\mysql-connector-java-bin.jar
java -Xmx500m -Dwzpath=wz\ -Djavax.net.ssl.keyStore=filename.keystore -Djavax.net.ssl.keyStorePassword=passwd  -Djavax.net.ssl.trustStore=filename.keystore -Djavax.net.ssl.trustStorePassword=passwd  -Dcom.sun.management.jmxremote.access.file=jmxremote.access -Drecvops=recvops.properties -Dsendops=sendops.properties net.channel.ChannelServer 
pause