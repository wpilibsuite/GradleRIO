import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

class WPIExtension {
    String wpilibVersion = "+"
    String ntcoreVersion = "+"
    String opencvVersion = "+"
    String cscoreVersion = "+"

    String talonSrxVersion = "+"
    String navxVersion = "+"

    WPIExtension(Project project) { }
}