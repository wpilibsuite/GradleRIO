package edu.wpi.first.toolchain;

import jaci.gradle.log.ETLogger;
import jaci.gradle.log.ETLoggerFactory;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainRegistryInternal;

public class ToolchainRegistrar<T extends GccToolChain> {

    private Class<T> implClass;
    private Project project;
    private ETLogger logger;

    public ToolchainRegistrar(Class<T> clazz, Project project) {
        this.implClass = clazz;
        this.project = project;
        this.logger = ETLoggerFactory.INSTANCE.create("ToolchainRegistrar");
    }

    void register(ToolchainOptions options, NativeToolChainRegistryInternal registry, Instantiator instantiator) {
        NamedDomainObjectFactory<T> factory = new NamedDomainObjectFactory<T>() {
            @Override
            public T create(String name) {
                options.name = name;
                options.project = project;
                logger.info("Creating: " + name + " (desc: " + options.descriptor.getName() + ") for class " + implClass.getName());
                return instantiator.newInstance(implClass, options);
            }
        };
        logger.info("Registering: " + implClass.getName() + " for toolchain " + options.descriptor.getToolchainName() + " (desc: " + options.descriptor.getName() + ")");
        registry.registerFactory(implClass, factory);
        registry.registerDefaultToolChain(options.descriptor.getToolchainName(), implClass);
    };

}
