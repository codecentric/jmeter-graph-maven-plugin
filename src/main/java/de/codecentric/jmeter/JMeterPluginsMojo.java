package de.codecentric.jmeter;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="create-graph")
public class JMeterPluginsMojo extends AbstractMojo {
    @Parameter
    private String text;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Hallo World, " + text);
    }
}
