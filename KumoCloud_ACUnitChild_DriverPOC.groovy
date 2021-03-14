/**
 *  Mitsubishi Electric KumoCloud AC Unit POC Child Driver
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
 *    2021-03-14  Simon Burke    Original Creation
 * 
 */
import java.text.DecimalFormat;

metadata {
	definition (name: "KumoCloud AC Unit", namespace: "simnet", author: "Simon Burke") {
        capability "Refresh"
        capability "Thermostat"
        capability "FanControl"

preferences {
		input(name: "AutoStatusPolling", type: "bool", title:"Automatic Status Polling", description: "Enable / Disable automatic polling of unit status from KumoCloud", defaultValue: true, required: true, displayDuringSetup: true)
        input(name: "StatusPollingInterval", type: "ENUM", multiple: false, options: ["20", "30", "60", "300"], title:"Status Polling Interval", description: "Number of seconds between automatic status updates", defaultValue: 30, required: true, displayDuringSetup: true)		
		input(name: "FansTextOrNumbers", type: "bool", title: "Famode Modes: Text or Numbers?", description: "Use HE Text Fan Modes or Numbers?", defaultValue: true, required: true, displayDuringSetup: true)
        
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
        
        
        //FanControl capability commands
        /*
        setSpeed(fanspeed)
            fanspeed required (ENUM) - Fan speed to set

        */
	}

}


def refresh() {
  parent.debugLog("refresh: Refresh process called")
  // Retrieve current state information from MELCloud Service   
  getStatusInfo()
  initialize()
}

def updated() {

    setFanModes()
    setThermostatModes()
    parent.debugLog("updated: AutoStatusPolling = ${AutoStatusPolling}, StatusPollingInterval = ${StatusPollingInterval}")
    updateStatusPolling()    
}

def setFanModes()
{
    def fanModes = []
    
    fanModes.add('Off')
    
    //Text or Numbers?
    if (FansTextOrNumbers == true) {
        parent.debugLog("setFanModes:Text-based Fan Modes")
        if(device.currentValue("NumberOfFanSpeeds").toInteger() == 3) {
            fanModes.add('Low')
            fanModes.add('Medium')
            fanModes.add('High')
        }
        else if(device.currentValue("NumberOfFanSpeeds").toInteger() == 2) {
            fanModes.add('Low')
            fanModes.add('High')
        }
        else
        {
        //if(device.currentValue("NumberOfFanSpeeds").toInteger() == 5) {
            fanModes.add('Low')
            fanModes.add('Medium Low')
            fanModes.add('Medium')
            fanModes.add('Medium High')
            fanModes.add('High')
        }

    }
    else {
        parent.debugLog("setFanModes:Number-based Fan Modes")
        if(device.currentValue("NumberOfFanSpeeds").toInteger() == 3) {
            fanModes.add('1')
            fanModes.add('2')
            fanModes.add('3')
        }
        else if(device.currentValue("NumberOfFanSpeeds").toInteger() == 2) {
            fanModes.add('1')
            fanModes.add('2')
        }
        else
        {
        //if(device.currentValue("NumberOfFanSpeeds").toInteger() == 5) {
            fanModes.add('1')
            fanModes.add('2')
            fanModes.add('3')
            fanModes.add('4')
            fanModes.add('5')
        }
    }
    
    if(device.currentValue("HasAutomaticFanSpeed") == "true") {
        fanModes.add('Auto')
    }
    
    fanModes.add('On')
    
    parent.debugLog("setFanModes: fanModes detected are ${fanModes}")
    //Apply settings
    sendEvent(name: 'supportedThermostatFanModes', value: fanModes)
}

def setThermostatModes()
{
    def thermostatModes = []
    
    if(device.currentValue("CanHeat") == "true") { thermostatModes.add('heat') }
    if(device.currentValue("CanDry") == "true") { thermostatModes.add('dry') }
    if(device.currentValue("CanCool") == "true") { thermostatModes.add('cool') }
    
    thermostatModes.add('fan')
    thermostatModes.add('auto')
    thermostatModes.add('off')
    
    parent.debugLog("setThermostatModes: thermostatModes detected are ${thermostatModes}")
    sendEvent(name: 'supportedThermostatModes', value: thermostatModes)
    
        
}

def initialize() {
    
    setFanModes()
    setThermostatModes()
    
    //Need to account for Fahrenheit here eventually...
    //if ("${device.currentValue("coolingSetpoint")}" > 40) {
    //    sendEvent(name: "coolingSetpoint", value: "23.0")
    //}
    
    //if ("${device.currentValue("heatingSetpoint")}" > 40) {
    //    sendEvent(name: "heatingSetpoint", value: "23.0")
    //}
}

def getFanModeMap() {
    
    if (FansTextOrNumbers == true) {
        [
            0:"Auto",
            1:"Low",
            2:"Medium  Low",
            3:"Medium",
            4:"Medium High",
            5:"High"
        ]
    }
    else {
        [
            0:"auto",
            1:"1",
            2:"2",
            3:"3",
            4:"4",
            5:"5"
        ]
    }
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
    //To be added for Kumo
    parent.debugLog("getStatusInfo: method called, yet to be implemented.")
}




def applyResponseStatus(statusInfo) {
    
    //To be added for Kumo
    parent.debugLog("applyResponseStatus: method called, yet to be implemented.")
    
}

def adjustSetTempRanges(statusInfo) {
  
    parent.debugLog("adjustSetTempRanges: method called, yet to be implemented.")
    
}

def adjustFeatures(statusInfo) {
    parent.debugLog("adjustFeatures: method called, yet to be implemented.")
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
            
          }
    }   
	catch (Exception e) {
        parent.errorLog "unitCommand : Unable to query Mitsubishi Electric MELCloud: ${e}"
	}

}


