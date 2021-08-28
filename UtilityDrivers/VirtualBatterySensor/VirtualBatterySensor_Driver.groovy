/**
 *  Virtual Battery Driver
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
 *    2021-08-28  Simon Burke    Original Creation
 *
 */
metadata {
	definition (name: "Virtual Battery Sensor", namespace: "simnet", author: "Simon Burke") {
        capability "Battery"
        attribute "battery", "number"
        attribute "lastUpdated", "date"
        
        command "setBattery", [[name:"batteryReading", type: "NUMBER", description: "Enter the new battery reading (%)" ] ]
	}
}

def installed() { initialized() }

def initialized() {
    
 state.warningCount = 0;
}



def setBattery(Number pBattery) {
    //Check that the reading is within the 0 - 100 range for a percentage value
    if(pBattery >= 0 && pBattery <= 100) {
        //Update the battery attribute value
        sendEvent(name: "battery", value: pBattery);
        
        //Update the lastUpdated attribute value
        def lastUpdate = new Date()
        sendEvent(name: "lastUpdated", value : lastUpdate.format("dd/MM/yyyy HH:mm"))
        
        //Reset warning count if there have been previous warnings
        if (state.warningCount > 0) {
            state.warningCount = 0;
            log.info("setBattery: warning count reset")
        }
    }
    // If the batter reading is outside the 0 - 100 range, log a warning and leave the current reading in place
    //   use the warning count state variable to make sure we don't spam the logs with repeated warnings
    else {
        if (state.warningCount < 10) {
            state.warningCount = state.warningCount + 1;
            log.warn("setBattery: Warning (${state.warningCount}) - battery level outside of 0-100 range, device not updated.  Battery value provided = ${pBattery}")
        }
    }
}

def getBattery() { return device.currentValue("battery"); }