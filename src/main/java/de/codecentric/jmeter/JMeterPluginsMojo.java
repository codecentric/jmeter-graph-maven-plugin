package de.codecentric.jmeter;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "create-graph")
public class JMeterPluginsMojo extends AbstractMojo {

	public static final String JMETER_CONFIG_ARTIFACT_NAME = "ApacheJMeter_config";
	public static final String JMETER_PLUGINS_ARTIFACT_NAME1 = "cmdrunner";
	public static final String JMETER_PLUGINS_ARTIFACT_NAME2 = "jmeter-plugins";

	public static final String JMETER_ARTIFACT_NAME = "ApacheJMeter";
	public static final String JMETER_CORE_ARTIFACT_NAME = "ApacheJMeter_core";
	public static final String JMETER_HTTP_ARTIFACT_NAME = "ApacheJMeter_http";
	public static final String COMMON_IO_NAME = "commons-io";

	@Parameter
	File directoryTestFiles;

	@Parameter
	List<Graph> graphs;

	@Parameter
	List<FilterResultParam> filterResultsTool;

	@Parameter
	JMeterProcessJVMSettings jMeterProcessJVMSettings;

	@Parameter
	Map<String, String> propertiesUser;

	@Parameter(defaultValue = "${project.build.directory}/jmeter")
	File workingDirectory;

	@Component
	MavenProject mavenProject;

	@Component
	MavenSession mavenSession;

	@Component
	BuildPluginManager pluginManager;

	@Component
	PluginDescriptor plugin;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (graphs == null && filterResultsTool == null) {
			getLog().error("Error no graphs defined and no filters defined");
			return;
		}

		File binDir = new File(workingDirectory.getAbsolutePath() + File.separator + "bin");
		File logDir = new File(workingDirectory.getAbsolutePath() + File.separator + "log");
		File libDir = new File(workingDirectory.getAbsolutePath() + File.separator + "lib");
		File libExtDir = new File(libDir.getAbsolutePath() + File.separator + "ext");

		createDirectoryIfNotExists(workingDirectory);
		createDirectoryIfNotExists(binDir);
		createDirectoryIfNotExists(logDir);
		createDirectoryIfNotExists(libDir);
		createDirectoryIfNotExists(libExtDir);

