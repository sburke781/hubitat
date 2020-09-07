/**
 *  Mitsubishi Electric MELCloud AC Unit Child Driver
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
 *    2020-08-08  Simon Burke    Original Creation
 * 
 */
import java.text.DecimalFormat;

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
        command "emergencyHeat" //Unsupported in MELView
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
  // Retrieve current state information from MELCloud Service   
  getStatusInfo()
  initialize()
}

def initialize() {
    // Adjust default enumerations setup as part of the Hubitat Thermostat capability
    //sendEvent(name: 'supportedThermostatFanModes', value: ['Low', 'Mid', 'High', 'Auto'])
    //sendEvent(name: 'supportedThermostatModes', value: ['heat', 'dry', 'cool', 'fan', 'auto', 'off'])
    
    //if ("${device.currentValue("coolingSetpoint")}" > 40)
    //{ sendEvent(name: "coolingSetpoint", value: "23.0")}
    
    //if ("${device.currentValue("heatingSetpoint")}" > 40)
    //{ sendEvent(name: "heatingSetpoint", value: "23.0")}
    
}

def getFanModeMap() {
    [
        0:"auto",
        1:"low",
        2:"low-medium",
        3:"medium",
        5:"medium-high",
        6:"high"
    ]
}

def getModeMap() {
    [
        1:"heat",
        2:"dry",
        3:"cool",
        7:"fan",
        8:"auto"
    ]
}


