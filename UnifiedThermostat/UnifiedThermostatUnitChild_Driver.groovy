/**
 *  Unified Thermostat Unit Child Driver
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
 *    Date        Who            Version	What
 *    ----        ---            -------	----
 *    2021-07-12  Simon Burke    1.0.0		Alpha release
 *    2021-07-15  Simon Burke    1.0.1		Removed temperature conversion that was causing inflated temperatures when using Fahrenheit
 * 
 */
import java.text.DecimalFormat;

metadata {
	definition (name: "Unified Thermostat Unit Child Driver", namespace: "simnet", author: "Simon Burke") {
        capability "Refresh"
        capability "Initialize"
        capability "Thermostat"
        capability "FanControl"

preferences {

        input(name: "AutoStatusPolling", type: "bool", title:"Automatic Status Polling", description: "Enable / Disable automatic polling of unit status", defaultValue: true, required: true, displayDuringSetup: true)
        input(name: "StatusPollingInterval", type: "ENUM", multiple: false, options: ["1", "2", "5", "10", "30", "60"], title:"Status Polling Interval", description: "Number of minutes between automatic status updates", defaultValue: 10, required: true, displayDuringSetup: true)		
		input(name: "FansTextOrNumbers", type: "bool", title: "Fan Modes: Text or Numbers?", description: "Use Text for Fan Modes (ON) or Numbers (OFF)?", defaultValue: true, required: true, displayDuringSetup: true)
        
    }
        
        
        attribute "unitId",                 "string"
        //attribute "setTemperature",         "number"
        
        
        attribute "TemperatureIncrement",   "number"  // e.g. 0.5
        
        //Temperature Ranges
        attribute "MinTempCool",            "number"  // e.g. 16.0
        attribute "MaxTempCool",            "number"  // e.g. 31.0
        attribute "MinTempDry",             "number"  // e.g. 16.0
        attribute "MaxTempDry",             "number"  // e.g. 31.0
        attribute "MinTempHeat",            "number"  // e.g. 10.0
        attribute "MaxTempHeat",            "number"  // e.g. 31.0
        attribute "MinTempAuto",            "number"  // e.g. 16.0
        attribute "MaxTempAuto",            "number"  // e.g. 31.0
        
        //Modes and Features
        attribute "CanHeat",                "string"  // e.g. true / false
        attribute "CanDry",                 "string"  // e.g. true / false
        attribute "CanCool",                "string"  // e.g. true / false
        attribute "HasAutomaticFanSpeed",   "string"  // e.g. true / false
        attribute "NumberOfFanSpeeds",      "number"  // e.g. 5
        
        //Thermostat capability attributes - showing default values from HE documentation
        /*
        attribute "coolingSetpoint",                "NUMBER"
        attribute "heatingSetpoint",                "NUMBER"
        attribute "schedule",                       "JSON_OBJECT"

        attribute "supportedThermostatFanModes",    "ENUM", ['on', 'circulate', 'auto']

        attribute "supportedThermostatModes",       "ENUM", ['auto', 'off', 'heat', 'emergency heat', 'cool']
        attribute "temperature",                    "NUMBER"
        attribute "thermostatFanMode",              "ENUM", ['on', 'circulate', 'auto']
        attribute "thermostatMode",                 "ENUM", ['auto', 'off', 'heat', 'emergency heat', 'cool']
        attribute "thermostatOperatingState",       "ENUM", ['heating', 'pending cool', 'pending heat', 'vent economizer', 'idle', 'cooling', 'fan only']
        attribute "thermostatSetpoint",             "NUMBER"
        */
        
        //FanControl capability attributes - showing default values from HE documentation
        /*
        speed - ENUM ["low","medium-low","medium","medium-high","high","on","off","auto"]
        */
        
        attribute "lastRunningMode",                "STRING"
        
        //MELCloud specific commands:
        command "on"
        
        //Thermostat capability commands
        /*
        command "auto"
        command "cool"
        command "emergencyHeat" //Currently unsupported
        command "fanAuto"       //Currently unsupported
        command "fanCirculate"  //Currently unsupported
        command "fanOn"         //Currently unsupported
        command "heat"
        command "off"
        
        command "setCoolingSetpoint", [[name:"temperature*", type: "NUMBER", description: "Enter the Cooling Set Point" ] ]
                // temperature required (NUMBER) - Cooling setpoint in degrees
        command "setHeatingSetpoint", [[name:"temperature*", type: "NUMBER", description: "Enter the Heating Set Point" ] ]
                // temperature required (NUMBER) - Heating setpoint in degrees
        command "setSchedule", [[name:"JSON_OBJECT", type: "JSON_OBJECT", description: "Enter the JSON for the schedule" ] ]
            // JSON_OBJECT (JSON_OBJECT) - JSON_OBJECT
            //Currently unsupported in MELCloud
*/
        //Providing command with fan modes supported by MELCloud
        //command "setThermostatFanMode", [[name:"fanmode*", type: "ENUM", description: "Pick a Fan Mode", constraints: ["Low", "Mid", "High", "Auto"] ] ]
                // fanmode required (ENUM) - Fan mode to set
        
        //command "setThermostatMode", [[name:"thermostatmode*", type: "ENUM", description: "Pick a Thermostat Mode", constraints: ['Heat', 'Dry', 'Cool', 'Fan', 'Auto'] ] ]
                // thermostatmode required (ENUM) - Thermostat mode to set
        
        
        //FanControl capability commands
        /*
        setSpeed(fanspeed)
            fanspeed required (ENUM) - Fan speed to set

        */
	}

}

def getUnitId() {
    
    return state.unitId
}

def setUnitId(pUnitId) {
    
    state.unitId = pUnitId
}

// Standard Driver Methods

/* Initialize() To-Do
      1. Come up with updated logic for handling high set point to take into account
           Celsius and Fahrenheit

*/

def initialize() {
  
    parent.debugLog("initialize: Initialize process started for unit ${getUnitId()}...")
    //update unit settings and status polling
    updated()
    //Run refresh process, including status updates for power, mode, temperatures, etc
    refresh()
    parent.debugLog("initialize: Initialize process completed for unit ${getUnitId()}...")
}

def refresh() {
    parent.debugLog("refresh: Refresh process called")
  
    // Retrieve current state information and apply updates
    //   to HE device attributes
    applyStatusUpdates(retrieveStatusInfo())   
}

// Updated - Run when the Save Preferences button is pressed on the Device Edit page
//            and when device is initialized
//        To-Do: Turn on polling
def updated() {

    // Retrieve current unit settings and features, applying updates
    //   to HE device attributes
    parent.debugLog("updated: Applying Unit Settings and Features for unit ${getUnitId()}...")
    applyUnitSettings(retrieveUnitSettings())
    
    parent.debugLog("updated: AutoStatusPolling = ${AutoStatusPolling}, StatusPollingInterval = ${StatusPollingInterval}")
    updateStatusPolling()
    
}

// Mode and Fan Mode Maps and Conversions

/* getFanModeMap() To-Do: Review these values for Kumo and MelView */
def getFanModeMap() {
    
    if (FansTextOrNumbers == true) {
        [
            "0":"Auto",
            "1":"Low",
            "2":"Medium Low",
            "3":"Medium",
            "4":"Medium High",
            "5":"High"
        ]
    }
    else {
        [
            "0":"auto",
            "1":"1",
            "2":"2",
            "3":"3",
            "4":"4",
            "5":"5"
        ]
    }
}

