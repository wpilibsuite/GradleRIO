package jaci.openrio.gradle;

import org.gradle.api.*;
import org.gradle.api.tasks.*;

import jaci.openrio.delegate.DelegateClient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

public class ToastConsole {
    static DelegateClient LOGGER_DELEGATE;
    static DelegateClient COMMAND_DELEGATE;

    static DataOutputStream commands_out;
    static BufferedReader logger_in;
    
    public static void init(Project project) {
        def consoleTask = project.task('toastConsole') << {
            GradleRIO.tryOnAll(project) {
                try {
                    LOGGER_DELEGATE = new DelegateClient(it, 5805, "TOAST_logger");
                    COMMAND_DELEGATE = new DelegateClient(it, 5805, "TOAST_command");
                    
                    LOGGER_DELEGATE.connect();
                    COMMAND_DELEGATE.connect();

                    commands_out = new DataOutputStream(COMMAND_DELEGATE.getSocket().getOutputStream());
                    logger_in = new BufferedReader(new InputStreamReader(LOGGER_DELEGATE.getSocket().getInputStream()));

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String line;
                            try {
                                while ((line = logger_in.readLine()) != null) {
                                    System.out.println(line);
                                }
                            } catch (IOException e) {
                                System.err.println("Client Disconnected");
                                System.exit(0);
                            }
                        }
                    }).start();

                    Scanner scanner = new Scanner(System.in);
                    while (true) {
                        String line = scanner.nextLine();
                        if (line.trim().equalsIgnoreCase("--exit"))
                            System.exit(0);
                    }
                } catch (Exception e) {
                    System.err.println("Client Disconnected")
                    System.exit(0);
                }
            }
        }
    }

}