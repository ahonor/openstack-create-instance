
/*
* CreateInstanceStep.java
* 
* User: Alex Honor <a href="mailto:alex@simplifyops.com">alex@simplifyops.com</a>
* Created: 04/14/14 9:27 AM
* 
*/
package org.rundeck.plugin.openstack;

import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.domain.Image;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.rest.RestContext;

import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 */
@Plugin(name = CreateInstanceStep.SERVICE_PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
public class CreateInstanceStep implements StepPlugin, Describable {
    /**
     * Define a name used to identify your plugin. It is a good idea to use a fully qualified package-style name.
     */
    public static final String SERVICE_PROVIDER_NAME = "org.rundeck.plugin.openstack.CreateInstanceStep";

    public static void main(String[] args) {
        System.out.println("Running the plugin...");
    }

    private boolean initialized = false;
    private String endpoint;
    private String identity;
    private String password;
    private String provider;
    private String osFamily;
    private String instanceName;
    private String flavor;
    private String imageName;
    private String groupName;

    private void initialize(Map<String, Object> properties) {
        endpoint = (String) properties.get("endpoint");
        identity = (String) properties.get("identity");
        password = (String) properties.get("password");
        provider = (String) properties.get("provider");
        osFamily = (String) properties.get("osFamily");
        instanceName = (String) properties.get("instanceName");
        groupName = (String) properties.get("groupName");
        flavor = (String) properties.get("flavor");
        imageName = (String) properties.get("imageName");

        initialized = true;
    }

    /**
     *
     */
    public Description getDescription() {
        return DescriptionBuilder.builder()
                .name(SERVICE_PROVIDER_NAME)
                .title("Openstack Create Instance Step")
                .description("Creates a new instance")
                .property(PropertyBuilder.builder()
                        .select("provider")
                        .title("Provider")
                        .description("Compute service provider")
                        .required(false)
                        .values("openstack-nova")
                        .build()
                )
                .property(PropertyBuilder.builder()
                        .string("endpoint")
                        .title("API endpoint")
                        .description("This is the Keystone endpoint to connect. eg, http://172.16.0.1:5000/v2.0/")
                        .required(true)
                        .build()
                )

                .property(PropertyBuilder.builder()
                        .string("identity")
                        .title("Identity")
                        .description("use the tenant name and user name with a colon between them instead of just a user name")
                        .required(true)
                        .build()
                )

                .property(PropertyBuilder.builder()
                        .string("password")
                        .title("Password")
                        .description("the password")
                        .required(true)
                        .build()
                )
                .property(PropertyBuilder.builder()
                        .string("flavor")
                        .title("Flavor")
                        .description("The hardware flavor")
                        .required(true)
                        .build()
                )
                .property(PropertyBuilder.builder()
                        .booleanType("os64Bit")
                        .title("64 bit")
                        .description("Operating System 64bit?")
                        .required(false)
                        .defaultValue("false")
                        .build()
                )
                .property(PropertyBuilder.builder()
                        .freeSelect("osFamily")
                        .title("OS Family")
                        .description("The OS Family")
                        .required(false)
                        .defaultValue("ubuntu")
                        .values("ubuntu", "centos")
                        .build()
                )

                .property(PropertyBuilder.builder()
                        .string("instanceName")
                        .title("Instance Name")
                        .description("The instance name")
                        .required(true)
                        .build()
                )
                .property(PropertyBuilder.builder()
                        .string("groupName")
                        .title("Group Name")
                        .description("The group name")
                        .required(true)
                        .build()
                )

                .build();
    }

    /**
     * This enum lists the known reasons this plugin might fail
     */
    static enum Reason implements FailureReason {
        RunNodesException
    }

    /**
     * Here is the meat of the plugin implementation, which should perform the appropriate logic for your plugin.
     * <p/>
     * The {@link PluginStepContext} provides access to the appropriate Nodes, the configuration of the plugin, and
     * details about the step number and context.
     */
    public void executeStep(final PluginStepContext context, final Map<String, Object> configuration)
            throws StepException {

        if (!initialized) initialize(configuration);
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        ComputeService compute = initComputeService(provider, endpoint, identity, password);

        // note this will create a user with the same name as you on the
        // node. ex. you can connect via ssh publicip
        //Statement bootInstructions = AdminAccess.standard();

        TemplateBuilder templateBuilder = compute.templateBuilder();

        try {
            compute.createNodesInGroup(groupName, 1, templateBuilder.build());
        } catch (RunNodesException e) {
            throw new StepException(e.getMessage(),Reason.RunNodesException);
        }


    }

    private static ComputeService initComputeService(String provider, String endpoint, String identity, String password) {

        Properties properties = new Properties();

        ContextBuilder builder = ContextBuilder.newBuilder(provider)
                .endpoint(endpoint)
                .credentials(identity, password)
                .overrides(properties);

        System.out.printf(">> initializing %s%n", builder.getApiMetadata());

        ComputeServiceContext context = builder.buildView(ComputeServiceContext.class);
        return context.getComputeService();
    }

}
