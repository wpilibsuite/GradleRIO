package test.frc.lib;

import com.ctre.CANTalon;
import test.lib.NonFrcLibrary;

public class FrcLibrary {

    static CANTalon myTalon;

    public static void libraryInit() {
        myTalon = new CANTalon(1);
        System.out.println(NonFrcLibrary.getLibraryString());
    }

    public static CANTalon getTalon() {
        return myTalon;
    }

}