//Unsupported commands from Thermostat capability
def emergencyHeat() { parent.debugLog("emergencyHeat: Not currently supported by KumoCloud driver") }


//Fan Adjustments

def fanAuto() { parent.debugLog("fanAuto: Not currently supported by KumoCloud driver") }

def fanCirculate() { parent.debugLog("fanAuto: Not currently supported by KumoCloud driver") }

//fanOn - see modes section

//Scheduling Adjustments - Unsupported at present
def setSchedule(JSON_OBJECT) { parent.debugLog("setSchedule: Not currently supported by KumoCloud driver") }

//Temperature Adjustments

def adjustCoolingSetpoint(givenTemp) {
 
    //To be added for Kumo
    parent.debugLog("adjustCoolingSetpoint: method called, yet to be implemented.")
    
}

def setCoolingSetpoint(givenTemp) {

    //To be added for Kumo
    parent.debugLog("setCoolingSetpoint: method called, yet to be implemented.")
}

def adjustHeatingSetpoint(givenTemp) {
    //To be added for Kumo
    parent.debugLog("adjustHeatingSetpoint: method called, yet to be implemented.")
    
}

def setHeatingSetpoint(givenTemp) {

    //To be added for Kumo
    parent.debugLog("setHeatingSetpoint: method called, yet to be implemented.")
}

def adjustSetTemperature(givenSetTemp) {

    //To be added for Kumo
    parent.debugLog("adjustSetTemperature: method called, yet to be implemented.")
}

def setTemperature(givenSetTemp) {
    
    def setTempValue = givenSetTemp.toFloat()
        
    def bodyJson = getUnitCommandBody( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,device.currentValue("thermostatMode")
                                      ,setTempValue
                                     )
        
    parent.debugLog("setTemperature: Setting Temperature to ${setTempValue} for ${device.label}")
    parent.debugLog("setTemperature: Body JSON = ${bodyJson}")
        
    //adjustSetTemperature(setTempValue)
    unitCommand("${bodyJson}")
        
    parent.infoLog("Temperature adjusted to ${setTempValue} for ${device.label}")
    
}

def adjustRoomTemperature(givenTemp) {

  //To be added for Kumo
    parent.debugLog("adjustRoomTemperature: method called, yet to be implemented.")
}

//Power and State Adjustments

def convertFanModeToKey(fanmode) {
 
    def fanModeKey = null
    
    //Convert Fan Mode selected, accounting for number / text based fan modes and differences in case of text-based 
    //   modes passed back from Thermostat tile vs Fan tile.
    if(fanmode.trim() == "auto"                                  || fanmode.trim() == "Auto")         fanModeKey = 0
    if(fanmode.trim() == "1" || fanmode.trim() == "low"          || fanmode.trim() == "Low")          fanModeKey = 1
    if(fanmode.trim() == "2" || fanmode.trim() == "medium-low"   || fanmode.trim() == "Medium Low")   fanModeKey = 2
    if(fanmode.trim() == "3" || fanmode.trim() == "medium"       || fanmode.trim() == "Medium")       fanModeKey = 3
    if(fanmode.trim() == "4" || fanmode.trim() == "medium-high"  || fanmode.trim() == "Medium High")  fanModeKey = 4
    if(fanmode.trim() == "5" || fanmode.trim() == "high"         || fanmode.trim() == "High")         fanModeKey = 5
    
    return fanModeKey
}

