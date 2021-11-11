/*
 * Status Avatar Driver
 *
 * Displays circular image with decorations indicating status details
 *
 */
metadata {
    definition(name: 'Status Avatar Driver', namespace: 'simnet', author: 'sburke781') {
        capability 'Switch'

        attribute 'lastUpdate', 'text'
        attribute 'switch', "ENUM ['on', 'off']"
		attribute 'iFrame', 'text'

        command 'refresh'
        
    }
}

preferences {
		input name: 'htmlFileName',   type: 'text',   title: 'HTML File Name (inc. file extension, e.g. imageCycle.html)', required: true,  defaultValue: 'imageCycle.html'
        input name: 'avatarImageURL', type: 'text',   title: 'Image URL',                                                  required: true,  defaultValue: ''
        input name: 'includeIFrame',  type: 'bool',   title: 'Produce an IFrame attribute for Dashboard display',          required: true,  defaultValue: true
        input name: 'imageHeight',   type: 'number',  title: 'Height of the avatar image',                                 required: true,  defaultValue: 625
        input name: 'imageWidth',    type: 'number',  title: 'Width of the avatar image',                                  required: true,  defaultValue: 600

		input(name: 'DebugLogging', type: 'bool',   title: 'Enable Debug Logging', displayDuringSetup: true, defaultValue: false)
        input(name: 'WarnLogging',  type: 'bool',   title: 'Enable Warning Logging', displayDuringSetup: true, defaultValue: true)
        input(name: 'ErrorLogging', type: 'bool',   title: 'Enable Error Logging', displayDuringSetup: true, defaultValue: true)
        input(name: 'InfoLogging',  type: 'bool',   title: 'Enable Description Text (Info) Logging', displayDuringSetup: true, defaultValue: false)
}

void installed() {
    debugLog('installed: Status Avatar device installed')
}

void updated() {
    debugLog('updated: update process called')
    writeHTML()
    refresh()
}

void refresh() {
    debugLog('refresh: Refreshing status avatar')
    updateAvatar(device.currentValue('switch'))
}

void on() {
    sendEvent(name: 'switch', value: 'on');
    updateAvatar('on')
}

void off() {
    sendEvent(name: 'switch', value: 'off');
    updateAvatar('off')
}

void updateAvatar(String pswitch) {
    String lastUpdate = null

    writeHTML(pswitch);
    if (includeIFrame) {
        sendEvent(name: 'iFrame', value: "<div style='height: 100%; width: 100%'><iframe src='http://${location.hub.localIP}:8080/local/${htmlFileName}' style='height: ${iFrameHeight}px; width: ${iFrameWidth}px; border: none;' scrolling=no version=${iFrameCounter()}></iframe><div>")
        lastUpdate = new Date().format('YYYY-MM-dd HH:mm:ss')
        //device.sendEvent(name: 'lastupdate', value: "${lastUpdate}")
        debugLog('updateAvatar: IFrame attribute updated')
    }
    else { debugLog('updateAvatar: IFrame attribute turned off, no update is needed') }
}

// HTML File Methods

void writeHTML(String pswitch) {
    String htmlContent = """<html>
<head>
<style>
.img-box{    
    
}

a.avatar-link{
    position: relative;
    display: inline-block;
}

.img-box img.user-avatar{
    border-radius: 50%;
    display: block;
}

.messages {
    border-radius: 50%;
    height: 20px;
    position: absolute;
    left: 5%;
    top: 5%;
    width: 20px;
    background-color: #669600;
	text-align: center;
}

.notifications {
    border-radius: 50%;
    height: 20px;
    position: absolute;
    right: 5%;
    top: 5%;
    width: 20px;
    background-color: #669600;
	text-align: center;
}

.img-box img.user-avatar[width="${imageWidth}"]{
   right: 2px;
   bottom: 2px;
}
</style>
</head>

<body>
<div class="img-box">
    <a class="avatar-link" href="#">
<img width="${imageWidth}" height="${imageHeight}" alt="avatar" src="${avatarImageURL}" class="user-avatar">
""";
    if(pswitch == "on") { htmlContent += """
<div class="notifications"></div>
"""; }
htmlContent += """   </a>
</div>

</body>
</html>""";
    
    writeFile(htmlFileName,htmlContent);
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