/* convertFanModeToKey() To-Do: Confirm mode values across each platform */
def convertFanModeToKey(pFanMode) {
    
    def vModeKey = null
    if(pFanMode.trim().toLowerCase() == "auto"         || pFanMode.trim() == "auto")   { vModeKey = 0 }
    if(pFanMode.trim().toLowerCase() == "low"          || pFanMode.trim() == "1"   )   { vModeKey = 1 }
    if(pFanMode.trim().toLowerCase() == "medium low"   || pFanMode.trim() == "2"   )   { vModeKey = 2 }
    if(pFanMode.trim().toLowerCase() == "medium"       || pFanMode.trim() == "3"   )   { vModeKey = 3 }
    if(pFanMode.trim().toLowerCase() == "medium high"  || pFanMode.trim() == "4"   )   { vModeKey = 4 }
    if(pFanMode.trim().toLowerCase() == "high"         || pFanMode.trim() == "5"   )   { vModeKey = 5 }
    
    return vModeKey
}

def adjustFanModes(pNumberOfFanSpeeds, pHasAutomaticFanSpeed) {
    def fanModes = []
    
    fanModes.add('Off')
    
    //Text or Numbers?
    if (FansTextOrNumbers == true || FansTextOrNumbers == "1") {
        parent.debugLog("adjustFanModes:Text-based Fan Modes")
        if(pNumberOfFanSpeeds.toInteger() == 3) {
            fanModes.add('Low')
            fanModes.add('Medium')
            fanModes.add('High')
        }
        else if(pNumberOfFanSpeeds.toInteger() == 2) {
            fanModes.add('Low')
            fanModes.add('High')
        }
        else
        {
        //if(pNumberOfFanSpeeds.toInteger() == 5) {
            fanModes.add('Low')
            fanModes.add('Medium Low')
            fanModes.add('Medium')
            fanModes.add('Medium High')
            fanModes.add('High')
        }

    }
    else {
        parent.debugLog("adjustFanModes:Number-based Fan Modes")
        if(pNumberOfFanSpeeds.toInteger() == 3) {
            fanModes.add('1')
            fanModes.add('2')
            fanModes.add('3')
        }
        else if(pNumberOfFanSpeeds.toInteger() == 2) {
            fanModes.add('1')
            fanModes.add('2')
        }
        else
        {
        //if(pNumberOfFanSpeeds.toInteger() == 5) {
            fanModes.add('1')
            fanModes.add('2')
            fanModes.add('3')
            fanModes.add('4')
            fanModes.add('5')
        }
    }
    
    if(pHasAutomaticFanSpeed == "true" || pHasAutomaticFanSpeed == "1") {
        fanModes.add('Auto')
    }
    
    fanModes.add('On')
    
    parent.debugLog("adjustFanModes: fanModes detected are ${fanModes}")
    //Apply settings
    sendEvent(name: 'supportedThermostatFanModes', value: fanModes)
}

/* getModeMap() To-Do: Review these values for Kumo and MelView */
def getModeMap() {
    [
        "1"  : "heat",
        "2"  : "dry",
        "3"  : "cool",
        "7"  : "fan",
        "8"  : "auto",
        "16" : "off",
        "33" : "auto",
        "35" : "auto"
    ]
}

def getOperatingStateMap() {
    [
        "1"  : "heating",
        "2"  : "drying",
        "3"  : "cooling",
        "7"  : "fan only",
        "8"  : "auto",
        "16" : "idle",
        "33" : "heating",
        "35" : "cooling"
    ]
}

def adjustThermostatModes(pCanHeat,pCanCool,pCanDry, pCanAuto) {
    
    parent.debugLog("adjustThermostatModes: Adjusting Thermostat Modes...")
    def thermostatModes = []
    parent.debugLog("adjustThermostatModes: CanHeat = ${pCanHeat}, CanCool = ${pCanCool}, CanDry = ${pCanDry}, CanAuto = ${pCanAuto}")
    
    if(pCanHeat == "true" || pCanHeat == "1") { thermostatModes.add('heat') }
    if(pCanCool == "true" || pCanCool == "1") { thermostatModes.add('cool') }
    if(pCanDry  == "true" || pCanDry  == "1") { thermostatModes.add('dry')  }
    if(pCanAuto == "true" || pCanAuto == "1") { thermostatModes.add('auto') }
    
    thermostatModes.add('fan')
    thermostatModes.add('off')
    
    parent.debugLog("adjustThermostatModes: thermostatModes detected are ${thermostatModes}")
    sendEvent(name: 'supportedThermostatModes', value: thermostatModes)
           
}

/* convertThermostatModeToKey() To-Do: Confirm mode values across each platform */
def convertThermostatModeToKey(pThermostatMode) {
    
    def vModeKey = null
    if(pThermostatMode.trim() == "heat")   vModeKey = 1
    if(pThermostatMode.trim() == "dry")    vModeKey = 2
    if(pThermostatMode.trim() == "cool")   vModeKey = 3
    if(pThermostatMode.trim() == "fan")    vModeKey = 7
    if(pThermostatMode.trim() == "auto")   vModeKey = 8
    
    return vModeKey
}

// Unit Settings and Status

def retrieveUnitSettings() {
    
    //Returns current features and settings information for the ac unit
    def settings = [:]
    def platform = parent.getPlatform()
    parent.debugLog("retrieveUnitSettings: Retrieving unit features and setting from ${platform} ")
    if (platform == "MELCloud")  { settings = retrieveUnitSettings_MELCloud() }
    if (platform == "MELView")   { settings = retrieveUnitSettings_MELView() }
    if (platform == "KumoCloud") { settings = retrieveUnitSettings_KumoCloud() }
    
    return settings
    
}

def retrieveUnitSettings_MELView() {
    
    def settings = [:]
    
    def postParams = [
        uri: "${parent.getBaseURL()}unitcapabilities.aspx",
        headers: parent.getStandardHTTPHeaders_MELView("no"),
        contentType: "application/json",
        body : "{'unitid': '${getUnitId()}'}"
	]
    
	try {
        
        httpPost(postParams) { resp ->
            parent.debugLog("retrieveUnitSettings_MELView: unit capabilities response - ${resp.data}")     
            
            //Current Temperature Settings
            settings.minTempCool = "${resp.data.max."3".min}"
            settings.maxTempCool = "${resp.data.max."3".max}"
            settings.minTempDry  = "${resp.data.max."3".min}"
            settings.maxTempDry  = "${resp.data.max."3".max}"
            settings.minTempHeat = "${resp.data.max."1".min}"
            settings.maxTempHeat = "${resp.data.max."1".max}"
            settings.minTempAuto = "${resp.data.max."8".min}"
            settings.maxTempAuto = "${resp.data.max."8".max}"
            
            
            //Current Feature Settings
            if ("${resp.data.hascoolonly}" == "1") {settings.canHeat = "0"} else {settings.canHeat = "1"}
            
            settings.canDry               = "${resp.data.hasdrymode}"
            settings.canCool              = "1"
            settings.canAuto              = "${resp.data.hasautomode}"
            settings.hasAutomaticFanSpeed = "${resp.data.hasautofan}"
            settings.numberOfFanSpeeds    = "${resp.data.fanstage}"
            
            parent.debugLog("retrieveUnitSettings_MELView: Features and Settings - ${settings}")
            
            
        }
    }
    catch (Exception e) {
        parent.errorLog("retrieveUnitSettings_MELView: Unable to query Mitsubishi Electric MELView: ${e}")
	}
    
    return settings
}

