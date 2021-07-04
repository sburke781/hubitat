/**
 *  Mitsubishi Electric Thermostat Parent Driver
 *
 *  Copyright 2021 Simon Burke
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
 *    2021-06-05  Simon Burke    Created Parent Driver
 * 
 */
metadata {
	definition (name: "MEL Thermostat Parent Driver", namespace: "simnet", author: "Simon Burke") {
        
        capability "Refresh"
        
        
	}

	preferences {
		
        def platformSelected = []
            platformSelected << ["MELView" : "MEL View (Aus/NZ)"]
            platformSelected << ["MELCloud" : "MEL Cloud (Europe)"]
            platformSelected << ["KumoCloud" : "Kumo Cloud (US)"]
        
		input name: "MELPlatform", type: "enum", title: "MEL Platform", required: true, multiple: false, options: platformSelected, defaultValue: "melView", displayDuringSetup: true
        input name: "UserName", type: "string", title:"Username / Email", description: "Username / Email used to authenticate with MEL platform", displayDuringSetup: true
		input name: "Password", type: "password", title:"Password", displayDuringSetup: true
		
        def languageSelected = []
            languageSelected << ["2" : "Čeština (2)"]
            languageSelected << ["3" : "Dansk (3)"]
            languageSelected << ["4" : "Deutsch (4)"]
            languageSelected << ["22" : "Ελληνικά (22)"]
            languageSelected << ["5" : "Eesti (5)"]
            languageSelected << ["6" : "Español (6)"]
            languageSelected << ["0" : "English (0)"]
            languageSelected << ["7" : "Français (7)"]
            languageSelected << ["23" : "Hrvatski - Srpski (23)"]
            languageSelected << ["8" : "Հայերեն (8)"]
            languageSelected << ["19" : "Italiano (19)"]
            languageSelected << ["9" : "Latviešu (9)"]
            languageSelected << ["10" : "Lietuvių (10)"]
            languageSelected << ["11" : "Magyar (11)"]
            languageSelected << ["12" : "Nederlands (12)"]
            languageSelected << ["13" : "Norsk (13)"]
            languageSelected << ["14" : "Polski (14)"]
            languageSelected << ["15" : "Português (15)"]
            languageSelected << ["16" : "Русский (16)"]
            languageSelected << ["24" : "Română (24)"]
            languageSelected << ["26" : "Shqip (26)"]
            languageSelected << ["25" : "Slovenščina (25)"]
            languageSelected << ["17" : "Suomi (17)"]
            languageSelected << ["18" : "Svenska (18)"]
            languageSelected << ["21" : "Türkçe (21)"]
            languageSelected << ["1" : "Български (1)"]
            languageSelected << ["20" : "Українська (20)"]
        
        input name: "Language", type: "enum", title:"Language", options: languageSelected, defaultValue: 0, description: "Select a language", displayDuringSetup: true
        
        input(name: "DebugLogging", type: "bool", title:"Enable Debug Logging", displayDuringSetup: true, defaultValue: false)
        input(name: "WarnLogging", type: "bool", title:"Enable Warning Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "ErrorLogging", type: "bool", title:"Enable Error Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "InfoLogging", type: "bool", title:"Enable Description Text (Info) Logging", displayDuringSetup: true, defaultValue: false)
        
    }

    
    
}




def refresh() {
  
  debugLog("refresh: Refresh process called")
  
  // Check the Base URL has been populated correctly, attempting to populate it if it hasn't
  def baseURLCheck = getBaseURL()
  if (baseURLCheck == null) { errorLog("${vmethodName}: Base URL is not set, check the platform Preference setting") }  
  
  // Authenticate with MEL Platform and
  //   record Authentication Code for use in future communications  
  setAuthCode()
  
  //Only need to call createChildACUnits method separately for MELView and MELCloud platforms
  //  KumoCloud receives these in the authentication reply, so calls it from within
  //  the setAuthCode_KumoCloud method
  if ("${MELPlatform}" != "KumoCloud") { createChildACUnits() }
    else { debugLog("refresh: createChildACUnits() method skipped, we are working with the Kumo Cloud platform") }
    
}

def updated() {
 
    setBaseURL(MELPlatform,null)
    
}

