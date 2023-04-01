/**
 *  Unified Thermostat Parent Driver
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
 *    2021-07-12  Simon Burke    1.0.0  - Alpha release
 *                               1.0.1  - No Change
 *    2021-07-17  Simon Burke    1.0.2  - Added Platform temperature scale preference and get/set methods
 *    2021-07-19  Simon Burke	 1.0.3  - Updated platform temperature scale to align case of F and C with HE scale
 *                               1.0.4  - No Change
 *                               1.0.5  - No Change
 *                               1.0.6  - No Change
 *    2021-08-15  Simon Burke    1.0.7  - Added heTempScale attribute and override command to override HE hub temp scale
 *    2022-11-26  Simon Burke    1.0.26 - Removed Platform Scale Preference setting
 *    2023-01-07  Alexander Laamanen 1.0.29 - MELCloud - Fixes to handle multiple AC Units in MELCloud setup
 *    2023-01-07  Simon Burke    1.0.30   Now use JsonOutput for larger HTTP response logging
                                          Automatically turn off Debug Logging after 30 minutes
 *    2023-01-09  Simon Burke    1.0.31   Detection of A/C Units configured under Floors and Areas in MELCloud
 *    2023-04-02  Simon Burke    1.0.32   Updated applyStatusUpdates in child driver to detect when no status data is available
 */

import groovy.json.JsonOutput;
import groovy.transform.Field

@Field static final Integer debugAutoDisableMinutes = 30

metadata {
	        definition (name:      "Unified Thermostat Parent Driver",
                        namespace: "simnet",
                        author:    "Simon Burke")
                 { 
                     capability "Refresh" //Adds the refresh command on the device page, allowing users to trigger the refresh() method
                     capability "Initialize" // Calls initialize when the device is created and when the hub restarts
                 }

	        preferences {
		
        // Platform and authentication Preferences
        def platformSelected = []
            platformSelected << ["MELView"   : "MEL View (Aus/NZ)" ]
            platformSelected << ["MELCloud"  : "MEL Cloud (Europe)"]
            platformSelected << ["KumoCloud" : "Kumo Cloud (US)"   ]
        
		input name: "Platform", type: "enum",     title: "Platform",        displayDuringSetup: true, required: true, multiple: false, options: platformSelected, defaultValue: "MELView"
        input name: "UserName", type: "string",   title:"Username / Email", displayDuringSetup: true, required: true, multiple: false
		input name: "Password", type: "password", title:"Password",         displayDuringSetup: true, required: true, multiple: false
		
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
        
        input(name: "Language", type: "enum", title:"Language", options: languageSelected, defaultValue: 0, description: "Select a language (Europe only)", displayDuringSetup: true)
        // Logging Preferences
        input(name: "DebugLogging", type: "bool", title:"Enable Debug Logging",                   displayDuringSetup: true, defaultValue: false)
        input(name: "WarnLogging",  type: "bool", title:"Enable Warning Logging",                 displayDuringSetup: true, defaultValue: true )
        input(name: "ErrorLogging", type: "bool", title:"Enable Error Logging",                   displayDuringSetup: true, defaultValue: true )
        input(name: "InfoLogging",  type: "bool", title:"Enable Description Text (Info) Logging", displayDuringSetup: true, defaultValue: false)
        
    } // End of Preferences

    attribute "heTempScale",                 "string"
    
    command "overrideHeTempScale", [[name:"givenHeTempScale", type: "STRING", description: "Enter the Temperature Scale override value (F, C or <Blank>)" ] ]
    
} // End of metadata

def overrideHeTempScale(givenHeTempScale) {
    
 sendEvent(name: "heTempScale", value : givenHeTempScale);   
}

def getHETempScale() {
    
    def vTempScale = ""
    if (checkNull(device.currentValue("heTempScale"), "") == "") { vTempScale = getTemperatureScale() }
    else { vTempScale = device.currentValue("heTempScale") }
                                  
    return vTempScale
}

def getPlatformScale() { return 'C' }

