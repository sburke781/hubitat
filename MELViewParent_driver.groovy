/**
 *  Mitsubishi Electric MELCloud Parent Driver
 *
 *  Copyright 2020 Simon Burke
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
 *    2020-02-09  Simon Burke    Created Parent Driver
 *    2020-02-16  Simon Burke    Added Debug, Info and Error Logging settings and methods
 * 
 */
metadata {
	definition (name: "MELCloud Parent Driver", namespace: "simnet", author: "Simon Burke") {
        
        capability "Refresh"
        
        attribute "authCode", "string"
	}

	preferences {
		input(name: "BaseURL", type: "string", title:"MELCloud Base URL", description: "Enter the base URL for the Mitsubishi Electric Cloud Service (MELCloud)", defaultValue: "https://api.melview.net/api/", required: true, displayDuringSetup: true)
		input(name: "UserName", type: "string", title:"MELCloud Username / Email", description: "Username / Email used to authenticate on Mitsubishi Electric cloud", displayDuringSetup: true)
		input(name: "Password", type: "password", title:"MELCloud Account Password", description: "Password for authenticating on Mitsubishi Electric cloud", displayDuringSetup: true)
        input(name: "DebugLogging", type: "bool", title:"Enable Debug Logging", displayDuringSetup: true, defaultValue: false)
        input(name: "ErrorLogging", type: "bool", title:"Enable Error Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "InfoLogging", type: "bool", title:"Enable Description Text (Info) Logging", displayDuringSetup: true, defaultValue: false)
    }

}


def refresh() {
  debugLog("refresh: Refresh process called")
  // Authenticate with MEL Cloud Service and
  //   record Authentication Code for use in future communications  
  setAuthCode()
  
  createChildACUnits()
  
}


def initialize() {
    if ("${UserName}" != "" && "${Password}" != "" && "${BaseURL}" != "") { refresh() }
}

def createChildACUnits() {
    //retrieves current status information for the ac unit
    
    def vUnitId = ""
    def vRoom = ""
    
    def bodyJson = "{ }"
    def headers = [:] 

    headers.put("Content-Type", "application/json")
    headers.put("Cookie", "auth=${device.currentValue("authCode", true)}")
    headers.put("accept", "application/json, text/javascript, */*; q=0.01")
    def postParams = [
        uri: "${BaseURL}rooms.aspx",
        headers: headers,
        contentType: "application/json",
        body : bodyJson
	]
    debugLog("${bodyJson}, ${headers.Cookie}")       
	try {
        
        httpPost(postParams) { resp -> 
            debugLog("GetRooms: Initial data returned from rooms.aspx: ${resp.data}") 
            resp?.data?.each { building -> // Each Building
                                building?.units?.each // Each AC Unit / Room
                                  { acUnit -> 
                                      vRoom     = acUnit.room
                                      vUnitId   = acUnit.unitid
                                      
                                      def childDevice = findChildDevice("${vUnitId}", "AC")
                                      if (childDevide == null) {
                                          createChildDevice("${vUnitId}", "${vRoom}", "AC")
                                          childDevice = findChildDevice("${vUnitId}", "AC")
                                          childDevice.sendEvent(name: "unitId", value: "${vUnitId}")
                                      }
                                      childDevice.refresh()
                                      debugLog("GetRooms: Interpretted results - ${vRoom}(${vUnitId}) - Power: ${vPower}, Mode: ${vModeDesc}(${vMode}), Temp: ${vTemp}, Set Temp: ${vSetTemp}" ) 
                                       
                                  } 
                             }
                           }
    }   
	catch (Exception e) {
        log.error "GetRooms : Unable to query Mitsubishi Electric cloud: ${e}"
	}
}


//Authentication

def setAuthCode() {

    def bodyJson = "{ \"user\": \"${UserName}\", \"pass\": \"${Password}\", \"appversion\": \"4.3.1010\" }"
    def headers = [:] 

    headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
    
    def postParams = [
        uri: "${BaseURL}login.aspx",
        headers: headers,
        contentType: "application/json",
        body : bodyJson
	]
           
	try {
        
        httpPost(postParams)
        { resp -> 
            //Look through the cookies returned in the header of the response for the authorisation code
            // to use in subsequent calls to the API
            
            def cookies = []
            def newAuthCode = "";
            
            resp.getHeaders('Set-Cookie').each {
                debugLog("setAuthCode: Cookie - ${it.value}")
                def cookie = it.value.split(';')[0]
                if (cookie.startsWith('auth=')) { newAuthCode = cookie.split('=')[1] }
                
            }
            
            if (newAuthCode != "") {
                sendEvent(name: "authCode", value : newAuthCode)
                debugLog("setAuthCode: New authentication code value has been set")
            }
            else {debugLog("setAuthCode: New authentication code was NOT set")}
        }
            
	}
	catch (Exception e) {
        errorLog("setAuthCode: Unable to query Mitsubishi Electric cloud: ${e}")
	}

}

def deriveChildDNI(childDeviceId, childDeviceType) {

    return "${device.deviceNetworkId}-id${childDeviceId}-type${childDeviceType}"
}

def findChildDevice(childDeviceId, childDeviceType) {
	getChildDevices()?.find { it.deviceNetworkId == deriveChildDNI(childDeviceId, childDeviceType)}
}

def getBaseURL() { return BaseURL }

def createChildDevice(childDeviceId, childDeviceName, childDeviceType) {
    debugLog("createChildDevice: Creating Child Device: ${childDeviceId}, ${childDeviceName}, ${childDeviceType}")
    
	def childDevice = findChildDevice(childDeviceId, childDeviceType)
    
    if (childDevice == null) {
        childDevice = addChildDevice("simnet", "MELCloud AC Unit", deriveChildDNI(childDeviceId, childDeviceType), [label: "${device.displayName} - ${childDeviceName}"])
        infoLog("createChildDevice: New MEL Air Conditioning Child Device created -  ${device.displayName} - ${childDeviceName}")
	}
    else {
      debugLog("createChildDevice: child device ${childDevice.deviceNetworkId} already exists")
	}
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