def getUnitCommandBody(givenPower, givenFanMode, givenOpMode, givenSetTemp) {
 
    def bodyJson = null
    def fanModeKey = null
    def fanModeText = null
    def modeKey = null
    def modeText = null
    def setTempText = null

    /*
       #EffectiveFlags:
        #Power:                0x01
        #OperationMode:        0x02
        #Temperature:        0x04
        #FanSpeed:            0x08
        #VaneVertical:        0x10
        #VaneHorizontal:    0x100
    */
    
    
    //Lookup the fan mode key for MEL based on fan mode provided
    fanModeKey = convertFanModeToKey(givenFanMode)
    
    //Compile the text to add to the unit command for the fan mode
    if (fanModeKey != null) {fanModeText = "\"SetFanSpeed\" : \"${fanModeKey}\"," }
    else {fanModeText = ""}
    
    //Lookup the operating mode key for MEL based on mode provided
    modeKey = convertThermostatModeToKey(givenOpMode)
        
    if (modeKey != null) {modeText = "\"OperationMode\" : \"${modeKey}\"," }
    else {modeText = ""}
    
    setTempText = "\"SetTemperature\" : \"${givenSetTemp}\","
    
    bodyJSON = "{ \"Power\" : \"${givenPower}\", ${modeText} ${setTempText} ${fanModeText} \"EffectiveFlags\" : \"15\", \"DeviceID\" : \"${device.currentValue("unitId")}\",  \"HasPendingCommand\" : \"true\" }"
    
    return "${bodyJSON}"
    
}

def adjustThermostatFanMode(givenFanModeKey) {

    //To be added for Kumo
    parent.debugLog("adjustThermostatFanMode: method called, yet to be implemented.")
}

