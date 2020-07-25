/**
 *  Mitsubishi Electric MELCloud AC Unit Child Driver
 *
 *  Copyright 2019 Simon Burke
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
 *    2019-12-06  Simon Burke    Original Creation
 *    2020-01-04  Simon Burke    Started adding Thermostat capability
 *    2020-02-09  Simon Burke    Thermostat capability and refreshing of state from MELCloud appear to be working
 *                               Starting work on splitting into Parent / Child Driver
 *    2020-02-16  Simon Burke    Adjusted code to use new parent driver logging, i.e. Debug, Info and Error Logging methods
 *                               Ensured all attributes were setup when device is created
 *                               Various minor code refinements
 *    2020-07-25  Simon Burke    Updated setHeating and setCoolingSetPoint methods to correct error showing up in logs
 *                                    relating to toDecimal method not being available, changed to toBigDecimal
 * 
 */
metadata {
	definition (name: "MELCloud AC Unit", namespace: "simnet", author: "Simon Burke") {
        capability "Refresh"
        capability "Thermostat"
        
        attribute "authCode", "string"
        attribute "unitId", "number"
        
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
        
        attribute "lastRunningMode",                "STRING"
        
        //MELCloud specific commands:
        command "on"
        
        //Thermostat capability commands
        /*
        command "auto"
        command "cool"
        command "emergencyHeat" //Unsupported in MELCloud
        command "fanAuto"
        command "fanCirculate"
        command "fanOn"
        command "heat"
        command "off"
        
        command "setCoolingSetpoint", [[name:"temperature*", type: "NUMBER", description: "Enter the Cooling Set Point" ] ]
                // temperature required (NUMBER) - Cooling setpoint in degrees
        command "setHeatingSetpoint", [[name:"temperature*", type: "NUMBER", description: "Enter the Heating Set Point" ] ]
                // temperature required (NUMBER) - Heating setpoint in degrees
        command "setSchedule", [[name:"JSON_OBJECT", type: "JSON_OBJECT", description: "Enter the JSON for the schedule" ] ]
            // JSON_OBJECT (JSON_OBJECT) - JSON_OBJECT
*/
        //Providing command with fan modes supported by MELCloud
        //command "setThermostatFanMode", [[name:"fanmode*", type: "ENUM", description: "Pick a Fan Mode", constraints: ["Low", "Mid", "High", "Auto"] ] ]
                // fanmode required (ENUM) - Fan mode to set
        
        //command "setThermostatMode", [[name:"thermostatmode*", type: "ENUM", description: "Pick a Thermostat Mode", constraints: ['Heat', 'Dry', 'Cool', 'Fan', 'Auto'] ] ]
                // thermostatmode required (ENUM) - Thermostat mode to set
        
	}

}


def refresh() {
  // Retrieve current state information from MEL Cloud Service   
  getRooms()
  initialize()
}

def initialize() {
    // Adjust default enumerations setup as part of the Hubitat Thermostat capability
    sendEvent(name: 'supportedThermostatFanModes', value: ['Low', 'Mid', 'High', 'Auto'])
    sendEvent(name: 'supportedThermostatModes', value: ['heat', 'dry', 'cool', 'fan', 'auto', 'off'])
    
    if ("${device.currentValue("coolingSetpoint")}" > 40)
    { sendEvent(name: "coolingSetpoint", value: "23.0")}
    
    if ("${device.currentValue("heatingSetpoint")}" > 40)
    { sendEvent(name: "heatingSetpoint", value: "23.0")}
    
}



def getRooms() {
    //retrieves current status information for the ac unit
    
    def vUnitId = ""
    def vRoom = ""
    def vPower = ""
    def vMode = ""
    def vModeDesc = ""
    def vTemp = ""
    def vSetTemp = ""
    
    def bodyJson = "{ }"
    def headers = [:] 

    headers.put("Content-Type", "application/json")
    headers.put("Cookie", "auth=${parent.currentValue("authCode", true)}")
    headers.put("accept", "application/json, text/javascript, */*; q=0.01")
    def postParams = [
        uri: "${parent.getBaseURL()}rooms.aspx",
        headers: headers,
        contentType: "application/json",
        body : bodyJson
	]
    parent.debugLog("${bodyJson}, ${headers.Cookie}") 
	try {
        
        httpPost(postParams) { resp -> 
            parent.debugLog("GetRooms: Initial data returned from rooms.aspx: ${resp.data}") 
            resp?.data?.each { building -> // Each Building
                                building?.units?.each // Each AC Unit / Room
                                  { acUnit ->
                                      vModeDesc = ""
                                      vRoom     = acUnit.room
                                      vUnitId   = acUnit.unitid
                                      vPower    = acUnit.power
                                      vMode     = acUnit.mode
                                      vTemp     = acUnit.temp
                                      vSetTemp  = acUnit.settemp
                                      
                                      if ("${vUnitId}" == "${device.currentValue("unitId")}") {
                                          
                                          if (vPower == "q") {vModeDesc = "off"}
                                          else {
                                              if (vMode == "1") {vModeDesc = "heat" }
                                              if (vMode == "2") {vModeDesc = "dry" }
                                              if (vMode == "3") {vModeDesc = "cool" }
                                              if (vMode == "7") {vModeDesc = "fan" }
                                              if (vMode == "8") {vModeDesc = "auto" }
                                          }
    
                                          sendEvent(name: "temperature", value: "${vTemp}")
                                          sendEvent(name: "thermostatOperatingState", value: "${vModeDesc}")
                                          sendEvent(name: "thermostatMode", value: "${vModeDesc}")
                                          
                                          if (  "${vModeDesc}" == "cool"
                                             || "${vModeDesc}" == "dry"
                                             || "${vModeDesc}" == "auto")
                                            { setCoolingSetpoint(vSetTemp) }

                                          if (   "${vModeDesc}" == "heat"
                                               ||"${vModeDesc}" == "auto")
                                            { setHeatingSetpoint(vSetTemp) }

                                          sendEvent(name: "lastRunningMode", value: "${vModeDesc}")
                                          parent.debugLog("GetRooms: Interpretted results - ${vRoom}(${vUnitId}) - Power: ${vPower}, Mode: ${vModeDesc}(${vMode}), Temp: ${vTemp}, Set Temp: ${vSetTemp}" ) 
                                      } 
                                  } 
                }
            }
    }   
	catch (Exception e) {
        parent.errorLog("GetRooms : Unable to query Mitsubishi Electric cloud: ${e}")
	}
}