def initialize() {
    debugLog("initialize: Method called...");
    updated();
    debugLog("initialize: Initialize process completed");
}

// updated() - Run when the "Save Preferences" button is pressed on the device edit page
def updated() {
   debugLog("updated: Update process called")
   
   if (   "${UserName}"     != ""
        && "${Password}"     != ""
        && "${getBaseURL()}" != "")
      { refresh() }
   else { debugLog("updated: Refresh process was not called, check Preferences for UserName, Password, Platform and the Base URL State variable") }

   if (DebugLogging) {
     log.debug "updated: Debug logging will be automatically disabled in ${debugAutoDisableMinutes} minutes"
     runIn(debugAutoDisableMinutes*60, "debugOff")
   }
   else { unschedule("debugOff") }

   debugLog("updated: Update process complete")
}

def refresh() {
  
  debugLog("refresh: Refresh process called")
  
  // Authenticate with Platform, including
  //   Populating the Platform Base URL if not already setup
  //   Recording Authentication Code for use in future communications
  //   Creating Child Thermostat units, if details are provided during the authentication process
  setAuthCode()
          
  //Only need to call createChildACUnits method separately for MELView and MELCloud platforms
  //  KumoCloud receives these in the authentication reply
  if ("${getPlatform()}" != "KumoCloud") { createChildACUnits() }
  else { debugLog("refresh: createChildACUnits() method skipped, we are working with the Kumo Cloud platform") }
    
  debugLog("refresh: Refresh process complete")
}

def createChildACUnits(givenUnitsList) {
    
    def unitsList
    def childDevice
    
    // Retreive list of Child AC Units
    if (givenUnitsList == null) { unitsList = "retrieveChildACUnits_${getPlatform()}"() }
    else { unitsList = givenUnitsList }
    
    if (unitsList == null) {
        errorLog("createChildACUnits: Unit List was null")
    }
    else {
        //Loops through the list, checking whether we need to create any
        for (unit in unitsList) {
          debugLog("createChildACUnits: Unit List - ${unit.unitId} - ${unit.unitName}")
      
          childDevice = findChildDevice("${unit.unitId}", "AC")
          if (childDevice == null) {
              createChildDevice("${unit.unitId}", "${unit.unitName}", "AC")
              childDevice = findChildDevice("${unit.unitId}", "AC")
              childDevice.setUnitId("${unit.unitId}")
              //childDevice.initialize()
              runIn(30, "initializeChildDevices", [overwrite: true])
          }
      
        }
    }
    
}

def initializeChildDevices() {
    debugLog("initializeChildDevices: initializing all child devices...")
    for (unit in getChildDevices()) { unit.initialize() }
    debugLog("initializeChildDevices: initialization complete")
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
                                debugLog("retrieveChildACUnits_MELView: Initial data returned from rooms.aspx: ${JsonOutput.toJson(resp.data)}");
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
        log.error "retrieveChildACUnits_MELView: Unable to query ${getPlatform()}: ${e}"
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
            debugLog("retrieveChildACUnits_MELCloud: Initial data returned from ListDevices: ${JsonOutput.toJson(resp.data)}");
            resp?.data?.Structure?.Devices[0]?.each { unit -> // Each Device
                                      
                                      unitDetail = [unitId   : "${unit.DeviceID}",
                                                    unitName : "${unit.DeviceName}"
                                                   ]
                                      unitsList.add(unitDetail)
                
                                  } //End of each unit
                                  
            resp?.data?.Structure?.Floors?.each { floor -> // Each Floor
                                    floor.Devices[0]?.each { unit -> // Each Device on a Floor
                                      
                                      unitDetail = [unitId   : "${unit.DeviceID}",
                                                    unitName : "${unit.DeviceName}"
                                                   ]
                                      unitsList.add(unitDetail)
                
                                  } //End of each unit on a floor
            } // End of Each Floor
            resp?.data?.Structure?.Areas?.each { area -> // Each Area
                                    area.Devices[0]?.each { unit -> // Each Device in an Area
                                      
                                      unitDetail = [unitId   : "${unit.DeviceID}",
                                                    unitName : "${unit.DeviceName}"
                                                   ]
                                      unitsList.add(unitDetail)
                
                                  } //End of each unit in an Area
            } // End of Each Area
        } // End of response (resp)
    }  // End of Try 
	catch (Exception e) {
        log.error "retrieveChildACUnits_MelCloud: Unable to query ${getPlatform()}: ${e}"
	}
    return unitsList
}

