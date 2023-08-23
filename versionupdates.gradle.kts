
val versionMap = listOf(
  wpilibVersion = "edu.wpi.first.wpilibj =wpilibj-java =+",
  smartDashboardVersion = "edu.wpi.first.tools =SmartDashboard =+ =win64",
  outlineViewerVersion = "edu.wpi.first.tools =OutlineViewer =+ =windowsx86-64@zip",
  robotBuilderVersion = "edu.wpi.first.tools =RobotBuilder =+",
  shuffleboardVersion = "edu.wpi.first.tools =Shuffleboard =+ =win64",
  pathWeaverVersion = "edu.wpi.first.tools =PathWeaver =+ =win64",
  glassVersion = "edu.wpi.first.tools =Glass =+ =windowsx86-64@zip",
  sysIdVersion = "edu.wpi.first.tools =SysId =+ =windowsx86-64@zip",
  roboRIOTeamNumberSetterVersion = "edu.wpi.first.tools =roboRIOTeamNumberSetter =+ =windowsx86-64@zip",
  dataLogToolVersion = "edu.wpi.first.tools =DataLogTool =+ =windowsx86-64@zip",
  opencvVersion = "edu.wpi.first.thirdparty.frc2023.opencv =opencv-java =+",
  googleTestVersion = "edu.wpi.first.thirdparty.frc2023 =googletest =+ =headers",
  niLibrariesVersion = "edu.wpi.first.ni-libraries =runtime =+ =allowedimages@zip",
  imguiVersion = "edu.wpi.first.thirdparty.frc2023 =imgui =+ =headers",
  wpimathVersion = "edu.wpi.first.wpimath =wpimath-java =+"
)

configurations {
  gradleRioVersions
}

val useDevelopmentProperty = "useDevelopment"

project.repositories.maven { repo ->
    repo.name = "WPI"
    if (project.hasProperty(useDevelopmentProperty)) {
      repo.url = "https://frcmaven.wpi.edu/artifactory/development"
    } else {
      repo.url = "https://frcmaven.wpi.edu/artifactory/release"
    }
}

dependencies {
  versionMap.each { key, value ->
    gradleRioVersions value
  }
}

val regex: String = "String\\s+?placeholder\\s+?=\\s+?listOf(\\\"|\\").+?listOf(\\\"|\\")"
val mavenDevRegex: String = "this\\.useDevelopment\\s*=\\s*(true|false)"
val validVersionsRegex: String = "validImageVersions = List\\.of\\((.+)\\);"

tasks.register("UpdateVersions") {
  doLast {
    val mavenExtFile = file("src/main/java/edu/wpi/first/gradlerio/wpi/WPIMavenExtension.java")
    val mavenExtText = mavenExtFile.text
    val toSet = "this.useDevelopment = false"
    if (project.hasProperty(useDevelopmentProperty)) {
      toSet = "this.useDevelopment = true"
    }

    mavenExtFile.text = mavenExtText.replaceAll(mavenDevRegex, toSet)


    val extFile = file("src/main/java/edu/wpi/first/gradlerio/wpi/WPIVersionsExtension.java")
    val extText = extFile.text
    configurations.gradleRioVersions.resolvedConfiguration.resolvedArtifacts.each {
      versionMap.each { key, value ->
        val id = it.moduleVersion.id
        if (value.startsWith("${id.group}:${it.name}:+".toString())) {
          val localRegex = regex.replace("placeholder", key)
          extText = extText.replaceAll(localRegex, "String ${key} = \"${id.version}\"".toString())
        }
      }
    }
    extFile.text = extText

    val allowedVersions = ""
    val first = true
    configurations.gradleRioVersions.resolvedConfiguration.resolvedArtifacts.each {
      if (it.classifier == "allowedimages") {
        val f = project.zipTree(it.file)
        f.visit {
          if (it.name == "allowed_images.txt") {
            it.file.eachLine {
              if (!first) {
                allowedVersions += ", "
              }
              first = false
              allowedVersions += "\"$it\""
            }
          }
        }
      }
    }

    val rootExtFile = file("src/main/java/edu/wpi/first/gradlerio/wpi/WPIExtension.java")
    val rootExtText = rootExtFile.text

    rootExtFile.text = rootExtText.replaceAll(validVersionsRegex, "validImageVersions = List.of($allowedVersions);")
  }
}

