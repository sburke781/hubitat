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
 *    2021-02-10  Simon Burke	 Addition of Scheduling
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
		input(name: "AutoUpdate", type: "bool", title:"Automatic Update", description: "Enable / Disable automatic update to date", defaultValue: true, required: true, displayDuringSetup: true)
        	input(name: "AutoUpdateInterval", type: "ENUM", multiple: false, options: ["20", "30", "60", "300"], title:"Auto Update Interval", description: "Number of seconds between automatic updates", defaultValue: 30, required: true, displayDuringSetup: true)		
        
    }

}

def refresh() {
 runCmd()   
}

def updated() {

    log.debug("updated: AutoPolling = ${AutoUpdate}, StatusPollingInterval = ${AutoUpdateInterval}")
    updatePolling()    
}

def runCmd() {
    
    
    //EEEE d MMMM, yyyy
    //HH:mm
    sendEvent(name: "formattedDate", value : new Date().format("${dateFormat}"));
    sendEvent(name: "formattedTime", value : new Date().format("${timeFormat}"));
}

def getSchedule() { }

def updatePolling() {

   def sched
   log.debug("updatePolling: Updating Automatic Polling called, about to unschedule refresh")
   unschedule("refresh")
   log.debug("updatePolling: Unscheduleing refresh complete")
   
   if(AutoUpdate == true) {
       
       sched = "2/${AutoUpdateInterval} * * ? * * *"
       log.debug("updatePolling: Setting up schedule with settings: schedule(\"${sched}\",refresh)")
       try{
           
           schedule("${sched}","refresh")
       }
       catch(Exception e) {
           log.error("updatePolling: Error - " + e)
       }
       
       log.debug("updatePolling: Scheduled refresh set")
   }
   else { log.debug("updatePolling: Automatic status polling disabled")  }
}