		for (Artifact artifact : plugin.getArtifacts()) {
			try {
				if (JMETER_CONFIG_ARTIFACT_NAME.equals(artifact.getArtifactId())) {
					// properties files, log4j2.xml and beanshell scripts
					getLog().info("Copy configuration files to " + binDir.getAbsolutePath());
					JarFile jarFile = new JarFile(artifact.getFile());
					Enumeration<JarEntry> entries = jarFile.entries();
					while (entries.hasMoreElements()) {
						JarEntry entry = entries.nextElement();
						if (!entry.isDirectory() && entry.getName().startsWith("bin")
								&& (entry.getName().endsWith(".properties") || entry.getName().endsWith(".xml")
										|| entry.getName().endsWith(".bsh"))) {
							File configFileDestination = new File(workingDirectory + File.separator + entry.getName());
							getLog().debug("Write configuration " + configFileDestination.getAbsoluteFile());
							FileUtils.copyInputStreamToFile(jarFile.getInputStream(entry), configFileDestination);
						}
					}
					jarFile.close();
				} else if (isJMeterDependency(artifact)) {
					getLog().debug("Copy artifact " + artifact.toString() + " to " + libDir.getAbsolutePath());
					File artifactDestination = new File(
							libDir.getAbsolutePath() + File.separator + artifact.getArtifactId() + ".jar");
					FileUtils.copyFile(artifact.getFile(), artifactDestination);
				} else if (isJMeterPluginsDependency(artifact)) {
					getLog().debug("Copy artifact " + artifact.toString() + " to " + libExtDir.getAbsolutePath());
					File artifactDestination = new File(
							libExtDir.getAbsolutePath() + File.separator + artifact.getArtifactId() + ".jar");
					FileUtils.copyFile(artifact.getFile(), artifactDestination);
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Could not copy JMeter dependencies to working directory", e);
			}
		}

		if (directoryTestFiles != null) {
			getLog().info("directoryConfigure=" + directoryTestFiles.getAbsolutePath());
			if (directoryTestFiles.isDirectory()) {

				getLog().info("Copy files in directory (directoryTestFiles) : " + directoryTestFiles.getAbsolutePath() + " to "
						+ binDir.getAbsolutePath());
				try {
					FileUtils.copyDirectory(directoryTestFiles, binDir);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				getLog().warn("WARN directoryTestFiles is not a directory : " + directoryTestFiles.getAbsolutePath());
			}

		}
		// merge user.properties and propertiesUser from xml configuration
		if (propertiesUser != null) {
			Properties userProp = new Properties();
			InputStream fileStream;
			try {
				fileStream = new FileInputStream(binDir.getAbsolutePath() + "/" + "user.properties");
				try {
					userProp.load(fileStream);
					fileStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			for (Map.Entry<String, String> entry : propertiesUser.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();

				userProp.put(key, value);

				getLog().info("Merge user properties, add key : " + key + " value : " + value + " to user.properties");
			}

			OutputStream output;
			try {
				output = new FileOutputStream(binDir.getAbsolutePath() + "/" + "user.properties");
				userProp.store(output, "user.properties merged");
				output.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (filterResultsTool != null) {
			for (FilterResultParam filterResultParam : filterResultsTool) {
				getLog().info(
						"Filtering results: " + filterResultParam != null ? filterResultParam.toString() : "<null>");
				try {

					Vector<org.twdata.maven.mojoexecutor.MojoExecutor.Element> listArguments = new Vector<Element>();

					Element elt = element(name("argument"), "-Dlog_file=");
					listArguments.add(elt);

					if (jMeterProcessJVMSettings != null) {
						getLog().info("Creating jMeterProcessJVMSettings: " + jMeterProcessJVMSettings != null
								? jMeterProcessJVMSettings.toString()
								: "<null>");
						if (jMeterProcessJVMSettings.xms != null) {
							elt = element(name("argument"),
									"-Xms" + String.valueOf(jMeterProcessJVMSettings.xms) + "m");
							listArguments.add(elt);
						}
						if (jMeterProcessJVMSettings.xmx != null) {
							elt = element(name("argument"),
									"-Xmx" + String.valueOf(jMeterProcessJVMSettings.xmx) + "m");
							listArguments.add(elt);
						}

						if (jMeterProcessJVMSettings.arguments != null) {
							for (Argument argument : jMeterProcessJVMSettings.arguments) {
								elt = element(name("argument"), argument.argument);
								listArguments.add(elt);
							}
						}
					}

					if (System.getProperty("log4j.configurationFile") == null) {
						elt = element(name("argument"), "-Dlog4j.configurationFile=" + binDir + "/log4j2.xml");
						listArguments.add(elt);
					}

					elt = element(name("argument"), "-classpath");
					listArguments.add(elt);

					elt = element(name("argument"), libDir.getAbsolutePath() + File.separator + "*" + File.pathSeparator
							+ libExtDir.getAbsolutePath() + File.separator + "*");
					listArguments.add(elt);

					elt = element(name("argument"), "kg.apc.cmd.UniversalRunner");
					listArguments.add(elt);

					elt = element(name("argument"), "--tool");
					listArguments.add(elt);

					elt = element(name("argument"), "FilterResults");
					listArguments.add(elt);

					// mandatory
					if (filterResultParam.inputFile != null) {
						elt = element(name("argument"), "--input-file");
						listArguments.add(elt);
						elt = element(name("argument"), filterResultParam.inputFile.getAbsolutePath());
						listArguments.add(elt);
					} else {
						getLog().error("Error filter inputFile is mandatory and inputFile=null");
						continue;
					}

					if (filterResultParam.outputFile != null) {
						elt = element(name("argument"), "--output-file");
						listArguments.add(elt);
						elt = element(name("argument"), filterResultParam.outputFile.getAbsolutePath());
						listArguments.add(elt);
					} else {
						getLog().error("Error filter outputFile is mandatory and outputFile=null");
						continue;
					}

					// optional

					if (filterResultParam.successFilter != null) {
						elt = element(name("argument"), "--success-filter");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(filterResultParam.successFilter));
						listArguments.add(elt);
					}

					if (filterResultParam.includeLabels != null) {
						elt = element(name("argument"), "--include-labels");
						listArguments.add(elt);
						elt = element(name("argument"), filterResultParam.includeLabels);
						listArguments.add(elt);
					}

					if (filterResultParam.excludeLabels != null) {
						elt = element(name("argument"), "--exclude-labels");
						listArguments.add(elt);
						elt = element(name("argument"), filterResultParam.excludeLabels);
						listArguments.add(elt);
					}

					if (filterResultParam.includeLabelRegex != null) {
						elt = element(name("argument"), "--include-label-regex");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(filterResultParam.includeLabelRegex));
						listArguments.add(elt);
					}

					if (filterResultParam.excludeLabelRegex != null) {
						elt = element(name("argument"), "--exclude-label-regex");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(filterResultParam.excludeLabelRegex));
						listArguments.add(elt);
					}

					if (filterResultParam.startOffset != null) {
						elt = element(name("argument"), "--start-offset");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(filterResultParam.startOffset));
						listArguments.add(elt);
					}

					if (filterResultParam.endOffset != null) {
						elt = element(name("argument"), "--end-offset");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(filterResultParam.endOffset));
						listArguments.add(elt);
					}

					if (filterResultParam.saveAsXml != null) {
						elt = element(name("argument"), "--save-as-xml");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(filterResultParam.saveAsXml));
						listArguments.add(elt);
					}

					List<MojoExecutor.Element> configuration = new ArrayList<MojoExecutor.Element>();
					configuration.add(element("executable", "java"));

					List<MojoExecutor.Element> elements = new ArrayList<MojoExecutor.Element>();

					for (Element argument : listArguments) {
						elements.add(argument);
					}

					MojoExecutor.Element parentArgs = new MojoExecutor.Element("arguments",
							elements.toArray(new MojoExecutor.Element[0]));

					configuration.add(parentArgs);
					executeMojo(plugin(groupId("org.codehaus.mojo"), artifactId("exec-maven-plugin"), version("1.2.1")),
							goal("exec"), configuration(configuration.toArray(new MojoExecutor.Element[0])),
							executionEnvironment(mavenProject, mavenSession, pluginManager));
				} catch (Throwable throwable) {
					throw new RuntimeException(throwable);
				}
			}
		}

