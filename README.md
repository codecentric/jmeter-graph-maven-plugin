jmeter-graph-maven-plugin
=========================

A maven plugin to create nice graphs (using the JMeter Plugins CMDRunner) from JMeter result files (*.jtl or *.csv) or FilterResultsTool.

See https://jmeter-plugins.org/wiki/JMeterPluginsCMD/ for more information for graphs and graphs parameters.

See https://jmeter-plugins.org/wiki/FilterResultsTool/ form more information for Filter Result Tools

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
                <groupId>de.codecentric</groupId>
                <artifactId>jmeter-graph-maven-plugin</artifactId>
                <version>1.0</version>
                <configuration>
                    <!-- cf Filter Results Tool in jmeter-plugins.org -->
                    <filterResultsTool>
                        <filterResultsParam>
                            <inputFile>${project.build.directory}/jmeter/results/gestdoc_sc01_menu_local_monit.csv</inputFile>
                            <outputFile>${project.build.directory}/jmeter/results/gestdoc_sc01_menu_regex_filtred.csv</outputFile>
                            <successFilter>false</successFilter>
                            <includeLabels>0.*</includeLabels>
                            <includeLabelRegex>true</includeLabelRegex>
                        </filterResultsParam>
                        <filterResultsParam>
                            <inputFile>${project.build.directory}/jmeter/results/gestdoc_sc01_menu_local_monit.csv</inputFile>
                            <outputFile>${project.build.directory}/jmeter/results/gestdoc_sc01_menu_offset_filtred.jtl</outputFile>
                            <successFilter>false</successFilter>
                            <startOffset>2</startOffset>
                            <endOffset>20</endOffset>
                            <saveAsXml>true</saveAsXml>
                        </filterResultsParam>
                    </filterResultsTool>
                    <graphs>
                      <!-- cf Filter JMeterPluginsCMD Command Line Tool in jmeter-plugins.org -->
                        <graph>
                            <pluginType>ResponseTimesOverTime</pluginType>
                            <inputFile>${project.build.directory}/jmeter/results/gestdoc_sc01_menu_local_monit.csv</inputFile>
                            <generatePng>${project.build.directory}/jmeter/results/ResponseTimesOverTime.png</generatePng>
                            <width>800</width>
                            <height>600</height>
                            <limitRows>50</limitRows>
                            <relativeTimes>no</relativeTimes>
                            <aggregateRows>no</aggregateRows>
                            <paintGradient>no</paintGradient>
                            <startOffset>2</startOffset>
                            <endOffset>20</endOffset>
                            <includeLabels>0.*</includeLabels>
                            <includeLabelRegex>true</includeLabelRegex>
                            <forceY>1000</forceY>
                            <autoScale>no</autoScale>
                            <lineWeight>2</lineWeight>
                        </graph>
                        <graph>
                            <inputFile>${project.build.directory}/jmeter/results/gestdoc_sc01_menu_local_monit.csv</inputFile>
                            <pluginType>TransactionsPerSecond</pluginType>
                            <width>800</width>
                            <height>600</height>
                            <generatePng>${project.build.directory}/jmeter/results/TransactionsPerSecond.png</generatePng>
                            <relativeTimes>no</relativeTimes>
                            <aggregateRows>yes</aggregateRows>
                            <paintGradient>no</paintGradient>
                        </graph>
                        <graph>
                            <pluginType>PageDataExtractorOverTime</pluginType>
                            <inputFile>${project.build.directory}/jmeter/results/pde_httpd.jtl</inputFile>
                            <generatePng>${project.build.directory}/jmeter/results/pde_httpd_all_workers.png</generatePng>
                            <extractorRegexps>(BusyWorkers|IdleWorkers):.*{;}[A-Za-z]+:.([0-9]+){;}false{;}true</extractorRegexps>
                            <width>1024</width>
                            <height>800</height>
                            <relativeTimes>no</relativeTimes>
                            <aggregateRows>no</aggregateRows>
                            <paintGradient>no</paintGradient>
                        </graph>
                        <graph>
                            <pluginType>PageDataExtractorOverTime</pluginType>
                            <inputFile>${project.build.directory}/jmeter/results/pde_httpd.jtl</inputFile>
                            <generatePng>${project.build.directory}/jmeter/results/pde_httpd_busy_workers.png</generatePng>
                            <extractorRegexps>(BusyWorkers):.*{;}BusyWorkers:.([0-9]+){;}false{;}true</extractorRegexps>
                            <width>1024</width>
                            <height>800</height>
                            <relativeTimes>no</relativeTimes>
                            <aggregateRows>no</aggregateRows>
                            <paintGradient>no</paintGradient>
                        </graph>
                        <graph>
                            <pluginType>PerfMon</pluginType>
                            <inputFile>${project.build.directory}/jmeter/results/perfmon.csv</inputFile>
                            <generatePng>${project.build.directory}/jmeter/results/Perfmon_CPU.png</generatePng>
                            <includeLabels>.*CPU.*</includeLabels>
                            <includeLabelRegex>true</includeLabelRegex>
                            <width>1024</width>
                            <height>800</height>
                            <relativeTimes>no</relativeTimes>
                            <aggregateRows>no</aggregateRows>
                            <paintGradient>no</paintGradient>
                        </graph>
                        <graph>
                            <pluginType>PerfMon</pluginType>
                            <inputFile>${project.build.directory}/jmeter/results/perfmon.csv</inputFile>
                            <generatePng>${project.build.directory}/jmeter/results/Perfmon_Memory.png</generatePng>
                            <includeLabels>.*Memory.*</includeLabels>
                            <includeLabelRegex>true</includeLabelRegex>
                            <width>1024</width>
                            <height>800</height>
                            <relativeTimes>no</relativeTimes>
                            <aggregateRows>no</aggregateRows>
                            <paintGradient>no</paintGradient>
                        </graph>
                        <graph>
                            <pluginType>JMXMon</pluginType>
                            <inputFile>${project.build.directory}/jmeter/results/gest_jmx_tomcat.jtl</inputFile>
                            <generatePng>${project.build.directory}/jmeter/results/JMX_memory_jvm.png</generatePng>
                            <includeLabels>used.HeapMemoryUsage.*</includeLabels>
                            <includeLabelRegex>true</includeLabelRegex>
                            <width>1024</width>
                            <height>800</height>
                            <relativeTimes>no</relativeTimes>
                            <aggregateRows>no</aggregateRows>
                            <paintGradient>no</paintGradient>
                        </graph>
                        <graph>
                            <pluginType>JMXMon</pluginType>
                            <inputFile>${project.build.directory}/jmeter/results/gest_jmx_tomcat.jtl</inputFile>
                            <generatePng>${project.build.directory}/jmeter/results/JMX_currentThreadsBusy.png</generatePng>
                            <includeLabels>.*currentThreadsBusy.*</includeLabels>
                            <includeLabelRegex>true</includeLabelRegex>
                            <width>1024</width>
                            <height>800</height>
                            <relativeTimes>no</relativeTimes>
                            <aggregateRows>no</aggregateRows>
                            <paintGradient>no</paintGradient>
                        </graph>
                        <graph>
                            <inputFile>${project.build.directory}/jmeter/results/gestdoc_sc01_menu_local_monit.csv</inputFile>
                            <pluginType>AggregateReport</pluginType>
                            <generateCsv>${project.build.directory}/jmeter/results/AggregateReport.csv</generateCsv>
                            <relativeTimes>no</relativeTimes>
                            <aggregateRows>no</aggregateRows>
                            <paintGradient>no</paintGradient>
                        </graph>
                        <graph>
                            <inputFile>${project.build.directory}/jmeter/results/gestdoc_sc01_menu_local_monit.csv</inputFile>
                            <pluginType>ResponseCodesPerSecond</pluginType>
                            <width>800</width>
                            <height>600</height>
                            <generatePng>${project.build.directory}/jmeter/results/ResponseCodesPerSecond.png</generatePng>
                            <relativeTimes>no</relativeTimes>
                            <aggregateRows>no</aggregateRows>
                            <paintGradient>no</paintGradient>
                        </graph>
                    </graphs>
                    <!- copy files in directoryTestFiles to JMETER_HOME/bin -->
                    <directoryTestFiles>${project.build.directory}/jmeter/testFiles</directoryTestFiles>
                    <!-- cf jmeter-maven-pugins -->
                    <jMeterProcessJVMSettings>
                        <xms>${jvm_xms}</xms>
                        <xmx>${jvm_xmx}</xmx>
                    </jMeterProcessJVMSettings>
                    <!-- merge this properties with user.properties file in JMETER_HOME/bin -->
                    <!-- property format = <property_name>property_value</property name> will be property_name=property_value in th user.properties file.
                    <propertiesUser>
                        <language>en</language>
                    </propertiesUser>
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
        <version>1.0</version>
        <executions>
          <execution>
            <id>create-graphs</id>
            <goals>
              <goal>create-graph</goal>
            </goals>
            <phase>verify</phase>
            <configuration>
              <!-- ... you can declare filterResultsTool here -->
             <graphs>
                <graph>
									<inputFile>${project.build.directory}/jmeter/results/gestdoc_sc01_menu_local_monit.csv</inputFile>
									<pluginType>ResponseCodesPerSecond</pluginType>
									<width>800</width>
									<height>600</height>
									<generatePng>${project.build.directory}/jmeter/results/ResponseCodesPerSecond.png</generatePng>
									<relativeTimes>no</relativeTimes>
									<aggregateRows>no</aggregateRows>
									<paintGradient>no</paintGradient>
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
