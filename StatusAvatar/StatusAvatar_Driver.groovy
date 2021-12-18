/*
 * Status Avatar Driver
 *
 * Displays circular image with decorations indicating status details
 *
 */
metadata {
    definition(name: 'Status Avatar Driver', namespace: 'simnet', author: 'sburke781') {
        capability 'Switch'

        attribute 'lastUpdate'  , 'text'
        attribute 'switch'      , "ENUM ['on', 'off']"
        attribute 'statusNum1'  , 'number'
        attribute 'statusNum2'  , 'number'
        attribute 'statusNum3'  , 'number'
        attribute 'statusNum4'  , 'number'
		attribute 'iFrame'      , 'text'

        command 'refresh'
        command 'setStatusNum1', [[name:'statusValue', type: 'NUMBER', description: 'Enter the status value (number)' ] ]
        command 'setStatusNum2', [[name:'statusValue', type: 'NUMBER', description: 'Enter the status value (number)' ] ]
        command 'setStatusNum3', [[name:'statusValue', type: 'NUMBER', description: 'Enter the status value (number)' ] ]
        command 'setStatusNum4', [[name:'statusValue', type: 'NUMBER', description: 'Enter the status value (number)' ] ]
    }
}

preferences {
		input name: 'htmlFileName',     type: 'text',    title: 'HTML File Name (inc. file extension, e.g. imageCycle.html)', required: true,  defaultValue: 'avatar.html'
        input name: 'avatarImageURL',   type: 'text',    title: 'Image URL',                                                  required: true,  defaultValue: ''
        input name: 'includeIFrame',    type: 'bool',    title: 'Produce an IFrame attribute for Dashboard display',          required: true,  defaultValue: true
        input name: 'imageHeight',      type: 'number',  title: 'Height of the avatar image',                                 required: true,  defaultValue: 100
        input name: 'imageWidth',       type: 'number',  title: 'Width of the avatar image',                                  required: true,  defaultValue: 100
        input name: 'imageRounding',    type: 'bool',    title: 'Round the avatar image?',                                    required: true,  defaultValue: true
        input name: 'dotSize',          type: 'number',  title: 'Size (in pixels) of dots displayed',                         required: true,  defaultValue: 20
        input name: 'dotDefaultColour', type: 'text',    title: 'Default colour for dots displayed (Hex Value)',              required: true,  defaultValue: 669600

        input name: 'avatarLink',       type: 'text',    title: 'URL Link opened when clicking on the avatar image',          required: true,  defaultValue: '#'
        input name: 'status1Link',      type: 'text',    title: 'URL Link opened when clicking on status 1 dot',              required: true,  defaultValue: '#'
        input name: 'status2Link',      type: 'text',    title: 'URL Link opened when clicking on status 2 dot',              required: true,  defaultValue: '#'
        input name: 'status3Link',      type: 'text',    title: 'URL Link opened when clicking on status 3 dot',              required: true,  defaultValue: '#'
        input name: 'status4Link',      type: 'text',    title: 'URL Link opened when clicking on status 4 dot',              required: true,  defaultValue: '#'
    
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
    //writeHTML()
    refresh()
}

void refresh() {
    debugLog('refresh: Refreshing status avatar')
    Map statusMap = getStatusMap();

    updateAvatar(statusMap.switchStatus, statusMap.statusNum1, statusMap.statusNum2, statusMap.statusNum3, statusMap.statusNum4)
}

void on() {
    sendEvent(name: 'switch', value: 'on')
    Map statusMap = getStatusMap();

    updateAvatar('on', statusMap.statusNum1, statusMap.statusNum2, statusMap.statusNum3, statusMap.statusNum4)
}

void off() {
    sendEvent(name: 'switch', value: 'off')
    Map statusMap = getStatusMap();

    updateAvatar('off', statusMap.statusNum1, statusMap.statusNum2, statusMap.statusNum3, statusMap.statusNum4)
}

void setStatusNum(Integer pstatusNumber, Number pstatusValue) {
    Boolean vvalidStatusNum   = false;
    Boolean vvalidStatusValue = false;

    if (pstatusNumber >= 1 && pstatusNumber <= 4) { vvalidStatusNum   = true }
    if (pstatusValue >= 0 && pstatusValue <= 99 ) { vvalidStatusValue = true }

    if (vvalidStatusNum && vvalidStatusValue) {
        sendEvent(name: "statusNum${pstatusNumber}", value: pstatusValue)
    }
    else {
        String verrorMessage = 'setStatusNum: Status update failed'
        if (!vvalidStatusNum) { verrorMessage += ", status number ${pstatusNumber} outside the range of 0 - 4" }
        if (!vvalidStatusValue) { verrorMessage += ", status value ${pstatusValue} outside the range of 0 - 99" }
        errorLog(verrorMessage)
    }
}

void setStatusNum1(Number pstatusValue) {
    setStatusNum(1, pstatusValue)
    Map statusMap = getStatusMap();

    updateAvatar(statusMap.switchStatus, (int)pstatusValue, statusMap.statusNum2, statusMap.statusNum3, statusMap.statusNum4)
}