		if (graphs != null) {
			for (Graph graph : graphs) {
				getLog().info("Creating graph: " + graph != null ? graph.toString() : "<null>");
				try {

					Vector<org.twdata.maven.mojoexecutor.MojoExecutor.Element> listArguments = new Vector<Element>();

					Element elt = element(name("argument"), "-Dlog_file=");
					listArguments.add(elt);

					if (jMeterProcessJVMSettings != null) {
						getLog().info("Creating jMeterProcessJVMSettings: " + jMeterProcessJVMSettings != null
								? jMeterProcessJVMSettings.toString()
								: "<null>");
						if (jMeterProcessJVMSettings.xms != null) {
							elt = element(name("argument"),
									"-Xms" + String.valueOf(jMeterProcessJVMSettings.xms) + "m");
							listArguments.add(elt);
						}
						if (jMeterProcessJVMSettings.xmx != null) {
							elt = element(name("argument"),
									"-Xmx" + String.valueOf(jMeterProcessJVMSettings.xmx) + "m");
							listArguments.add(elt);
						}

						if (jMeterProcessJVMSettings.arguments != null) {
							for (Argument argument : jMeterProcessJVMSettings.arguments) {
								elt = element(name("argument"), argument.argument);
								listArguments.add(elt);
							}
						}
					}

					if (System.getProperty("log4j.configurationFile") == null) {
						elt = element(name("argument"), "-Dlog4j.configurationFile=" + binDir + "/log4j2.xml");
						listArguments.add(elt);
					}

					elt = element(name("argument"), "-classpath");
					listArguments.add(elt);

					elt = element(name("argument"), libDir.getAbsolutePath() + File.separator + "*" + File.pathSeparator
							+ libExtDir.getAbsolutePath() + File.separator + "*");
					listArguments.add(elt);

					elt = element(name("argument"), "kg.apc.cmd.UniversalRunner");
					listArguments.add(elt);

					elt = element(name("argument"), "--tool");
					listArguments.add(elt);

					elt = element(name("argument"), "Reporter");
					listArguments.add(elt);

					// mandatory
					if (graph.inputFile != null) {
						elt = element(name("argument"), "--input-jtl");
						listArguments.add(elt);
						elt = element(name("argument"), graph.inputFile.getAbsolutePath());
						listArguments.add(elt);
					} else {
						getLog().error("Error grah inputFile is mandatory and inputFile=null");
						continue;
					}

					if (graph.pluginType != null) {
						elt = element(name("argument"), "--plugin-type");
						listArguments.add(elt);
						elt = element(name("argument"), graph.pluginType);
						listArguments.add(elt);
					} else {
						getLog().error("Error graph pluginType is mandatory and pluginType=null");
						continue;
					}

					// optional
					if (graph.width != null) {
						elt = element(name("argument"), "--width");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.width));
						listArguments.add(elt);
					}

					if (graph.height != null) {
						elt = element(name("argument"), "--height");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.height));
						listArguments.add(elt);
					}

					if (graph.generatePng != null) {
						elt = element(name("argument"), "--generate-png");
						listArguments.add(elt);
						elt = element(name("argument"), graph.generatePng.getAbsolutePath());
						listArguments.add(elt);
					}

					if (graph.generateCsv != null) {
						elt = element(name("argument"), "--generate-csv");
						listArguments.add(elt);
						elt = element(name("argument"), graph.generateCsv.getAbsolutePath());
						listArguments.add(elt);
					}

					if (graph.limitRows != null) {
						elt = element(name("argument"), "--limit-rows");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.limitRows));
						listArguments.add(elt);
					}

					if (graph.granulation != null) {
						elt = element(name("argument"), "--granulation");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.granulation));
						listArguments.add(elt);
					}

					if (graph.relativeTimes != null) {
						elt = element(name("argument"), "--relative-times");
						listArguments.add(elt);
						elt = element(name("argument"), graph.relativeTimes);
						listArguments.add(elt);
					}

					if (graph.aggregateRows != null) {
						elt = element(name("argument"), "--aggregate-rows");
						listArguments.add(elt);
						elt = element(name("argument"), graph.aggregateRows);
						listArguments.add(elt);
					}

					if (graph.paintGradient != null) {
						elt = element(name("argument"), "--paint-gradient");
						listArguments.add(elt);
						elt = element(name("argument"), graph.paintGradient);
						listArguments.add(elt);
					}
					if (graph.paintZeroing != null) {
						elt = element(name("argument"), "--paint-zeroing");
						listArguments.add(elt);
						elt = element(name("argument"), graph.paintZeroing);
						listArguments.add(elt);
					}

					if (graph.paintMarkers != null) {
						elt = element(name("argument"), "--paint-markers");
						listArguments.add(elt);
						elt = element(name("argument"), graph.paintMarkers);
						listArguments.add(elt);
					}

					if (graph.preventOutliers != null) {
						elt = element(name("argument"), "--prevent-outliers");
						listArguments.add(elt);
						elt = element(name("argument"), graph.preventOutliers);
						listArguments.add(elt);
					}

					if (graph.forceY != null) {
						elt = element(name("argument"), "--force-y");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.forceY));
						listArguments.add(elt);
					}

					if (graph.hideLowCounts != null) {
						elt = element(name("argument"), "--hide-low-counts");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.hideLowCounts));
						listArguments.add(elt);
					}

					if (graph.successFilter != null) {
						elt = element(name("argument"), "--success-filter");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.successFilter));
						listArguments.add(elt);
					}

					if (graph.includeLabels != null) {
						elt = element(name("argument"), "--include-labels");
						listArguments.add(elt);
						elt = element(name("argument"), graph.includeLabels);
						listArguments.add(elt);
					}

					if (graph.excludeLabels != null) {
						elt = element(name("argument"), "--exclude-labels");
						listArguments.add(elt);
						elt = element(name("argument"), graph.excludeLabels);
						listArguments.add(elt);
					}

					if (graph.autoScale != null) {
						elt = element(name("argument"), "--auto-scale");
						listArguments.add(elt);
						elt = element(name("argument"), graph.autoScale);
						listArguments.add(elt);
					}

					if (graph.lineWeight != null) {
						elt = element(name("argument"), "--line-weight");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.lineWeight));
						listArguments.add(elt);
					}

					if (graph.extractorRegexps != null) {
						elt = element(name("argument"), "--extractor-regexps");
						listArguments.add(elt);
						elt = element(name("argument"), graph.extractorRegexps);
						listArguments.add(elt);
					}

					if (graph.includeLabelRegex != null) {
						elt = element(name("argument"), "--include-label-regex");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.includeLabelRegex));
						listArguments.add(elt);
					}

					if (graph.excludeLabelRegex != null) {
						elt = element(name("argument"), "--exclude-label-regex");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.excludeLabelRegex));
						listArguments.add(elt);
					}

					if (graph.startOffset != null) {
						elt = element(name("argument"), "--start-offset");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.startOffset));
						listArguments.add(elt);
					}

					if (graph.endOffset != null) {
						elt = element(name("argument"), "--end-offset");
						listArguments.add(elt);
						elt = element(name("argument"), String.valueOf(graph.endOffset));
						listArguments.add(elt);
					}

					List<MojoExecutor.Element> configuration = new ArrayList<MojoExecutor.Element>();
					configuration.add(element("executable", "java"));

					List<MojoExecutor.Element> elements = new ArrayList<MojoExecutor.Element>();

					for (Element argument : listArguments) {
						elements.add(argument);
					}

					MojoExecutor.Element parentArgs = new MojoExecutor.Element("arguments",
							elements.toArray(new MojoExecutor.Element[0]));

					configuration.add(parentArgs);
					executeMojo(plugin(groupId("org.codehaus.mojo"), artifactId("exec-maven-plugin"), version("1.2.1")),
							goal("exec"), configuration(configuration.toArray(new MojoExecutor.Element[0])),
							executionEnvironment(mavenProject, mavenSession, pluginManager));
				} catch (Throwable throwable) {
					throw new RuntimeException(throwable);
				}
			}
		}
	}

	private boolean isJMeterPluginsDependency(Artifact artifact) {
		return isDependencyOf(artifact, JMETER_PLUGINS_ARTIFACT_NAME1)
				|| isDependencyOf(artifact, JMETER_PLUGINS_ARTIFACT_NAME2);
	}

	private boolean isJMeterDependency(Artifact artifact) {
		return isDependencyOf(artifact, JMETER_ARTIFACT_NAME) || isDependencyOf(artifact, JMETER_CORE_ARTIFACT_NAME)
				|| isDependencyOf(artifact, JMETER_HTTP_ARTIFACT_NAME) || isDependencyOf(artifact, COMMON_IO_NAME);
	}

	private boolean isDependencyOf(Artifact artifact, String parentArtifactName) {
		getLog().debug("artifact:" + artifact + ", parentArtifactName:" + parentArtifactName
				+ ", artifact.getDependencyTrail()=" + artifact.getDependencyTrail());
		for (String parent : artifact.getDependencyTrail()) {
			if (parent.contains(parentArtifactName)) {
				getLog().debug("isDependencyOf=true");
				return true;
			}
		}
		getLog().debug("isDependencyOf=false");
		return false;
	}

	private void createDirectoryIfNotExists(File directory) throws MojoExecutionException {
		getLog().info("Set up jmeter in " + directory);
		if (!directory.exists()) {
			if (!directory.mkdirs()) {
				throw new MojoExecutionException(
						"Could not make working directory: '" + directory.getAbsolutePath() + "'");
			}
		}
	}

	public static class propertiesUser {
		List<String> propertiesUser;
	}

	public static class JMeterProcessJVMSettings {
	/*
	 <jMeterProcessJVMSettings> 
		 <xms>1024</xms>
		 <xmx>1024</xmx>
		 <arguments>
		   <argument>-Xprof</argument>
		   <argument>-Xfuture</argument>
		 </arguments>
	 </jMeterProcessJVMSettings>
	 */
		Integer xms;
		Integer xmx;
		List<Argument> arguments;

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("JMeterProcessJVMSettings [xms=");
			builder.append(xms);
			builder.append(", xmx=");
			builder.append(xmx);
			builder.append(", arguments=");
			builder.append(arguments);
			builder.append("]");
			return builder.toString();
		}
	}

	public static class Argument {
		String argument;

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Argument [argument=");
			builder.append(argument);
			builder.append("]");
			return builder.toString();
		}
	}

	public static class FilterResultParam {
	/*
	<filterResultsParam>
		 <inputFile>${project.build.directory}/jmeter/results/gestdoc_sc01_menu.csv</inputFile>
		 <outputFile>${project.build.directory}/jmeter/results/gestdoc_sc01_menu_regex_filtred.csv</outputFile>
		 <successFilter>false</successFilter>
		 <includeLabels>0.*</includeLabels>
		 <includeLabelRegex>true</includeLabelRegex>
		 <startOffset>2</startOffset>
		 <endOffset>20</endOffset>
		 <saveAsXml>false</saveAsXml>
	</filterResultsParam>
	*/
		File inputFile;
		File outputFile;
		Boolean successFilter;
		String includeLabels;
		Boolean includeLabelRegex;
		String excludeLabels;
		Boolean excludeLabelRegex;
		Integer startOffset;
		Integer endOffset;
		Boolean saveAsXml;
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("FilterResultParam [inputFile=");
			builder.append(inputFile);
			builder.append(", outputFile=");
			builder.append(outputFile);
			builder.append(", successFilter=");
			builder.append(successFilter);
			builder.append(", includeLabels=");
			builder.append(includeLabels);
			builder.append(", includeLabelRegex=");
			builder.append(includeLabelRegex);
			builder.append(", excludeLabels=");
			builder.append(excludeLabels);
			builder.append(", excludeLabelRegex=");
			builder.append(excludeLabelRegex);
			builder.append(", startOffset=");
			builder.append(startOffset);
			builder.append(", endOffset=");
			builder.append(endOffset);
			builder.append(", saveAsXml=");
			builder.append(saveAsXml);
			builder.append("]");
			return builder.toString();
		}
	}

	public static class Graph {
		File inputFile;
		String pluginType;
		Integer width;
		Integer height;
		File generatePng;
		File generateCsv;
		Integer granulation;
		String relativeTimes;
		String aggregateRows;
		String paintGradient;
		String paintZeroing;
		String paintMarkers;
		String preventOutliers;
		Integer limitRows;
		Integer forceY;
		Integer hideLowCounts;
		Boolean successFilter;
		String includeLabels;
		String excludeLabels;
		String autoScale;
		Integer lineWeight;
		String extractorRegexps;
		Boolean includeLabelRegex;
		Boolean excludeLabelRegex;
		Integer startOffset;
		Integer endOffset;

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Graph [inputFile=");
			builder.append(inputFile);
			builder.append(", pluginType=");
			builder.append(pluginType);
			builder.append(", width=");
			builder.append(width);
			builder.append(", height=");
			builder.append(height);
			builder.append(", generatePng=");
			builder.append(generatePng);
			builder.append(", generateCsv=");
			builder.append(generateCsv);
			builder.append(", granulation=");
			builder.append(granulation);
			builder.append(", relativeTimes=");
			builder.append(relativeTimes);
			builder.append(", aggregateRows=");
			builder.append(aggregateRows);
			builder.append(", paintGradient=");
			builder.append(paintGradient);
			builder.append(", paintZeroing=");
			builder.append(paintZeroing);
			builder.append(", paintMarkers=");
			builder.append(paintMarkers);
			builder.append(", preventOutliers=");
			builder.append(preventOutliers);
			builder.append(", limitRows=");
			builder.append(limitRows);
			builder.append(", forceY=");
			builder.append(forceY);
			builder.append(", hideLowCounts=");
			builder.append(hideLowCounts);
			builder.append(", successFilter=");
			builder.append(successFilter);
			builder.append(", includeLabels=");
			builder.append(includeLabels);
			builder.append(", excludeLabels=");
			builder.append(excludeLabels);
			builder.append(", autoScale=");
			builder.append(autoScale);
			builder.append(", lineWeight=");
			builder.append(lineWeight);
			builder.append(", extractorRegexps=");
			builder.append(extractorRegexps);
			builder.append(", includeLabelRegex=");
			builder.append(includeLabelRegex);
			builder.append(", excludeLabelRegex=");
			builder.append(excludeLabelRegex);
			builder.append(", startOffset=");
			builder.append(startOffset);
			builder.append(", endOffset=");
			builder.append(endOffset);
			builder.append("]");
			return builder.toString();
		}

	}
}
