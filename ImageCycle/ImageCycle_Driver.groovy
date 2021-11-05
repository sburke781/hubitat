/*
 * Image Cycle Driver
 *
 * Stores a HTML file locally on the HE hub for display of images that cycle periodically on screen
 *
 */
metadata {
    definition(name: 'Image Cycle', namespace: 'simnet', author: 'sburke781') {
	    capability 'Actuator'

		attribute 'lastupdate', 'string'
		attribute 'iFrame', 'text'

        command 'refresh'
        command 'setImageList', [[name:'imageMap', type: 'STRING', description: 'Enter the image list (JSON)' ] ]
        command 'setBackgroundList', [[name:'backgroundMap', type: 'STRING', description: 'Enter the background list (JSON)' ] ]
    }
}

preferences {
		input name: 'dataFileName', type: 'text',   title: 'Data File Name (inc. file extension, e.g. imageCycle.json)', required: true,  defaultValue: 'imageCycle.json'
        input name: 'htmlFileName', type: 'text',   title: 'HTML File Name (inc. file extension, e.g. imageCycle.html)', required: true,  defaultValue: 'imageCycle.html'
        input name: 'cycleFreq',    type: 'number', title: 'Number of seconds each image is displayed',                  required: true,  defaultValue: 1
        input name: 'includeIFrame',type: 'bool',   title: 'Produce an IFrame attribute for Dashboard display',          required: true,  defaultValue: true
        input name: 'iFrameHeight', type: 'number', title: 'Height of the IFrame',                                       required: true,  defaultValue: 625
        input name: 'iFrameWidth',  type: 'number', title: 'Width of the IFrame',                                        required: true,  defaultValue: 600

		input(name: 'DebugLogging', type: 'bool',   title: 'Enable Debug Logging', displayDuringSetup: true, defaultValue: false)
        input(name: 'WarnLogging',  type: 'bool',   title: 'Enable Warning Logging', displayDuringSetup: true, defaultValue: true)
        input(name: 'ErrorLogging', type: 'bool',   title: 'Enable Error Logging', displayDuringSetup: true, defaultValue: true)
        input(name: 'InfoLogging',  type: 'bool',   title: 'Enable Description Text (Info) Logging', displayDuringSetup: true, defaultValue: false)
}

void installed() {
    debugLog('installed: Image Cycle device installed')
}

void updatePreferences(String pdataFile, String phtmlFile, int pcycleFreq, boolean pincludeIFrame, int piFrameHeight, int piFrameWidth ) {
    dataFileName  = pdataFile
    htmlFileName  = phtmlFile
    cycleFreq     = pcycleFreq
    includeIFrame = pincludeIFrame
    iFrameHeight  = piFrameHeight
    iFrameWidth   = piFrameWidth
    updated()
}

void updated() {
    debugLog('updated: update process called')
    writeHTML()
    refresh()
}

void refresh() {
    debugLog('refresh: Refreshing image cycle')
    String lastUpdate = null

    if (fileExists(dataFileName)) { setImageList(null) }

    if (includeIFrame) {
        sendEvent(name: 'iFrame', value: "<div style='height: 100%; width: 100%'><iframe src='http://${location.hub.localIP}:8080/local/${htmlFileName}' style='height: ${iFrameHeight}px; width: ${iFrameWidth}px; border: none;' scrolling=no version=${iFrameCounter()}></iframe><div>")
        lastUpdate = new Date().format('YYYY-MM-dd HH:mm:ss')
        device.sendEvent(name: 'lastupdate', value: "${lastUpdate}")
        debugLog('refresh: IFrame attribute updated')
    }
    else { debugLog('refresh: IFrame attribute turned off, no updated is needed') }
}

void setDataFileName(String pdataFileName) {

    dataFileName = pdataFileName;
    infoLog("Data file name set to ${pdataFileName}")
}

// Data File Methods

void addBackgroundImage(int pposition, String purl) {
    debugLog("setBackgroundImage: Background image being added...Position = ${pposition}, URL = ${purl}")
    if (pposition > 3 || pposition < 1 || purl == null || purl == '') { errorLog("setBackgroundImage: Error with parameters provided: Position = ${pposition}, URL = ${purl}") }
    else {
        debugLog('setBackgroundImage: to be implemented....')
    }
}