void setStatusNum2(Number pstatusValue) {
    setStatusNum(2, pstatusValue)
    Map statusMap = getStatusMap();

    updateAvatar(statusMap.switchStatus, statusMap.statusNum1, (int)pstatusValue, statusMap.statusNum3, statusMap.statusNum4)
}

void setStatusNum3(Number pstatusValue) {
    setStatusNum(3, pstatusValue)
    Map statusMap = getStatusMap();

    updateAvatar(statusMap.switchStatus, statusMap.statusNum1, statusMap.statusNum2, (int)pstatusValue, statusMap.statusNum4)
}

void setStatusNum4(Number pstatusValue) {
    setStatusNum(4, pstatusValue)
    Map statusMap = getStatusMap();

    updateAvatar(statusMap.switchStatus, statusMap.statusNum1, statusMap.statusNum2, statusMap.statusNum3, (int)pstatusValue)
}

Map getStatusMap() {
    Map statusMap = [:]
    statusMap.put('switchStatus', ((device.currentValue('switch') == null) ? '' : device.currentValue('switch')))
    statusMap.put('statusNum1', ((device.currentValue('statusNum1') == null) ? 0 : (int)device.currentValue('statusNum1')))
    statusMap.put('statusNum2', ((device.currentValue('statusNum2') == null) ? 0 : (int)device.currentValue('statusNum2')))
    statusMap.put('statusNum3', ((device.currentValue('statusNum3') == null) ? 0 : (int)device.currentValue('statusNum3')))
    statusMap.put('statusNum4', ((device.currentValue('statusNum4') == null) ? 0 : (int)device.currentValue('statusNum4')))

    return statusMap
}

void updateAvatar(String pswitch, int pstatus1, int pstatus2, int pstatus3, int pstatus4) {
    String lastUpdate = null

    writeHTML(pswitch, pstatus1, pstatus2, pstatus3, pstatus4)
    if (includeIFrame) {
        sendEvent(name: 'iFrame', value: "<div style='height: 100%; width: 100%'><iframe src='http://${location.hub.localIP}:8080/local/${htmlFileName}' style='height: ${imageHeight + 15}px; width: ${imageWidth + 15}px; border: none;' scrolling=no version=${iFrameCounter()}></iframe><div>")
        lastUpdate = new Date().format('YYYY-MM-dd HH:mm:ss')
        //device.sendEvent(name: 'lastupdate', value: "${lastUpdate}")
        debugLog('updateAvatar: IFrame attribute updated')
    }
    else { debugLog('updateAvatar: IFrame attribute turned off, no update is needed') }
}

// HTML File Methods

void writeHTML(String pswitch, int pstatus1, int pstatus2, int pstatus3, int pstatus4) {
    String htmlContent = """<html>
<head>
<style>
.img-box{    
   left: 0px;
   top: 0px; 
}

a.avatar-link{
    position: relative;
    display: inline-block;
}

.img-box img.user-avatar{
    ${if (imageRounding) { 'border-radius: 50%;' }}
    display: block;
}

.top-left {
    border-radius: 50%;
    height: ${dotSize}px;
    position: absolute;
    left: 6%;
    top: 6%;
    width: ${dotSize}px;
    background-color: #${dotDefaultColour};
	text-align: center;
    z-index: 20;
}

.top-right {
    border-radius: 50%;
    height: ${dotSize}px;
    position: absolute;
    right: 6%;
    top: 6%;
    width: ${dotSize}px;
    background-color: #${dotDefaultColour};
	text-align: center;
    z-index: 20;
}

.bottom-left {
    border-radius: 50%;
    height: ${dotSize}px;
    position: absolute;
    left: 6%;
    bottom: 6%;
    width: ${dotSize}px;
    background-color: #${dotDefaultColour};
	text-align: center;
    z-index: 20;
}

.bottom-right {
    border-radius: 50%;
    height: ${dotSize}px;
    position: absolute;
    right: 6%;
    bottom: 6%;
    width: ${dotSize}px;
    background-color: #${dotDefaultColour};
	text-align: center;
    z-index: 20;
}

.img-box img.user-avatar[width="${imageWidth}"]{
   left: 0px;
   top: 0px;
   z-index: 10;
}
</style>
</head>

<body>
<div class="img-box">
<a class="avatar-link" href="${avatarLink}" target="_blank">
<img width="${imageWidth}" height="${imageHeight}" alt="avatar" src="${avatarImageURL}" class="user-avatar">
"""
if (pswitch == 'on' || pstatus1 > 0) { htmlContent += """
<div class="top-right"><a href="${status1Link}" target="_blank">${if(pstatus1 == null || pstatus1 == 0) { "X" } else {pstatus1}}</a></div>
""" }
if (pstatus2 > 0) { htmlContent += """
<div class="top-left"><a href="${status2Link}" target="_blank">${pstatus2}</a></div>
""" }
if (pstatus3 > 0) { htmlContent += """
<div class="bottom-right"><a href="${status3Link}" target="_blank">${pstatus3}</a></div>
""" }
if (pstatus4 > 0) { htmlContent += """
<div class="bottom-left"><a href="${status4Link}" target="_blank">${pstatus4}</a></div>
""" }
htmlContent += """   </a>
</div>

</body>
</html>"""

    writeFile(htmlFileName, htmlContent)
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