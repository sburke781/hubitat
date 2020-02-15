/**
 *  SensorPush Temperature Sensor
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
 *
 * 
 */
metadata {
	definition (name: "SensorPush Gateway", namespace: "simnet", author: "Simon Burke") {
        capability "Temperature Measurement"
        capability "Sensor"
        capability "Refresh"
        attribute "Temperature", "Number"
        attribute "lastUpdatedSource", "string"
        attribute "lastUpdatedHE", "string"
        attribute "spAuthCode", "string"
        attribute "spAccessToken", "string"
        
	}

	preferences {
		input(name: "spBaseURL", type: "string", title:"SensorPush Base URL", description: "Enter the base URL for the SensorPush Cloud Service", required: true, displayDuringSetup: true)
		
		input(name: "UserName", type: "string", title:"SensorPush Username / Email", description: "Username / Email used to authenticate on SensorPush cloud", displayDuringSetup: true)
		input(name: "Password", type: "password", title:"SensorPush Account Password", description: "Password for authenticating on SensorPush cloud", displayDuringSetup: true)
        
    }
    
    

}


def refresh() {
 runCmd()   
}

def runCmd() {

    def bodyJson = "{ \"email\": \"${UserName}\", \"password\": \"${Password}\" }"
    def headers = [:] 

    headers.put("accept", "application/json")
    //log.debug(spBaseURL)
    def postParams = [
        uri: "${spBaseURL}/oauth/authorize",
        headers: headers,
        contentType: "application/json",
        requestContentType: "application/json",
		body : bodyJson
	]
           
	try {
        //log.debug("Requesting SensorPush Authorization Code")
        
        httpPost(postParams)
        { resp -> 

            sendEvent(name: "spAuthCode", value : resp.data.authorization)
            //log.debug(resp.data)
        }
        
                
	}
	catch (Exception e) {
        log.debug "Unable to query sensorpush cloud: ${e}"
	}
    
    
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
            //log.debug(resp.data)
        }
        
                
	}
	catch (Exception e) {
        log.debug "Unable to query sensorpush cloud: ${e}"
	}
    
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
            
                def samplesBodyJson = "{ \"sensors\": [\"${it.value.id}\"], \"limit\": 1 }"
                def samplesPostParams = [
		            uri: "${spBaseURL}/samples",
                    headers: headers,
                    contentType: "application/json",
                    requestContentType: "application/json",
		            body : samplesBodyJson
	                ]
                
                httpPost(samplesPostParams)
                { samples ->
                    samples?.data?.sensors?.each { it2 ->
                        
                        def tempStr = (String)(it2.value.temperature)
                        tempStr = tempStr.replace("[","").replace("]","")
                        def temperature = (Double.parseDouble(tempStr) - 32) * 5 / 9
                        
                        def childTempDevice = findChildDevice(it.value.id, "Temperature")
    
                        if (childTempDevice == null) {
                            createSensor(it.value.id, it.value.name, "Temperature")
                            childTempDevice = findChildDevice(it.value.id, "Temperature")
                        }
                        
                        if (childTempDevice == null) {
                            log.debug("Lookup of newly created sensor failed... ${it.value.id}, ${it.value.name}, Temperature")                         
                        }
                        else {
                            childTempDevice.sendEvent(name: "temperature", value: String.format("%.2f",temperature))
                        }
                        
                        def humidity = (String)(it2.value.humidity)
                        humidity = humidity.replace("[","").replace("]","")
                        
                        def childHumDevice = findChildDevice(it.value.id, "Humidity")
    
                        if (childHumDevice == null) {
                            createSensor(it.value.id, it.value.name, "Humidity")
                            childHumDevice = findChildDevice(it.value.id, "Humidity")
                        }
                        
                        if (childHumDevice == null) {
                            log.debug("Lookup of newly created sensor failed... ${it.value.id}, ${it.value.name}, Humidity")                         
                        }
                        else {
                            childHumDevice.sendEvent(name: "humidity", value: humidity)
                        }
                        
                        log.debug("SensorPush Gateway: ${it.value.name} - Temperature: ${String.format("%.2f",temperature)}, Humidity: ${humidity}")
                
                        
                    }
                }
            }
            
        }
        
                
	}
	catch (Exception e) {
        log.debug "Unable to query sensorpush cloud: ${e}"
	}
    
    
}

def deriveSensorDNI(sensorId, sensorType) {

    return "${device.deviceNetworkId}-id${sensorId}-type${sensorType}"
}

def findChildDevice(sensorId, sensorType) {
	getChildDevices()?.find { it.deviceNetworkId == deriveSensorDNI(sensorId, sensorType)}
}

def createSensor(sensorId, sensorName, sensorType) {
    log.debug("createSensor: Creating SensorPush Sensor: ${sensorId}, ${sensorName}, ${sensorType}")
    
	def childDevice = findChildDevice(sensorId, sensorType)
    
    if (childDevice == null) {
        childDevice = addChildDevice("hubitat", "Virtual ${sensorType} Sensor", deriveSensorDNI(sensorId, sensorType), [label: "${device.displayName} (${sensorType} Sensor - ${sensorName})", isComponent: false])
	}
    else {
      log.debug("createSensor: child device ${childDevice.deviceNetworkId} already exists")
	}
	
}