def getStatusInfo() {
    //retrieves current status information for the ac unit
    def statusInfo = [:]
    def bodyJson = "{ }"
    def headers = [:] 

    headers.put("Content-Type", "application/json; charset=UTF-8")
    headers.put("Accept", "application/json, text/javascript, */*; q=0.01")
    headers.put("Referer", "${parent.getBaseURL()}/")
    headers.put("X-Requested-With","XMLHttpRequest")
    headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
    headers.put("X-MitsContextKey","${parent.currentValue("authCode", true)}")
    
    def getParams = [
        uri: "${parent.getBaseURL()}/Mitsubishi.Wifi.Client/User/ListDevices",
        headers: headers,
        contentType: "application/json; charset=UTF-8",
        body : bodyJson
	]
    parent.debugLog("getStatusInfo: Body = ${bodyJson}, Headers = ${headers}")       
	try {
        
        httpGet(getParams) { resp ->
            
            parent.debugLog("getStatusInfo: Initial data returned from ListDevices: ${resp.data}")
            
            statusInfo.unitid = "${resp?.data?.Structure?.Devices?.Device.DeviceID}".replace("[","").replace("]","")
            statusInfo.power = "${resp?.data?.Structure?.Devices?.Device.Power}".replace("[","").replace("]","")
            statusInfo.setmode = "${resp?.data?.Structure?.Devices?.Device.OperationMode}".replace("[","").replace("]","").toInteger()
            statusInfo.roomtemp = "${resp?.data?.Structure?.Devices?.Device.RoomTemperature}".replace("[","").replace("]","")
            statusInfo.settemp = "${resp?.data?.Structure?.Devices?.Device.SetTemperature}".replace("[","").replace("]","")
            
            parent.debugLog("updating ${statusInfo.unitId}")  
            applyResponseStatus(statusInfo)
            
            /*

Device Detail


Power:true,
RoomTemperature:21.0,
SetTemperature:21.0,
OperationMode:3,                         1 = Heating | OperationMode 3 = Cooling
DefaultCoolingSetTemperature:21.0,
DefaultHeatingSetTemperature:null,



VaneVerticalSwing:false,          On "7"
VaneHorizontalSwing:false,        On "12"


ActualFanSpeed:2,
FanSpeed:0,
AutomaticFanSpeed:true,
VaneVerticalDirection:0,

VaneHorizontalDirection:0,


InStandbyMode:false,


CanCool:true,
CanHeat:true,
CanDry:true,
HasAutomaticFanSpeed:true,
AirDirectionFunction:true,
SwingFunction:true,
NumberOfFanSpeeds:5,
UseTemperatureA:true,
TemperatureIncrementOverride:0,
TemperatureIncrement:0.5,
MinTempCoolDry:16.0,
MaxTempCoolDry:31.0,
MinTempHeat:10.0,
MaxTempHeat:31.0,
MinTempAutomatic:16.0,
MaxTempAutomatic:31.0,
LegacyDevice:false,
UnitSupportsStandbyMode:true,
HasWideVane:false,
ModelIsAirCurtain:false,
ModelSupportsFanSpeed:true,
ModelSupportsAuto:true,
ModelSupportsHeat:true,
ModelSupportsDry:true,
ModelSupportsVaneVertical:true,
ModelSupportsVaneHorizontal:true,
ModelSupportsWideVane:true,
ModelDisableEnergyReport:false,
ModelSupportsStandbyMode:true,
ModelSupportsEnergyReporting:true,
ProhibitSetTemperature:false,
ProhibitOperationMode:false,
ProhibitPower:false,

EffectiveFlags:0,
LastEffectiveFlags:0,

RoomTemperatureLabel:0,

HeatingEnergyConsumedRate1:0,
HeatingEnergyConsumedRate2:0,
CoolingEnergyConsumedRate1:0,
CoolingEnergyConsumedRate2:0,
AutoEnergyConsumedRate1:0,
AutoEnergyConsumedRate2:0,
DryEnergyConsumedRate1:0,
DryEnergyConsumedRate2:0,
FanEnergyConsumedRate1:0,
FanEnergyConsumedRate2:0,
OtherEnergyConsumedRate1:0,
OtherEnergyConsumedRate2:0,
HasEnergyConsumedMeter:true,
CurrentEnergyConsumed:18500,
CurrentEnergyMode:3,
CoolingDisabled:false,
MinPcycle:1,
MaxPcycle:1,
EffectivePCycle:1,
MaxOutdoorUnits:255,
MaxIndoorUnits:255,
MaxTemperatureControlUnits:0,

DeviceID:271155,
MacAddress:d4:53:83:28:f1:66,
SerialNumber:1910243189,
TimeZoneID:119,
DiagnosticMode:0,
DiagnosticEndDate:null,
ExpectedCommand:1,
Owner:null,
DetectedCountry:null,
AdaptorType:-1,
FirmwareDeployment:null,
FirmwareUpdateAborted:false,
LinkedDevice:null,
WifiSignalStrength:-49,
WifiAdapterStatus:NORMAL,
Position:Unknown,
PCycle:1,
RecordNumMax:0,
LastTimeStamp:2020-08-08T05:25:00,
ErrorCode:8000,
HasError:false,
LastReset:2020-07-22T20:06:58.223,
FlashWrites:0,
Scene:null,
SSLExpirationDate:2037-12-31T00:00:00,
SPTimeout:1,
Passcode:null,
ServerCommunicationDisabled:false,
ConsecutiveUploadErrors:1,
DoNotRespondAfter:null,
OwnerRoleAccessLevel:1,
OwnerCountry:217,
HideEnergyReport:false,
ExceptionHash:null,
ExceptionDate:null,
ExceptionCount:null,
Rate1StartTime:null,
Rate2StartTime:null,
ProtocolVersion:0,
UnitVersion:0,
FirmwareAppVersion:17000,
FirmwareWebVersion:0,
FirmwareWlanVersion:0,
HasErrorMessages:false,
HasZone2:false,
Offline:false





*/
            
        }
    }   
	catch (Exception e) {
        parent.errorLog "getStatusInfo : Unable to query Mitsubishi Electric MELCloud: ${e}"
	}
}




def applyResponseStatus(statusInfo) {
    
    parent.debugLog("applyResponseStatus: Status Info: ${statusInfo}")
    
    adjustThermostatMode(statusInfo.setmode, statusInfo.power)
    adjustRoomTemperature(statusInfo.roomtemp)
    adjustSetTemperature(statusInfo.settemp)
    adjustThermostatFanMode(statusInfo.setfan)

}