def retrieveChildACUnits_KumoCloud() {
    //Units are captured differently for Kumo, so we won't return any here...
   return null   
}

//Authentication

def setAuthCode() {
    
    debugLog("setAuthCode: method called, Platform = ${getPlatform()}")
    
    // Check the Base URL has been populated correctly, attempting to populate it if it hasn't
    def baseURLCheck = getBaseURL()
    if (baseURLCheck == null) { errorLog("setAuthCode: Base URL is not set, check the platform Preference setting.  New authentication code was NOT set") }
    else {    
        
        def newAuthCode = "retrieveAuthCode_${getPlatform()}"()
        if (newAuthCode != "") {
              
           state.authCode = newAuthCode
           debugLog("setAuthCode: New authentication code value has been set")
           infoLog("A new authentication code value has been set")
        }
        else { errorLog("setAuthCode: New authentication code was NOT set") }
    
    }
    
}


def retrieveAuthCode_KumoCloud() {
    
    def vnewAuthCode = ""
    def unitsList    = []
    
   
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
              debugLog("retrieveAuthCode_KumoCloud: HTTP Response = ${JsonOutput.toJson(resp.data)}")
              vnewAuthCode = "${resp?.data[0].token}";
            
        
              debugLog("retrieveAuthCode_KumoCloud: New Auth Code - ${vnewAuthCode}");
              resp?.data[2].children.each { child ->
                  debugLog("retrieveAuthCode_KumoCloud: Child - ${JsonOutput.toJson(child)}")
                  child.zoneTable?.each { unit ->
                    unitsList.add(parseKumoUnit(unit))
                  
                  }
                  
                  child.children?.each { child2 ->
                    if (child2[0] != null) {
                        if (child2[0].containsKey("zoneTable")) {
                        child2[0].zoneTable?.each { unit ->
                            unitsList.add(parseKumoUnit(unit))
                            }
                        }
                    }
                  }
              }
          }
        createChildACUnits(unitsList)
    }
	catch (Exception e) {
        errorLog("retrieveAuthCode_KumoCloud: Unable to query Mitsubishi Electric ${getPlatform()}: ${e}")
	}
    return vnewAuthCode
}

def parseKumoUnit(unit) {
 
    def unitDetail  = [:]
    debugLog("parseKumoUnit: Unit (Serial / Label) - ${unit.value.serial} / ${unit.value.label}")
    unitDetail = [unitId   : "${unit.value.serial}",
                  unitName : "${unit.value.label}"
                     ]
    return unitDetail
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
        errorLog("retrieveAuthCode_MELView: Unable to query ${getPlatform()}: ${e}")
	}
    return vnewAuthCode

}

