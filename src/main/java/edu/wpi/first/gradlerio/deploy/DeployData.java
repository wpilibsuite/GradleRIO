package edu.wpi.first.gradlerio.deploy;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

class DeployLogFile {
    public String deployHost;
    public String deployUser;
    public String deployDate;
    public String codePath;
    public String gitHash;
    public String gitBranch;
    public String gitDesc;
    public static final String LOG_FILE_PATH = "/home/lvuser/deploy.json";
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
    File deployDotJson;

    DeployLogFile() {
        data.put(deployItems[0], deployHost);
        data.put(deployItems[1], deployUser);
        data.put(deployItems[2], deployDate);
        data.put(deployItems[3], codePath);
        data.put(deployItems[4], gitHash);
        data.put(deployItems[5], gitBranch);
        data.put(deployItems[6], gitDesc);

        jsonDeploy = builder.toJson(data);

        deployDotJson = new File(LOG_FILE_PATH);
    }
}