def unitCommand(command) {

    
    def statusInfo = [:]
    def bodyJson = "${command}"
    def headers = [:] 

    headers.put("X-MitsContextKey","${parent.currentValue("authCode", true)}")
    headers.put("Sec-Fetch-Site","same-origin")
    headers.put("Origin","${parent.getBaseURL()}/")
    headers.put("Accept-Encoding","gzip, deflate, br")
    headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
    headers.put("Sec-Fetch-Mode","cors")
    headers.put("Accept", "application/json, text/javascript, */*; q=0.01")
    headers.put("Referer", "${parent.getBaseURL()}/")
    headers.put("X-Requested-With","XMLHttpRequest")
    headers.put("Cookie","policyaccepted=true")
    headers.put("Content-Type", "application/json; charset=UTF-8")

    def postParams = [
        uri: "${parent.getBaseURL()}/Mitsubishi.Wifi.Client/Device/SetAta",
        headers: headers,
        contentType: "application/json; charset=UTF-8",
        body : bodyJson
	]
    parent.debugLog("unitCommand: Body = ${bodyJson}, Headers = ${headers}")       
	try {
        
        httpPost(postParams) { resp ->
            
            parent.debugLog("unitCommand: Initial data returned from SetAta: ${resp.data}")
            
            statusInfo.unitid = "${resp?.data?.DeviceID}"
            statusInfo.power = "${resp?.data?.Power}"
            statusInfo.setmode = "${resp?.data?.OperationMode}".toInteger()
            statusInfo.roomtemp = "${resp?.data?.RoomTemperature}"
            statusInfo.settemp = "${resp?.data?.SetTemperature}"
            
            parent.debugLog("unitCommand: updating ${statusInfo.unitid}")  
            
            applyResponseStatus(statusInfo)            
            
          }
    }   
	catch (Exception e) {
        parent.errorLog "unitCommand : Unable to query Mitsubishi Electric MELCloud: ${e}"
	}

}


//Unsupported commands from Thermostat capability
def emergencyHeat() { parent.debugLog("Emergency Heat Command not supported by MELCloud") }


//Fan Adjustments

def fanAuto() { }

def fanCirculate() { }

//fanOn - see modes section

//Scheduling Adjustments - Unsupported at present
def setSchedule(JSON_OBJECT) { parent.debugLog("setSchedule not currently supported by MELCloud") }

//Temperature Adjustments

def adjustCoolingSetpoint(temperature) {
 
    if (device.currentValue("coolingSetpoint") != temperature) {
        sendEvent(name: "coolingSetpoint", value : temperature)
        parent.infoLog("adjustCoolingSetpoint:  Cooling Set Point adjusted to ${temperature} for ${device.label}")
    }
    
    if (device.currentValue("thermostatSetpoint") != temperature) {
        sendEvent(name: "thermostatSetpoint", value: temperature)
        parent.infoLog("adjustCoolingSetpoint: Thermostat Set Point adjusted to ${temperature} for ${device.label}")
    }
}

def setCoolingSetpoint(temperature) {

    adjustCoolingSetpoint(temperature)
    if (device.currentValue("thermostatOperatingState") == 'cooling') {
        setTemperature(temperature.toBigDecimal())
    }
}

def adjustHeatingSetpoint(temperature) {
    
    
    if (device.currentValue("heatingSetpoint") != temperature.toBigDecimal()) {
        sendEvent(name: "heatingSetpoint", value : temperature.toBigDecimal())
        parent.infoLog("adjustHeatingSetpoint:  Heating Set Point adjusted to ${temperature.toBigDecimal()} for ${device.label}")
    }
    
    if (device.currentValue("thermostatSetpoint") != temperature.toBigDecimal()) {
        sendEvent(name: "thermostatSetpoint", value: temperature.toBigDecimal())
        parent.infoLog("adjustHeatingSetpoint: Thermostat Set Point adjusted to ${temperature.toBigDecimal()} for ${device.label}")
    }
    
}

def setHeatingSetpoint(temperature) {

    adjustHeatingSetpoint(temperature)
    if (device.currentValue("thermostatOperatingState") == 'heating') {
        setTemperature(temperature.toBigDecimal())
    }
}