def retrieveUnitSettings_MELCloud() {
    
    parent.debugLog("retrieveUnitSettings_MELCloud: Retrieval process started...")
    
    def settings = [:]
    def getParams = [
        uri: "${parent.getBaseURL()}/Mitsubishi.Wifi.Client/User/ListDevices",
        headers: parent.getStandardHTTPHeaders_MELCloud("no"),
        contentType: "application/json; charset=UTF-8",
        body : "{ }"
	]
    
	try {
        
        httpGet(getParams) { resp ->
            
            parent.debugLog("retrieveUnitSettings_MELCloud: Initial data returned from ListDevices: ${resp.data}")
                
                //statusInfo.unitId   = "${resp?.data?.Structure?.Devices?.Device.DeviceID}".replace("[","").replace("]","")
                
                //Temperature Ranges Configured
                settings.minTempCool  = "${resp?.data?.Structure?.Devices?.Device.MinTempCoolDry}".replace("[","").replace("]","")
                settings.maxTempCool  = "${resp?.data?.Structure?.Devices?.Device.MaxTempCoolDry}".replace("[","").replace("]","")
                settings.minTempDry   = "${resp?.data?.Structure?.Devices?.Device.MinTempCoolDry}".replace("[","").replace("]","")
                settings.maxTempDry   = "${resp?.data?.Structure?.Devices?.Device.MaxTempCoolDry}".replace("[","").replace("]","")
                settings.minTempHeat  = "${resp?.data?.Structure?.Devices?.Device.MinTempHeat}".replace("[","").replace("]","")
                settings.maxTempHeat  = "${resp?.data?.Structure?.Devices?.Device.MaxTempHeat}".replace("[","").replace("]","")
                settings.minTempAuto  = "${resp?.data?.Structure?.Devices?.Device.MinTempAutomatic}".replace("[","").replace("]","")
                settings.maxTempAuto  = "${resp?.data?.Structure?.Devices?.Device.MaxTempAutomatic}".replace("[","").replace("]","")
                
                //Modes and Features
                settings.canHeat              = "${resp?.data?.Structure?.Devices?.Device.CanHeat}".replace("[","").replace("]","")
                settings.canDry               = "${resp?.data?.Structure?.Devices?.Device.CanDry}".replace("[","").replace("]","")
                settings.canCool              = "${resp?.data?.Structure?.Devices?.Device.CanCool}".replace("[","").replace("]","")
                settings.canAuto              = "${resp?.data?.Structure?.Devices?.Device.ModelSupportsAuto}".replace("[","").replace("]","")
                settings.hasAutomaticFanSpeed = "${resp?.data?.Structure?.Devices?.Device.HasAutomaticFanSpeed}".replace("[","").replace("]","")
                settings.numberOfFanSpeeds    = "${resp?.data?.Structure?.Devices?.Device.NumberOfFanSpeeds}".replace("[","").replace("]","").toInteger()

        }
    }   
	catch (Exception e) {
        parent.errorLog "retrieveUnitSettings_MELCloud : Unable to query Mitsubishi Electric MELCloud: ${e}"
	}
    
    return settings
}

def retrieveUnitSettings_KumoCloud() {
    
    parent.debugLog("retrieveUnitSettings_KumoCloud: Retrieval process started...")
    
    def settings = [:]
    def postParams = [
        uri: "${parent.getBaseURL()}/getInfrequentDeviceUpdates",
        headers: parent.getStandardHTTPHeaders_KumoCloud("no"),
        contentType: "application/json; charset=UTF-8",
        body : "[ \"${parent.getAuthCode()}\",[\"${getUnitId()}\"] ]"
	]
    
    try {
        
        httpPost(postParams) { resp ->
            
            parent.debugLog("retrieveUnitSettings_KumoCloud: Initial data returned: ${resp.data}")
            //Temperature Ranges Configured
            settings.minTempCool  = "${resp.data[2].adapter_status[0].min_setpoint}"
            settings.maxTempCool  = "${resp.data[2].adapter_status[0].max_setpoint}"
            settings.minTempDry   = "${resp.data[2].adapter_status[0].min_setpoint}"
            settings.maxTempDry   = "${resp.data[2].adapter_status[0].max_setpoint}"
            settings.minTempHeat  = "${resp.data[2].adapter_status[0].min_setpoint}"
            settings.maxTempHeat  = "${resp.data[2].adapter_status[0].max_setpoint}"
            settings.minTempAuto  = "${resp.data[2].adapter_status[0].min_setpoint}"
            settings.maxTempAuto  = "${resp.data[2].adapter_status[0].max_setpoint}"

            //Modes and Features
            settings.canHeat              = "${resp.data[2].adapter_status[0].mode_heat}"
            settings.canDry               = "${resp.data[2].adapter_status[0].mode_dry}"
            settings.canCool              = "true"
            
            if ("${resp.data[2].adapter_status[0].auto_mode_disable}" == "false") { settings.canAuto = "true" } else { settings.canAuto = "false" }
            if ("${resp.data[2].adapter_status[0].auto_mode_disable}" == "false") { settings.hasAutomaticFanSpeed = "true" } else { settings.hasAutomaticFanSpeed = "false" }
            
            settings.numberOfFanSpeeds    = "5"

        }
    }   
	catch (Exception e) {
        parent.errorLog "retrieveUnitSettings_KumoCloud : Unable to query Mitsubishi Electric KumoCloud: ${e}"
	}
    
    return settings
}

/* applyUnitSettings() TO-DO - Work on handling Fahrenheit / Celsius */
def applyUnitSettings(givenSettings) {
    
    parent.debugLog("applyUnitSettings: Unit Settings are ${givenSettings}")
    
    def minTempCoolValue  = givenSettings.minTempCool?.toFloat().round(1)
	def maxTempCoolValue  = givenSettings.maxTempCool?.toFloat().round(1)
    def minTempDryValue   = givenSettings.minTempDry?.toFloat().round(1)
	def maxTempDryValue   = givenSettings.maxTempDry?.toFloat().round(1)
    def minTempHeatValue  = givenSettings.minTempHeat?.toFloat().round(1)
    def maxTempHeatValue  = givenSettings.maxTempHeat?.toFloat().round(1)
    def minTempAutoValue  = givenSettings.minTempAuto?.toFloat().round(1)
    def maxTempAutoValue  = givenSettings.maxTempAuto?.toFloat().round(1)
    
    //Temperature Ranges Configured
    sendEvent(name: "MinTempCool", value: minTempCoolValue)
    sendEvent(name: "MaxTempCool", value: maxTempCoolValue)
    sendEvent(name: "MinTempDry" , value: minTempDryValue )
    sendEvent(name: "MaxTempDry" , value: maxTempDryValue )
    sendEvent(name: "MinTempHeat", value: minTempHeatValue)
    sendEvent(name: "MaxTempHeat", value: maxTempHeatValue)
    sendEvent(name: "MinTempAuto", value: minTempAutoValue)
    sendEvent(name: "MaxTempAuto", value: maxTempAutoValue)

    //Modes and Features
    sendEvent(name: "CanHeat",              value: givenSettings.canHeat)
    sendEvent(name: "CanCool",              value: givenSettings.canCool)
    sendEvent(name: "CanDry",               value: givenSettings.canDry)
    sendEvent(name: "CanAuto",              value: givenSettings.canAuto)
    sendEvent(name: "HasAutomaticFanSpeed", value: givenSettings.hasAutomaticFanSpeed)
    sendEvent(name: "NumberOfFanSpeeds",    value: givenSettings.numberOfFanSpeeds)
    
    adjustFanModes(givenSettings.numberOfFanSpeeds, givenSettings.hasAutomaticFanSpeed)
    adjustThermostatModes(givenSettings.canHeat, givenSettings.canCool, givenSettings.canDry, givenSettings.canAuto)
}

