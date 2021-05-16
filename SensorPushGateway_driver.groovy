/**
 *  SensorPush Gateway
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
 *    2019-12-04  Simon Burke    Original Creation
 *    2020-03-29  Simon Burke    Adjusted logging
 *                                Commented out some logging to remove noise
 *                                Adjusted some error logging to report as errors instead of debug logs
 *                                Intention is to eventually include logging preference switches
 *    2021-02-20  Simon Burke    Split runCmd method to reduce the number of API calls and consumption of CPU:
 *                                getAuthToken - Retrieves Authorisation token from SensorPush
 *                                getAccessToken - Retrieves Access token from SensorPush
 *                                sensors - Calls getAccessToken, retrieves the list of sensors
 *                                            and creates new child devices if required
 *                                samples - Calls getAccessToken, retrieves latest sample for each sensor
 *                                            , calls sensors if new sensor is detected
 *                                            , updates each child sensor device with latest reading
 *   2021-02-27  Simon Burke	Updated samples method, replacing code to always convert to degrees C with
 *					a call to convertTemperatureIfNeeded to convert temperature readings
 *					based on HE hub temperature scale setting, including before and after
 *					debug logging for the temperature conversion
 *				Minor adjustments to some logging to include correct method references
 *				Adjusted notes on HE Community thread to include step to accept terms and
 *					conditions on SensorPuish Dashboard web page - thanks @minardisucks-insteon
 *  2021-04-17  Simon Burke	Updated automatic polling to correct the use of the polling interval preference
 *					setting.  It was only impacting the second within the minute when the
 *					polling occurred, now it correctly impacts the minute in the hour when
 *					polling occurs
 *				Removed redundant attributes lastUpdatedSource and lastUpdatedHE
 * 2021-05-16  Simon Burke	Additional fix to CRON setup
 * 
 */
metadata {
	definition (name: "SensorPush Gateway", namespace: "simnet", author: "Simon Burke") {
        capability "Temperature Measurement"
        capability "Sensor"
        capability "Refresh"
        attribute "spAuthCode", "string"
        attribute "spAccessToken", "string"
        
        command "getAuthToken"
	}

	preferences {
		input(name: "spBaseURL", type: "string", title:"SensorPush Base URL", description: "Enter the base URL for the SensorPush Cloud Service", defaultValue: "https://api.sensorpush.com/api/v1", required: true, displayDuringSetup: true)
		
		input(name: "UserName", type: "string", title:"SensorPush Username / Email", description: "Username / Email used to authenticate on SensorPush cloud", displayDuringSetup: true)
		input(name: "Password", type: "password", title:"SensorPush Account Password", description: "Password for authenticating on SensorPush cloud", displayDuringSetup: true)
        
        input(name: "AutoSensorPolling", type: "bool", title:"Automatic Sensor Polling", description: "Enable / Disable automatic polling of sensors from SensorPush", defaultValue: true, required: true, displayDuringSetup: true)
        input(name: "SensorPollingInterval", type: "string", title:"Sensor Polling Interval", description: "Number of minutes between automatic sensor updates", defaultValue: 1, required: true, displayDuringSetup: true)		
        
        input(name: "DebugLogging", type: "bool", title:"Enable Debug Logging", displayDuringSetup: true, defaultValue: false)
        input(name: "WarnLogging", type: "bool", title:"Enable Warning Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "ErrorLogging", type: "bool", title:"Enable Error Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "InfoLogging", type: "bool", title:"Enable Description Text (Info) Logging", displayDuringSetup: true, defaultValue: false)
    }
    
    

}


def updated() {

    debugLog("updated: AutoStatusPolling = ${AutoSensorPolling}, StatusPollingInterval = ${SensorPollingInterval}")
    updateSensorPolling()    
}

def getSchedule() { }

def updateSensorPolling() {

   def sched
   debugLog("updateSensorPolling: Updating Sensor Polling called, about to unschedule refresh")
   unschedule("refresh")
   debugLog("updateSensorPolling: Unscheduleing refresh complete")
   
   if(AutoSensorPolling == true) {
       
       sched = "0 0/${SensorPollingInterval} * ? * * *"
       
       debugLog("updateSensorPolling: Setting up schedule with settings: schedule(\"${sched}\",refresh)")
       try{
           
           schedule("${sched}","refresh")
       }
       catch(Exception e) {
           errorLog("updateSensorPolling: Error - " + e)
       }
       
       infoLog("updateSensorPolling: Scheduled refresh set")
   }
   else { infoLog("updateSensorPolling: Automatic sensor polling disabled")  }
}




def refresh() {
 debugLog("refresh: running samples()")
 samples()   
 debugLog("refresh: samples() complete")
}

