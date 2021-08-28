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
 *    2021-08-29  Simon Burke    Added batteryStatus attribute
 */
metadata {
	definition (name: 'Virtual Battery Sensor', namespace: 'simnet', author: 'Simon Burke') {
        capability 'Battery'
        attribute 'battery', 'number'
        attribute 'lastUpdated', 'date'
        attribute 'batteryStatus', 'string'

        command 'setBattery', [[name:'batteryReading', type: 'NUMBER', description: 'Enter the new battery reading (%)' ] ]
	}
}

void installed() { initialized() }

void initialized() {

 state.warningCount = 0
 //Detect whether battery status exists, if not, set to Idle
  //sendEvent(name: "batteryStatus", value: "Idle");
}

void setBattery(Number pbattery) {
    String vbatteryStatus = getBatteryStatus()

    //Check that the reading is within the 0 - 100 range for a percentage value
    if(pbattery >= 0 && pbattery <= 100) {
        if (getBattery() == null || getBattery() == pbattery) { vbatteryStatus = 'Idle' }
        else {
            if (getBattery() > pbattery) { vbatteryStatus = 'Discharging' }
            else { vbatteryStatus = 'Charging' }
        }
        //Update the battery and batteryStatus attributes
        sendEvent(name: 'battery',       value: pbattery)
        sendEvent(name: 'batteryStatus', value: vbatteryStatus)
        //Update the lastUpdated attribute value
        Date lastUpdate = new Date()
        sendEvent(name: 'lastUpdated', value : lastUpdate.format('dd/MM/yyyy HH:mm'))

        //Reset warning count if there have been previous warnings
        if (state.warningCount > 0) {
            state.warningCount = 0
            log.info('setBattery: warning count reset')
        }
    }
    // If the batter reading is outside the 0 - 100 range, log a warning and leave the current reading in place
    //   use the warning count state variable to make sure we don't spam the logs with repeated warnings
    else {
        if (state.warningCount < 10) {
            state.warningCount = state.warningCount + 1
            log.warn("setBattery: Warning (${state.warningCount}) - battery level outside of 0-100 range, device not updated.  Battery value provided = ${pBattery}")
        }
    }
}

Number getBattery()       { return device.currentValue('battery') }
String getBatteryStatus() { return device.currentValue('batteryStatus') }