def retrieveStatusInfo() {
    
    //Returns current status information for the ac unit
    parent.debugLog("retrieveStatusInfo: Retrieving status info from ${parent.getPlatform()} ")
    return "retrieveStatusInfo_${parent.getPlatform()}"()
    
}

def retrieveStatusInfo_MELView() { 
    
    def statusInfo = [:]
    def postParams = [
        uri: "${parent.getBaseURL()}unitCommand.aspx",
        headers: parent.getStandardHTTPHeaders_MELView("no"),
        contentType: "application/json",
        body : "{'unitid': '${getUnitId()}'}"
	]
    parent.debugLog("retrieveStatusInfo_MELView: Post Params = ${postParams}")
	try {
        
        httpPost(postParams) { acUnit ->
                                 parent.debugLog("retrieveStatusInfo_MELView: unit command response - ${acUnit.data}")     
                                 statusInfo.unitId   = acUnit.data.id
                
                                 //Current Status Information
                                 statusInfo.power    = "${acUnit.data.power}"
                                 statusInfo.setMode  = "${acUnit.data.setmode}"
                                 statusInfo.roomTemp = "${acUnit.data.roomtemp}"
                                 statusInfo.setTemp  = "${acUnit.data.settemp}"
                                 statusInfo.setFan   = "${acUnit.data.setfan}"
                                 
                            }
        parent.debugLog("retrieveStatusInfo_MELView: Status Info - ${statusInfo}")
    }   
	catch (Exception e) {
        parent.errorLog("retrieveStatusInfo_MELView: Unable to query Mitsubishi Electric MELView: ${e}")
	}

    return statusInfo
    

}

def retrieveStatusInfo_KumoCloud() { 
    
    def statusInfo = [:]
    def postParams = [
        uri: "${parent.getBaseURL()}/getDeviceUpdates",
        headers: parent.getStandardHTTPHeaders_KumoCloud("no"),
        contentType: "application/json",
        body : "[ \"${parent.getAuthCode()}\",[\"${getUnitId()}\"] ]"
	]
    
	try {
        
        httpPost(postParams) { acUnit ->
                                 parent.debugLog("retrieveStatusInfo_KumoCloud: response - ${acUnit.data}")     
                                 statusInfo.unitId   = "${acUnit.data[2][0].device_serial}".replace("[","").replace("]","")
                
                                 //Current Status Information
                                 statusInfo.power    = "${acUnit.data[2][0].power}".replace("[","").replace("]","")
                                 statusInfo.setMode  = "${acUnit.data[2][0].operation_mode}".replace("[","").replace("]","")
                                 statusInfo.roomTemp = "${acUnit.data[2][0].room_temp}".replace("[","").replace("]","")
                                 
                                 if(statusInfo.setMode == "3" || statusInfo.setMode == "35") { statusInfo.setTemp  = "${acUnit.data[2][0].sp_cool}".replace("[","").replace("]","") }
                                 if(statusInfo.setMode == "1" || statusInfo.setMode == "33") { statusInfo.setTemp  = "${acUnit.data[2][0].sp_heat}".replace("[","").replace("]","") }
                                 //statusInfo.setTemp  = "${acUnit.data[2][0].set_temp}".replace("[","").replace("]","")
                                 
                                 statusInfo.setFan   = "${acUnit.data[2][0].fan_speed}".replace("[","").replace("]","")
                                 
                            }
        parent.debugLog("retrieveStatusInfo_KumoCloud: Status Info - ${statusInfo}")
    }   
	catch (Exception e) {
        parent.errorLog("retrieveStatusInfo_KumoCloud: Unable to query Mitsubishi Electric Kumo Cloud: ${e}")
	}

    return statusInfo
}

def retrieveStatusInfo_MELCloud() {
    
    def statusInfo = [:]
    
    def getParams = [
        uri: "${parent.getBaseURL()}/Mitsubishi.Wifi.Client/User/ListDevices",
        headers: parent.getStandardHTTPHeaders_MELCloud("no"),
        contentType: "application/json; charset=UTF-8",
        body : "{ }"
	]
    
	try {
        
        httpGet(getParams) { resp ->
            
            parent.debugLog("retrieveStatusInfo_MELCloud: Initial data returned from ListDevices: ${resp.data}")
            if ("${resp?.data?.Structure?.Devices?.Device.HasPendingCommand}".replace("[","").replace("]","") != null
               && "${resp?.data?.Structure?.Devices?.Device.HasPendingCommand}".replace("[","").replace("]","") != "true") {
                
                statusInfo.unitId   = "${resp?.data?.Structure?.Devices?.Device.DeviceID}".replace("[","").replace("]","")
                
                //Current Status Information
                statusInfo.power    = "${resp?.data?.Structure?.Devices?.Device.Power}".replace("[","").replace("]","")
                statusInfo.setMode  = "${resp?.data?.Structure?.Devices?.Device.OperationMode}".replace("[","").replace("]","").toInteger()
                statusInfo.roomTemp = "${resp?.data?.Structure?.Devices?.Device.RoomTemperature}".replace("[","").replace("]","")
                statusInfo.setTemp  = "${resp?.data?.Structure?.Devices?.Device.SetTemperature}".replace("[","").replace("]","")
                statusInfo.setFan   = "${resp?.data?.Structure?.Devices?.Device.FanSpeed}".replace("[","").replace("]","").toInteger()

                //Temperature Ranges Configured
                statusInfo.minTempCoolDry   = "${resp?.data?.Structure?.Devices?.Device.MinTempCoolDry}".replace("[","").replace("]","")
                statusInfo.maxTempCoolDry   = "${resp?.data?.Structure?.Devices?.Device.MaxTempCoolDry}".replace("[","").replace("]","")
                statusInfo.minTempHeat      = "${resp?.data?.Structure?.Devices?.Device.MinTempHeat}".replace("[","").replace("]","")
                statusInfo.maxTempHeat      = "${resp?.data?.Structure?.Devices?.Device.MaxTempHeat}".replace("[","").replace("]","")
                statusInfo.minTempAutomatic = "${resp?.data?.Structure?.Devices?.Device.MinTempAutomatic}".replace("[","").replace("]","")
                statusInfo.maxTempAutomatic = "${resp?.data?.Structure?.Devices?.Device.MaxTempAutomatic}".replace("[","").replace("]","")
                
                //Modes and Features
                statusInfo.canHeat              = "${resp?.data?.Structure?.Devices?.Device.CanHeat}".replace("[","").replace("]","")
                statusInfo.canDry               = "${resp?.data?.Structure?.Devices?.Device.CanDry}".replace("[","").replace("]","")
                statusInfo.canCool              = "${resp?.data?.Structure?.Devices?.Device.CanCool}".replace("[","").replace("]","")
                statusInfo.hasAutomaticFanSpeed = "${resp?.data?.Structure?.Devices?.Device.HasAutomaticFanSpeed}".replace("[","").replace("]","")
                statusInfo.numberOfFanSpeeds    = "${resp?.data?.Structure?.Devices?.Device.NumberOfFanSpeeds}".replace("[","").replace("]","").toInteger()
            
            }
            else {
                parent.debugLog("retrieveStatusInfo_MELCloud: There are pending commands, status will be updated when there are no pending commands.")
            }
            
        }
    }   
	catch (Exception e) {
        parent.errorLog "retrieveStatusInfo_MELCloud : Unable to query Mitsubishi Electric MELCloud: ${e}"
	}
    return statusInfo
}

