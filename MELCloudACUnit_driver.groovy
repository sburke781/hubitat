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
        
        attribute "unitId",                 "number"
        attribute "setTemperature",         "number"
        
        
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
        
        attribute "lastRunningMode",                "STRING"
        
        //MELCloud specific commands:
        command "on"
        
        //Thermostat capability commands
        /*
        command "auto"
        command "cool"
        command "emergencyHeat" //Currently unsupported in MELCloud
        command "fanAuto"       //Currently unsupported in MELCloud
        command "fanCirculate"  //Currently unsupported in MELCloud
        command "fanOn"         //Currently unsupported in MELCloud
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
        
	}

}


def refresh() {
  parent.debugLog("refresh: Refresh process called")
  // Retrieve current state information from MELCloud Service   
    getStatusInfo()
  initialize()
}

def initialize() {
    
    def fanModes = []
    def thermostatModes = []
    
    // Adjust default enumerations setup as part of the Hubitat Thermostat capability
    
    if(device.currentValue("HasAutomaticFanSpeed") == "true") { fanModes.add('auto') }
    if(device.currentValue("NumberOfFanSpeeds").toInteger() == 5) {
        fanModes.add('low')
        fanModes.add('low-medium')
        fanModes.add('medium')
        fanModes.add('medium-high')
        fanModes.add('high')
    }
    if(device.currentValue("NumberOfFanSpeeds").toInteger() == 3) {
        fanModes.add('low')
        fanModes.add('medium')
        fanModes.add('high')
    }
    if(device.currentValue("NumberOfFanSpeeds").toInteger() == 2) {
        fanModes.add('low')
        fanModes.add('high')
    }
    
    if(device.currentValue("CanHeat") == "true") { thermostatModes.add('heat') }
    if(device.currentValue("CanDry") == "true") { thermostatModes.add('dry') }
    if(device.currentValue("CanCool") == "true") { thermostatModes.add('cool') }
    thermostatModes.add('fan')
    thermostatModes.add('auto')
    thermostatModes.add('off')
    
    parent.debugLog("initialize: thermostatModes detected are ${thermostatModes}")
    parent.debugLog("initialize: fanModes detected are ${fanModes}")
    
    sendEvent(name: 'supportedThermostatModes', value: thermostatModes)
    sendEvent(name: 'supportedThermostatFanModes', value: fanModes)
    
    
    if ("${device.currentValue("coolingSetpoint")}" > 40) {
        sendEvent(name: "coolingSetpoint", value: "23.0")
    }
    
    if ("${device.currentValue("heatingSetpoint")}" > 40) {
        sendEvent(name: "heatingSetpoint", value: "23.0")
    }
    
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
    headers.put("X-MitsContextKey","${parent.getAuthCode()}")
    
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
            if ("${resp?.data?.Structure?.Devices?.Device.HasPendingCommand}".replace("[","").replace("]","") != null
               && "${resp?.data?.Structure?.Devices?.Device.HasPendingCommand}".replace("[","").replace("]","") != "true") {
                
                statusInfo.unitid   = "${resp?.data?.Structure?.Devices?.Device.DeviceID}".replace("[","").replace("]","")
                
                //Current Status Information
                statusInfo.power    = "${resp?.data?.Structure?.Devices?.Device.Power}".replace("[","").replace("]","")
                statusInfo.setmode  = "${resp?.data?.Structure?.Devices?.Device.OperationMode}".replace("[","").replace("]","").toInteger()
                statusInfo.roomtemp = "${resp?.data?.Structure?.Devices?.Device.RoomTemperature}".replace("[","").replace("]","")
                statusInfo.settemp  = "${resp?.data?.Structure?.Devices?.Device.SetTemperature}".replace("[","").replace("]","")
                statusInfo.setfan   = "${resp?.data?.Structure?.Devices?.Device.FanSpeed}".replace("[","").replace("]","").toInteger()

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

                
                parent.debugLog("getStatusInfo: Updating status for UnitId ${statusInfo.unitid}")
                applyResponseStatus(statusInfo)
            
            }
            else {
                parent.debugLog("getStatusInfo: There are pending commands, status will be updated when there are no pending commands.")
            }
            /*

Device Detail Examples and Notes

Power:true,
InStandbyMode:false,
RoomTemperature:21.0,
SetTemperature:21.0,
OperationMode:3,                         1 = Heating, 2 = Dry, 3 = Cooling, 7 = Fan, 8 = Auto
DefaultCoolingSetTemperature:21.0,
DefaultHeatingSetTemperature:null,

VaneVerticalSwing:false,          On "7"
VaneHorizontalSwing:false,        On "12"

VaneVerticalDirection:0,
VaneHorizontalDirection:0,

ActualFanSpeed:2,                      
FanSpeed:0,                            0 = auto, 1 = low, 2= low-medium, 3 = medium, 5 = medium-high, 6 = high

UnitSupportsStandbyMode:true,
NumberOfFanSpeeds:5,
AutomaticFanSpeed:true,               FanSpeed = 0
CanHeat:true,                         OperationMode = 1
CanDry:true,                          OperationMode = 2
CanCool:true,                         OperationMode = 3
HasAutomaticFanSpeed:true,
AirDirectionFunction:true,
SwingFunction:true,


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

DeviceID:<Integer>,
MacAddress:<ab:01:cd:02:ef:03>,
SerialNumber:<Integer>,
TimeZoneID:<Integer>,

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
    
    adjustFeatures(statusInfo)
    adjustSetTempRanges(statusInfo)
    
    adjustThermostatMode(statusInfo.setmode, statusInfo.power)
    adjustRoomTemperature(statusInfo.roomtemp)
    adjustSetTemperature(statusInfo.settemp)
    adjustThermostatFanMode(statusInfo.setfan)

    parent.debugLog("applyResponseStatus: Status update complete")
}

def adjustSetTempRanges(statusInfo) {
    parent.debugLog("adjustSetTempRanges: Status Info: ${statusInfo}")
    
    def minTempCoolDryValue     = convertTemperatureIfNeeded(statusInfo.minTempCoolDry.toFloat(),"c",1)
	def maxTempCoolDryValue     = convertTemperatureIfNeeded(statusInfo.maxTempCoolDry.toFloat(),"c",1)
    def minTempHeatValue        = convertTemperatureIfNeeded(statusInfo.minTempHeat.toFloat(),"c",1)
    def maxTempHeatValue        = convertTemperatureIfNeeded(statusInfo.maxTempHeat.toFloat(),"c",1)
    def minTempAutomaticValue   = convertTemperatureIfNeeded(statusInfo.minTempAutomatic.toFloat(),"c",1)
    def maxTempAutomaticValue   = convertTemperatureIfNeeded(statusInfo.maxTempAutomatic.toFloat(),"c",1)
    
    parent.debugLog("adjustSetTempRanges: Converted temperature ranges: minTempCoolDryValue = ${minTempCoolDryValue}, maxTempCoolDryValue = ${maxTempCoolDryValue}, minTempHeatValue = ${minTempHeatValue}, maxTempHeatValue = ${maxTempHeatValue}, minTempAutomaticValue = ${minTempAutomaticValue}, maxTempAutomatic = ${maxTempAutomaticValue}")
    
    sendEvent(name: "MinTempCool", value: minTempCoolDryValue)
    sendEvent(name: "MaxTempCool", value: maxTempCoolDryValue)
    sendEvent(name: "MinTempDry", value: minTempCoolDryValue)
    sendEvent(name: "MaxTempDry", value: maxTempCoolDryValue)
    sendEvent(name: "MinTempHeat", value: minTempHeatValue)
    sendEvent(name: "MaxTempHeat", value: maxTempHeatValue)
    sendEvent(name: "MinTempAuto", value: minTempAutomaticValue)
    sendEvent(name: "MaxTempAuto", value: maxTempAutomaticValue)
    
    parent.debugLog("adjustSetTempRanges: Temperature ranges updated")
}

def adjustFeatures(statusInfo) {
    parent.debugLog("adjustFeatures: Status Info: ${statusInfo}")
    
    sendEvent(name: "CanHeat", value: statusInfo.canHeat)
    sendEvent(name: "CanDry", value: statusInfo.canDry)
    sendEvent(name: "CanCool", value: statusInfo.canCool)
    sendEvent(name: "HasAutomaticFanSpeed", value: statusInfo.hasAutomaticFanSpeed)
    sendEvent(name: "NumberOfFanSpeeds", value: statusInfo.numberOfFanSpeeds)
    
    parent.debugLog("adjustFeatures: Features updated")
}

def unitCommand(command) {

    
    def statusInfo = [:]
    def bodyJson = "${command}"
    def headers = [:] 

    headers.put("X-MitsContextKey","${parent.getAuthCode()}")
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
            
            parent.debugLog("unitCommand: Status will be updated when there are no pending commands.")
            parent.debugLog("unitCommand: Initial data returned from SetAta: ${resp.data}")
            
            /*
            if ("${resp?.data?.HasPendingCommand}" != null && "${resp?.data?.HasPendingCommand}" != "true") {
             
                statusInfo.unitid   = "${resp?.data?.Structure?.Devices?.Device.DeviceID}".replace("[","").replace("]","")
                
                //Current Status Information
                statusInfo.power    = "${resp?.data?.Structure?.Devices?.Device.Power}".replace("[","").replace("]","")
                statusInfo.setmode  = "${resp?.data?.Structure?.Devices?.Device.OperationMode}".replace("[","").replace("]","").toInteger()
                statusInfo.roomtemp = "${resp?.data?.Structure?.Devices?.Device.RoomTemperature}".replace("[","").replace("]","")
                statusInfo.settemp  = "${resp?.data?.Structure?.Devices?.Device.SetTemperature}".replace("[","").replace("]","")
                statusInfo.setfan   = "${resp?.data?.Structure?.Devices?.Device.FanSpeed}".replace("[","").replace("]","").toInteger()

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
                
                parent.debugLog("unitCommand: Updating ${statusInfo.unitid}")  
            
                applyResponseStatus(statusInfo)            
            }
            else {
                parent.debugLog("unitCommand: There are pending commands, status will be updated when there are no pending commands.")
            }
            */
            
          }
    }   
	catch (Exception e) {
        parent.errorLog "unitCommand : Unable to query Mitsubishi Electric MELCloud: ${e}"
	}

}


