require 'tempfile'
require 'open-uri'
require 'fileutils'
require 'zip'
require 'digest'
require_relative 'common'

URL = "http://www.kauailabs.com/public_files/navx-mxp/navx-mxp-libs.zip"
GROUP = "thirdparty.frc.kauai"
ARTIFACT_JAVA = "Navx-Java"
ARTIFACT_ZIP = "Navx-Zip"
BASEPATH = "maven/#{GROUP.gsub('.', '/')}"
TMPDIR = "tmp"
FETCH_NEW = !ARGV.include?("--no-fetch")

zip_file = "#{TMPDIR}/navx-mxp-libs.zip"
vers = "0.0.0"

if FETCH_NEW
    FileUtils.rm_rf(TMPDIR) if File.exists?(TMPDIR)
    FileUtils.mkdir_p(TMPDIR)

    puts "Fetching NavX Release..."
    tmp = open(zip_file, "wb")
    open("#{URL}", "rb") do |readfile_zip|
        tmp.write(readfile_zip.read)
    end
    tmp.close
    puts "NavX Release Fetched!"
end

artifacts = [ [ARTIFACT_ZIP, zip_file] ]

Zip::File.open(zip_file) do |zip|
    entry = zip.select { |x| x.name.include? "version.txt" }.first
    vers = entry.get_input_stream.read.strip
    puts "NavX Version: #{vers}"

    jar = zip.select { |x| x.name.include?("roborio/java/lib") && x.name.include?(".jar") }.first
    jarfile = "#{TMPDIR}/navx_frc.jar"

    FileUtils.rm(jarfile) if File.exists?(jarfile)
    jar.extract jarfile

    artifacts << [ARTIFACT_JAVA, jarfile]
end

populate_artifacts artifacts, vers