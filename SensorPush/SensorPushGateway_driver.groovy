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
 *
 * 2021-05-16  Simon Burke	Additional fix to CRON setup
 * 2021-12-28  Simon Burke  Changed HTTP calls to be asynchronous
 * 2021-12-31  Simon Burke  Fix for null json returned in samples (included callback method in getAccessToken())
 *                          Made same fix for sensors to include callback method in async call
 * 2022-02-02  Simon Burke  Added ignore SSL issues to HTTP calls after certificate appears to be signed by any untrusted party
 * 2024-10-19  Simon Burke  Removed State Change = True from sendEvent calls to reduce noise in Event history
 *                          Added checks before submitting temperature and humidity events to reduce changes in Last Activity to improve device monitoring
 * 2024-10-20  Simon Burke  Minor code improvements - variable typing, additional debug logs, re-arranging code
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


void updated() {

    debugLog("updated: AutoStatusPolling = ${AutoSensorPolling}, StatusPollingInterval = ${SensorPollingInterval}")
    updateSensorPolling()    
}

def getSchedule() { }

void updateSensorPolling() {

   String sched = "";
   debugLog("updateSensorPolling: Updating Sensor Polling called, about to unschedule refresh");
   unschedule("refresh");
   debugLog("updateSensorPolling: Unscheduleing refresh complete");
   
   if(AutoSensorPolling == true) {
       
       sched = "0 0/${SensorPollingInterval} * ? * * *";
       
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




void refresh() {
 debugLog("refresh: running samples()")
 samples()   
 debugLog("refresh: samples() complete")
}

void samples() {

    String bodyJson = ""
    Map headers = [:] 
    Map postParams = [:]
    
    getAccessToken()
    headers.put("accept", "application/json")
    headers.put("Authorization", device.currentValue("spAccessToken", true))
    bodyJson = "{ \"limit\": 1 }"  // Only get the latest reading for each sensor
    postParams = [
		uri: "${spBaseURL}/samples",
        headers: headers,
        contentType: "application/json",
        requestContentType: "application/json",
		body : bodyJson,
        ignoreSSLIssues: true
	]
           
	try {
        asynchttpPost('samplesCallback', postParams);
    }
    catch(Exception e)
    {
        errorLog("samples: Exception ${e}")   
    }
    
}

void samplesCallback(resp, data) {
  debugLog("samplesCallback: Response status = ${resp.getStatus()}");
  if (resp.getStatus() == 200) {
    resp?.getJson().sensors?.each { sensor ->
      
      /*** Temperature Reading ***/
              
      com.hubitat.app.DeviceWrapper childTempDevice = findChildDevice(sensor.key, "Temperature")
    
      if (childTempDevice == null) {
        //Could not find sensor, run the sensors method to create any new sensor child devices
        sensors()
        //Attempt to do the lookup again
        childTempDevice = findChildDevice(sensor.key, "Temperature")
      }
                        
      if (childTempDevice == null) {
        //Still could not find the device
        errorLog("samplesCallback: Lookup of newly created sensor failed... ${sensor.key}, Temperature")                         
      }
      else {
        String tempStr = (String)(sensor.value.temperature);
        tempStr = tempStr.replace("[","").replace("]","");
        String temperature = convertTemperatureIfNeeded(tempStr.toFloat(),"F",1);
        debugLog("samplesCallback: ${childTempDevice.displayName} - Current reading = ${childTempDevice.currentValue("temperature", true)}, reading received = ${tempStr}, converted reading = ${temperature}");
                            
        //Check if the reading has actually changed, if it has, record an event
        //   The aim here is to minimise the device events that could trigger automations and minimise the changes to the device's Last Activity value to improve accuracy in monitoring device health
        if (childTempDevice.currentValue("temperature", true) == null || childTempDevice.currentValue("temperature", true).toString() != temperature) {
          Map map = [:];
                                
          map.name            = "temperature";
          map.value           = temperature.toString();
          map.unit            = "°" + getTemperatureScale();
          map.descriptionText = "${childTempDevice.displayName}: temperature is ${map.value}${map.unit}";
          childTempDevice.sendEvent(map);
          infoLog(map.descriptionText)
        }
        else {
          //Nothing has changed, record the fact we received the same reading
          infoLog("${childTempDevice.displayName}: temperature has not changed from ${temperature.toString()}°${getTemperatureScale()}")
        }
      }
      
      /*** Humidity Reading ***/

      com.hubitat.app.DeviceWrapper childHumDevice = findChildDevice(sensor.key, "Humidity");
    
      if (childHumDevice == null) {
        //Could not find sensor, run the sensors method to create any new sensor child devices
        sensors()
        //Attempt to do the lookup again
        childHumDevice = findChildDevice(sensor.key, "Humidity")
      }
                        
      if (childHumDevice == null) {
        //Still could not find the device
        errorLog("samplesCallback: Lookup of newly created sensor failed... ${sensor.key}, Humidity")                         
      }
      else {
        String humidity = (String)(sensor.value.humidity);
        humidity = humidity.replace("[","").replace("]","");
                            
        debugLog("samplesCallback: ${childHumDevice.displayName} - Current humidity reading is ${childHumDevice.currentValue("humidity", true)}, new humidity reading is ${humidity}");          
        //Check if the reading has actually changed, if it has, record an event
        //   The aim here is to minimise the device events that could trigger automations and minimise the changes to the device's Last Activity value to improve accuracy in monitoring device health
        if (childHumDevice.currentValue("humidity", true) == null || childHumDevice.currentValue("humidity", true).toString() != humidity) {
          Map map = [:]
                                
          map.name            = "humidity"
          map.value           = humidity
          map.unit            = "%"
          map.descriptionText = "${childHumDevice.displayName}: humidity is ${map.value}${map.unit}"
          infoLog(map.descriptionText)
          childHumDevice.sendEvent(map)
        }
        else {
          //Record the fact we received the same reading
          infoLog("${childHumDevice.displayName}: humidity has not changed from ${humidity.toString()}%")
        }
      }
    }
  }
  else {
      //We got a status other than 200 back
      warnLog("There was a problem retrieving the latest readings from the SensorPush cloud.  If the issue continues, check your gateway is configured correctly and you Internet connection is working.");
  }      
}

void getAuthToken() {
 
    String bodyJson = ""
    Map headers = [:] 
    Map postParams = [:]
        
    headers.put("accept", "application/json")
    
    bodyJson = "{ \"email\": \"${UserName}\", \"password\": \"${Password}\" }"
    postParams = [
        uri: "${spBaseURL}/oauth/authorize",
        headers: headers,
        contentType: "application/json",
        requestContentType: "application/json",
		body : bodyJson,
        ignoreSSLIssues: true
	]
           
	try {
        asynchttpPost('getAuthTokenCallback', postParams);        
	}
	catch (Exception e) {
        errorLog("getAuthToken: Unable to query sensorpush cloud: ${e}")
	}
    
}

void getAuthTokenCallback(resp, data) {
    debugLog("getAuthTokenCallback: Response status = ${resp.getStatus()}");
    sendEvent(name: "spAuthCode", value : resp.getJson().authorization)
}

void getAccessToken(){
    
    String bodyJson = ""
    Map headers = [:] 
    Map postParams = [:]
        
    headers.put("accept", "application/json")
        
    bodyJson = "{ \"authorization\": \"${device.currentValue("spAuthCode", true)}\" }"
    postParams = [
		uri: "${spBaseURL}/oauth/accesstoken",
        headers: headers,
        contentType: "application/json",
        requestContentType: "application/json",
		body : bodyJson,
        ignoreSSLIssues: true
	]
           
	try {
        asynchttpPost('getAccessTokenCallback',postParams)
	}
	catch (Exception e) {
        errorLog("getAccessToken: Unable to query sensorpush cloud: ${e}")
	}
    
}

void getAccessTokenCallback(resp, data) {
    debugLog("getAccessTokenCallback: Response status = ${resp.getStatus()}");
    sendEvent(name: "spAccessToken", value : resp.getJson().accesstoken)
}

void sensors() {
    
    debugLog("sensors: Sensors starting")
    String bodyJson = ""
    Map headers = [:] 
    Map postParams = [:]
    
    getAccessToken()
    headers.put("accept", "application/json")
    headers.put("Authorization", device.currentValue("spAccessToken", true))
    bodyJson = "{ }"
    postParams = [
		uri: "${spBaseURL}/devices/sensors",
        headers: headers,
        contentType: "application/json",
        requestContentType: "application/json",
		body : bodyJson,
        ignoreSSLIssues: true
	]
           
	try {
        asynchttpPost('sensorsCallback',postParams)

	}
	catch (Exception e) {
        errorLog("sensors: Unable to query sensorpush cloud whilst getting sensor data: ${e}")
	}
    debugLog("sensors: Sensors completed")
    
}

void sensorsCallback(resp, data) {
            debugLog("sensorsCallback: Response status = ${resp.getStatus()}");
            resp?.getJson().each { it ->
            
                com.hubitat.app.DeviceWrapper childTempDevice = findChildDevice(it.value.id, "Temperature")
    
                if (childTempDevice == null) {
                    createSensor(it.value.id, it.value.name, "Temperature")
                    childTempDevice = findChildDevice(it.value.id, "Temperature")
                }
                        
                if (childTempDevice == null) {
                    errorLog("sensorsCallback: Lookup of newly created sensor failed... ${it.value.id}, ${it.value.name}, Temperature")                         
                }
                
                com.hubitat.app.DeviceWrapper childHumDevice = findChildDevice(it.value.id, "Humidity")
    
                if (childHumDevice == null) {
                    createSensor(it.value.id, it.value.name, "Humidity")
                    childHumDevice = findChildDevice(it.value.id, "Humidity")
                }
            }
}

String deriveSensorDNI(sensorId, sensorType) {

    return "${device.deviceNetworkId}-id${sensorId}-type${sensorType}"
}

com.hubitat.app.DeviceWrapper findChildDevice(sensorId, sensorType) {
	getChildDevices()?.find { it.deviceNetworkId == deriveSensorDNI(sensorId, sensorType)}
}

void createSensor(sensorId, sensorName, sensorType) {
    debugLog("createSensor: Creating SensorPush Sensor: ${sensorId}, ${sensorName}, ${sensorType}")
    
	com.hubitat.app.DeviceWrapper childDevice = findChildDevice(sensorId, sensorType)
    
    if (childDevice == null) {
        childDevice = addChildDevice("hubitat", "Virtual ${sensorType} Sensor", deriveSensorDNI(sensorId, sensorType), [label: "${device.displayName} (${sensorType} Sensor - ${sensorName})", isComponent: false])
	}
    else {
      debugLog("createSensor: child device ${childDevice.deviceNetworkId} already exists")
	}
	
}


//Utility methods
void debugLog(debugMessage) {
	if (DebugLogging == true) {log.debug(debugMessage)}	
}

void errorLog(errorMessage) {
    if (ErrorLogging == true) { log.error(errorMessage)}  
}

void infoLog(infoMessage) {
    if(InfoLogging == true) {log.info(infoMessage)}    
}

void warnLog(warnMessage) {
    if(WarnLogging == true) {log.warn(warnMessage)}    
}