def initialize() {
    if ("${UserName}" != "" && "${Password}" != "" && "${getBaseURL()}" != "") { refresh() }
}


def createChildACUnits(givenUnitsList) {
    
    def unitsList = []
    def childDevice
    
    // Retreive list of Child AC Units
    if (MELPlatform == "MELCloud" ) { unitsList = retrieveChildACUnits_MELCloud()  }
    if (MELPlatform == "MELView"  ) { unitsList = retrieveChildACUnits_MELView()   }
    if (MELPlatform == "KumoCloud") { unitsList = givenUnitsList }
    
    if (unitsList == null) {
        errorLog("createChildACUnits: Unit List was null")
    }
    else {
        //Loops through the list, checking whether we need to create any
        for (unit in unitsList) {
          debugLog("createChildACUnits: Unit List - ${unit.unitId} - ${unit.unitName}")
      
          childDevice = findChildDevice("${unit.unitId}", "AC")
          if (childDevide == null) {
              createChildDevice("${unit.unitId}", "${unit.unitName}", "AC")
              childDevice = findChildDevice("${unit.unitId}", "AC")
              childDevice.sendEvent(name: "unitId", value: "${unit.unitId}")
              childDevice.initialize()
          }
      
        }
    }
    
}




def retrieveChildACUnits_MELView() {
    //retrieves current status information for each AC unit
    
    def unitsList = []
    def unitDetail = [:]
    
    def postParams = [
        uri: "${getBaseURL()}rooms.aspx",
        headers: getStandardHTTPHeaders_MELView("no"),
        contentType: "application/json",
        body : "{ }"
	]
    
	try {
        
        httpPost(postParams) { resp -> 
                                debugLog("retrieveChildACUnits_MELView: Initial data returned from rooms.aspx: ${resp.data}") 
            
                                resp?.data?.each { building -> // Each Building
                                                    building?.units?.each // Each AC Unit / Room
                                                      { unit -> 

                                                        unitDetail = [unitId   : "${unit.unitid}",
                                                                      unitName : "${unit.room}"
                                                                   ]
                                                        unitsList.add(unitDetail)
                                                      } 
                                                 }
                             }
       }   
	catch (Exception e) {
        log.error "retrieveChildACUnits_MELView: Unable to query Mitsubishi Electric ${MELPlatform}: ${e}"
	 }
    return unitsList
}


def retrieveChildACUnits_MELCloud()
{
    //Retrieves current list of ac units
    
    def unitsList  = []
    def unitDetail = [:]
    
    def getParams = [
        uri: "${getBaseURL()}/Mitsubishi.Wifi.Client/User/ListDevices",
        headers: getStandardHTTPHeaders_MELCloud("no"),
        contentType: getStandardHTTPContentType_MELCloud(),
        body : "{ }"
	]
    
    debugLog("retrieveChildACUnits_MelCloud: Body = { }, Headers = ${headers}")
	try {
        
        httpGet(getParams) { resp -> 
                        
            resp?.data?.Structure?.Devices?.each { unit -> // Each Device
                                      
                                      unitDetail = [unitId   : "${unit.DeviceID}".replace("[","").replace("]",""),
                                                    unitName : "${unit.DeviceName}".replace("[","").replace("]","")
                                                   ]
                                      unitsList.add(unitDetail)
                
                                  } //End of each unit
                           } // End of response (resp)
    }  // End of Try 
	catch (Exception e) {
        log.error "retrieveChildACUnits_MelCloud: Unable to query Mitsubishi Electric ${MELPlatform}: ${e}"
	}
    return unitsList
}






//Authentication

def setAuthCode() {
    
    debugLog("setAuthCode: method called, MELPlatform = ${MELPlatform}")
    
    def newAuthCode = ""
    
    if ("${MELPlatform}" == "MELCloud" ) { newAuthCode = retrieveAuthCode_MELCloud() }
    if ("${MELPlatform}" == "MELView"  ) { newAuthCode = retrieveAuthCode_MELView()  }
    if ("${MELPlatform}" == "KumoCloud") { newAuthCode = retrieveAuthCode_KumoCloud()     }
    
    if (newAuthCode != "") {
              
       state.authCode = newAuthCode
       debugLog("setAuthCode: New authentication code value has been set")
       infoLog("A new authentication code value has been set")
    }
    else { errorLog("setAuthCode: New authentication code was NOT set") }
    
}


