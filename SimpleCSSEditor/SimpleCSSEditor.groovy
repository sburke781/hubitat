/*
 * Simple CSS Editor Driver
 *
 * Used to present the Simple CSS Editor iFrame on HE Dashboards
 *
 */
metadata {
    definition(name: 'Simple CSS Editor', namespace: 'simnet', author: 'sburke781') {
	    capability 'Actuator'

		attribute 'iFrame', 'string'
        
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
    debugLog('installed: BoM Radar Images device installed')
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
 
    String uri = 'https://raw.githubusercontent.com/sburke781/hubitat/SimpleCSSEditor/SimpleCSSEditor/SimpleCSSEditor.html'

    def params = [
        uri: uri,
        contentType: 'text/plain; charset=UTF-8',
        textParser: true,
        headers: [:]
    ]

    try {
        httpGet(params) { resp ->
            if (resp != null) {
               String data = resp.data.text;
               writeFile('SimpleCSSEditor.html', data);
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
