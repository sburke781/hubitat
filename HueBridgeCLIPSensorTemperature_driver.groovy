/**
 *  Hue Bridge CLIP Sensor Temperature
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
 *    2019-11-28  Simon Burke    Original Creation
 *
 * 
 */
metadata {
	definition (name: "Hue Bridge CLIP Sensor Temperature", namespace: "simnet", author: "Simon Burke") {
        capability "Temperature Measurement"
        capability "Sensor"
        capability "Refresh"
        attribute "Temperature", "Number"
        attribute "lastUpdatedHue", "string"
        attribute "lastUpdatedHE", "string"
	}

	preferences {
		input(name: "hueBridgeIP", type: "string", title:"Hue Bridge IP Address", description: "Enter IP Address of your Hue Bridge", required: true, displayDuringSetup: true)
		input(name: "hueBridgePort", type: "string", title:"Device Port", description: "Enter Port of your HTTP server (defaults to 80)", defaultValue: "80", required: false, displayDuringSetup: true)
		input(name: "userId", type: "string", title:"Hue API User Id", description: "User Id configured in Hue Bridge", displayDuringSetup: true)
		input(name: "sensorId", type: "integer", title:"CLIP Sensor Id", description: "Sensor Id from Hue Bridge", displayDuringSetup: true)
    }
    
    

}

def parse(String description) {
	log.debug(description)
}

def webSocketStatus(String message) {
    log.debug("webSocketStatus Message: ${message}")
}

def refresh() {
 runCmd()   
}

def runCmd() {
	
    def localHttpPort = (hueBridgePort==null) ? "80" : hueBridgePort
    def path = "/api/${userId}/sensors/${sensorId}" 
    def body =  ""
	def headers = [:] 
    
    def lastUpdate = new Date()
    
    headers.put("HOST", "${hueBridgeIP}:${localHttpPort}")
	headers.put("Content-Type", "application/json")

	try {
        //log.debug("Connecting to Hue Bridge")
        //interfaces.webSocket.connect("http://${hueBridgeIP}:${localHttpPort}/websocket")
		//log.debug("Websocket connection established with Hue Bridge")
        
        //log.debug("Retrieving Hue Bridge CLIP Sensor Temperature ${sensorId} - URI: http://${hueBridgeIP}:${localHttpPort}, path: ${path}, Body: ${body}")
        httpGet(uri: "http://${hueBridgeIP}:${localHttpPort}${path}",
                 contentType: 'application/json',
                 requestContentType: 'application/json'
                ) { resp -> 

            sendEvent(name: "Temperature", value : resp.data.state.temperature / 100)
            //sendEvent(name: "lastUpdatedHue", value : Date.parse("[YYYY]-[MM]-[DD]T[hh]:[mm]:[ss]", resp.data.state.lastupdated))
            sendEvent(name: "lastUpdatedHE", value : lastUpdate.format("dd/MM/yyyy HH:mm:ss"))
            log.debug("${device.label}: ${resp.data.state.temperature / 100}")
        }
        
        
        //log.debug("Sensor temperature retrieved")
        
	}
	catch (Exception e) {
        log.debug "Unable to query hue bridge: ${e}"
	}  
}