def setThermostatFanMode(fanmode) {

    def bodyJson = getUnitCommandBody( true //Power
                                      ,fanmode
                                      ,device.currentValue("thermostatMode")
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    
    
    parent.debugLog("setThermostatFanMode: ${fanmode.trim()}, ${fanModeKey}")
    
    if (bodyJSON != null) {
        adjustThermostatFanMode(convertFanModeToKey(fanmode))
    
        if(    device.currentValue("thermostatFanMode") == null || device.currentValue("thermostatFanMode") != fanmode.trim())
           {
            parent.debugLog("setThermostatFanMode: Setting Fan Mode to ${fanmode.trim()} for ${device.label}")
            parent.debugLog("setThermostatFanMode: body = ${bodyJson}")
            unitCommand("${bodyJson}")
            parent.debugLog("setThermostatFanMode: Fan Mode set to ${fanmode.trim()} for ${device.label} (${device.currentValue("unitId")})")
            parent.infoLog("Fan Mode set to ${fanmode.trim()} for ${device.label} (${device.currentValue("unitId")})")
        }
        else { parent.debugLog("setThermostatFanMode: No action taken")  }
        
    
        }
    else { parent.warnLog("setThermostatFanMode: Warning - Fan Mode not identified") }
}

//Fan Speed method from the Fan Control capability
//  Simply calling the Fan Mode method that is part of the Thermostat capability 
def setSpeed(fanspeed) { setThermostatFanMode(fanspeed) }

def adjustThermostatMode(thermostatmodeX, power) {

    //To be added for Kumo
    parent.debugLog("adjustThermostatMode: method called, yet to be implemented.")
}

def convertThermostatModeToKey(thermostatmodeX) {
    
    def modeKey = null
    if(thermostatmodeX.trim() == "heat")   modeKey = 1
    if(thermostatmodeX.trim() == "dry")    modeKey = 2
    if(thermostatmodeX.trim() == "cool")   modeKey = 3
    if(thermostatmodeX.trim() == "fan")    modeKey = 7
    if(thermostatmodeX.trim() == "auto")   modeKey = 8
    
    return modeKey
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
	
    //To be added for Kumo
    parent.debugLog("adjustThermostatOperatingState: method called, yet to be implemented.") 
    
}

def on() {

    def bodyJson = getUnitCommandBody( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,device.currentValue("thermostatMode")
                                      ,device.currentValue("thermostatSetpoint")
                                     )
       
    parent.debugLog("on: Turning ON device ${device.label} (${device.currentValue("unitId")})")
    parent.debugLog("auto: Body = ${bodyJson}")
    unitCommand("${bodyJson}")
    parent.infoLog("Power turned ON for ${device.label} (${device.currentValue("unitId")})")

}

def off() {

    def bodyJson = getUnitCommandBody( false //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,device.currentValue("thermostatMode")
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    
    
    parent.debugLog("off: Turning OFF device ${device.label} (${device.currentValue("unitId")})")
    parent.debugLog("auto: Body = ${bodyJson}")
    unitCommand("${bodyJson}")
    parent.infoLog("Power turned OFF for ${device.label} (${device.currentValue("unitId")})")

}

def auto() {

    def bodyJson = getUnitCommandBody( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,"auto" //thermostatMode
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    
    
    parent.debugLog("auto: Changing operating mode to AUTO for ${device.label} (${device.currentValue("unitId")})")
    parent.debugLog("auto: Body = ${bodyJson}")
    unitCommand("${bodyJson}")
    parent.infoLog("Operating mode changed to AUTO for ${device.label} (${device.currentValue("unitId")})")

}

def fanOn() { parent.debugLog("fanOn: Not currently supported by MELCloud driver") }

def fan() {
    
    def bodyJson = getUnitCommandBody( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,"fan" //thermostatMode
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    //bodyJson = "{ \"Power\" : \"true\", \"operationMode\" : \"7\", \"EffectiveFlags\" : \"3\", \"DeviceID\" : \"${device.currentValue("unitId")}\",  \"HasPendingCommand\" : \"true\" }"

    parent.debugLog("fan: Changing operating mode to FAN for ${device.label} (${device.currentValue("unitId")})")
    parent.debugLog("auto: Body = ${bodyJson}")
    unitCommand("${bodyJson}")
    parent.infoLog("fan: Operating mode changed to FAN for ${device.label} (${device.currentValue("unitId")})")   
    
}

def cool() {

    def bodyJson = getUnitCommandBody( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,"cool" //thermostatMode
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    
    adjustCoolingSetpoint(device.currentValue("thermostatSetpoint"))
    
    parent.debugLog("cool: Changing operating mode to COOL for ${device.label} (${device.currentValue("unitId")})")
    parent.debugLog("auto: Body = ${bodyJson}")
    unitCommand("${bodyJson}")
    parent.infoLog("Operating mode changed to COOL for ${device.label} (${device.currentValue("unitId")})")

}

def dry() {

    def bodyJson = getUnitCommandBody( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,"dry" //thermostatMode
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    
    adjustCoolingSetpoint(device.currentValue("thermostatSetpoint"))
    
    parent.debugLog("dry: Changing operating mode to DRY for ${device.label} (${device.currentValue("unitId")})")
    parent.debugLog("auto: Body = ${bodyJson}")
    unitCommand("${bodyJson}")
    parent.infoLog("Operating mode changed to DRY for ${device.label} (${device.currentValue("unitId")})")


}

def heat() {

    def bodyJson = getUnitCommandBody( true //Power
                                      ,device.currentValue("thermostatFanMode")
                                      ,"heat" //thermostatMode
                                      ,device.currentValue("thermostatSetpoint")
                                     )
    
    adjustHeatingSetpoint(device.currentValue("thermostatSetpoint"))
    
    parent.debugLog("heat: Changing operating mode to HEAT for ${device.label} (${device.currentValue("unitId")})")
    parent.debugLog("auto: Body = ${bodyJson}")
    unitCommand("${bodyJson}")
    parent.infoLog("heat: Operating mode changed to HEAT for ${device.label} (${device.currentValue("unitId")})")

}

def getSchedule() { }

def updateStatusPolling() {

   def sched
   parent.debugLog("updateStatusPolling: Updating Status Polling called, about to unschedule refresh")
   unschedule("refresh")
   parent.debugLog("updateStatusPolling: Unscheduleing refresh complete")
   
   if(AutoStatusPolling == true) {
       
       sched = "2/${StatusPollingInterval} * * ? * * *"
       parent.debugLog("updateStatusPolling: Setting up schedule with settings: schedule(\"${sched}\",refresh)")
       try{
           
           schedule("${sched}","refresh")
       }
       catch(Exception e) {
           parent.debugLog("updateStatusPolling: Error - " + e)
       }
       
       parent.debugLog("updateStatusPolling: Scheduled refresh set")
   }
   else { parent.debugLog("updateStatusPolling: Automatic status polling disabled")  }
}



def checkNull(value, alternative) {
 
    if(value == null) { return alternative }
    return value
    
}