def samples() {

    def bodyJson = ""
    def postParams = [:]
    def headers = [:]
    getAccessToken()
    headers.put("accept", "application/json")
    headers.put("Authorization", device.currentValue("spAccessToken", true))
    bodyJson = "{ \"limit\": 1 }"
    postParams = [
		uri: "${spBaseURL}/samples",
        headers: headers,
        contentType: "application/json",
        requestContentType: "application/json",
		body : bodyJson
	]
           
	try {
        httpPost(postParams)
        { resp -> 
            //log.debug("samplesTest: resp = ${resp.data}")
            resp?.data?.sensors?.each { sensor ->
                                        
                        def tempStr = (String)(sensor.value.temperature)
                        tempStr = tempStr.replace("[","").replace("]","")
                        debugLog("samples: Temp String = ${tempStr}")
                        def temperature = convertTemperatureIfNeeded(tempStr.toFloat(),"F",1)
                        debugLog("samples: Converted Temperature = ${temperature}")
                        
                        def childTempDevice = findChildDevice(sensor.key, "Temperature")
    
                        if (childTempDevice == null) {
                            //Could not find sensor, run the sensors method to create any new sensor child devices
                            sensors()
                            //Attempt to do the lookup again
                            childTempDevice = findChildDevice(sensor.key, "Temperature")
                        }
                        
                        if (childTempDevice == null) {
                            //Still could not find the device
                            errorLog("samples: Lookup of newly created sensor failed... ${sensor.key}, Temperature")                         
                        }
                        else {
                            childTempDevice.sendEvent(name: "temperature", value: temperature.toString())
                        }
                        
                        def humidity = (String)(sensor.value.humidity)
                        humidity = humidity.replace("[","").replace("]","")
                        
                        def childHumDevice = findChildDevice(sensor.key, "Humidity")
    
                        if (childHumDevice == null) {
                            //Could not find sensor, run the sensors method to create any new sensor child devices
                            sensors()
                            //Attempt to do the lookup again
                            childHumDevice = findChildDevice(sensor.key, "Humidity")
                        }
                        
                        if (childHumDevice == null) {
                            //Still could not find the device
                            errorLog("samples: Lookup of newly created sensor failed... ${sensor.key}, Humidity")                         
                        }
                        else {
                            childHumDevice.sendEvent(name: "humidity", value: humidity)
                        }
                       
            
            }
        }
    }
    catch(Exception e)
    {
        errorLog("samples: Exception ${e}")   
    }
    
}

def getAuthToken() {
 
    def bodyJson = ""
    def headers = [:] 
    def postParams = [:]
        
    headers.put("accept", "application/json")
    
    bodyJson = "{ \"email\": \"${UserName}\", \"password\": \"${Password}\" }"
    postParams = [
        uri: "${spBaseURL}/oauth/authorize",
        headers: headers,
        contentType: "application/json",
        requestContentType: "application/json",
		body : bodyJson
	]
           
	try {
        
        
        httpPost(postParams)
        { resp -> 

            sendEvent(name: "spAuthCode", value : resp.data.authorization)
            
        }
        
                
	}
	catch (Exception e) {
        errorLog("getAuthToken: Unable to query sensorpush cloud: ${e}")
	}
    
}


def getAccessToken(){
    
    def bodyJson = ""
    def headers = [:] 
    def postParams = [:]
        
    headers.put("accept", "application/json")
        
    bodyJson = "{ \"authorization\": \"${device.currentValue("spAuthCode", true)}\" }"
    postParams = [
		uri: "${spBaseURL}/oauth/accesstoken",
        headers: headers,
        contentType: "application/json",
        requestContentType: "application/json",
		body : bodyJson
	]
           
	try {
        //log.debug("Requesting SensorPush Authorization Code")
        
        httpPost(postParams)
        { resp -> 

            sendEvent(name: "spAccessToken", value : resp.data.accesstoken)
            //log.debug("getAccessToken: Access Token = ${resp.data.accesstoken}")
        }
        
                
	}
	catch (Exception e) {
        errorLog("getAccessToken: Unable to query sensorpush cloud: ${e}")
	}
    
}

def sensors() {

    
    debugLog("sensors: Sensors starting")
    def bodyJson = ""
    def postParams = [:]
    def headers = [:]
    getAccessToken()
    headers.put("accept", "application/json")
    headers.put("Authorization", device.currentValue("spAccessToken", true))
    bodyJson = "{ }"
    postParams = [
		uri: "${spBaseURL}/devices/sensors",
        headers: headers,
        contentType: "application/json",
        requestContentType: "application/json",
		body : bodyJson
	]
           
	try {
        httpPost(postParams)
        { resp -> 

            //sendEvent(name: "spAccessToken", value : resp.data.accesstoken)
            resp?.data?.each { it ->
            
                def childTempDevice = findChildDevice(it.value.id, "Temperature")
    
                if (childTempDevice == null) {
                    createSensor(it.value.id, it.value.name, "Temperature")
                    childTempDevice = findChildDevice(it.value.id, "Temperature")
                }
                        
                if (childTempDevice == null) {
                    errorLog("sensors: Lookup of newly created sensor failed... ${it.value.id}, ${it.value.name}, Temperature")                         
                }
                
                def childHumDevice = findChildDevice(it.value.id, "Humidity")
    
                if (childHumDevice == null) {
                    createSensor(it.value.id, it.value.name, "Humidity")
                    childHumDevice = findChildDevice(it.value.id, "Humidity")
                }
            }
            
        }
        
                
	}
	catch (Exception e) {
        errorLog("sensors: Unable to query sensorpush cloud whilst getting sensor data: ${e}")
	}
    debugLog("sensors: Sensors completed")
    
}

def deriveSensorDNI(sensorId, sensorType) {

    return "${device.deviceNetworkId}-id${sensorId}-type${sensorType}"
}

def findChildDevice(sensorId, sensorType) {
	getChildDevices()?.find { it.deviceNetworkId == deriveSensorDNI(sensorId, sensorType)}
}

def createSensor(sensorId, sensorName, sensorType) {
    debugLog("createSensor: Creating SensorPush Sensor: ${sensorId}, ${sensorName}, ${sensorType}")
    
	def childDevice = findChildDevice(sensorId, sensorType)
    
    if (childDevice == null) {
        childDevice = addChildDevice("hubitat", "Virtual ${sensorType} Sensor", deriveSensorDNI(sensorId, sensorType), [label: "${device.displayName} (${sensorType} Sensor - ${sensorName})", isComponent: false])
	}
    else {
      debugLog("createSensor: child device ${childDevice.deviceNetworkId} already exists")
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
