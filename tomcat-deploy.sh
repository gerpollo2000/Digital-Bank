#!/bin/bash
TOMCAT_USER='root'
SHUTDOWN_WAIT=20
svnDir=/root/Digital-Bank
deployDir=/opt/tomcat/apache-tomcat-9.0.40/webapps/
tomcatDir=/opt/tomcat/apache-tomcat-9.0.40/
warName=digitalbank-2.1.0.local.war
warFile=$svnDir/target/$warName

export CATALINA_BASE=$tomcatDir
export CATALINA_HOME=$CATALINA_BASE
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/
export JRE_HOME=/usr/lib/jvm/java-11-openjdk-amd64/
export PATH=$PATH:$JAVA_HOME/bin
export CLASSPATH=$JAVA_HOME/lib
export LD_LIBRARY_PATH=/usr/local/lib
export CONF_DIR=$deployDir/ROOT_conf
export CLASSPATH=$CLASSPATH:$CONF_DIR
export CATALINA_PID="$CATALINA_HOME/tomcat.pid"
export JAVA_OPTS="-server -d64 -Djava.awt.headless=true -Xms2G -Xmx2G  -Xmn700m -XX:PermSize=128m -XX:MaxPermSize=512m -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection  -XX:CMSMaxAbortablePrecleanTime=5000 -XX:CMSInitiatingOccupancyFraction=80  -XX:+DisableExplicitGC  -XX:+CMSClassUnloadingEnabled -XX:+PrintGCDetails -XX:+PrintGCTimeStamps  -Djava.net.preferIPv4Stack=true -Dorg.apache.catalina.session.StandardSession.ACTIVITY_CHECK=true"


tomcat_pid() {
  echo `ps aux | grep $CATALINA_HOME | grep -v grep | awk '{ print $2 }'`
}

start() {
	systemctl start tomcat
   return 0
}

stop() {
	systemctl stop tomcat
  return 0
}

status() {
	systemctl status tomcat
}

deploy(){
    systemctl stop tomcat
    cd $svnDir
    git pull
    mvn clean package
    rm -r  /opt/tomcat/latest/webapps/ban*
    rm -r  /opt/tomcat/latest/webapps/digi*
    rm -r  /opt/tomcat/latest/webapps/ROO*
    cp -f /root/Digital-Bank/target/digitalbank-2.1.0.local.war /opt/tomcat/latest/webapps/ROOT.war
    systemctl start tomcat
    
}


case "$1" in
 start)
        start
        ;;
 stop)
        stop
        ;;
 restart)
       stop
       start
       ;;
 deploy)
       stop
       deploy
       start
       ;;
 status)
       status
       ;;
*)
        echo "Usage: $0 {start|stop|restart|deploy|status}"
        exit 1
        ;;
esac
exit 0
