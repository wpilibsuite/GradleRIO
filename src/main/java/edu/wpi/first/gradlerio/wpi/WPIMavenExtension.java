package edu.wpi.first.gradlerio.wpi;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.internal.reflect.DirectInstantiator;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

public class WPIMavenExtension extends DefaultNamedDomainObjectSet<WPIMavenRepo> {

    private final Project project;

    private final Set<String> vendorCacheGroupIds = new HashSet<>();
    private boolean useDevelopment;
    private boolean useLocal;
    private boolean useFrcMavenLocalDevelopment;
    private boolean useFrcMavenLocalRelease;
    private boolean useMavenCentral;
    private boolean useFrcMavenVendorCache;
    private boolean enableRepositoryGroupLimits;

    @Inject
    public WPIMavenExtension(Project project) {
        super(WPIMavenRepo.class, DirectInstantiator.INSTANCE, CollectionCallbackActionDecorator.NOOP);
        this.project = project;

        this.useDevelopment = true; // Do not rename without changing versionupdates.gradle
        this.useLocal = true;
        this.useFrcMavenLocalDevelopment = false;
        this.useFrcMavenLocalRelease = false;
        this.useMavenCentral = true;
        this.useFrcMavenVendorCache = true;
        this.enableRepositoryGroupLimits = true;

        // mirror("AU") { WPIMavenRepo mirror ->
        //     mirror.release = "http://wpimirror.imjac.in/m2/release"
        //     mirror.development = "http://wpimirror.imjac.in/m2/development"
        // }
    }

    public boolean isEnableRepositoryGroupLimits() {
        return enableRepositoryGroupLimits;
    }

    public void setEnableRepositoryGroupLimits(boolean enableRepositoryGroupLimits) {
        this.enableRepositoryGroupLimits = enableRepositoryGroupLimits;
    }

    public Set<String> getVendorCacheGroupIds() {
        return vendorCacheGroupIds;
    }

    public boolean isUseFrcMavenVendorCache() {
        return useFrcMavenVendorCache;
    }

    public void setUseFrcMavenVendorCache(boolean useFrcMavenVendorCache) {
        this.useFrcMavenVendorCache = useFrcMavenVendorCache;
    }

    public Project getProject() {
        return project;
    }

    public boolean isUseDevelopment() {
        return useDevelopment;
    }

    public boolean isUseLocal() {
        return useLocal;
    }

    public boolean isUseFrcMavenLocalDevelopment() {
        return useFrcMavenLocalDevelopment;
    }

    public boolean isUseFrcMavenLocalRelease() {
        return useFrcMavenLocalRelease;
    }

    public boolean isUseMavenCentral() {
        return useMavenCentral;
    }

    public void setUseDevelopment(boolean useDevelopment) {
        this.useDevelopment = useDevelopment;
    }

    public void setUseLocal(boolean useLocal) {
        this.useLocal = useLocal;
    }

    public void setUseFrcMavenLocalDevelopment(boolean useFrcMavenLocalDevelopment) {
        this.useFrcMavenLocalDevelopment = useFrcMavenLocalDevelopment;
    }

    public void setUseFrcMavenLocalRelease(boolean useFrcMavenLocalRelease) {
        this.useFrcMavenLocalRelease = useFrcMavenLocalRelease;
    }

    public void setUseMavenCentral(boolean useMavenCentral) {
        this.useMavenCentral = useMavenCentral;
    }

    // Mirror = source for WPILib artifacts
    // Repo = source for any artifacts

    // Repo should always take precedence over mirror in the case they want
    // to provide custom builds of WPILib artifacts.

    public WPIMavenRepo mirror(String name, final Action<WPIMavenRepo> config) {
        WPIMavenRepo mirr = project.getObjects().newInstance(WPIMavenRepo.class, name);
        mirr.setPriority(WPIMavenRepo.PRIORITY_MIRROR);
        config.execute(mirr);
        this.add(mirr);
        return mirr;
    }

    public WPIMavenRepo repo(String name, final Action<WPIMavenRepo> config) {
        WPIMavenRepo mirr = project.getObjects().newInstance(WPIMavenRepo.class, name);
        config.execute(mirr);
        this.add(mirr);
        return mirr;
    }

    public WPIMavenRepo vendor(String name, final Action<WPIMavenRepo> config) {
        WPIMavenRepo mirr = project.getObjects().newInstance(WPIMavenRepo.class, name);
        mirr.setPriority(WPIMavenRepo.PRIORITY_VENDOR);
        config.execute(mirr);
        this.add(mirr);
        return mirr;
    }

    public void useMirror(String name) {
        this.all(m -> {
            if (m.getName().equals(name)) {
                m.setPriority(WPIMavenRepo.PRIORITY_MIRROR_INUSE);
            }
        });
    }

    public void useRepo(String name) {
        this.all(m -> {
            if (m.getName().equals(name)) {
                m.setPriority(WPIMavenRepo.PRIORITY_REPO_INUSE);
            }
        });
    }
}
