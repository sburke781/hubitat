/**
 *  mySubaru Driver
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
 *    2022-05-21  Simon Burke    1.0.0 - Alpha release
 */
metadata {
	        definition (name:      "mySubaru Parent Driver",
                        namespace: "simnet",
                        author:    "Simon Burke")
                 { 
                     capability "Refresh" //Adds the refresh command on the device page, allowing users to trigger the refresh() method
                 }

	        preferences {
		
        // Platform and authentication Preferences
        def platformList = []
            platformList << ["USA"  : "United States of America" ]
            platformList << ["CAN"  : "Canada"]
        
		// Platform and authentication Preferences
        input name: "Platform", type: "enum",     title: "Platform (Country)",        displayDuringSetup: true, required: true, multiple: false, options: platformList, defaultValue: "USA"
        input name: "UserName", type: "string",   title:"Username / Email", displayDuringSetup: true, required: true, multiple: false
		input name: "Password", type: "password", title:"Password",         displayDuringSetup: true, required: true, multiple: false
        input name: "PIN", type: "password", title:"PIN",         displayDuringSetup: true, required: true, multiple: false
		
        
        // Logging Preferences
        input(name: "DebugLogging", type: "bool", title:"Enable Debug Logging",                   displayDuringSetup: true, defaultValue: false)
        input(name: "WarnLogging",  type: "bool", title:"Enable Warning Logging",                 displayDuringSetup: true, defaultValue: true )
        input(name: "ErrorLogging", type: "bool", title:"Enable Error Logging",                   displayDuringSetup: true, defaultValue: true )
        input(name: "InfoLogging",  type: "bool", title:"Enable Description Text (Info) Logging", displayDuringSetup: true, defaultValue: false)
        
    } // End of Preferences

    //attribute "heTempScale",                 "string"
    
    //command "overrideHeTempScale", [[name:"givenHeTempScale", type: "STRING", description: "Enter the Temperature Scale override value (F, C or <Blank>)" ] ]
    
} // End of metadata

def refresh() {
    
    login();
    
}

// Authentication

def login() {
  debugLog("login: login process called");

  def bodyJson = "{\"env\": \"cloudprod\", \"loginUsername\": \"${UserName}\", \"password\": \"${Password}\", \"deviceId\": null, \"passwordToken\": null, \"selectedVin\": null, \"pushToken\": null, \"deviceType\": \"android\"}";

  def postParams = [
        uri: "${getBaseURL()}/${getAPIVersion()}/login.json",
        headers: getStandardHTTPHeaders(),
        contentType: 'application/json',
        body : bodyJson
	]


    try {
        
        httpPost(postParams)
        { resp -> 
            debugLog("login: ${resp.data}")
            
        }
            
	}
	catch (Exception e) {
        errorLog("login: Unable to query platform ${getPlatform()}: ${e}")
	}
}



// Get Status Information

def locateCar() {

}

// Actions

def unlockDoors() {

}

def lockDoors() {


}

def startEngine() {


}


// Platform Utility Methods

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
    def platformURLList = [  "USA"   : "https://mobileapi.prod.subarucs.com"
                            ,"CAN"  : "https://mobileapi.ca.prod.subarucs.com"
                          ]

    //Set the BaseURL state variable
    //   If an alternate platform is passed in, the get method returns the value in the pAlternateURL
    //   parameter, instead of a value from the platformURLList
    //   e.g. passing in to this method a pPlatform of "CAN" would result in the URL from the
    //   list above, "https://mobileapi.ca.prod.subarucs.com", whereas passing in a platform of anything
    //   other than USA or CAN, will result in the pAlternateURL parameter
    //   being stored in the BaseURL state variable.
    state.BaseURL = platformURLList.get(pPlatform, pAlternateURL)
}

def getAPIVersion() {
    
    //If the API Version is not set, set it using the current Platform preference setting
    //  Null is passed in as we don't want to set an alternate URL in this case
    if (state.APIVersion == null) { setAPIVersion(Platform, "null")  }
    
    //Hopefully the API Version is now set, so return it
    return state.APIVersion
}

def setAPIVersion(pPlatform, pAlternateVersion) {

    debugLog("setAPIVersion: Platform provided = ${pPlatform}, alternateVersion = ${pAlternateVersion}")
    //Define the default list of API Versions for the different platforms supported
    def platformVersionList = [  "USA"  : "g2v21"
                                ,"CAN"  : "g2v21"
                              ]

    //Set the APIVersion state variable
    //  See comments in setBaseURL for using alternate values
    state.APIVersion = platformVersionList.get(pPlatform, pAlternateVersion)
}

def getRequestSource() {
    
    //If the Request Source is not set, set it using the current Platform preference setting
    //  Null is passed in as we don't want to set an alternate source in this case
    if (state.RequestSource == null) { setRequestSource(Platform, "null")  }
    
    //Hopefully the Request Source is now set, so return it
    return state.RequestSource
}

def setRequestSource(pPlatform, pAlternateSource) {

    debugLog("setRequestSource: Platform provided = ${pPlatform}, alternateSource = ${pAlternateSource}")
    //Define the default list of Request Sources for the different platforms supported
    def platformSourceList = [  "USA"  : "com.subaru.telematics.app.remote"
                               ,"CAN"  : "ca.subaru.telematics.remote"
                             ]

    //Set the RequestSource state variable
    //  See comments in setBaseURL for using alternate values
    state.RequestSource = platformSourceList.get(pPlatform, pAlternateSource)
}


def getStandardHTTPHeaders() {

    def headers = [:] 

    headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10; Android SDK built for x86 Build/QSR1.191030.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/74.0.3729.185 Mobile Safari/537.36")
    headers.put("Origin", "file://")
    headers.put("X-Requested-With", getRequestSource())
    headers.put("Accept-Language", "en-US,en;q=0.9")
    headers.put("Accept-Encoding", "gzip, deflate")
    headers.put("Accept", "*/*")

    return headers
}


// Device Utility Methods

def deriveChildDNI(childDeviceId, childDeviceType) {

    return "${device.deviceNetworkId}-id${childDeviceId}-type${childDeviceType}"
}

def findChildDevice(childDeviceId, childDeviceType) {
	getChildDevices()?.find { it.deviceNetworkId == deriveChildDNI(childDeviceId, childDeviceType)}
}

def createChildDevice(childDeviceId, childDeviceName, childDeviceType) {
    
    debugLog("createChildDevice: Creating Child Device: ${childDeviceId}, ${childDeviceName}, ${childDeviceType}")
    
	def childDevice = findChildDevice(childDeviceId, childDeviceType)
    
    if (childDevice == null) {
        childDevice = addChildDevice("simnet", "mySubaru Car Child Driver", deriveChildDNI(childDeviceId, childDeviceType), [label: "${device.displayName} - ${childDeviceName}"])
        infoLog("createChildDevice: New mySubaru Car Child Device created -  ${device.displayName} - ${childDeviceName}")
	}
    else {
      debugLog("createChildDevice: child device ${childDevice.deviceNetworkId} already exists")
	}
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

// General Utility methods

def checkNull(value, alternative) {
 
    if(value == null) { return alternative }
    return value
    
}