def unitCommand(command) {
    // Re-usable method that submits a command to the MEL Cloud Service, based on the command text passed in
    // See https://github.com/NovaGL/diy-melview for more details on commands and this API more generally
 
    def bodyJson = "{ \"unitid\": \"${device.currentValue("unitId", true)}\", \"v\": 2, \"commands\": \"${command}\", \"lc\": 1 }"
    def headers = [:] 

    headers.put("Content-Type", "application/json")
    headers.put("Cookie", "auth=${parent.currentValue("authCode", true)}")
    headers.put("accept", "application/json")
    def postParams = [
        uri: "${parent.getBaseURL()}unitcommand.aspx",
        headers: headers,
        contentType: "application/json",
        body : bodyJson
	]
    parent.debugLog("${bodyJson}, ${headers.Cookie}")       
	try {
        
        httpPost(postParams) { resp -> parent.debugLog("UnitCommand (${command}): Response - ${resp.data}") }
    }
	catch (Exception e) {
        parent.errorLog("UnitCommand (${command}): Unable to query Mitsubishi Electric cloud: ${e}")
	}
}


//Unsupported commands from Thermostat capability
def emergencyHeat() { parent.debugLog("Emergency Heat Command not supported by MELCloud") }


//Fan Adjustments

//MELCloud Commands ('FS' 2 - LOW, 3 - MID, 5 - HIGH, 0 - AUTO)

def fanAuto() {
    unitCommand("FS0")
}

def fanCirculate() {
    unitCommand("FS5")
}

//fanOn - see modes section

//Scheduling Adjustments - Unsupported at present
def setSchedule(JSON_OBJECT) {parent.debugLog("setSchedule not currently supported by MELCloud")}



//Temperature Adjustments



def setCoolingSetpoint(temperature) {

    sendEvent(name: "coolingSetpoint", value : temperature)
    parent.infoLog("${device.label} - Cooling Set Point adjusted to ${temperature}")
    if (device.currentValue("thermostatOperatingState") == 'cool') {
        setTemperature(temperature.toBigDecimal())
    }
}

def setHeatingSetpoint(temperature) {

    sendEvent(name: "heatingSetpoint", value : temperature)
    parent.infoLog("${device.label} - Heating Set Point adjusted to ${temperature}")
    if (device.currentValue("thermostatOperatingState") == 'heat') {
        setTemperature(temperature.toBigDecimal())
    }
}

def setThermostatFanMode(fanmode) {
    
    sendEvent(name: "thermostatFanMode", value : fanmode)
    parent.infoLog("${device.label} - Fan Mode set to ${fanmode}")
}


def setThermostatMode(thermostatmodeX) {
    
    if (thermostatmodeX != device.currentValue("thermostatMode")) {
        
        sendEvent(name: "thermostatMode", value : thermostatmodeX)
        sendEvent(name: "thermostatOperatingState", value : thermostatmodeX)
        parent.infoLog("${device.label} - Thermostat Mode Set to ${thermostatmodeX}")
        
        if (thermostatmodeX == 'dry') { dry() }
        if (thermostatmodeX == 'cool') { cool() }
        if (thermostatmodeX == 'heat') { heat() }
        if (thermostatmodeX == 'auto') { auto() }
        if (thermostatmodeX == 'fan') { fanOn() }
    }
}


def setTemperature(temperature) {
    parent.debugLog("setTemperature: Adjusting Temperature to ${temperature}")
    
    unitCommand("TS${temperature}")
    sendEvent(name: "thermostatSetPoint", value: temperature.toBigDecimal())
    parent.infoLog("${device.label} - Temperature adjusted to ${temperature}")
    
    
}


//Power and State Adjustments

def on() {
    
    unitCommand("PW1")    
}

def off() {
    
    unitCommand("PW0")
}

//Modes:

//"MD1" - HEAT
//"MD2" - DRY
//"MD3" - Cooling
//"MD7" - FAN
//"MD8" - Auto

def auto() {
    on()
    unitCommand("MD8")
}

def fanOn() {
    on()
    unitCommand("MD7")
}

def cool() {
    on()
    unitCommand("MD3")
    setTemperature(device.currentValue("coolingSetpoint"))
}

def dry() {
    on()
    unitCommand("MD2")    
}

def heat() {
    on()
    unitCommand("MD1")
    setTemperature(device.currentValue("heatingSetpoint"))
}





