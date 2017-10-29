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
LIBREGEX = /href=\"(CTRE_Phoenix_FRCLibs_NON-WINDOWS(_[a-zA-Z0-9\.]+)?\.zip)\"/
NUMREGEX = /[0-9]+/
GROUP = "thirdparty.frc.ctre"
ARTIFACT_JAVA = "Phoenix-Java"
ARTIFACT_JNI = "Phoenix-JNI"
ARTIFACT_ZIP = "Phoenix-Zip"
BASEPATH = "maven/#{GROUP.gsub('.', '/')}"
TMPDIR = "tmp"
FETCH_NEW = !ARGV.include?("--no-fetch")

zip_file = "#{TMPDIR}/ctre_phoenix.zip"
vers = "0.0.0"

if FETCH_NEW
    FileUtils.rm_rf(TMPDIR) if File.exists?(TMPDIR)
    FileUtils.mkdir_p(TMPDIR)

    puts "Fetching CTRE Release..."
    tmp = open(zip_file, "wb")
    open(URL, "rb") do |readfile|
        libs = readfile.read.scan(LIBREGEX).select { |x| x.last != nil }
        libs.sort_by! { |x| tot=1; x.last.scan(NUMREGEX).reverse.map { |y| tot *= 100; y.to_i * tot }.inject(0){|sum,x| sum + x } }
        lib = libs.last.first	# Read HTML, parse releases, get the latest, get the full name (regex group 1)
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
    vers = vers_content.match(/CTRE Phoenix Framework: ([0-9\.a-zA-Z]*)/)[1]
	puts "CTRE Version: #{vers}"

    libs = zip.select { |x| x.name.include? "java/lib/" }
    jar = libs.select { |x| x.name.include? 'CTRE_Phoenix.jar' }.first
    srcjar = libs.select { |x| x.name.include? 'CTRE_Phoenix-sources.jar' }.first
    native = libs.select { |x| x.name.include? ".so" }.first

    jarfile = "#{TMPDIR}/ctre_phoenix_java.jar"
    srcfile = "#{TMPDIR}/ctre_phoenix_java_src.jar"
    jnifile = "#{TMPDIR}/ctre_phoenix_jni.so"

    FileUtils.rm(jarfile) if File.exists?(jarfile)
    jar.extract(jarfile)
    FileUtils.rm(srcfile) if File.exists?(srcfile)
    srcjar.extract(srcfile)
    FileUtils.rm(jnifile) if File.exists?(jnifile)
    native.extract(jnifile)

    artifacts << [ARTIFACT_JAVA, jarfile, srcfile]
    artifacts << [ARTIFACT_JNI, jnifile]
end

populate_artifacts artifacts, vers