def adjustSetTemperature(temperature) {

    def setTempValue = convertTemperatureIfNeeded(statusInfo.settemp.toFloat(),"c",1)
	
    parent.debugLog("adjustSetTemperature: Temperature passed in was ${temperature}, adjusting Set Temperature to converted value ${setTempValue}")
    parent.debugLog("adjustSetTemperature: Checking if we are heating...")
    if (device.currentValue("thermostatOperatingState") == 'heating') {
        adjustHeatingSetpoint(setTempValue)
    }
    parent.debugLog("adjustSetTemperature: Checking if we are cooling...")
    if (device.currentValue("thermostatOperatingState") == 'cooling') {
        adjustCoolingSetpoint(setTempValue)
    }
    parent.debugLog("adjustSetTemperature: Changing set temperature attribute")
    sendEvent(name: "setTemperature", value: setTempValue)
}

def setTemperature(temperature) {

    if(device.currentValue("setTemperature") != temperature) {
        bodyJson = "{ \"SetTemperature\" : \"${temperature}\", \"EffectiveFlags\" : \"4\", \"DeviceID\" : \"${device.currentValue("unitId")}\",  \"HasPendingCommand\" : \"true\" }"
    
        parent.debugLog("setTemperature: Adjusting Temperature to ${temperature} for ${device.label}")
    
        unitCommand("${bodyJson}")
        parent.infoLog("setTemperature: Temperature adjusted to ${temperature} for ${device.label} (${device.currentValue("unitId")})")
    }
    else {
        parent.debugLog("setTemperature: No change required for temperature")
    
    }
	
    //adjustSetTemperature will be called as a result of calling the unitCommand and applying the status updates that comes back
}

def adjustRoomTemperature(temperature) {

  def tempscaleUnit = "°${location.temperatureScale}"
  def roomtempValue = convertTemperatureIfNeeded(roomtempValue.toFloat(),"c",1)	
  
  parent.debugLog("adjustRoomTemperature: Temperature provided = ${temperature}, Units = ${tempscaleunit}, Converted Value = ${roomtempvalue}")
  sendEvent(name: "temperature", value: roomtempValue)
}

//Power and State Adjustments

def adjustThermostatFanMode(fanmode) {

    parent.debugLog("adjustThermostatFanMode: Adjusting Fan Mode to ${fanmode}")
    if (fanmode != null) {
        def fanModeValue = fanModeMap[fanmode]
    	sendEvent(name: "thermostatFanMode", value: fanModeValue)
    }
}

def setThermostatFanMode(fanmode) {
  
  // adjustThermostatFanMode will be called by the apply status info method, called by the unit command method

}

def adjustThermostatMode(thermostatmodeX, power) {

    parent.debugLog("adjustThermostatMode: Adjusting Thermostat Mode to ${thermostatmodeX}, power passed in is ${power}")
	
    def vModeDesc = ""
    if (power == "q" || power == 0) {vModeDesc = "off"}
    else {
        vModeDesc = modeMap[thermostatmodeX]
    }

    parent.debugLog("adjustThermostatMode: Parsed mode: ${vModeDesc}")
    sendEvent(name: "thermostatMode", value: vModeDesc)
	
}

def setThermostatMode(thermostatmodeX) {

  //adjustThermostatMode will be called by the apply status info method, called by the unit command method

}

def adjustThermostatOperatingState(thermostatModeX) {
	
    def operatingState
    if (thermostatModeX == "off") {
        operatingState = "idle"
    } else if (thermostatModeX == "heat") {
        operatingState = "heating"
    } else if (thermostatModeX == "cool") {
        operatingState = "cooling"
    } else if (thermostatModeX == "auto") {
        operatingState = "cooling"
    }
    
    parent.debugLog("adjustThermostatOperatingState: OperatingState: ${operatingState}")
    if (operatingState != null) {
        sendEvent(name: "adjustThermostatOperatingState", value: operatingState)
        if (operatingState != "idle") {
            sendEvent(name: "adjustLastRunningMode", value: thermostatModeX)
        }
    }
	
}

def on() { }

def off() { }

def auto() { }

def fanOn() { }

def cool() { }

def dry() { }

def heat() { }




