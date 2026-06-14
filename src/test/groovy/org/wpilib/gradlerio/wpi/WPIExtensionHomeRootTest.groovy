package org.wpilib.gradlerio.wpi

import spock.lang.Specification

class WPIExtensionHomeRootTest extends Specification {
    def "WPILib home root uses platform specific locations"() {
        expect:
        WPIExtension.computeHomeRoot(isWindows, isMacOsX, isLinux, userHome, publicFolder, xdgDataHome) == expected

        where:
        isWindows | isMacOsX | isLinux | userHome       | publicFolder       | xdgDataHome        || expected
        true      | false    | false   | "/home/user"   | "C:\\Users\\Team"  | null               || new File("C:\\Users\\Team", "wpilib")
        true      | false    | false   | "/home/user"   | null               | null               || new File("C:\\Users\\Public", "wpilib")
        false     | true     | false   | "/Users/team"  | null               | null               || new File("/Users/team", ".wpilib")
        false     | false    | true    | "/home/team"   | null               | "/data/team/share" || new File("/data/team/share", "wpilib")
        false     | false    | true    | "/home/team"   | null               | null               || new File("/home/team/.local/share", "wpilib")
        false     | false    | true    | "/home/team"   | null               | ""                 || new File("/home/team/.local/share", "wpilib")
        false     | false    | false   | "/home/team"   | null               | "/data/team/share" || new File("/home/team", ".wpilib")
    }
}