def applyStatusUpdates(statusInfo) {
    parent.debugLog("applyResponseStatus: Status Info: ${statusInfo}")
    
    if (!statusInfo.isEmpty()) {
        parent.debugLog("applyStatusUpdates: About to adjust thermostat mode details...")
        adjustThermostatMode(statusInfo.setMode, statusInfo.power)
        parent.debugLog("applyStatusUpdates: About to adjust temperatures...")
        adjustRoomTemperature(statusInfo.roomTemp)
        adjustSetTemperature(statusInfo.setTemp, statusInfo.setMode, statusInfo.power)
        adjustThermostatFanMode(statusInfo.setFan)

        parent.debugLog("applyResponseStatus: Status update complete")
    }
    else { parent.debugLog("applyResponseStatus: No status information was provided, no further action was taken") }
}


// Unit Control Methods

// Temperature Control

/* adjustRoomTemperature() To-Do: Handle Fahrenheit / Celsius */
def adjustRoomTemperature(givenTemp) {

  def tempscaleUnit = "°${location.temperatureScale}"
  def roomtempValue = givenTemp.toFloat().round(1)	
  
  parent.debugLog("adjustRoomTemperature: Temperature provided = ${givenTemp}, Units = ${tempscaleUnit}, Converted Value = ${roomtempValue}")
  if (device.currentValue("temperature") == null || device.currentValue("temperature") != roomtempValue) {
      parent.debugLog("adjustRoomTemperature: updating room temperature from ${device.currentValue("temperature")} to ${roomtempValue}")
      sendEvent(name: "temperature", value: roomtempValue)
  }
  else { parent.debugLog("adjustRoomTemperature: No action taken") }
}

// adjustHeatingSetpoint() To-Do: Use Minimum Heating Set Point instead of 23
def adjustHeatingSetpoint(givenTemp) {
    def heatingSetTempValue = givenTemp.toFloat().round(1)
	def currHeatingSetTempValue = checkNull(device.currentValue("heatingSetpoint"),"23.0").toFloat().round(1)
    def currThermSetTempValue = checkNull(device.currentValue("thermostatSetpoint"),"23.0").toFloat().round(1)
    
    parent.debugLog("adjustHeatingSetpoint: Current heatingSetpoint ${currHeatingSetTempValue}, Current ThermostatSetpoint = ${currThermSetTempValue}, New heatingSetpoint = ${heatingSetTempValue}")
    
    if (currHeatingSetTempValue != heatingSetTempValue) {
        sendEvent(name: "heatingSetpoint", value : heatingSetTempValue)
        parent.infoLog("Heating Set Point adjusted to ${heatingSetTempValue} for ${device.label}")
    }
    
    if (currThermSetTempValue != heatingSetTempValue) {
        sendEvent(name: "thermostatSetpoint", value: heatingSetTempValue)
        parent.infoLog("Thermostat Set Point adjusted to ${heatingSetTempValue} for ${device.label}")
    }
    
}

def setHeatingSetpoint(givenTemp) {

    def correctedTemp = givenTemp
    
    parent.debugLog("setHeatingSetpoint: Setting Heating Set Point to ${givenTemp}, current minimum ${device.currentValue("MinTempHeat")}, current maximum ${device.currentValue("MaxTempHeat")}")
    
    //Check allowable heating temperature range and correct where necessary
    //Minimum
    if (givenTemp < device.currentValue("MinTempHeat")) {
        correctedTemp = device.currentValue("MinTempHeat")
        parent.debugLog("setHeatingSetpoint: Temperature selected = ${givenTemp}, corrected to minimum heating set point ${correctedTemp}")
    }
    
    //Maximum
    if (givenTemp > device.currentValue("MaxTempHeat")) {
        correctedTemp = device.currentValue("MaxTempHeat")
        parent.debugLog("setHeatingSetpoint: Temperature selected = ${givenTemp}, corrected to maximum heating set point ${correctedTemp}")
    }
        
    adjustHeatingSetpoint(correctedTemp)
    if (device.currentValue("thermostatOperatingState") == "heating") {
        setTemperature(correctedTemp)
    }
}

// adjustCoolingSetpoint() To-Do: Use Maximum Heating Set Point instead of 23
def adjustCoolingSetpoint(givenTemp) {
 
    def coolingSetTempValue = givenTemp.toFloat().round(1)
	def currCoolingSetTempValue = checkNull(device.currentValue("coolingSetpoint"),"23.0").toFloat().round(1)
    def currThermSetTempValue = checkNull(device.currentValue("thermostatSetpoint"),"23.0").toFloat().round(1)
    
    parent.debugLog("adjustCoolingSetpoint: Current coolingSetpoint ${currCoolingSetTempValue}, Current ThermostatSetpoint = ${currThermSetTempValue}, New coolingSetpoint = ${coolingSetTempValue}")
    
    if (currCoolingSetTempValue != coolingSetTempValue) {
        sendEvent(name: "coolingSetpoint", value : coolingSetTempValue)
        parent.infoLog("Cooling Set Point adjusted to ${coolingSetTempValue} for ${device.label}")
    }
    
    
    if (device.currentValue("thermostatOperatingState") == "cooling" && currThermSetTempValue != coolingSetTempValue) {
        sendEvent(name: "thermostatSetpoint", value: coolingSetTempValue)
        parent.infoLog("Thermostat Set Point adjusted to ${coolingSetTempValue} for ${device.label}")
    }
    
}

def setCoolingSetpoint(givenTemp) {

    def correctedTemp = givenTemp
    
    parent.debugLog("setCoolingSetpoint: Setting Cooling Set Point to ${givenTemp}, current minimum ${device.currentValue("MinTempCool")}, current maximum ${device.currentValue("MaxTempCool")}")
    
    //Check allowable cooling temperature range and correct where necessary
    //Minimum
    if (givenTemp < device.currentValue("MinTempCool")) {
        correctedTemp = device.currentValue("MinTempCool")
        parent.debugLog("setCoolingSetpoint: Temperature selected = ${givenTemp}, corrected to minimum cooling set point ${correctedTemp}")
    }
    
    //Maximum
    if (givenTemp > device.currentValue("MaxTempCool")) {
        correctedTemp = device.currentValue("MaxTempCool")
        parent.debugLog("setCoolingSetpoint: Temperature selected = ${givenTemp}, corrected to maximum cooling set point ${correctedTemp}")
    }
        
    adjustCoolingSetpoint(correctedTemp)
    if (device.currentValue("thermostatOperatingState") == "cooling") {
        setTemperature(correctedTemp)
    }
}