def retrieveAuthCode_KumoCloud() {
    
    def vnewAuthCode = ""
    def unitsList    = []
    def unitDetail  = [:]
   
    def vbodyJson = "{ \"username\": \"${UserName}\", \"password\": \"${Password}\", \"AppVersion\": \"2.2.0\" }"
        
    def postParams = [
        uri: "${getBaseURL()}/login",
        headers: getStandardHTTPHeaders_KumoCloud("yes"),
        contentType: "application/json; charset=UTF-8",
        body : vbodyJson
	]
           
	try {
        
        httpPost(postParams)
        { resp ->
            debugLog("${resp?.data}")
            vnewAuthCode = "${resp?.data[0].token}";
            
        
            debugLog("retrieveAuthCode_KumoCloud: New Auth Code - ${vnewAuthCode}");
            resp?.data[2].children.each { child ->
                debugLog("retrieveAuthCode_KumoCloud: Child - ${child}")
                child.zoneTable.each { unit ->
                  debugLog("retrieveAuthCode_KumoCloud: Unit (Serial / Label) - ${unit.value.serial} / ${unit.value.label}")
                  unitDetail = [unitId   : "${unit.value.serial}",
                                unitName : "${unit.value.label}"
                               ]
                  unitsList.add(unitDetail)
              
                }
            }
        }
        createChildACUnits(unitsList)
        
    }
	catch (Exception e) {
        errorLog("retrieveAuthCode_KumoCloud: Unable to query Mitsubishi Electric ${MELPlatform}: ${e}")
	}
    return vnewAuthCode
}




           
            
                

def retrieveAuthCode_MELView() {

    debugLog("retrieveAuthCode_MELView: method called")
    
    def vnewAuthCode = "";
    
    def bodyJson = "{ \"user\": \"${UserName}\", \"pass\": \"${Password}\", \"appversion\": \"4.3.1010\" }"
    def headers = [:] 

    headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
    
    def postParams = [
        uri: "${getBaseURL()}login.aspx",
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
            
            resp.getHeaders('Set-Cookie').each {
                debugLog("retrieveAuthCode_MELView: Cookie - ${it.value}")
                def cookie = it.value.split(';')[0]
                if (cookie.startsWith('auth=')) { vnewAuthCode = cookie.split('=')[1] }
                
            }
        }
        debugLog("retrieveAuthCode_MELView: New Auth Code - ${vnewAuthCode}");
            
	}
	catch (Exception e) {
        errorLog("retrieveAuthCode_MELView: Unable to query Mitsubishi Electric ${MELPlatform}: ${e}")
	}
    return vnewAuthCode

}

def retrieveAuthCode_MELCloud() {
    
    debugLog("retrieveAuthCode_MELCloud: method called")
    
    def vnewAuthCode = "";
    
    def bodyJson = "{ \"Email\": \"${UserName}\", \"Password\": \"${Password}\", \"Language\": \"13\", \"AppVersion\": \"1.18.5.1\", \"Persist\": \"True\", \"CaptchaResponse\": \"\" }"
    def postParams = [
        uri: "${getBaseURL()}/Mitsubishi.Wifi.Client/Login/ClientLogin",
        headers: getStandardHTTPHeaders_MELCloud("yes"),
        contentType: getStandardHTTPContentType_MELCloud(),
        body : bodyJson
	]
           
	try {
        
        httpPost(postParams)
        { resp -> 
            debugLog("retrieveAuthCode_MELCloud: ${resp.data}")
                       
            vnewAuthCode = "${resp?.data?.LoginData?.ContextKey?.value}";
            debugLog("retrieveAuthCode_MELCloud: New Auth Code - ${vnewAuthCode}");
            
        }
            
	}
	catch (Exception e) {
        errorLog("retrieveAuthCode_MELCloud: Unable to query Mitsubishi Electric ${MELPlatform}: ${e}")
	}
    return vnewAuthCode
}

def getAuthCode() { return state.authCode }

def deriveChildDNI(childDeviceId, childDeviceType) {

    return "${device.deviceNetworkId}-id${childDeviceId}-type${childDeviceType}"
}

