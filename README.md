[![Build Status](https://api.travis-ci.org/Cka3o4Huk/jmeter-graph-maven-plugin.svg?branch=master)](https://travis-ci.org/Cka3o4Huk/jmeter-graph-maven-plugin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.Cka3o4Huk.jmeter-graph-plugin/jmeter-graph-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.Cka3o4Huk.jmeter-graph-plugin/jmeter-graph-maven-plugin)

jmeter-graph-maven-plugin
=========================

A maven plugin to create nice graphs (using the JMeter Plugins CMDRunner) from JMeter result files (*.jtl).

See https://blog.codecentric.de/2013/12/jmeter-tests-mit-maven-und-jenkins-automatisieren/ for more information.

For a full example, take a look at the [jmeter-maven-example project](https://github.com/mlex/jmeter-maven-example/).

Usage
-----

Just include the plugin in your `pom.xml` and execute `mvn jmeter-graph:create-graph`.

```xml
<project>
  <!-- ... -->
  <build>
    <plugins>
      <plugin>
        <groupId>com.github.Cka3o4Huk.jmeter-graph-plugin</groupId>
        <artifactId>jmeter-graph-maven-plugin</artifactId>
        <version>0.1.1</version>
        <configuration>
          <inputFile>${project.build.directory}/jmeter/results/SimpleWebservicePerformanceTest.jtl</inputFile>
          <graphs>
            <graph>
              <pluginType>ThreadsStateOverTime</pluginType>
              <width>800</width>
              <height>600</height>
              <outputFile>${project.build.directory}/jmeter/results/SimpleWebservicePerformanceTest-ThreadsStateOverTime.png</outputFile>
            </graph>
            <!-- ... you can declare more <graph>-elements here -->
          </graphs>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

You can also bind the graph-generation to a maven-phase, e.g. `verify`:

```xml
<project>
  <!-- ... -->
  <build>
    <plugins>
      <plugin>
        <groupId>de.codecentric</groupId>
        <artifactId>jmeter-graph-maven-plugin</artifactId>
        <version>0.1.0</version>
        <executions>
          <execution>
            <id>create-graphs</id>
            <goals>
              <goal>create-graph</goal>
            </goals>
            <phase>verify</phase>
            <configuration>
              <inputFile>${project.build.directory}/jmeter/results/SimpleWebservicePerformanceTest.jtl</inputFile>
              <graphs>
                <graph>
                  <pluginType>ThreadsStateOverTime</pluginType>
                  <width>800</width>
                  <height>600</height>
                  <outputFile>${project.build.directory}/jmeter/results/SimpleWebservicePerformanceTest-ThreadsStateOverTime.png</outputFile>
                </graph>
                <!-- ... you can declare more <graph>-elements here -->
              </graphs>
            </configuration>
          </execution>
        </execution>
      </plugin>
    </plugins>
  </build>
</project>
```
