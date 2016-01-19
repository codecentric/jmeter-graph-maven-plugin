package de.codecentric.jmeter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "create-graph")
public class JMeterPluginsMojo extends AbstractMojo {

	public static final String FILENAME_REPLACE_PATTERN = "%FILENAME%";
    public static final String JMETER_CONFIG_ARTIFACT_NAME = "ApacheJMeter_config";
    public static final String JMETER_PLUGINS_ARTIFACT_NAME = "jmeter-plugins";
    public static final String JMETER_ARTIFACT_NAME = "ApacheJMeter";
    public static final String JMETER_CORE_ARTIFACT_NAME = "ApacheJMeter_core";

    @Parameter
    File inputFile;

    @Parameter
    String inputFilePattern;

    @Parameter
    List<Graph> graphs;

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
    
    @Parameter(defaultValue = "false")
    boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(skip){
            return;
        }
        
        if (graphs == null) {
            getLog().error("No graphs defined.");
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
                    getLog().debug("Copy configuration files to " + binDir.getAbsolutePath());
                    JarFile jarFile = new JarFile(artifact.getFile());
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (!entry.isDirectory() &&
                                entry.getName().startsWith("bin") &&
                                entry.getName().endsWith(".properties")) {
                            File configFileDestination = new File(workingDirectory + File.separator + entry.getName());
                            getLog().debug("Write configuration " + configFileDestination.getAbsoluteFile());
                            FileUtils.copyInputStreamToFile(
                                    jarFile.getInputStream(entry),
                                    configFileDestination);
                        }
                    }
                    jarFile.close();
                } else if (isJMeterDependency(artifact)) {
                    getLog().debug("Copy artifact " + artifact.toString() + " to " + libDir.getAbsolutePath());
                    File artifactDestination = new File(libDir.getAbsolutePath() + File.separator + artifact.getArtifactId() + ".jar");
                    FileUtils.copyFile(artifact.getFile(), artifactDestination);
                } else if (isJMeterPluginsDependency(artifact)) {
                    getLog().debug("Copy artifact " + artifact.toString() + " to " + libExtDir.getAbsolutePath());
                    File artifactDestination = new File(libExtDir.getAbsolutePath() + File.separator + artifact.getArtifactId() + ".jar");
                    FileUtils.copyFile(artifact.getFile(), artifactDestination);
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Could not copy JMeter dependencies to working directory", e);
            }
        }

        for (File source : getInputFiles()) {
        	getLog().info("Processing " + source);

	        for (Graph graph : graphs) {
	            getLog().debug("Creating graph: " + graphs != null ? graphs.toString() : "<null>");
	            String outputResultFile = graph.getOutputFile(source).getAbsolutePath();
	            
	            ArrayList<Element> argList = new ArrayList<Element>();
				argList.add(element(name("argument"), "-classpath"));
				argList.add(element(name("argument"),
						libDir.getAbsolutePath() + File.separator + "*" +
								File.pathSeparator +
								libExtDir.getAbsolutePath() + File.separator + "*"));
				argList.add(element(name("argument"), "kg.apc.cmd.UniversalRunner"));
				argList.add(element(name("argument"), "--tool"));
				argList.add(element(name("argument"), "Reporter"));
				argList.add(element(name("argument"), "--input-jtl"));
				argList.add(element(name("argument"), source.getAbsolutePath()));
				argList.add(element(name("argument"), "--plugin-type"));
				argList.add(element(name("argument"), graph.pluginType));
				
				if (graph.relativeTimes != null) {
					argList.add(element(name("argument"), "--relative-times"));
					argList.add(element(name("argument"), "no".equalsIgnoreCase(graph.relativeTimes)? "no" : "yes"));
				}
				if (graph.includeLabels != null) {
					argList.add(element(name("argument"), "--include-labels"));
					argList.add(element(name("argument"), graph.includeLabels));
				}
				if (graph.excludeLabels != null) {
					argList.add(element(name("argument"), "--exclude-labels"));
					argList.add(element(name("argument"), graph.excludeLabels));
				}
				
                // branch in case we have csv file
				if(outputResultFile.endsWith(".csv")){
					argList.add(element(name("argument"), "--generate-csv"));
				} else {
					argList.add(element(name("argument"), "--width"));
					argList.add(element(name("argument"), String.valueOf(graph.width)));
					argList.add(element(name("argument"), "--height"));
					argList.add(element(name("argument"), String.valueOf(graph.height)));
					if(!StringUtils.isBlank(graph.graphPerRow)){
						argList.add(element(name("argument"), "--graph-per-row"));
						argList.add(element(name("argument"), graph.graphPerRow));
					}
					if(graph.granulation != null){
						argList.add(element(name("argument"), "--granulation"));
						argList.add(element(name("argument"), String.valueOf(graph.granulation)));
					}
					if(!StringUtils.isBlank(graph.preventOutliers)){
						argList.add(element(name("argument"), "--prevent-outliers"));
						argList.add(element(name("argument"), graph.preventOutliers));
					}
					if(!StringUtils.isBlank(graph.lineWeight)){
						argList.add(element(name("argument"), "--line-weight"));
						argList.add(element(name("argument"), graph.lineWeight));
					}
					argList.add(element(name("argument"), "--generate-png"));
				}
				
				argList.add(element(name("argument"), outputResultFile));
				
				try {
        			executeMojo(
                        plugin(
                                groupId("org.codehaus.mojo"),
                                artifactId("exec-maven-plugin"),
                                version("1.2.1")),
                        goal("exec"),
                        configuration(
                                element(name("executable"), "java"),
                                element(name("workingDirectory"), binDir.getAbsolutePath()),
                                element(name("arguments"),
                                		argList.toArray(new Element[0]))),
                        executionEnvironment(
                                mavenProject,
                                mavenSession,
                                pluginManager));
		        } catch (Throwable throwable) {
		            throw new RuntimeException(throwable);
		        }
	        }
        }
    }

	private Collection<File> getInputFiles() {
		final Collection<File> dataInputFiles = new ArrayList<File>();

        if (inputFile != null) {
        	dataInputFiles.add(inputFile);
        }

        if (StringUtils.isNotEmpty(inputFilePattern)) {
            final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            try {
				for (Resource resource : resolver.getResources("file:" + inputFilePattern)) {
					getLog().info("Adding " + resource.getFile());
					dataInputFiles.add(resource.getFile());
				}
			} catch (IOException e) {
				getLog().warn("Unable to add input files: " + e.getMessage());
			}
        }

		return dataInputFiles;
	}

    private boolean isJMeterPluginsDependency(Artifact artifact) {
        return isDependencyOf(artifact, JMETER_PLUGINS_ARTIFACT_NAME);
    }

    private boolean isJMeterDependency(Artifact artifact) {
        return isDependencyOf(artifact, JMETER_ARTIFACT_NAME) ||
                isDependencyOf(artifact, JMETER_CORE_ARTIFACT_NAME);
    }

    private boolean isDependencyOf(Artifact artifact, String parentArtifactName) {
        for (String parent : artifact.getDependencyTrail()) {
            if (parent.contains(parentArtifactName))
                return true;
        }
        return false;
    }

    private void createDirectoryIfNotExists(File directory) throws MojoExecutionException {
        getLog().debug("Set up jmeter in " + directory);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new MojoExecutionException("Could not make working directory: '"
                        + directory.getAbsolutePath() + "'");
            }
        }
    }

    public static class Graph {
        String pluginType;
        Integer width;
        Integer height;
        String relativeTimes;
        String includeLabels;
        String excludeLabels;
        File outputFile;
        String outputFilePattern;
        String graphPerRow;
        Integer granulation;
        String preventOutliers;
        String lineWeight;
        
        File getOutputFile(File inputFile) {
        	if (outputFile != null) {
        		return outputFile;
        	}

        	return new File(outputFilePattern.replace(FILENAME_REPLACE_PATTERN, FilenameUtils.removeExtension(inputFile.getName())));
        }

        Graph() {
            width = 800;
            height = 600;
        }

        @Override
        public String toString() {
            return "Graph{" +
                    "pluginType='" + pluginType + '\'' +
                    ", width=" + width +
                    ", height=" + height +
                    ", relativeTimes=" + relativeTimes +
                    ", includeLabels=" + includeLabels +
                    ", excludeLabels=" + excludeLabels +
                    ", outputFile=" + outputFile +
                    ", outputFilePattern=" + outputFilePattern +
                    ", graphPerRow=" + graphPerRow +
                    ", granulation=" + granulation + 
                    ", preventOutliers= " + preventOutliers +
                    '}';
        }
    }
    
    //public  static class  ArrayHelper<T> {
    /**
     * A little unwind arrays to varargs utility
     * @param elems
     * @return
     */
    public static   Element[] array(Object... elems)
    {
	 	List<Element> list = new ArrayList<Element>();
	 	for (Object el : elems) {
	 		if (el instanceof Element) {
	 			list.add((Element)el);
	 		} else 
	 			if ( el instanceof Element[])  {
	 				for (Element el1: ((Element[])el)) {
	 					list.add(el1);
	 				}
	 			}
	 	}
        return list.toArray(new Element[list.size()]);
    }
    
    public static Element[] argument(String argName){
        return argument(argName, "");
    }
    
    public static Element[] argument(String argName, String argValue){
        return argument(argName, argValue, true);
    }
    
    public static Element[] argument(String argName, String argValue, boolean condition){
        if(!condition) {
            return new Element[0];
        }
        
        Element first = element(name("argument"), argName);
        
        if(StringUtils.isBlank(argValue)){
            return array(first);
        }
        
        return array(first, element(name("argument"), argValue));
    }
}
