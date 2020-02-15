/**
 *  Hue Bridge CLIP Sensor Switch
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
 *    2019        Simon Burke    Original Creation
 *
 * 
 */
metadata {
	definition (name: "Hue Bridge CLIP Sensor Switch", namespace: "simnet", author: "Simon Burke") {
        capability "Switch"
        capability "Momentary"
	}

	preferences {
		input(name: "hueBridgeIP", type: "string", title:"Hue Bridge IP Address", description: "Enter IP Address of your Hue Bridge", required: true, displayDuringSetup: true)
		input(name: "hueBridgePort", type: "string", title:"Device Port", description: "Enter Port of your HTTP server (defaults to 80)", defaultValue: "80", required: false, displayDuringSetup: true)
		input(name: "userId", type: "string", title:"Hue API User Id", description: "User Id configured in Hue Bridge", displayDuringSetup: true)
		input(name: "sensorId", type: "integer", title:"CLIP Sensor Id", description: "Sensor Id from Hue Bridge", displayDuringSetup: true)
        input(name: "sensorOnValue", type: "integer", title: "CLIP Sensor On Value", description: "Enter integer value for when the On Button is pressed", required: true, displayDuringSetup: true)
        input(name: "sensorOffValue", type: "integer", title: "CLIP Sensor Off Value", description: "Enter integer value for when the Off Button is pressed", required: true, displayDuringSetup: true)
    }
}

def parse(String description) {
	log.debug(description)
}

def push() {
    on()
}

def toggleOff() {
    sendEvent(name: "switch", value: "off", isStateChange: true)
}

def on() {
	runCmd(1)
}

def off() {
	sendEvent(name: "switch", value: "off", isStateChange: true)
    runCmd(0)
}

def webSocketStatus(String message) {
    log.debug("webSocketStatus Message: ${message}")
}

def runCmd(Integer varSensorValue) {
	
    def localHttpPort = (hueBridgePort==null) ? "80" : hueBridgePort
    def path = "/api/${userId}/sensors/${sensorId}/state" 
    def body =  "{\"status\" : ${varSensorValue}}"
	def headers = [:] 
    headers.put("HOST", "${hueBridgeIP}:${localHttpPort}")
	headers.put("Content-Type", "application/json")

	try {
        log.debug("Connecting to Hue Bridge")
        interfaces.webSocket.connect("http://${hueBridgeIP}:${localHttpPort}/websocket")
		log.debug("Websocket connection established with Hue Bridge")
        
        log.debug("Updating Hue Bridge CLIP Sensor ${sensorId} with value ${varSensorValue} - URI: http://${hueBridgeIP}:${localHttpPort}, path: ${path}, Body: ${body}")
        httpPut(uri: "http://${hueBridgeIP}:${localHttpPort}",
                 path: "${path}",
                 contentType: 'application/json',
                 requestContentType: 'application/json',
                 headers: [:],
                 body: body
                ) { }
        log.debug("Hue Bridge Updated")
        
	}
	catch (Exception e) {
        log.debug "Unable to update hue bridge: ${e}"
	}  
}
