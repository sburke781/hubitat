/**
 *  TV HeadEnd EPG Importer
 *
 *  Copyright 2022 Simon Burke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 *    2022-01-02  Simon Burke    Original Creation
 * 
 */
metadata {
	definition (name: "TVHE EPG Importer", namespace: "simnet", author: "Simon Burke") {
        capability "Refresh"
        attribute "commsError", "boolean"
        
        command "retrieveEPG"
	}

	preferences {
		input(name: "server", type: "string", title:"Server Name or IP", description: "Enter the server name or IP address for the host of TV HeadEnd", required: true, displayDuringSetup: true)
		input(name: "port", type: "number", title:"TV HeadEnd Port", description: "Port for TV HeadEnd (Default 9981)", defaulValue: 9981, required: true, displayDuringSetup: true)
		input(name: "fileName", type: "string", title:"EPG File Name", description: "Enter the name of the .json file to be stored to the HE hub", defaultValue: 'TVHE_EPG.json', required: true, displayDuringSetup: true)
        input(name: "AutoPolling", type: "bool", title:"Automatic Polling", description: "Enable / Disable automatic polling of EPG", defaultValue: true, required: true, displayDuringSetup: true)
        input(name: "PollingInterval", type: "string", title:"Polling Interval", description: "Number of minutes between automatic EPG updates", defaultValue: 5, required: true, displayDuringSetup: true)		
        
        input(name: "DebugLogging", type: "bool", title:"Enable Debug Logging", displayDuringSetup: true, defaultValue: false)
        input(name: "WarnLogging", type: "bool", title:"Enable Warning Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "ErrorLogging", type: "bool", title:"Enable Error Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "InfoLogging", type: "bool", title:"Enable Description Text (Info) Logging", displayDuringSetup: true, defaultValue: false)
    }

}

def updated() {

    debugLog("updated: AutoPolling = ${AutoPolling}, PollingInterval = ${PollingInterval}")
    updateAutoPolling()    
}

def getSchedule() { }

def updateAutoPolling() {

   def sched
   debugLog("updateAutoPolling: Update Automatic Polling called, about to unschedule refresh")
   unschedule("refresh")
   debugLog("updateAutoPolling: Unscheduleing refresh complete")
   
   if(AutoPolling == true) {
       
       sched = "0 0/${PollingInterval} * ? * * *"
       
       debugLog("updateAutoPolling: Setting up schedule with settings: schedule(\"${sched}\",refresh)")
       try{
           
           schedule("${sched}","refresh")
       }
       catch(Exception e) {
           errorLog("updateAutoPolling: Error - " + e)
       }
       
       infoLog("Automatic EPG Polling now enabled, running every ${PollingInterval} minutes")
   }
   else { infoLog("Automatic EPG Polling now disabled")  }
}

def refresh() {
 debugLog("refresh: running retrieveEPG()")
 retrieveEPG()
 debugLog("refresh: retrieveEPG() complete")
}

def retrieveEPG() {

    getParams = [
		uri: "http://${server}:${port}/api/epg/events/grid?mode=now",
        headers: [:],
        contentType: "application/json",
		body : ''
	]
           
	try {
        asynchttpGet('retrieveEPGCallback', getParams);
    }
    catch(Exception e)
    {
        errorLog("retrieveEPG: Exception ${e}")   
    }
    
}

void retrieveEPGCallback(resp, data) {
    // write to file
    writeFile(fileName, resp.getData())
}

// File Methods

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
def debugLog(debugMessage) {
	if (DebugLogging == true) {log.debug(debugMessage)}	
}

def errorLog(errorMessage) {
    if (ErrorLogging == true) { log.error(errorMessage)}  
}

def infoLog(infoMessage) {
    if(InfoLogging == true) {log.info(infoMessage)}    
}

def warnLog(warnMessage) {
    if(WarnLogging == true) {log.warn(warnMessage)}    
}