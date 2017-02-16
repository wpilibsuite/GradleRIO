def populate_artifacts artifacts, vers
    artifacts.each do |a|
        artifact_id = a[0]
        puts "Populating Maven Artifact #{artifact_id}..."
        artifact_tmp = a[1]
        base_dir = "#{BASEPATH}/#{artifact_id}"
        vers_dir = "#{base_dir}/#{vers}"
        artifact_file = "#{vers_dir}/#{artifact_id}-#{vers}#{File.extname(a[1])}"
        pom_file = "#{vers_dir}/#{File.basename(artifact_file, ".*")}.pom"
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
            [Dir.glob("#{base_dir}/*").reject { |x| x.include? "maven_metadata" }.map { |f| "           <version>#{File.basename f}</version>" }],
            "       </versions>",
            "       <lastUpdated>#{DateTime.now.new_offset(0).strftime("%Y%m%d%H%M%S")}</lastUpdated>",
            "   </versioning>",
            "</metadata>"
        ].flatten.join("\n")
        File.write meta_file, metadata
        File.write "#{meta_file}.md5", Digest::MD5.hexdigest(metadata)
        File.write "#{meta_file}.sha1", Digest::SHA1.hexdigest(metadata)
        puts "Artifact Populated!"
    end
end