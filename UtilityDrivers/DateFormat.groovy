/**
 *  Date / Time Display
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
 *    2020-04-18  Simon Burke    Original Creation
 * 
 */
metadata {
	definition (name: "Date / Time Display", namespace: "simnet", author: "Simon Burke") {
        capability "Refresh"
        attribute "formattedDate", "string"
        attribute "formattedTime", "string"
	}

	preferences {
		input(name: "dateFormat", type: "string", title:"Date Format", description: "Enter the date format to apply for display purposes", defaultValue: "EEEE d MMMM, yyyy", required: true, displayDuringSetup: true)
		input(name: "timeFormat", type: "string", title:"Time Format", description: "Enter the time format to apply for display purposes", defaultValue: "HH:mm", required: false, displayDuringSetup: true)
    }
    
    

}

def refresh() {
 runCmd()   
}

def runCmd() {
    
    
    //EEEE d MMMM, yyyy
    //HH:mm
    sendEvent(name: "formattedDate", value : new Date().format("${dateFormat}"));
    sendEvent(name: "formattedTime", value : new Date().format("${timeFormat}"));
}