//Unsupported commands from Thermostat capability
def emergencyHeat() { parent.debugLog("emergencyHeat: Not currently supported by MELCloud driver") }


//Fan Adjustments

def fanAuto() { parent.debugLog("fanAuto: Not currently supported by MELCloud driver") }

def fanCirculate() { parent.debugLog("fanAuto: Not currently supported by MELCloud driver") }

//fanOn - see modes section

//Scheduling Adjustments - Unsupported at present
def setSchedule(JSON_OBJECT) { parent.debugLog("setSchedule: Not currently supported by MELCloud driver") }

//Temperature Adjustments

def adjustCoolingSetpoint(temperature) {
 
    def coolingSetTempValue = convertTemperatureIfNeeded(temperature.toFloat(),"c",1)
	def currCoolingSetTempValue = convertTemperatureIfNeeded(device.currentValue("coolingSetpoint").toFloat(),"c",1)
    def currThermSetTempValue = convertTemperatureIfNeeded(device.currentValue("thermostatSetpoint").toFloat(),"c",1)
    
    parent.debugLog("adjustCoolingSetpoint: Current coolingSetpoint ${currCoolingSetTempValue}, Current ThermostatSetpoint = ${currThermSetTempValue}, New coolingSetpoint = ${coolingSetTempValue}")
    
    if (currCoolingSetTempValue != coolingSetTempValue) {
        sendEvent(name: "coolingSetpoint", value : coolingSetTempValue)
        parent.infoLog("Cooling Set Point adjusted to ${coolingSetTempValue} for ${device.label}")
    }
    
    if (currThermSetTempValue != coolingSetTempValue) {
        sendEvent(name: "thermostatSetpoint", value: coolingSetTempValue)
        parent.infoLog("Thermostat Set Point adjusted to ${coolingSetTempValue} for ${device.label}")
    }
    
}

