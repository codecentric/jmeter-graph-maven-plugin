package de.codecentric.jmeter;

import kg.apc.cmdtools.PluginsCMD;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.LogEvent;
import org.apache.log.LogTarget;
import org.apache.log.format.ExtendedPatternFormatter;
import org.apache.log.format.Formatter;
import org.apache.log.output.io.StreamTarget;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

@Mojo(name = "create-graph")
public class JMeterPluginsMojo extends AbstractMojo {

    @Parameter
    private File inputFile;

    @Parameter
    private List<Graph> graphs;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (graphs == null) {
            getLog().error("No graphs defined.");
        }
        LoggingManager.addLogTargetToRootLogger(new LogTarget[]{
                new StreamTarget(System.out, new ExtendedPatternFormatter(LoggingManager.DEFAULT_PATTERN))});
        for (Graph graph : graphs) {
            getLog().info("Creating graph: " + graphs != null ? graphs.toString() : "<null>");
            try {
                int result = new PluginsCMD().processParams(new String[]{
                        "--tool",
                        "Reporter",
                        "--input-jtl",
                        inputFile.getPath(),
                        "--plugin-type",
                        graph.pluginType,
                        "--width",
                        String.valueOf(graph.width),
                        "--height",
                        String.valueOf(graph.height),
                        "--generate-png",
                        graph.outputFile.getPath()});
                if (result != 0) {
                    getLog().error("PluginsCMD exited with resultCode=" + result);
                }
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
        }

        public static class Graph {
            String pluginType;
            Integer width;
            Integer height;
            File outputFile;

            @Override
            public String toString() {
                return "Graph{" +
                        "pluginType='" + pluginType + '\'' +
                        ", width=" + width +
                        ", height=" + height +
                        ", outputFile=" + outputFile +
                        '}';
            }
        }
    }
