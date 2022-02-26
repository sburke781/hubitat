/*
 * Simple CSS Editor Driver
 *
 * Used to present the Simple CSS Editor iFrame on HE Dashboards
 *
 * Relies heavily on the great work by @thebearmay for file manager access
 *  and @mbarone for displaying a modal iFrame
 */
metadata {
    definition(name: 'Simple CSS Editor', namespace: 'simnet', author: 'sburke781') {
	    capability 'Actuator'

		attribute 'iFrame', 'text'
        attribute "iFrameLauncher", "text"
        command 'refresh'
    }
}

preferences {
		
        input (name: 'DebugLogging', type: 'bool',   title:'Enable Debug Logging',                   defaultValue: false)
        input (name: 'WarnLogging',  type: 'bool',   title:'Enable Warning Logging',                 defaultValue: true )
        input (name: 'ErrorLogging', type: 'bool',   title:'Enable Error Logging',                   defaultValue: true )
        input (name: 'InfoLogging',  type: 'bool',   title:'Enable Description Text (Info) Logging', defaultValue: false)
}

// Standard device methods
void installed() {
    debugLog('installed: Simple CSS Editor device installed')
    downloadEditorHTML();
}

void updated() {
    debugLog('updated: update process called')
    downloadEditorHTML();
}

void refresh() {
    debugLog('refresh: Refresh process called')
    downloadEditorHTML();
}

void downloadEditorHTML() {
 
    String uri = 'https://raw.githubusercontent.com/sburke781/hubitat/SimpleCSSEditor/SimpleCSSEditor.html'

    xferFile(uri, 'SimpleCSSEditor.html');
    setIframe();

}


def setIframe() {
    
    String src = "http://${location.hub.localIP}/local/SimpleCSSEditor.html";
    
    def launcher = "<button style='height:100%;width:100%;' onclick='document.getElementById(`${device.displayName.replaceAll('\\s','')}`).style.display=`block`;'>Simple CSS Editor</button><div id=${device.displayName.replaceAll('\\s','')} class='modal' style='display:none;position:fixed;top:0;left:10%;width:800px;height:100%;background-color:rgba(120,120,120,.85); z-index:990 !important;'><button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}').style.display='none'; style='float:right;margin:5px;'> X </button><button onclick=document.getElementById('${device.displayName.replaceAll('\\s','')}-iframe').src='${src}'; style='float:right;margin:5px;'>Refresh</button><iframe id='${device.displayName.replaceAll('\\s','')}-iframe' src='${src}' style='height:100%;width:100%;border:none;'></iframe></div>";
    sendEvent(name: "iFrameLauncher", value: launcher)

}





Boolean writeFile(String fName, String fData) {
    now = new Date()
    String encodedString = "thebearmay$now".bytes.encodeBase64().toString();    

try {
		def params = [
			uri: 'http://127.0.0.1:8080',
			path: '/hub/fileManager/upload',
			query: [
				'folder': '/'
			],
			headers: [
				'Content-Type': "multipart/form-data; boundary=$encodedString"
			],
            body: """--${encodedString}
Content-Disposition: form-data; name="uploadFile"; filename="${fName}"
Content-Type: text/plain

${fData}

--${encodedString}
Content-Disposition: form-data; name="folder"


--${encodedString}--""",
			timeout: 300,
			ignoreSSLIssues: true
		]
		httpPost(params) { resp ->
		}
		return true
	}
	catch (e) {
		errorLog("Error writing file $fName: ${e}")
	}
	return false
}

Boolean xferFile(fileIn, fileOut) {
    fileBuffer = (String) readExtFile(fileIn)
    retStat = writeFile(fileOut, fileBuffer)
    return retStat
}

String readExtFile(fName){
    def params = [
        uri: fName,
        contentType: "text/html",
        textParser: true
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
               int i = 0
               String delim = ""
               i = resp.data.read() 
               while (i != -1){
                   char c = (char) i
                   delim+=c
                   i = resp.data.read() 
               } 
               return delim
            }
            else {
                errorLog("Null Response")
            }
        }
    } catch (exception) {
        errorLog("Read Ext Error: ${exception.message}")
        return null;
    }
}

//Logging methods
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