// TO-DO: Look at use of the value 23.0 for the US
//        Tidy up use of conversions and checks and logging, particularly when we get a null value returned from API
def adjustSetTemperature(pSetTemp, pThermostatMode, pPower) {

    def vSetTemp
    if ("${pSetTemp}".isNumber()) { vSetTemp = pSetTemp.toFloat().round(1) }
    else { vSetTemp = null }
    
    
    def vCurrentSetTempConv
	def vCurrentSetTemp = device.currentValue("thermostatSetpoint")
    if ("${vCurrentSetTemp}".isNumber()) { vCurrentSetTempConv = vCurrentSetTemp.toFloat().round(1)}
    else { vCurrentSetTempConv = null }
    parent.debugLog("adjustSetTemperature: Temperature passed in was ${pSetTemp} which was parsed as ${vSetTemp}, current set temperature is ${vCurrentSetTempConv}")
    
    if (vSetTemp != null && (vCurrentSetTempConv == null || vCurrentSetTempConv != vSetTemp)) {
            
        parent.debugLog("adjustSetTemperature: Changing Set Temperature from ${vCurrentSetTempConv} to ${vSetTemp}")
    	sendEvent(name: "thermostatSetpoint", value: vSetTemp)
        
        def vMode
        if (pMode == null) { vMode = device.currentValue("thermostatMode") }
        else { vMode = deriveThermostatMode(pThermostatMode, pPower) }
        
        parent.debugLog("adjustSetTemperature: Current mode is ${vMode}")
        
        if (vMode == "heat") {
            parent.debugLog("adjustSetTemperature: Heating mode detected, adjusting heating set point")
            adjustHeatingSetpoint(vSetTemp)
        }
        
        if (vMode == "cool" || vMode == "dry") {
            parent.debugLog("adjustSetTemperature: Cooling / Drying mode detected, adjusting cooling set point")
            adjustCoolingSetpoint(vSetTemp)
        }
        
    }
    else { parent.debugLog("adjustSetTemperature: No action taken, either no change in temperature or null temperature provided") }
}

def setTemperature(givenSetTemp) {
    
    def vPlatform = parent.getPlatform()
    def setTempValue = givenSetTemp.toFloat().round(1)
    def currThermSetTempValue = checkNull(device.currentValue("thermostatSetpoint"),"23.0").toFloat().round(1)
    
    if(currThermSetTempValue != setTempValue) {
        parent.debugLog("setTemperature: Setting Temperature to ${setTempValue} for ${device.label}")
        adjustSetTemperature(givenSetTemp, null, null)
        
        if (vPlatform == "MELCloud")  { setTemperature_MELCloud(setTempValue)  }
        if (vPlatform == "MELView")   { setTemperature_MELView(setTempValue)   }
        if (vPlatform == "KumoCloud") { setTemperature_KumoCloud(setTempValue) }
        
        parent.debugLog("setTemperature: Temperature adjusted to ${setTempValue} for ${device.label}")
    }
    else {
        parent.debugLog("setTemperature: No action taken")
    
    }
}

def setTemperature_MELView(givenSetTemp) {

    parent.debugLog("setTemperature_MELCloud: Command = TS${givenSetTemp}")
    unitCommand_MELView("TS${givenSetTemp}")
    parent.debugLog("setTemperature_MELView: Unit Command submitted")
}

def setTemperature_KumoCloud(givenSetTemp) {

    def setTempValue = givenSetTemp.toFloat()
    def bodyJson = "{\"sp${device.currentValue("thermostatMode").toLowerCase().capitalize()}\":${setTempValue}}"
    parent.debugLog("setTemperature_KumoCloud: Body JSON = ${bodyJson}")
    
    unitCommand_KumoCloud("${bodyJson}")
    parent.debugLog("setTemperature_KumoCloud: Unit Command submitted")
}

def setTemperature_MELCloud(givenSetTemp) {
        
    def bodyJson = getUnitCommandBody_MELCloud( true //Power
                                               ,device.currentValue("thermostatFanMode")
                                               ,device.currentValue("thermostatMode")
                                               ,givenSetTemp
                                              )

    
    parent.debugLog("setTemperature_MELCloud: Body JSON = ${bodyJson}")

    unitCommand_MELCloud("${bodyJson}")
    
    parent.debugLog("setTemperature_MELCloud: Unit Command submitted")
}

// Fan Mode Control

def adjustThermostatFanMode(pFanModeKey) {

    // Convert the MEL Fan Mode Key provided to a Fan Mode and Speed recognised by HE
    def vFanModeValue    = fanModeMap["${pFanModeKey}"].trim()
    def vFanControlSpeed = ""
    
    if(vFanModeValue == "Auto")                                  vFanControlSpeed = "auto"
    if(vFanModeValue == "Low"         || vFanModeValue == "1")   vFanControlSpeed = "low"
    if(vFanModeValue == "Medium Low"  || vFanModeValue == "2")   vFanControlSpeed = "medium-low"
    if(vFanModeValue == "Medium"      || vFanModeValue == "3")   vFanControlSpeed = "medium"
    if(vFanModeValue == "Medium High" || vFanModeValue == "4")   vFanControlSpeed = "medium-high"
    if(vFanModeValue == "High"        || vFanModeValue == "5")   vFanControlSpeed = "high"
    
    parent.debugLog("adjustThermostatFanMode: MEL Fan Mode Key ${checkNull(pFanModeKey,"")} parsed as HE Fan Mode Value ${checkNull(vFanModeValue,"")} and HE Fan Speed ${vFanControlSpeed}")
    
    // Adjust the Fan Mode
    if(vFanModeValue == null) { parent.warnLog("adjustThermostatFanMode: Warning - Unknown Fan Mode selected, no action taken") }
    else {
        // Adjust the thermostatFanMode Attribute
        if (checkNull(device.currentValue("thermostatFanMode"),"") != vFanModeValue) {
    	    	sendEvent(name: "thermostatFanMode", value: vFanModeValue)
                parent.debugLog("adjustThermostatFanMode: Fan Mode adjusted to ${vFanModeValue} for ${device.label} (${getUnitId()})")
	    }
        else { parent.debugLog("adjustThermostatFanMode: No change to Fan Mode detected, no action taken") }
    }
    
    // Adjust the Fan Speed
    if(vFanControlSpeed == "") { parent.warnLog("adjustThermostatFanMode: Warning - Unknown Fan Speed selected, no action taken") }
    else {
        // Adjust the speed Attribute
        if (checkNull(device.currentValue("speed"),"") != vFanControlSpeed) {
            sendEvent(name: "speed", value: vFanControlSpeed)
            parent.infoLog("Fan Speed adjusted to ${vFanControlSpeed} for ${device.label} (${getUnitId()})")
        }
        else { parent.debugLog("adjustThermostatFanMode: No change to Fan Speed detected, no action taken") }
    }

}

def setThermostatFanMode(pFanMode) {

    def vPlatform = parent.getPlatform()
    def vFanMode = pFanMode.trim()
    def vFanModeKey = convertFanModeToKey(vFanMode)
    parent.debugLog("setThermostatFanMode: HE Fan Mode ${pFanMode} parsed as MEL Fan Mode Key ${vFanModeKey}")
    
    if (vFanModeKey != null) {
        if(checkNull(device.currentValue("thermostatFanMode"),"") != vFanMode)
        {
            adjustThermostatFanMode(vFanModeKey)
            parent.debugLog("setThermostatFanMode: Setting Fan Mode to ${vFanMode}(${vFanModeKey}) for ${device.label} (${getUnitId()})")
                        
            if (vPlatform == "MELCloud" ) { setThermostatFanMode_MELCloud  (vFanModeKey) }
            if (vPlatform == "MELView"  ) { setThermostatFanMode_MELView   (vFanModeKey) }
            if (vPlatform == "KumoCloud") { setThermostatFanMode_KumoCloud (vFanModeKey) }
                        
            parent.infoLog("Fan Mode set to ${vFanMode} for ${device.label} (${getUnitId()})")
        }
        else { parent.debugLog("setThermostatFanMode: No action taken")  }
        
    }
    else { parent.warnLog("setThermostatFanMode: Warning - Fan Mode ${pFanMode} not identified in MEL Fan Mode List, no action taken") }
}

