

package iup

import net.lingala.zip4j.core.ZipFile

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

import groovy.json.JsonSlurper
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient

import org.joda.time.DateTime
import org.apache.commons.io.FileUtils

import groovyx.net.http.ContentType

import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import static groovyx.net.http.ContentType.*

import java.util.regex.*

public class IUp {
	
	private SimpleStringProperty messageProperty;
	private SimpleDoubleProperty progressProperty;
	
	static DateTimeFormatter formatter = DateTimeFormat.forPattern('yyyyMMdd-HHmmss')
	
	private File logFile;

	public IUp(SimpleStringProperty messageProperty, SimpleDoubleProperty progressProperty) {
		this.messageProperty = messageProperty;
		this.progressProperty = progressProperty;
		
	}

	def logInfoMessage(String log) {
		logInfoMessage(log, null)
	}
	
	def logInfoMessage(String log, Integer progress) {
		logMessage(log, progress, false)
	}
	
	def logDebugMessage(String log) {
		logMessage(log, null, true)
	}
	
	def logMessage(String log, Integer progress, boolean debug) {
		
		// always log to file (iup.log)
		this.logFile.append(log)
		this.logFile.append(System.getProperty("line.separator"))
		
		if (this.messageProperty == null || debug) {
			// println log -> launch4j executable doesn't like this line... ><
		} else {
			this.messageProperty.setValue(log) // info only
		}
		
		if (this.progressProperty != null && progress != null) {
			this.progressProperty.setValue(progress/100)
		}
	}
	
	def getLatestArtifact() {
		
		try {
			
			// ------------------------------------------
			// step 0: create log file
			this.logFile = new File("iup.log")
			this.logFile.write("")
		
			// ------------------------------------------
			// step 1: load config from json file
			
			logInfoMessage("Loading config from config.json...")
			
			Map config = loadConfig();
			
			logInfoMessage( "Config loaded ${config}.", 5)
		
			
			// ------------------------------------------
			// step 2: archive old a10 folder 
			
			// archiving d:\magpie\test\a10 to d:\magpie\test\archive\a10-20150119-152658
			File archive = new File(config['archiveDir'], "${config['artifactId']}-${formatter.print(new DateTime())}")
			File artifact = new File(config['targetDir'], config['artifactId']) 
			
			logInfoMessage("Archiving ${artifact} to ${archive}")
			
			archiveArtifact(artifact, archive)
			
			logInfoMessage("Folder ${artifact} moved to ${archive}.", 10)
			
			
			// ------------------------------------------
			// step 3: get all versions from artifactory and look for latest one 
			
			String version = getLatestArtifactName(config)
			
			logInfoMessage("Latest version found ${version}.", 20)
			
			
			// ------------------------------------------
			// step 4: download zip from artifactory
			
			// ex: http://localhost:8081/artifactory/libs-release-local/m10/a10/10.3.0.0-20141111.164342/a10-10.3.0.0-20141111.164342.zip
			String url = "${config['artifactoryServer']}/${config['artifactoryName']}/${config['repoKey']}/${config['groupId']}/${config['artifactId']}/${version}/${config['artifactId']}-${version}.zip"
			
			logInfoMessage("Retrieving zip from url ${url}...")
		
			// download zip from artifactory to target folder and rename it to [artifactId].zip (ex: a10.zip)
			File zipFile = new File(config['targetDir'], "${config['artifactId']}.zip") 
			FileUtils.copyURLToFile(new URL(url), zipFile)
			
			logInfoMessage("Artifact zip downloaded.", 60)
			
			
			// ------------------------------------------
			// step 5: extract zip file
			
			File outputDir = new File(config['targetDir']) // , config['artifactId']
			
			logInfoMessage("Extracting file ${zipFile} to ${outputDir}...")
			
			unzipArtifact(zipFile, outputDir)
			
			logInfoMessage("File extracted.", 90)
			
			
			// ------------------------------------------
			// step 6: delete zip file
			
			logInfoMessage("Deleting zip file ${zipFile}...")
			
			zipFile.delete()
			
			logInfoMessage("Zip file deleted.", 100)
			
			logInfoMessage("App updated successfully.")
		
				
		} catch (Exception e) {
			e.printStackTrace()
			logInfoMessage(e.getMessage())
			
		}
	}

	def loadConfig() {
		
		JsonSlurper slurper = new JsonSlurper()
		Map config = slurper.parseText(new File("config.json").text)
		Map settings = slurper.parseText(new File("settings.json").text)
		
		// settings overrides config
		config << settings
		
		return config
	}
		
	def archiveArtifact(File artifact, File archive) {
		
		if (!artifact.exists()){
			logInfoMessage("No folder ${artifact} found. Skip archive.")
			return
		}
		
		FileUtils.moveDirectory(artifact, archive)
		
	}
	