void addImage(int pposition, String purl) {
    debugLog("addImage: Image being added...Position = ${pposition}, URL = ${purl}")
    if (pposition > 99 || pposition < 1 || purl == '') { errorLog("addImage: Error with parameters provided: Position = ${pposition}, URL = ${purl}") }
    else {
        debugLog('addImage: to be implemented....')
    }
}

void setBackgroundList(String pbackgrounds) {
    Map newBackgrounds = null
    Map jsonData  = null

    debugLog("setImageList: new backgrounds provided = ${pbackgrounds}")
    // Read in the existing data file
    try {
        debugLog('setBackgroundList: Attempting to read in existing file')
        jsonData = readImageFile()
        debugLog('setBackgroundList: File read')
    }
    catch (Exception e) {
        errorLog("setBackgroundList: Error reading image data file - ${e}")
    }
    debugLog("setBackgroundList: jsonData returned = ${jsonData}")

    // Attempt to parse the image list JSON provided
    try {
        newBackgrounds = new groovy.json.JsonSlurper().parseText(pbackgrounds)
        debugLog("setBackgroundList: newBackgrounds = ${newBackgrounds}")
    }
    catch (Exception e) {
        errorLog("setBackgroundList: Error parsing new backgrounds - ${e}")
    }

    if (jsonData != null) {
        // Remove the existing backgrounds
        jsonData.findAll { (it.key =~ "^background.*") } each { jsonData.remove it.key }
        // Add the new background list provided
        jsonData.putAll(newBackgrounds)
    }
    else jsonData = newBackgrounds
    debugLog("setBackgroundList: new JsonData = ${jsonData}")

    // Write the updated data file
    writeImageFile(groovy.json.JsonOutput.toJson(jsonData))
}

void setImageList(String pimages) {
    Map newImages = null
    Map jsonData  = null

    debugLog("setImageList: new images provided = ${pimages}")
    // Read in the existing data file
    try {
        debugLog('setImageList: Attempting to read in existing file')
        jsonData = readImageFile()
        debugLog('setImageList: File read')
    }
    catch (Exception e) {
        errorLog("setImageList: Error reading image data file - ${e}")
    }
    debugLog("setImageList: jsonData returned = ${jsonData}")
    // Attempt to parse the image list JSON provided
    try {
        newImages = new groovy.json.JsonSlurper().parseText(pimages)
        debugLog("setImageList: newImages = ${newImages}")
    }
    catch (Exception e) {
        errorLog("setImageList: Error parsing new images - ${e}")
    }

    if (jsonData != null) {
        // Remove the existing images
        jsonData.findAll { (it.key =~ "^image.*") } each { jsonData.remove it.key }
        // Add the new image list provided
        jsonData.putAll(newImages)
    }
    else { jsonData = newImages }
    debugLog("setImageList: new JsonData = ${jsonData}")

    // Write the updated data file
    writeImageFile(groovy.json.JsonOutput.toJson(jsonData))
}

void writeImageFile(String pimagesJson) {
    debugLog("writeImageFile: writing to ${dataFileName}: ${pimagesJson}")
    writeFile(dataFileName, pimagesJson)
}

Map readImageFile() {
    debugLog("readImageFile: reading file ${dataFileName}")
    String data = readFile(dataFileName)
    debugLog("readImageFile: data file contents = ${data}")

    Map json = null

    if (data != null) {
        json = new groovy.json.JsonSlurper().parseText(data)
        debugLog("readImageFile: json from data file ${dataFileName} = ${json}")
    }
    return json
}

// HTML File Methods

