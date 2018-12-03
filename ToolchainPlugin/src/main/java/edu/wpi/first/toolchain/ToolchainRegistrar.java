package edu.wpi.first.toolchain;

import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainRegistryInternal;

public class ToolchainRegistrar<T extends GccToolChain> {

    private Class<T> implClass;
    private Project project;

    public ToolchainRegistrar(Class<T> clazz, Project project) {
        this.implClass = clazz;
        this.project = project;
    }

    void register(ToolchainOptions options, NativeToolChainRegistryInternal registry, Instantiator instantiator) {
        NamedDomainObjectFactory<T> factory = new NamedDomainObjectFactory<T>() {
            @Override
            public T create(String name) {
                options.name = name;
                options.project = project;
                return instantiator.newInstance(implClass, options);
            }
        };
        registry.registerFactory(implClass, factory);
        registry.registerDefaultToolChain(options.descriptor.getToolchainName(), implClass);
    };

}
