require 'tempfile'
require 'open-uri'
require 'fileutils'
require 'zip'
require 'digest'

# Nightly Script that runs on my server to download CTRE Toolsuite and make it available
# as a maven download

# All new releases seem to be at this address, with no archiving for non-installer versions
URL = "http://www.ctr-electronics.com/downloads/lib/CTRE_FRCLibs_NON-WINDOWS.zip"
GROUP = "thirdparty.frc.ctre"
ARTIFACT_JAVA = "Toolsuite-Java"
ARTIFACT_JNI = "Toolsuite-JNI"
ARTIFACT_ZIP = "Toolsuite-Zip"
BASEPATH = "maven/#{GROUP.gsub('.', '/')}"
TMPDIR = "tmp"
FETCH_NEW = false

zip_file = "#{TMPDIR}/ctre_toolsuite.zip"
vers = "0.0.0"

if FETCH_NEW
    FileUtils.rm_rf(TMPDIR) if File.exists?(TMPDIR)
    FileUtils.mkdir_p(TMPDIR)

    tmp = open(zip_file, "wb")
    open(URL, "rb") do |readfile|
        tmp.write(readfile.read)
    end
    tmp.close
end

artifacts = [ [ARTIFACT_ZIP, zip_file] ]

Zip::File.open(zip_file) do |zip|
    entry = zip.select { |x| x.name.include? "VERSION_NOTES" }.first
    vers_content = entry.get_input_stream.read
    vers = vers_content.match(/CTRE Toolsuite: ([0-9\.a-zA-Z]*)/)[1]

    libs = zip.select { |x| x.name.include? "java/lib/" }
    jar = libs.select { |x| x.name.include? ".jar" }.first
    native = libs.select { |x| x.name.include? ".so" }.first

    jarfile = "#{TMPDIR}/ctre_toolsuite_java.jar"
    jnifile = "#{TMPDIR}/ctre_toolsuite_jni.so"

    if FETCH_NEW
        jar.extract(jarfile)
        native.extract(jnifile)
    end

    artifacts << [ARTIFACT_JAVA, jarfile]
    artifacts << [ARTIFACT_JNI, jnifile]
end

artifacts.each do |a|
    artifact_id = a[0]
    artifact_tmp = a[1]
    base_dir = "#{BASEPATH}/#{artifact_id}"
    vers_dir = "#{base_dir}/#{vers}"
    artifact_file = "#{vers_dir}/#{File.basename a[1]}"
    pom_file = "#{vers_dir}/#{File.basename(a[1], ".*")}.pom"
    meta_file = "#{base_dir}/maven_metadata.xml"

    FileUtils.mkdir_p vers_dir
    FileUtils.cp artifact_tmp, artifact_file
    File.write "#{artifact_file}.md5", Digest::MD5.file(artifact_file).hexdigest
    File.write "#{artifact_file}.sha1", Digest::SHA1.file(artifact_file).hexdigest

    pom = <<-POM
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>
    <groupId>#{GROUP}</groupId>
    <artifactId>#{artifact_id}</artifactId>
    <version>#{vers}</version>
</project>
    POM
    File.write pom_file, pom
    File.write "#{pom_file}.md5", Digest::MD5.hexdigest(pom)
    File.write "#{pom_file}.sha1", Digest::SHA1.hexdigest(pom)

    metadata = [
        "<metadata>",
        "   <groupId>#{GROUP}</groupId>",
        "   <artifactId>#{artifact_id}</artifactId>",
        "   <versioning>",
        "       <release>#{vers}</release>",
        "       <versions>",
        [Dir.glob("#{base_dir}/*").map { |f| "           <version>#{File.basename f}</version>" }],
        "       </versions>",
        "       <lastUpdated>#{DateTime.now.new_offset(0).strftime("%Y%m%d%H%M%S")}</lastUpdated>",
        "   </versioning>",
        "</metadata>"
    ].flatten.join("\n")
    File.write meta_file, metadata
    File.write "#{meta_file}.md5", Digest::MD5.hexdigest(metadata)
    File.write "#{meta_file}.sha1", Digest::SHA1.hexdigest(metadata)
end