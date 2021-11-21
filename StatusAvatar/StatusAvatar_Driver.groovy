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
		input name: 'htmlFileName',     type: 'text',    title: 'HTML File Name (inc. file extension, e.g. avatar.html)',     required: true,  defaultValue: 'avatar.html'
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
    
        //input("src", "text", title: "iFrame Url",  required: true)
        //input("openText", "text", title: "Button text to Open iFrame", defaultValue:"Show",  required: false)
        input("closeText", "text", title: "Button text to close pop-up iFrame", defaultValue:"Close", required: false)
        input("popupWidth", "number", title: "Width of pop-up iFrames as a percentage (default: 100)", defaultValue:100, required: false)
		input("popupHeight", "number", title: "Height of pop-up iFrames as a percentage (default: 100)", defaultValue:100, required: false)
        
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
    String lastUpdate = null;
    String viFrame = '';
    //setPopUpIframe();
    writeHTML(pswitch, pstatus1, pstatus2, pstatus3, pstatus4)
    if (includeIFrame) {
        viFrame = "<div style='display: flex;justify-content: center;height:100%;margin:0;background-color:Transparent;'><iframe src='http://${location.hub.localIP}/local/${htmlFileName}' style='position:relative;top:0;left:0;height:100%;width:100%;border:none' scrolling=no version=${iFrameCounter()}></iframe><div>";
        // Pop-Up iFrame
        viFrame += "<div id=${device.displayName.replaceAll('\\s','')} class='modal' style='display:none;position:fixed;top:0;left:0;width:100%;height:100%;background-color:rgba(0,0,0,.85);z-index:998;'>"
        // Close Button
        viFrame += "<button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none';document.getElementById('${device.displayName.replaceAll('\\s','')}-iframe').src=''; style='float:right;margin:5px;font-size:16px;padding:5px;margin:5px;background-color:rgb(125,125,125)'>${closeText}</button>"
        // iFrame to be displayed
        viFrame += "<div style='display: flex;justify-content: center;'><iframe id='${device.displayName.replaceAll('\\s','')}-iframe' src='' style='height:${popupHeight}%;width:${popupWidth}%;border:none;z-index:999;'></iframe></div>"
        // Closing out the Div
        viFrame += '</div>'
        
        sendEvent(name: 'iFrame', value: viFrame)
        lastUpdate = new Date().format('YYYY-MM-dd HH:mm:ss')
        //device.sendEvent(name: 'lastupdate', value: "${lastUpdate}")
        
        debugLog('updateAvatar: IFrame attribute updated')
    }
    else { debugLog('updateAvatar: IFrame attribute turned off, no update is needed') }
}
//width: ${imageWidth + 16}px;
  //height: ${imageHeight + 16}px;
// HTML File Methods

void writeHTML(String pswitch, int pstatus1, int pstatus2, int pstatus3, int pstatus4) {
    String htmlContent = """<html>
<head>
<style>

html, body, .container {
    height: 100%;
}
.container {
    display: -webkit-flexbox;
    display: -ms-flexbox;
    display: -webkit-flex;
    display: flex;
    -webkit-flex-align: center;
    -ms-flex-align: center;
    -webkit-align-items: center;
    align-items: center;
    justify-content: center;
}

.btn{
    position: relative;
    width: 96px;
    height: 96px;
    top:0;
    left:0;
    ${if (imageRounding) { 'border-radius: 50%;' }}
    border: Transparent;
    background-color:Transparent;border:0;
}

.user-avatar{
    position: relative;
    width: 80px;
    height: 80px;
    top:0;
    left:0;
    ${if (imageRounding) { 'border-radius: 50%;' }}
    border: Transparent;
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
    z-index: 2;
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
    z-index: 2;
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
    z-index: 2;
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
    z-index: 2;
}

</style>
</head>

<body>
<div class="container">
<!-- <a class="avatar-link" href="#" target="_blank"> -->
<button type="button" alt="avatar" class="btn" onclick="window.parent.document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='block';window.parent.document.getElementById('${device.displayName.replaceAll('\\s','')}-iframe').src='${avatarLink}'">
<img class="user-avatar" src="${avatarImageURL}"/>

"""
    //${device.currentValue('avatarIFrameLauncher')}
if (pswitch == 'on' || pstatus1 > 0) { htmlContent += """
<div class="top-right"><a href="${status1Link}" target="_blank">${((pstatus1 == null || pstatus1 == 0) ? "" : pstatus1)}</a></div>
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
htmlContent += """</button>
<!--    </a>  -->
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