	def getLatestArtifactName(Map config) {
		
		RESTClient artifactory = new RESTClient(config['artifactoryServer'])
		
		String path = "/${config['artifactoryName']}/api/search/gavc" 
		
		HttpResponseDecorator response = artifactory.get(path: "/${config['artifactoryName']}/api/search/gavc", query: [g: config['groupId'],a: config['artifactId'], repo: config['repoKey']]) // , v: '10.3.0.0*'
		
		// convert from input stream to map
		BufferedReader streamReader = new BufferedReader(new InputStreamReader(response.data))
		JsonSlurper slurper = new JsonSlurper()
		Map results = slurper.parse(streamReader)
		
		String latestVersion = parseResults(results.get('results'), config)
		
		return latestVersion 
	}
	
	def parseResults(List uriList, Map config) {
		
		logInfoMessage("Parsing results...")
		
		List<Version> versionList = new ArrayList<Version>()
		String uri, matchURI
		def matcher, parse

		for(int i = 0; i < uriList.size(); i++) {
			
			uri = ((Map) uriList[i]).get("uri")
			
			logDebugMessage("Matching uri ${uri}...")
			
			// a10-10.3.0.0-20141111.164342.zip
			matcher = uri =~ /.+\/[a-zA-Z0-9]+-([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+-[0-9]{8}\.[0-9]{6})\.zip/

			if (!matcher.matches()) continue;

			matchURI = matcher[0][1]

			logDebugMessage("Match found ${matchURI}.")
			
			versionList.add(Version.getVersion(matchURI))
			
		}
		
		logInfoMessage("Results are parsed.")
		
		Collections.sort(versionList)
		
		return versionList.last().getString()
		
	}
	
	def unzipArtifact(File artifactFile, File outputDir) {
		
		// initiate ZipFile object with the path/name of the zip file.
		ZipFile zipFile = new ZipFile(artifactFile) 
		
		// extracts all files to the path specified
		zipFile.extractAll(outputDir.getAbsolutePath())
	}
	
	public static void main(String[] args) {
		IUp iUp = new IUp(null, null);
		iUp.getLatestArtifact();
	}
	
}

/**
 * 
 * 	10.1.0.1 (dev)
	10.1.0.2 (dev)
	10.1.0.0 (dev release)
	10.1.1.1 (patch)
	10.1.1.2 (patch)
	10.1.1.0 (patch release)
	10.2.0.1 (dev)
 *
 */
class Version implements Comparable {
	
	static DateTimeFormatter formatter = DateTimeFormat.forPattern('yyyyMMdd.HHmmss')
	
	Integer major, dev, patch, build
	DateTime timestamp
	
	
	static Version getVersion(String text) {

		// 10.3.0.0-20141113.021400
		def matcher = text =~ /([0-9]+)\.([0-9]+)\.([0-9]+)\.([0-9]+)-([0-9]{8}\.[0-9]{6})/
		
		if (!matcher.matches()) return null
		
		// the first element matcher[0][0] is the full match string (ex: 10.3.0.0-20141113.021400) 
		 return new Version(matcher[0][1], matcher[0][2], matcher[0][3], matcher[0][4], matcher[0][5])
		
	}

	Version(String majorStr, String devStr, String patchStr, String buildStr, String timestampStr) {
		this.major = new Integer(majorStr)
		this.dev = new Integer(devStr)
		this.patch = new Integer(patchStr)
		this.build = new Integer(buildStr)
		this.timestamp = formatter.parseDateTime(timestampStr)
	}
	
	public Version(int m, int d, int p, int b, DateTime t) {
		this.major = m
		this.dev = d
		this.patch = p
		this.build = b
		this.timestamp = t
	}

	
	@Override
	public int compareTo(Object object) {
		
		if (!object instanceof Version) throw new Exception("Failed to compare Version with unknown class %{object.getClass()}")
		
		Version version = (Version) object
		
		if (!this.major.equals(version.major)) return this.major.compareTo(version.major)
		if (!this.dev.equals(version.dev)) return this.dev.compareTo(version.dev)
		if (!this.patch.equals(version.patch)) return this.patch.compareTo(version.patch)
		if (!this.build.equals(version.build)) return this.build.compareTo(version.build)
		
		return this.timestamp.compareTo(version.timestamp)
	}
	
	public boolean equals(Object object) {
		if (!object instanceof Version) return false
		
		Version version = (Version) object
		
		if (!this.major.equals(version.major)) return false
		if (!this.dev.equals(version.dev)) return false
		if (!this.patch.equals(version.patch)) return false
		if (!this.build.equals(version.build)) return false
		
		return this.timestamp.equals(version.timestamp)
	}
	
	public String getString() {
		return "${this.major}.${this.dev}.${this.patch}.${this.build}-${formatter.print(this.timestamp)}"
	}
	
}
