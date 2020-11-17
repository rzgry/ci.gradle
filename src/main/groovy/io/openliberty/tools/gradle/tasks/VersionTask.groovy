/**
 * (C) Copyright IBM Corporation 2014, 2020.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.gradle.tasks

import io.openliberty.tools.common.plugins.util.InstallFeatureUtil
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.logging.LogLevel

import org.gradle.api.tasks.TaskAction
import io.openliberty.tools.ant.ServerTask
import io.openliberty.tools.gradle.tasks.InstallLibertyTask

class VersionTask extends AbstractTask {

    VersionTask() {
        configure({
            description 'Returns the version of the liberty installation'
            group 'Liberty'
        })
    }

    /**
     * Gets the Runtime specified using a libertyRuntime dependency if it exists
     * Ex. libertyRuntime 'com.ibm.websphere.appserver.runtime:wlp-webProfile8:19.0.0.9'
     * @return
     */
    private Dependency getLibertyRuntimeDependency() {
        Configuration libertyRuntimeConfig = project.configurations.getByName('libertyRuntime')
        if (libertyRuntimeConfig != null && libertyRuntimeConfig.dependencies.size() >= 0) {
            Dependency libertyRuntime = libertyRuntimeConfig.dependencies[0]
            return libertyRuntime
        }
        return null
    }

    @TaskAction
    void checkVersion() {
      String version;
      File serverInstallDir = getInstallDir(project);
        // If the server is installed, return the version from the installed liberty properties file
        if (serverInstallDir != null && serverInstallDir.exists()) {
            logger.warn("Retrieving version from install directory");
            List<InstallFeatureUtil.ProductProperties> propertiesList = InstallFeatureUtil.loadProperties(serverInstallDir);
            version = InstallFeatureUtil.getOpenLibertyVersion(propertiesList);
        }
        // else use the version configured in the gradle project
        else {
            logger.warn("Retrieving version from gradle project")
            // if version is specified in project.liberty.runtime it
            // overrides the version specified in the libertyRuntime dependency
            if (project.liberty.runtime != null && project.liberty.runtime.version != null) {
                version = project.liberty.runtime.version
            }
            // else we use the version specified via libertyRuntime dependency
            else {
                Dependency libertyRuntimeDependency = getLibertyRuntimeDependency()
                if (libertyRuntimeDependency != null) {
                    version = libertyRuntimeDependency.version
                } else {
                    // if no liberty runtime dependency is specified we need to use the default
                    Dependency defaultRuntimeDep = project.dependencies.create(InstallLibertyTask.defaultRuntime);
                    Configuration detachedConfig = project.configurations.detachedConfiguration(defaultRuntimeDep)
                    ResolvedConfiguration resolvedConfig = detachedConfig.getResolvedConfiguration()
                    if (resolvedConfig.hasError()) {
                        resolvedConfig.rethrowFailure()
                    }
                    if (resolvedConfig.firstLevelModuleDependencies.size() >= 0) {
                        version = resolvedConfig.firstLevelModuleDependencies[0].moduleVersion
                    }
                }
            }
        }
        logger.warn(version)
    }

}
