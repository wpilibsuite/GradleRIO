package edu.wpi.first.gradlerio.deploy;

import com.google.gson.GsonBuilder;

import edu.wpi.first.gradlerio.deploy.roborio.RoboRIO;
import edu.wpi.first.deployutils.deploy.artifact.FileArtifact;

import com.google.gson.Gson;

import java.util.Map;

import org.gradle.api.GradleException;

import java.util.HashMap;
import java.io.IOException;
import java.io.File;
import java.lang.Runtime;
import java.time.LocalDateTime;

class DeployLogFile {
    public String deployHost;
    public String deployUser;
    public String deployDate;
    public String codePath;
    public String gitHash;
    public String gitBranch;
    public String gitDesc;
    public static final String LOG_FILE_PATH = "/tmp/deploy.json";
    public File deployFile;
    public String[] deployItems = {
        "deployHost",
        "deployUser",
        "deployDate",
        "codePath",
        "gitHash",
        "gitBranch",
        "gitDesc",
    };

    Gson builder = new GsonBuilder().create();
    Map<String, String> data = new HashMap<String, String>();
    String jsonDeploy;
    FileArtifact deployArtifact;
    boolean inGitRepo;

    DeployLogFile(RoboRIO target) {
        String[] command = {"git", "rev-parse", "--is-inside-work-tree"};

        try {
            Runtime.getRuntime().exec(command);
        } catch(IOException e) {
            inGitRepo = false;
        }

        try {
            deployHost = Runtime.getRuntime().exec("hostname").getOutputStream().toString().strip();
            data.put(deployItems[0], deployHost);
        } catch (IOException e) {
            throw new GradleException("Couldn't get hostname", e);
        }

        deployUser = System.getProperty("user.name");
        data.put(deployItems[1], deployUser);

        deployDate = LocalDateTime.now().toString();
        data.put(deployItems[2], deployDate);

        codePath = System.getProperty("user.dir");
        data.put(deployItems[3], codePath);

        if (inGitRepo) {
            String[] command2 = {"git", "rev-parse", "HEAD"};
            try {
                gitHash = Runtime.getRuntime().exec(command2).getOutputStream().toString().strip();
                data.put(deployItems[4], gitHash);
            } catch (IOException e) {
                throw new GradleException("Couldn't get git hash", e);
            }

            String[] command3 = {"git", "rev-parse", "--abbrev-ref" ,"HEAD"};
            try {
                gitBranch = Runtime.getRuntime().exec(command3).getOutputStream().toString().strip();
                data.put(deployItems[5], gitBranch);
            } catch (IOException e) {
                throw new GradleException("Couldn't get git branch", e);
            }

            try {
                String[] command4 = {"git", "describe", "--dirty=-dirty", "--always"};
                gitDesc = Runtime.getRuntime().exec(command4).getOutputStream().toString().strip();
                data.put(deployItems[6], gitDesc);
            } catch (IOException e) {
                throw new GradleException("Couldn't get git description", e);
            }
        }

        jsonDeploy = builder.toJson(data);

        deployFile = new File(LOG_FILE_PATH, jsonDeploy);

        try {
            deployFile.createNewFile();
        } catch (IOException e) {
            throw new GradleException("Couldn't write deploy log file", e);
        }

        deployArtifact = new FileArtifact("/home/lvuser/deploy.json", target);

        target.getArtifacts().add(deployArtifact);
    }
}
