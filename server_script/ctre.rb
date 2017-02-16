require 'tempfile'
require 'open-uri'
require 'fileutils'
require 'zip'
require 'digest'
require_relative 'common'

# Nightly Script that runs on my server to download CTRE Toolsuite and make it available
# as a maven download
# Requires gem rubyzip

URL = "http://www.ctr-electronics.com/downloads/lib/"
LIBREGEX = /href=\"(CTRE_FRCLibs_NON-WINDOWS(_[a-zA-Z0-9\.]+)?\.zip)\"/
GROUP = "thirdparty.frc.ctre"
ARTIFACT_JAVA = "Toolsuite-Java"
ARTIFACT_JNI = "Toolsuite-JNI"
ARTIFACT_ZIP = "Toolsuite-Zip"
BASEPATH = "maven/#{GROUP.gsub('.', '/')}"
TMPDIR = "tmp"
FETCH_NEW = !ARGV.include?("--no-fetch")

zip_file = "#{TMPDIR}/ctre_toolsuite.zip"
vers = "0.0.0"

if FETCH_NEW
    FileUtils.rm_rf(TMPDIR) if File.exists?(TMPDIR)
    FileUtils.mkdir_p(TMPDIR)

    puts "Fetching CTRE Release..."
    tmp = open(zip_file, "wb")
    open(URL, "rb") do |readfile|
		lib = readfile.read.scan(LIBREGEX).last.first	# Read HTML, parse releases, get the latest, get the full name (regex group 1)
		open("#{URL}#{lib}", "rb") do |readfile_zip|
			tmp.write(readfile_zip.read)
		end
    end
    tmp.close
    puts "CTRE Release Fetched!"
end

artifacts = [ [ARTIFACT_ZIP, zip_file] ]

Zip::File.open(zip_file) do |zip|
    entry = zip.select { |x| x.name.include? "VERSION_NOTES" }.first
    vers_content = entry.get_input_stream.read
    vers = vers_content.match(/CTRE Toolsuite: ([0-9\.a-zA-Z]*)/)[1]
	puts "CTRE Version: #{vers}"

    libs = zip.select { |x| x.name.include? "java/lib/" }
    jar = libs.select { |x| x.name.include? ".jar" }.first
    native = libs.select { |x| x.name.include? ".so" }.first

    jarfile = "#{TMPDIR}/ctre_toolsuite_java.jar"
    jnifile = "#{TMPDIR}/ctre_toolsuite_jni.so"

    FileUtils.rm(jarfile) if File.exists?(jarfile)
    jar.extract(jarfile)
    FileUtils.rm(jnifile) if File.exists?(jnifile)
    native.extract(jnifile)

    artifacts << [ARTIFACT_JAVA, jarfile]
    artifacts << [ARTIFACT_JNI, jnifile]
end

populate_artifacts artifacts, vers