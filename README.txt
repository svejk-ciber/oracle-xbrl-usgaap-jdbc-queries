Example of talking JDBC with an Oracle 11G EE database with Oracle's XBRL 
extension installed. First, you need to set up the sample USGAAP 2008 XBRL repository 
as described in:
  http://docs.oracle.com/cd/E20212_01/doc/doc.11/e17070/using_xbrl.htm
Then you can run this program to execute the sample queries through JDBC.

Download ojdbc6.jar and xdb6.jar from:
  http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-112010-090769.html
  
Install them in your local repo:
$ mvn install:install-file -Dfile=$HOME/Downloads/ojdbc6.jar -DgroupId=com.oracle \
		-DartifactId=ojdbc6 -Dversion=11.2.0.3 -Dpackaging=jar -DgeneratePom=true
  
$ mvn install:install-file -Dfile=$HOME/Downloads/xdb6.jar -DgroupId=com.oracle \
		-DartifactId=xdb6 -Dversion=11.2.0.3 -Dpackaging=jar -DgeneratePom=true 
		
Also install xmlparserv2.jar from the DB distribution (otherwise you'll get 
NullPointerExceptions dealing with Oracle's XMLType):		
$ mvn install:install-file -Dfile=$ORACLE_HOME/lib/xmlparserv2.jar -DgroupId=com.oracle \
		-DartifactId=xmlparserv2 -Dversion=11.2.0.3 -Dpackaging=jar -DgeneratePom=true

To build and run:
$ mvn install
$ java -classpath \
$HOME/.m2/repository/com/oracle/xdb6/11.2.0.3/xdb6-11.2.0.3.jar:\
$HOME/.m2/repository/com/oracle/ojdbc6/11.2.0.3/ojdbc6-11.2.0.3.jar:\
$HOME/.m2/repository/com/oracle/xmlparserv2/11.2.0.3/xmlparserv2-11.2.0.3.jar:\
target/classes \
  org.examle.xbrl.RunQueries          