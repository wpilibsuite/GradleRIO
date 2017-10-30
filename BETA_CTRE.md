Beta CTRE Phoenix Instructions
===

Download the Phoenix libraries and rename the zip file to 'phoenix.zip'. Place it in a new folder `libs/` in your project root.

In your build.gradle, do the following (depending on your language)
### C++
```gradle
model {
    libraries {
        ctre(NativeLib) {
            targetPlatform = 'roborio'
            headerDirs = []
            staticMatchers = ['**/*.a']
            headerDirs = ['cpp/include']
            libraryMatchers = ['cpp/lib/*.a']
            file = 'libs/phoenix.zip'
        }
    }

    // Then you can link it into your component like usual, e.g:
    components {
        frcUserProgram(NativeExecutableSpec) {
            targetPlatform 'roborio'
            sources.cpp {
                source {
                    srcDirs = ['src/cpp']
                    include '**/*.cpp'
                }
                exportedHeaders{
                    srcDirs = ['src/cpp', 'src/include']
                    include '**/*.hpp', '**/*.h'
                }
                lib library: "wpilib"
                lib library: "ctre"
            }
        }
    }
}
```

### Java
```gradle
dependencies {
    compile zipTree('libs/phoenix.zip').matching { it.include('java/lib/CTRE_Phoenix.jar') }
    nativeLib zipTree('libs/phoenix.zip').matching { it.include('java/lib/libCTRE_PhoenixCCI.so') }
}
```