def retrieveAuthCode_MELCloud() {
    
    debugLog("retrieveAuthCode_MELCloud: method called")
    
    def vnewAuthCode = "";
    
    def bodyJson = "{ 'Email': '${UserName}', 'Password': '${Password}', 'Language': '${Language}', 'AppVersion': '1.18.5.1', 'Persist': 'True', 'CaptchaResponse': '' }"
    def postParams = [
        uri: "${getBaseURL()}/Mitsubishi.Wifi.Client/Login/ClientLogin",
        headers: getStandardHTTPHeaders_MELCloud("yes"),
        contentType: getStandardHTTPContentType_MELCloud(),
        body : bodyJson
	]
           
	try {
        
        httpPost(postParams)
        { resp -> 
            debugLog("retrieveAuthCode_MELCloud: ${JsonOutput.toJson(resp.data)}")
                       
            vnewAuthCode = "${resp?.data?.LoginData?.ContextKey?.value}";
            debugLog("retrieveAuthCode_MELCloud: New Auth Code - ${vnewAuthCode}");
            
        }
            
	}
	catch (Exception e) {
        errorLog("retrieveAuthCode_MELCloud: Unable to query ${getPlatform()}: ${e}")
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
 
    return Platform;
}

def getBaseURL() {
    
    //If the Base URL is not set, set it using the current Platform preference setting
    //  Null is passed in as we don't want to set an alternate URL in this case
    if (state.BaseURL == null) { setBaseURL(Platform, "null")  }
    
    //Hopefully the Base URL is now set, so return it
    return state.BaseURL
}

def setBaseURL(pPlatform, pAlternateURL) {

    debugLog("setBaseURL: Platform provided = ${pPlatform}, alternateURL = ${pAlternateURL}")
    //Define the default list of Base URL's for the different platforms supported
    def platformURLList = [  "MELView"   : "https://api.melview.net/api/"
                            ,"MELCloud"  : "https://app.melcloud.com"
                            ,"KumoCloud" : "https://geo-c.kumocloud.com"
                          ]

    //Set the BaseURL state variable
    //   If an alternate platform is passed in, the get method returns the value in the pAlternateURL
    //   parameter, instead of a value from the platformURLList
    //   e.g. passing in to this method a pPlatform of "melView" would result in the URL from the
    //   list above, "https://api.melview.net/api/", whereas passing in a platform of anything
    //   other than melView, melCloud or kumoCloud, will result in the pAlternateURL parameter
    //   being stored in the BaseURL state variable.
    state.BaseURL = platformURLList.get(pPlatform, pAlternateURL)
}

def createChildDevice(childDeviceId, childDeviceName, childDeviceType) {
    
    debugLog("createChildDevice: Creating Child Device: ${childDeviceId}, ${childDeviceName}, ${childDeviceType}")
    
	def childDevice = findChildDevice(childDeviceId, childDeviceType)
    
    if (childDevice == null) {
        childDevice = addChildDevice("simnet", "Unified Thermostat Unit Child Driver", deriveChildDNI(childDeviceId, childDeviceType), [label: "${device.displayName} - ${childDeviceName}"])
        infoLog("createChildDevice: New Unified Thermostat Unit Child Device created -  ${device.displayName} - ${childDeviceName}")
	}
    else {
      debugLog("createChildDevice: child device ${childDevice.deviceNetworkId} already exists")
	}
}

//API specific utility methods

// MELCloud

def getStandardHTTPHeaders_MELCloud(excludeAuthCode) {
    
    def headers = [:] 

    if (excludeAuthCode == null || excludeAuthCode == "no") {
        headers.put("X-MitsContextKey","${getAuthCode()}")
    }
    headers.put("Sec-Fetch-Site","same-origin")
    headers.put("Origin","${getBaseURL()}/")
    headers.put("Accept-Encoding","gzip, deflate, br")
    headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
    headers.put("Sec-Fetch-Mode","cors")
    headers.put("Accept", "application/json, text/javascript, */*; q=0.01")
    headers.put("Referer", "${getBaseURL()}/")
    headers.put("X-Requested-With","XMLHttpRequest")
    headers.put("Cookie","policyaccepted=true")
    headers.put("Content-Type", "application/json; charset=UTF-8")
        
    return headers
}

def getStandardHTTPContentType_MELCloud() {  return "application/json; charset=UTF-8" }

// MelView

def getStandardHTTPHeaders_MELView(excludeAuthCode) {

    def headers = [:]
    headers.put("Content-Type", "application/json")
    headers.put("Cookie", "auth=${getAuthCode()}")
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

def debugOff() {

   log.warn("Disabling debug logging");
   device.updateSetting("DebugLogging", [value:"false", type:"bool"])
}

// General Utility methods

def checkNull(value, alternative) {
 
    if(value == null) { return alternative }
    return value
    
}

