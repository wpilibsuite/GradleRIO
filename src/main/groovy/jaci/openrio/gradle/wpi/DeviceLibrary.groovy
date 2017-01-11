import org.gradle.api.file.FileCollection;

class DeviceLibrary {
    final String name
    FileCollection java
    FileCollection libs

    DeviceLibrary(String name) {
        this.name = name
    }

    void java(FileCollection files) {
        java = java == null ? files : java + files
    }

    void libs(FileCollection files) {
        libs = libs == null ? files : libs + files
    }
}