def setCoolingSetpoint(temperature) {

    def correctedTemp = temperature
    
    parent.debugLog("setCoolingSetpoint: Setting Cooling Set Point to ${temperature}, current minimum ${device.currentValue("MinTempCool")}, current maximum ${device.currentValue("MaxTempCool")}")
    
    //Check allowable cooling temperature range and correct where necessary
    //Minimum
    if (temperature.toBigDecimal() < device.currentValue("MinTempCool").toBigDecimal()) {
        correctedTemp = device.currentValue("MinTempCool")
        parent.debugLog("setCoolingSetpoint: Temperature selected = ${temperature}, corrected to minimum cooling set point ${correctedTemp}")
    }
    
    //Maximum
    if (temperature.toBigDecimal() > device.currentValue("MaxTempCool").toBigDecimal()) {
        correctedTemp = device.currentValue("MaxTempCool")
        parent.debugLog("setCoolingSetpoint: Temperature selected = ${temperature}, corrected to maximum cooling set point ${correctedTemp}")
    }
        
    adjustCoolingSetpoint(correctedTemp)
    if (device.currentValue("thermostatOperatingState") == "cooling") {
        setTemperature(correctedTemp.toBigDecimal())
    }
}

def adjustHeatingSetpoint(temperature) {
    def heatingSetTempValue = convertTemperatureIfNeeded(temperature.toFloat(),"c",1)
	def currHeatingSetTempValue = convertTemperatureIfNeeded(device.currentValue("heatingSetpoint").toFloat(),"c",1)
    def currThermSetTempValue = convertTemperatureIfNeeded(device.currentValue("thermostatSetpoint").toFloat(),"c",1)
    
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

def setHeatingSetpoint(temperature) {

    def correctedTemp = temperature
    
    parent.debugLog("setHeatingSetpoint: Setting Heating Set Point to ${temperature}, current minimum ${device.currentValue("MinTempHeat")}, current maximum ${device.currentValue("MaxTempHeat")}")
    
    //Check allowable heating temperature range and correct where necessary
    //Minimum
    if (temperature.toBigDecimal() < device.currentValue("MinTempHeat").toBigDecimal()) {
        correctedTemp = device.currentValue("MinTempHeat")
        parent.debugLog("setHeatingSetpoint: Temperature selected = ${temperature}, corrected to minimum heating set point ${correctedTemp}")
    }
    
    //Maximum
    if (temperature.toBigDecimal() > device.currentValue("MaxTempHeat").toBigDecimal()) {
        correctedTemp = device.currentValue("MaxTempHeat")
        parent.debugLog("setHeatingSetpoint: Temperature selected = ${temperature}, corrected to maximum heating set point ${correctedTemp}")
    }
        
    adjustHeatingSetpoint(correctedTemp)
    if (device.currentValue("thermostatOperatingState") == "heating") {
        setTemperature(correctedTemp.toBigDecimal())
    }
}

def adjustSetTemperature(temperature) {

    def setTempValue = convertTemperatureIfNeeded(temperature.toFloat(),"c",1)
	def currentSetTempValue = convertTemperatureIfNeeded(device.currentValue("setTemperature").toFloat(),"c",1)
    def currentOperatingState = device.currentValue("thermostatOperatingState")
    
    parent.debugLog("adjustSetTemperature: Temperature passed in was ${temperature}, current set temperature is ${currentSetTempValue} and Operating State is ${currentOperatingState}")
    if (currentSetTempValue == null || currentSetTempValue != setTempValue) {
        parent.debugLog("adjustSetTemperature: Changing Set Temperature from ${currentSetTempValue} to ${setTempValue}")
    	sendEvent(name: "setTemperature", value: setTempValue)
        
        parent.debugLog("adjustSetTemperature: Checking if we are heating...")
        if (currentOperatingState == "heating") {
            parent.debugLog("adjustSetTemperature: Heating detected")
            adjustHeatingSetpoint(setTempValue)
        }
        
        parent.debugLog("adjustSetTemperature: Checking if we are cooling...")
        if (currentOperatingState == "cooling") {
            parent.debugLog("adjustSetTemperature: Cooling detected")
            adjustCoolingSetpoint(setTempValue)
        }
        
    }
    else { parent.debugLog("adjustSetTemperature: No action taken") }
}

def setTemperature(temperature) {
    
    if(device.currentValue("setTemperature") != temperature) {
        bodyJson = "{ \"SetTemperature\" : \"${temperature}\", \"EffectiveFlags\" : \"4\", \"DeviceID\" : \"${device.currentValue("unitId")}\",  \"HasPendingCommand\" : \"true\" }"
    
        parent.debugLog("setTemperature: Setting Temperature to ${temperature} for ${device.label}")
    
        unitCommand("${bodyJson}")
        parent.infoLog("setTemperature: Temperature adjusted to ${temperature} for ${device.label} (${device.currentValue("unitId")})")
    }
    else {
        parent.debugLog("setTemperature: No action taken")
    
    }

    
    //adjustSetTemperature will be called as a result of calling the unitCommand and applying the status updates that comes back
}

def adjustRoomTemperature(temperature) {

  def tempscaleUnit = "°${location.temperatureScale}"
  def roomtempValue = convertTemperatureIfNeeded(temperature.toFloat(),"c",1)	
  
  parent.debugLog("adjustRoomTemperature: Temperature provided = ${temperature}, Units = ${tempscaleUnit}, Converted Value = ${roomtempValue}")
  if (device.currentValue("temperature") == null || device.currentValue("temperature") != roomtempValue) {
      parent.debugLog("adjustRoomTemperature: updating room temperature from ${device.currentValue("temperature")} to ${roomtempValue}")
      sendEvent(name: "temperature", value: roomtempValue)
  }
  else { parent.debugLog("adjustRoomTemperature: No action taken") }
}

//Power and State Adjustments

def adjustThermostatFanMode(fanmode) {

    parent.debugLog("adjustThermostatFanMode: Adjusting Fan Mode to ${fanmode}")
    if (fanmode != null) {
        def fanModeValue = fanModeMap[fanmode]
        parent.debugLog("adjustThermostatFanMode: fanModeValue = ${fanModeValue}")
	    if (device.currentValue("thermostatFanMode") == null || device.currentValue("thermostatFanMode") != fanModeValue) {
    		sendEvent(name: "thermostatFanMode", value: fanModeValue)
            parent.infoLog("Fan Mode adjusted to ${fanModeValue}")
	    }
        else { parent.debugLog("adjustThermostatFanMode: No action taken") }
    }
}

def setThermostatFanMode(fanmode) {

    def fanModeKey = null
    if(fanmode.trim() == "auto")          fanModeKey = 0
    if(fanmode.trim() == "low")           fanModeKey = 1
    if(fanmode.trim() == "low-medium")    fanModeKey = 2
    if(fanmode.trim() == "medium")        fanModeKey = 3
    if(fanmode.trim() == "medium-high")   fanModeKey = 5
    if(fanmode.trim() == "high")          fanModeKey = 6
    
    adjustThermostatFanMode(fanModeKey)
    
    parent.debugLog("setThermostatFanMode: ${fanmode.trim()}, ${fanModeKey}")
    if(    fanModeKey != null &&
           (device.currentValue("thermostatFanMode") == null || device.currentValue("thermostatFanMode") != fanmode.trim())
      ) {
        bodyJson = "{ \"SetFanSpeed\" : \"${fanModeKey}\", \"EffectiveFlags\" : \"8\", \"DeviceID\" : \"${device.currentValue("unitId")}\",  \"HasPendingCommand\" : \"true\" }"
    
        parent.debugLog("setThermostatFanMode: Setting Fan Mode to ${fanmode.trim()} for ${device.label}")
        parent.debugLog("setThermostatFanMode: body = ${bodyJson}")
        unitCommand("${bodyJson}")
        parent.debugLog("setThermostatFanMode: Fan Mode set to ${fanmode.trim()} for ${device.label} (${device.currentValue("unitId")})")
        parent.infoLog("Fan Mode set to ${fanmode.trim()} for ${device.label} (${device.currentValue("unitId")})")
    }
    else {
        parent.debugLog("setThermostatFanMode: No change required for Fan Mode")
    
    }
}

def adjustThermostatMode(thermostatmodeX, power) {

    parent.debugLog("adjustThermostatMode: Adjusting Thermostat Mode to ${thermostatmodeX}, power passed in is ${power}")
	
    def vModeDesc = ""
    if (power == "q" || power == 0 || power == "false") {vModeDesc = "off"}
    else {
        vModeDesc = modeMap[thermostatmodeX]
    }

    parent.debugLog("adjustThermostatMode: Parsed mode: ${vModeDesc}")
    if (device.currentValue("thermostatMode") == null || device.currentValue("thermostatMode") != vModeDesc) {
    	sendEvent(name: "thermostatMode", value: vModeDesc)
    }
    adjustThermostatOperatingState(vModeDesc)
}

def setThermostatMode(thermostatmodeX) {

  parent.debugLog("setThermostatMode: Thermostat Mode passed in = ${thermostatmodeX}")
    
  def modeKey = null
    if(thermostatmodeX.trim() == "heat")   modeKey = 1
    if(thermostatmodeX.trim() == "dry")    modeKey = 2
    if(thermostatmodeX.trim() == "cool")   modeKey = 3
    if(thermostatmodeX.trim() == "fan")    modeKey = 7
    if(thermostatmodeX.trim() == "auto")   modeKey = 8
  
  def power = null
  if(thermostatmodeX.trim() == "off") power = 0
  else power = 1
  adjustThermostatMode(modeKey, power)
  
  if (thermostatmodeX == "off") { off() }
  else if (thermostatmodeX == "heat") { heat() }
  else if (thermostatmodeX == "dry") { dry() }
  else if (thermostatmodeX == "cool") { cool() }
  else if (thermostatmodeX == "fan") { fan() }
  else if (thermostatmodeX == "auto") { auto() }
  
  parent.debugLog("setThermostatMode: Thermostat Mode set")
  
}

def adjustThermostatOperatingState(thermostatModeX) {
	
    def operatingState
    if (thermostatModeX == "off" ) {
        operatingState = "idle"
    } else if (thermostatModeX == "heat") {
        operatingState = "heating"
    } else if (thermostatModeX == "cool") {
        operatingState = "cooling"
    } else if (thermostatModeX == "auto") {
        operatingState = "cooling"
    }
    
    parent.debugLog("adjustThermostatOperatingState: Thermostat Mode passed in = ${thermostatModeX}, OperatingState: ${operatingState}")
    if (operatingState != null && (device.currentValue("thermostatOperatingState") == null || device.currentValue("thermostatOperatingState") != operatingState)) {
        sendEvent(name: "thermostatOperatingState", value: operatingState)
        if (operatingState != "idle" && (device.currentValue("lastRunningMode") == null || device.currentValue("lastRunningMode") != thermostatModeX)) {
            sendEvent(name: "lastRunningMode", value: thermostatModeX)
        }
    }
	
}

def on() {

    bodyJson = "{ \"Power\" : \"true\", \"EffectiveFlags\" : \"1\", \"DeviceID\" : \"${device.currentValue("unitId")}\",  \"HasPendingCommand\" : \"true\" }"
    
    parent.debugLog("on: Turning ON device ${device.label} (${device.currentValue("unitId")})")
    unitCommand("${bodyJson}")
    parent.infoLog("Power turned ON for ${device.label} (${device.currentValue("unitId")})")

}

def off() {

    bodyJson = "{ \"Power\" : \"false\", \"EffectiveFlags\" : \"1\", \"DeviceID\" : \"${device.currentValue("unitId")}\",  \"HasPendingCommand\" : \"true\" }"
    
    parent.debugLog("off: Turning OFF device ${device.label} (${device.currentValue("unitId")})")
    unitCommand("${bodyJson}")
    parent.infoLog("Power turned OFF for ${device.label} (${device.currentValue("unitId")})")

}

def auto() {

    bodyJson = "{ \"Power\" : \"true\", \"operationMode\" : \"8\", \"EffectiveFlags\" : \"3\", \"DeviceID\" : \"${device.currentValue("unitId")}\",  \"HasPendingCommand\" : \"true\" }"
    
    parent.debugLog("auto: Changing operating mode to AUTO for ${device.label} (${device.currentValue("unitId")})")
    unitCommand("${bodyJson}")
    parent.infoLog("Operating mode changed to AUTO for ${device.label} (${device.currentValue("unitId")})")

}

def fanOn() { parent.debugLog("fanOn: Not currently supported by MELCloud driver") }

def cool() {

    bodyJson = "{ \"Power\" : \"true\", \"operationMode\" : \"3\", \"EffectiveFlags\" : \"3\", \"DeviceID\" : \"${device.currentValue("unitId")}\",  \"HasPendingCommand\" : \"true\" }"
    
    parent.debugLog("cool: Changing operating mode to COOL for ${device.label} (${device.currentValue("unitId")})")
    unitCommand("${bodyJson}")
    parent.infoLog("Operating mode changed to COOL for ${device.label} (${device.currentValue("unitId")})")

}

def dry() {

    bodyJson = "{ \"Power\" : \"true\", \"operationMode\" : \"2\", \"EffectiveFlags\" : \"3\", \"DeviceID\" : \"${device.currentValue("unitId")}\",  \"HasPendingCommand\" : \"true\" }"
    
    parent.debugLog("dry: Changing operating mode to DRY for ${device.label} (${device.currentValue("unitId")})")
    unitCommand("${bodyJson}")
    parent.infoLog("Operating mode changed to DRY for ${device.label} (${device.currentValue("unitId")})")


}

def heat() {

    bodyJson = "{ \"Power\" : \"true\", \"operationMode\" : \"1\", \"EffectiveFlags\" : \"3\", \"DeviceID\" : \"${device.currentValue("unitId")}\",  \"HasPendingCommand\" : \"true\" }"
    
    parent.debugLog("heat: Changing operating mode to HEAT for ${device.label} (${device.currentValue("unitId")})")
    unitCommand("${bodyJson}")
    parent.infoLog("heat: Operating mode changed to HEAT for ${device.label} (${device.currentValue("unitId")})")

}



