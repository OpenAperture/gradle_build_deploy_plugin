package io.openaperture.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Hello world!
 *
 */
public class BuildDeployPlugin implements Plugin<Project> {
	@Override
	public void apply(Project target) {
		// target.getTasks().create("buildDeployTask", BuildDeployTask.class);
	}
}