def findChildDevice(childDeviceId, childDeviceType) {
	getChildDevices()?.find { it.deviceNetworkId == deriveChildDNI(childDeviceId, childDeviceType)}
}

def getPlatform() {
 
    return MELPlatform;
}

def getBaseURL() {
    
    //If the Base URL is not set, set it using the current MELPlatform preference setting
    //  Null is passed in as we don't want to set an alternate URL in this case
    if (state.BaseURL == null) { setBaseURL(MELPlatform, "null")  }
    
    //Hopefully the Base URL is now set, so return it
    return state.BaseURL
}

def setBaseURL(platform, alternateURL) {

    debugLog("setBaseURL: Platform provided = ${platform}, alternateURL = ${alternateURL}")
    //Define the default list of Base URL's for the different MEL platforms
    def platformURLList = [  "MELView"   : "https://api.melview.net/api/"
                            ,"MELCloud"  : "https://app.melcloud.com"
                            ,"KumoCloud" : "https://geo-c.kumocloud.com"
                          ]

    //Set the BaseURL state variable
    //   If an alternate platform is passed in, the get method returns the value in the alternateURL
    //   parameter, instead of a value from the platformURLList
    //   e.g. passing in to this method a platform of "melView" would result in the URL from the
    //   list above, "https://api.melview.net/api/", whereas passing in a platform of anything
    //   other than melView, melCloud or kumoCloud, will result in the alternateURL parameter
    //   being stored in the BaseURL state variable.
    state.BaseURL = platformURLList.get(platform, alternateURL)
}

def createChildDevice(childDeviceId, childDeviceName, childDeviceType) {
    
    debugLog("createChildDevice: Creating Child Device: ${childDeviceId}, ${childDeviceName}, ${childDeviceType}")
    
	def childDevice = findChildDevice(childDeviceId, childDeviceType)
    
    if (childDevice == null) {
        childDevice = addChildDevice("simnet", "MEL Thermostat Unit", deriveChildDNI(childDeviceId, childDeviceType), [label: "${device.displayName} - ${childDeviceName}"])
        infoLog("createChildDevice: New MEL Thermostat Child Device created -  ${device.displayName} - ${childDeviceName}")
	}
    else {
      debugLog("createChildDevice: child device ${childDevice.deviceNetworkId} already exists")
	}
}

//API specific utility methods

// MELCloud

def getStandardHTTPHeaders_MELCloud(excludeAuthCode) {
    
    def headers = [:] 

    headers.put("Content-Type", "application/json; charset=UTF-8")
    headers.put("Accept", "application/json, text/javascript, */*; q=0.01")
    headers.put("Referer", "${getBaseURL()}/")
    headers.put("Origin","${getBaseURL()}")
    headers.put("X-Requested-With","XMLHttpRequest")
    headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
    if (excludeAuthCode == null || excludeAuthCode == "no") {
        headers.put("X-MitsContextKey","${state.authCode}")
    }
    
    return headers
}

def getStandardHTTPContentType_MELCloud() {  return "application/json; charset=UTF-8" }

// MelView

def getStandardHTTPHeaders_MELView(excludeAuthCode) {

    def headers = [:]
    headers.put("Content-Type", "application/json")
    headers.put("Cookie", "auth=${state.authCode}")
    headers.put("accept", "application/json, text/javascript, */*; q=0.01")
    return headers
}

// KumoCloud

def getStandardHTTPHeaders_KumoCloud(excludeAuthCode) {
    
    def headers = [:] 

    headers.put("Accept-Encoding", "gzip, deflate, br")
    headers.put("Connection", "keep-alive")
    headers.put("Accept", "application/json, text/plain, */*") 
    headers.put("DNT", "1")
    headers.put("User-Agent", "")
    headers.put("Content-Type", "application/json;charset=UTF-8")
    headers.put("Origin", "https://app.kumocloud.com")
    headers.put("Sec-Fetch-Site", "same-site")
    headers.put("Sec-Fetch-Mode", "cors")
    headers.put("Sec-Fetch-Dest", "empty")
    headers.put("Referer", "https://app.kumocloud.com")
    headers.put("Accept-Language", "en-US,en;q=0.9")

    return headers
}

//Logging Utility methods
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
