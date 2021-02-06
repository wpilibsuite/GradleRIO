package edu.wpi.first.gradlerio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;
import groovy.json.JsonSlurper;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

public class JsonUtil {

    private static <T> void genericMerge(List<File> files, List<T> toMergeInto) {
        JsonSlurper slurper = new JsonSlurper();
        for (File f : files) {
            toMergeInto.addAll((List<T>)slurper.parse(f));
        //merged.addAll(slurper.parse(f) as List)
        }
    }

    public static void mergeArrays(List<File> files, File outfile) {

        List<?> merged = new ArrayList<>();
        genericMerge(files, merged);

        GsonBuilder gbuilder = new GsonBuilder();
        gbuilder.setPrettyPrinting();
        String json = gbuilder.create().toJson(merged);

        outfile.getParentFile().mkdirs();
        try {
            ResourceGroovyMethods.setText(outfile, json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
