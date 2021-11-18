package edu.wpi.first.gradlerio.wpi;

import javax.inject.Inject;

import org.gradle.api.Named;

public class WPIMavenRepo implements Named {
    private String release = null;
    private String development = null;
    private int priority = PRIORITY_REPO;

    private String name;

    public static final int PRIORITY_REPO = 100;
    public static final int PRIORITY_REPO_INUSE = 50;

    public static final int PRIORITY_OFFICIAL = 150;
    public static final int PRIORITY_MIRROR = 250;
    public static final int PRIORITY_MIRROR_INUSE = 120;

    public static final int PRIORITY_VENDOR_WITHOUT_CACHE = 175;
    public static final int PRIORITY_WPILIB_VENDOR_CACHE = 200;
    public static final int PRIORITY_VENDOR_ALLOWS_CACHE = 225;

    @Inject
    public WPIMavenRepo(String name) {
        this.setName(name);
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getDevelopment() {
        return development;
    }

    public void setDevelopment(String development) {
        this.development = development;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
