Building
========
In order to build this plugin, you need to have a couple things in your local Maven repository.  Do a 'mvn clean install' of net.epicforce.migrate.ahp, and import the UCB client.

The UCB client can be imported as follows.  Download the ibm-ucb-client.zip file from your install of UCB (it's under 'Tools' in the upper right), and then unzip it.  There should be some documentation and a JAR.  Import the JAR with the following command:

```
mvn install:install-file -Dfile=ibm-ucb-client.jar \
                         -DgroupId=com.urbancode \
                         -DartifactId=ibm-ucb-client \
                         -Dversion=1.0 \
                         -Dpackaging=jar
```

After these two steps, a simple:

```
mvn clean package
```

will create a jar which you can run with:

```
java -jar target/ahp2ucb-1.0-SNAPSHOT.jar
```