def setThermostatFanMode_MELCloud(pFanModeKey) {
    
    def vBodyJson = getUnitCommandBody_MELCloud( true //Power
                                      ,pFanModeKey
                                      ,device.currentValue("thermostatMode")
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    
    unitCommand_MELCloud("${vBodyJson}")
}

def setThermostatFanMode_MELView (pFanModeKey) {
    
    unitCommand_MELView("FS${pFanModeKey}")
}

def setThermostatFanMode_KumoCloud (pFanModeKey) {
    
    unitCommand_KumoCloud("{\"fanSpeed\":${pFanModeKey}}")
    
}

//Fan Speed method from the Fan Control capability
//  Simply calling the Fan Mode method that is part of the Thermostat capability 
def setSpeed(pFanspeed) { setThermostatFanMode(pFanspeed) }

// Thermostat Mode Control

def adjustThermostatMode(pThermostatMode, pPower) {

    parent.debugLog("adjustThermostatMode: Adjust Thermostat Mode called")
    def vModeDesc = deriveThermostatMode(pThermostatMode, pPower)
    parent.debugLog("adjustThermostatMode: Thermostat Mode provided ${pThermostatMode}, Power provided ${pPower}, parsed as Mode Description ${vModeDesc}")
    
    if (checkNull(device.currentValue("thermostatMode"),"") != vModeDesc) {
    	sendEvent(name: "thermostatMode", value: vModeDesc)
        if (vModeDesc != "off" && checkNull(device.currentValue("lastRunningMode"),"") != vModeDesc) {
            sendEvent(name: "lastRunningMode", value: vModeDesc)
        }
    }
    adjustThermostatOperatingState(pThermostatMode,pPower)
}

/* adjustThermostatOperatingState To-Do: use map for mode to state translation */
def adjustThermostatOperatingState(pThermostatMode, pPower) {
	
    def vOperatingState
    if (pPower == "1") { vOperatingState = operatingStateMap["${pThermostatMode}"] }
    else { vOperatingState = "idle" }
    
    parent.debugLog("adjustThermostatOperatingState: Thermostat Mode passed in = ${pThermostatMode}, Power passed in ${pPower}, OperatingState: ${vOperatingState}")
    if (checkNull(device.currentValue("thermostatOperatingState"),"") != vOperatingState) {
        sendEvent(name: "thermostatOperatingState", value: vOperatingState)
    }    
    
}

def deriveThermostatMode(pThermostatMode, pPower) {
 
    def vModeDesc
    if (pPower == "1") { vModeDesc = modeMap["${pThermostatMode}"] }
    else { vModeDesc = "off" }
    
    return vModeDesc
    
}

def setThermostatMode(pThermostatMode) {

  parent.debugLog("setThermostatMode: Thermostat Mode passed in = ${pThermostatMode}")
  "${pThermostatMode}"()
  parent.debugLog("setThermostatMode: Thermostat Mode set")
}

def on() {
    
    parent.debugLog("on: Turning ON device ${device.label} (${getUnitId()})")
    "on_${parent.getPlatform()}"()
    parent.infoLog("Power turned ON for ${device.label} (${getUnitId()})")
    
    adjustThermostatMode(convertThermostatModeToKey("on"), "1")
    parent.debugLog("on: Thermostat Mode adjusted")
}

def on_MELCloud() {
    def vBodyJson = getUnitCommandBody_MELCloud( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,device.currentValue("thermostatMode")
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    unitCommand_MELCloud("${vBodyJson}")
}

def on_KumoCloud() {
    unitCommand_KumoCloud("{\"power\":1}")
}

def on_MELView() {
    
 unitCommand_MELView("PW1")   
}

def off() {
    
    parent.debugLog("off: Turning OFF device ${device.label} (${getUnitId()})")
    "off_${parent.getPlatform()}"()
    parent.infoLog("Power turned OFF for ${device.label} (${getUnitId()})")
    
    adjustThermostatMode(convertThermostatModeToKey("off"), "0")
    parent.debugLog("off: Thermostat Mode adjusted")
}

def off_KumoCloud() {
    
    unitCommand_KumoCloud("{\"power\":0}")
}

def off_MELCloud() {
    
    def vBodyJson = getUnitCommandBody_MELCloud( false //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,device.currentValue("thermostatMode")
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    unitCommand_MELCloud("${vBodyJson}")
}

def off_MELView() {
    
    unitCommand_MELView("PW0")
}

def heat() {
    
    adjustHeatingSetpoint(device.currentValue("thermostatSetpoint"))
    
    parent.debugLog("heat: Adjusting Thermostat Mode to Heating for ${device.label} (${getUnitId()})")
    "heat_${parent.getPlatform()}"()
    parent.infoLog("Thermostat Mode set to Heating for ${device.label} (${getUnitId()})")
    
    adjustThermostatMode(convertThermostatModeToKey("heat"), "1")
    parent.debugLog("heat: Thermostat Mode adjusted")
}

def heat_KumoCloud() {

    unitCommand_KumoCloud("{\"power\":1,\"operationMode\":1}")
}

def heat_MELCloud() {
    
    def vBodyJson = getUnitCommandBody_MELCloud( false //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,"heat" //thermostatMode
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    unitCommand_MELCloud("${vBodyJson}")
}

def heat_MELView() {
    
    unitCommand_MELView("PW1,MD1")
}
    
def dry() {
    
    parent.debugLog("dry: Adjusting Thermostat Mode to Dry for ${device.label} (${getUnitId()})")
    "dry_${parent.getPlatform()}"()
    parent.infoLog("Thermostat Mode set to Dry for ${device.label} (${getUnitId()})")
    
    adjustThermostatMode(convertThermostatModeToKey("dry"), "1")
    parent.debugLog("dry: Thermostat Mode adjusted")
}

def dry_KumoCloud() {
    
    unitCommand_KumoCloud("{\"power\":1,\"operationMode\":2}")
}

def dry_MELCloud() {
    
    def vBodyJson = getUnitCommandBody_MELCloud( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,"dry" //thermostatMode
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    unitCommand_MELCloud("${vBodyJson}")
}

def dry_MELView() {
    
    unitCommand_MELView("PW1,MD2")
}

def cool() {
    
    parent.debugLog("cool: Adjusting Thermostat Mode to Cooling for ${device.label} (${getUnitId()})")
    "cool_${parent.getPlatform()}"()
    parent.infoLog("Thermostat Mode set to Cooling for ${device.label} (${getUnitId()})")
    
    adjustThermostatMode(convertThermostatModeToKey("cool"), "1")
    parent.debugLog("cool: Thermostat Mode adjusted")
}

def cool_KumoCloud() {
    
    unitCommand_KumoCloud("{\"power\":1,\"operationMode\":3}")
}

def cool_MELCloud() {
    
    def vBodyJson = getUnitCommandBody_MELCloud( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,"cool" //thermostatMode
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    
    unitCommand_MELCloud("${vBodyJson}")
}

def cool_MELView() {
    
    unitCommand_MELView("PW1,MD3")
}

def fan() {
    
    parent.debugLog("fan: Adjusting Thermostat Mode to Fan for ${device.label} (${getUnitId()})")
    "fan_${parent.getPlatform()}"()
    parent.infoLog("Thermostat Mode set to Fan for ${device.label} (${getUnitId()})")
    
    adjustThermostatMode(convertThermostatModeToKey("fan"), "1")
    parent.debugLog("fan: Thermostat Mode adjusted")
}

def fan_KumoCloud() {
    
    unitCommand_KumoCloud("{\"power\":1,\"operationMode\":7}")  
}

def fan_MELCloud() {
    
   def vBodyJson = getUnitCommandBody_MELCloud( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,"fan" //thermostatMode
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    unitCommand_MELCloud("${vBodyJson}") 
}

def fan_MELView() {
    
    unitCommand_MELView("PW1,MD7")
}

def auto() {
    
    parent.debugLog("auto: Adjusting Thermostat Mode to Auto for ${device.label} (${getUnitId()})")
    "auto_${parent.getPlatform()}"()
    parent.infoLog("Thermostat Mode set to Auto for ${device.label} (${getUnitId()})")  
    
    adjustThermostatMode(convertThermostatModeToKey("auto"), "1")
    parent.debugLog("auto: Thermostat Mode adjusted")
}

def auto_KumoCloud() {
    
    unitCommand_KumoCloud("{\"power\":1,\"operationMode\":8}")
}

def auto_MELCloud() {
    
    def vBodyJson = getUnitCommandBody_MELCloud( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,"auto" //thermostatMode
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    unitCommand_MELCloud("${vBodyJson}")  
}

def auto_MELView() {
    
     unitCommand_MELView("PW1,MD8")
}

// Platform Specific API Command Methods

def unitCommand_MELCloud(pCommand) {
    
    def vPostParams = [
        uri: "${parent.getBaseURL()}/Mitsubishi.Wifi.Client/Device/SetAta",
        headers: parent.getStandardHTTPHeaders_MELCloud("no"),
        contentType: "application/json; charset=UTF-8",
        body : "${pCommand}"
	]
    
	try {
        
        httpPost(vPostParams) { resp ->
            
            parent.debugLog("unitCommand_MELCloud: Initial data returned from SetAta: ${resp.data}")
            
          }
    }   
	catch (Exception e) {
        parent.errorLog "unitCommand : Unable to query Mitsubishi Electric ${parent.getPlatform()}: ${e}"
	}

}

def unitCommand_KumoCloud(pCommand) {

    def vBodyJson = "[\"${parent.getAuthCode()}\",{\"${getUnitId()}\":${pCommand}}]"
    parent.debugLog("unitCommand_KumoCloud: Body of command = ${vBodyJson}")
    def vPostParams = [
        uri: "${parent.getBaseURL()}/sendDeviceCommands/v2",
        headers: parent.getStandardHTTPHeaders_KumoCloud("no"),
        contentType: "application/json; charset=UTF-8",
        body : vBodyJson
	]
    
    try {
        httpPost(vPostParams) { resp ->
            parent.debugLog("unitCommand: Initial data returned from unitCommand: ${resp.data}")
          }
    }   
	catch (Exception e) {
        parent.errorLog "unitCommand_KumoCloud : Unable to query Mitsubishi Electric ${parent.getPlatform()}: ${e}"
	}
    
}

def unitCommand_MELView(pCommand) {
    // Re-usable method that submits a command to the MELView Service, based on the command text passed in
    // See https://github.com/NovaGL/diy-melview for more details on commands and this API more generally
 
    def vBodyJson = "{ \"unitid\": \"${getUnitId()}\", \"v\": 2, \"commands\": \"${pCommand}\", \"lc\": 1 }"
    
    def vPostParams = [
        uri: "${parent.getBaseURL()}unitcommand.aspx",
        headers: parent.getStandardHTTPHeaders_MELView("no"),
        contentType: "application/json",
        body : vBodyJson
	]
    
	try {
        httpPost(vPostParams) { resp -> parent.debugLog("unitCommand_MELView: (${pCommand}): Response - ${resp.data}") }
    }
	catch (Exception e) {
        parent.errorLog("unitCommand_MELView: (${pCommand}): Unable to query Mitsubishi Electric ${parent.getPlatform()}: ${e}")
	}
}

def getUnitCommandBody_MELCloud(pPower, pFanMode, pOpMode, pSetTemp) {
 
    def vBodyJson = null
    def vFanModeKey = null
    def vFanModeText = null
    def vModeKey = null
    def vModeText = null
    def vSetTempText = null

    /*
       #EffectiveFlags:
        #Power:                0x01
        #OperationMode:        0x02
        #Temperature:        0x04
        #FanSpeed:            0x08
        #VaneVertical:        0x10
        #VaneHorizontal:    0x100
    */
    
    // Compile the fan mode text
    
    // Lookup the fan mode key for MEL based on fan mode provided
    vFanModeKey = convertFanModeToKey(pFanMode)
        
    if (vFanModeKey != null) {
        vFanModeText = "\"SetFanSpeed\" : \"${vFanModeKey}\","
    }
    else { vFanModeText = "" }
    
    // Compile the Thermostat Mode text
    
    // Lookup the operating mode key for MEL based on mode provided
    vModeKey = convertThermostatModeToKey(pOpMode)
        
    if (vModeKey != null) {vModeText = "\"OperationMode\" : \"${vModeKey}\"," }
    else {vModeText = ""}
    
    // Compile the Set Temperature Text
    
    vSetTempText = "\"SetTemperature\" : \"${pSetTemp}\","
    
    vBodyJSON = "{ \"Power\" : \"${pPower}\", ${vModeText} ${vSetTempText} ${vFanModeText} \"EffectiveFlags\" : \"15\", \"DeviceID\" : \"${getUnitId()}\",  \"HasPendingCommand\" : \"true\" }"
    
    return "${vBodyJSON}"
    
}

// Thermostat Mode methods from Thermostat capability currently unsupported by MEL Thermostat Driver

def emergencyHeat() { parent.debugLog("emergencyHeat: Not currently supported by MEL Thermostat driver") }
def fanAuto() { parent.debugLog("fanAuto: Not currently supported by MEL Thermostat driver") }
def fanCirculate() { parent.debugLog("fanCirculate: Not currently supported by MEL Thermostat driver") }
def setSchedule(JSON_OBJECT) { parent.debugLog("setSchedule: Not currently supported by MEL Thermostat driver") }

// Scheduled Status Update Methods

def getSchedule() { }

def updateStatusPolling() {

   def vSchedule
   parent.debugLog("updateStatusPolling: Updating Status Polling called, about to unschedule refresh")
   unschedule("refresh")
   parent.debugLog("updateStatusPolling: Unscheduleing refresh complete")
   
   if(AutoStatusPolling == true) {
       
       vSchedule = "0 0/${StatusPollingInterval} * ? * * *"
       parent.debugLog("updateStatusPolling: Setting up schedule with settings: schedule(\"${vSchedule}\",refresh)")
       try{
           
           schedule("${vSchedule}","refresh")
       }
       catch(Exception e) {
           parent.debugLog("updateStatusPolling: Error - " + e)
       }
       
       parent.debugLog("updateStatusPolling: Scheduled refresh set")
   }
   else { parent.debugLog("updateStatusPolling: Automatic status polling is disabled, no further action was taken")  }
}

// Utility Methods

def checkNull(value, alternative) {
 
    if(value == null) { return alternative }
    return value
    
}
