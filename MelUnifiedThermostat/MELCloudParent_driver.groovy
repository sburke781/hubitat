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
 *    2020-08-08  Simon Burke    Created Parent Driver
 * 
 */
metadata {
	definition (name: "MELCloud Parent Driver", namespace: "simnet", author: "Simon Burke") {
        
        capability "Refresh"
        
        attribute "authCode", "string"
	}

	preferences {
		input(name: "BaseURL", type: "string", title:"MELCloud Base URL", description: "Enter the base URL for the Mitsubishi Electric MELCloud Service", defaultValue: "https://app.melcloud.com", required: true, displayDuringSetup: true)
		input(name: "UserName", type: "string", title:"MELCloud Username / Email", description: "Username / Email used to authenticate on Mitsubishi Electric MELCloud", displayDuringSetup: true)
		input(name: "Password", type: "password", title:"MELCloud Account Password", description: "Password for authenticating on Mitsubishi Electric MELCloud", displayDuringSetup: true)
        input(name: "DebugLogging", type: "bool", title:"Enable Debug Logging", displayDuringSetup: true, defaultValue: false)
        input(name: "WarnLogging", type: "bool", title:"Enable Warning Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "ErrorLogging", type: "bool", title:"Enable Error Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "InfoLogging", type: "bool", title:"Enable Description Text (Info) Logging", displayDuringSetup: true, defaultValue: false)
    }

}


def refresh() {
  debugLog("refresh: Refresh process called")
  // Authenticate with MELCloud Service and
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

    headers.put("Content-Type", "application/json; charset=UTF-8")
    headers.put("Accept", "application/json, text/javascript, */*; q=0.01")
    headers.put("Referer", "${BaseURL}/")
    headers.put("X-Requested-With","XMLHttpRequest")
    headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
    headers.put("X-MitsContextKey","${state.authCode}")
    
    def getParams = [
        uri: "${BaseURL}/Mitsubishi.Wifi.Client/User/ListDevices",
        headers: headers,
        contentType: "application/json; charset=UTF-8",
        body : bodyJson
	]
    debugLog("createChildACUnits: Body = ${bodyJson}, Headers = ${headers}")       
	try {
        
        httpGet(getParams) { resp -> 
            //debugLog("createChildACUnits: Initial data returned from ListDevices: ${resp.data}") 
            
            resp?.data?.Structure?.Devices?.each { acUnit -> // Each Device
                                 
                                      vRoom     = "${acUnit.DeviceName}".replace("[","").replace("]","")
                                      vUnitId   = "${acUnit.DeviceID}".replace("[","").replace("]","")
                                      debugLog("createChildACUnits: ${vUnitId}, ${vRoom}")
                                      
                                      def childDevice = findChildDevice("${vUnitId}", "AC")
                                      if (childDevide == null) {
                                          createChildDevice("${vUnitId}", "${vRoom}", "AC")
                                          childDevice = findChildDevice("${vUnitId}", "AC")
                                          childDevice.sendEvent(name: "unitId", value: "${vUnitId}")
                                      }
                                      childDevice.refresh()
                                      
                                  } 
                             

                           }
    }   
	catch (Exception e) {
        log.error "createChildACUnits : Unable to query Mitsubishi Electric MELCloud: ${e}"
	}
}


//Authentication

def setAuthCode() {
      
    def bodyJson = "{ \"Email\": \"${UserName}\", \"Password\": \"${Password}\",  \"AppVersion\": \"1.18.5.1\", \"Persist\": \"True\", \"CaptchaResponse\": \"\" }"
    def headers = [:] 

    headers.put("Content-Type", "application/json; charset=UTF-8")
    headers.put("Accept", "application/json, text/javascript, */*; q=0.01")
    headers.put("Referer", "${BaseURL}/")
    headers.put("Origin","${BaseURL}")
    headers.put("X-Requested-With","XMLHttpRequest")
    headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
    headers.put("Sec-Fetch-Mode","cors")
            
    def postParams = [
        uri: "${BaseURL}/Mitsubishi.Wifi.Client/Login/ClientLogin",
        headers: headers,
        contentType: "application/json; charset=UTF-8",
        body : bodyJson
	]
           
	try {
        
        httpPost(postParams)
        { resp -> 
            debugLog("setAuthCode: ${resp.data}")
            def newAuthCode = "";
            
            debugLog("setAuthCode: ContextKey - ${resp?.data?.LoginData?.ContextKey?.value}");
            newAuthCode = "${resp?.data?.LoginData?.ContextKey?.value}";
            
            if (newAuthCode != "") {
                //sendEvent(name: "authCode", value : newAuthCode)
                state.authCode = newAuthCode
                debugLog("setAuthCode: New authentication code value has been set")
            }
            else {debugLog("setAuthCode: New authentication code was NOT set")}
        }
            
	}
	catch (Exception e) {
        errorLog("setAuthCode: Unable to query Mitsubishi Electric MELCloud: ${e}")
	}

}

def getAuthCode() { return state.authCode }

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

def warnLog(warnMessage) {
    if(WarnLogging == true) {log.warn(warnMessage)}    
}