void writeHTML() {
    writeFile(htmlFileName,"""<html>

<head>
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.10.1/jquery.min.js"></script>
	<!-- Style settings for different image types -->
	<style>
		img
		{
			display: 'block';
			position: absolute;
		}
        img.background01 { z-index:1; }
		img.background02 { z-index:2; }
		img.background03 { z-index:3; }
		img.images 		 { z-index:4; }
		
    </style>

	<!-- Script to extract the image listing from the JSON file and create the image elements -->
	<script>

	\$(document).ready(function () {
		
			\$.ajax({
			  dataType: "json",
              url: "${dataFileName}",
			  success: function (data) { 
				
				// Find the DIV element that will contain the images
				var divContainer = document.getElementById("picLayer");

				\$.each( data, function( key, val ) {
				
					var img = document.createElement("img");      // IMAGE
					if (key.includes("background")) { img.className = key;      }
					if (key.includes("image")) 	 { img.className = "images"; }
					
					img.src = val;
					divContainer.appendChild(img);
				});

			  }
			}).fail(function(jqXHR, textStatus, errorThrown) {
			  console.error(jqXHR, textStatus, errorThrown);
			  console.error(jqXHR.responseJSON);
			});	
		
		});

	</script>
</head>

<body>
	<!-- DIV container that will house the images -->
	<div id="picLayer" style="position:absolute; width:100%; height:100%; margin-left:0%; margin-top:0% scrolling=no" />
</body>

	<!-- Script to cycle through the image elements with the "images" class, leaving any background images alone -->
	<script>
		var x = 0;
		function myFunction(){  
		
		
		   var Layer2Images = document.querySelectorAll("img.images"); 
		   if (x == Layer2Images.length)
			  x=0;
		   for (i = 0; i < Layer2Images.length; i++) {
			Layer2Images[i].style.display = 'none';
		   }
		   Layer2Images[x].style.display = 'block';
		   x++;
		}

setInterval(myFunction, ${cycleFreq * 1000})

	</script>

</html>""")
}

void poll() {
    refresh()
}

int iFrameCounter() {
    if (state.iFrameCounter == null || state.iFrameCounter > 100) { state.iFrameCounter = 0 }
    else { state.iFrameCounter += 1 }
    return state.iFrameCounter
}

Boolean fileExists(String fName) {

    String uri = "http://${location.hub.localIP}:8080/local/${fName}";

     def params = [
        uri: uri
    ]

    try {
        httpGet(params) { resp ->
            if (resp != null) {
                return true
            }
            else {
                return false
            }
        }
    } catch (Exception) {
        if (exception.message == 'Not Found') {
            debugLog("File DOES NOT Exists for $fName)")
        } else {
            errorLog("Find file $fName) :: Connection Exception: ${exception.message}")
        }
        return false
    }

}

String readFile(String fName) {
    String uri = "http://${location.hub.localIP}:8080/local/${fName}"

    def params = [
        uri: uri,
        contentType: 'text/html; charset=UTF-8',
        headers: [:]
    ]

    try {
        httpGet(params) { resp ->
            if (resp != null) {
               String data = resp.getData()
               return data
            }
            else {
                errorLog('Null Response')
            }
        }
    } catch (exception) {
        errorLog("Read Error: ${exception.message}")
        return null
    }
}

Boolean appendFile(String fName, String newData) {
    try {
        String fileData = readFile(fName)
        fileData = fileData.substring(0, fileData.length() - 1)
        return writeFile(fName, fileData + newData)
    } catch (exception) {
        if (exception.message == "Not Found") {
            return writeFile(fName, newData)
        }
        else {
            errorLog("Append $fName Exception: ${exception}")
            return false
        }
    }
}

Boolean writeFile(String fName, String fData) {
try {
		def params = [
			uri: 'http://127.0.0.1:8080',
			path: '/hub/fileManager/upload',
			query: [
				'folder': '/'
			],
			headers: [
				'Content-Type': 'multipart/form-data; boundary=----WebKitFormBoundaryDtoO2QfPwfhTjOuS'
			],
			body: """------WebKitFormBoundaryDtoO2QfPwfhTjOuS
Content-Disposition: form-data; name="uploadFile"; filename="${fName}"
Content-Type: text/plain

${fData}

------WebKitFormBoundaryDtoO2QfPwfhTjOuS
Content-Disposition: form-data; name="folder"


------WebKitFormBoundaryDtoO2QfPwfhTjOuS--""",
			timeout: 300,
			ignoreSSLIssues: true
		]
		httpPost(params) { resp ->
		}
		return true
	}
	catch (e) {
		errorLog "Error writing file $fName: ${e}"
	}
	return false
}

//Utility methods
void debugLog(String debugMessage) {
	if (DebugLogging == true) { log.debug(debugMessage) }
}

void errorLog(String errorMessage) {
    if (ErrorLogging == true) { log.error(errorMessage) }
}

void infoLog(String infoMessage) {
    if (InfoLogging == true) { log.info(infoMessage) }
}

void warnLog(String warnMessage) {
    if (WarnLogging == true) { log.warn(warnMessage) }
}