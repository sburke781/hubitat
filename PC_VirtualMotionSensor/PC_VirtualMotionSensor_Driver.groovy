/**
 *  PC Virtual Motion Sensor Driver
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
 *    2021-04-10  Simon Burke    Initial creation
 * 
 */
metadata {
	definition (name: "PC Virtual Motion Sensor Driver", namespace: "simnet", author: "Simon Burke") {
        
        capability "MotionSensor"
        
        command "active"
        command "inactive"
        
        attribute "motion", "ENUM", ['inactive', 'active']
        
        attribute "PollingPeriod", "number"
        attribute "ActivityThreshold", "number"
                
        attribute "InfoLogging", "bool"
        attribute "DebugLogging", "bool"
        attribute "WarnLogging", "bool"
        attribute "ErrorLogging", "bool"
	}

	preferences {
        input(name: "PollingPeriod", type: "number", title:"Polling Period (minutes)", displayDuringSetup: true, defaultValue: 1)
        input(name: "ActivityThreshold", type: "number", title:"Activity Threshold (seconds)", displayDuringSetup: true, defaultValue: 300)
        
        def autoInactiveList = []
            autoInactiveList << ["-1" : "Disabled"]
            autoInactiveList << ["5" : "5 seconds"]
            autoInactiveList << ["15" : "15 seconds"]
            autoInactiveList << ["30" : "30 seconds"]
            autoInactiveList << ["45" : "45 seconds"]
            autoInactiveList << ["60" : "1 minute"]
            
        
        input(name: "AutoInactive", type: "enum", title:"Auto Inactive", options: autoInactiveList, displayDuringSetup: true, defaultValue: -1)
        input(name: "InfoLogging", type: "bool", title:"Enable Description Text (Info) Logging", displayDuringSetup: true, defaultValue: false)
        input(name: "DebugLogging", type: "bool", title:"Enable Debug Logging", displayDuringSetup: true, defaultValue: false)
        input(name: "WarnLogging", type: "bool", title:"Enable Warning Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "ErrorLogging", type: "bool", title:"Enable Error Logging", displayDuringSetup: true, defaultValue: true)
        
    }

}

def installed() {
    //This method is called when the device is first created.
    debugLog("installed: installed method called")   
    updated()
    sendEvent(name: "motion", value: "inactive")
}

def updated() {
    //This method is called when the preferences of a device are updated
    debugLog("updated: updated method called")
    
    //Sync Device Attributes with Preferences
    sendEvent(name: "PollingPeriod", value: PollingPeriod)
    sendEvent(name: "ActivityThreshold", value: ActivityThreshold)
    sendEvent(name: "InfoLogging", value: InfoLogging)
    sendEvent(name: "DebugLogging", value: DebugLogging)
    sendEvent(name: "WarnLogging", value: WarnLogging)
    sendEvent(name: "ErrorLogging", value: ErrorLogging)
    
    //Schedule Auto Inactive
    updateAutoInactiveSchedule()
}
    

def refresh() {
  debugLog("refresh: Refresh process called")
}

def initialize() {
    debugLog("initialize: initialize called")
}

def active() {
    debugLog("active: active called")
    sendEvent(name: "motion", value: "active")
}

def inactive() {
    debugLog("inactive: inactive called")
    sendEvent(name: "motion", value: "inactive")
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



// Auto Inactive Scheduling

def getSchedule() { }

def updateAutoInactiveSchedule() {

   def sched
   debugLog("updateAutoInactiveSchedule: Updating Auto Inactive called, about to unschedule")
   unschedule("inactive")
   debugLog("updateAutoInactiveSchedule: Unscheduleing Auto Inactive complete")
   
    if(AutoInactive.toInteger() > 0) {
       
       if(AutoInactive.toInteger() == 60) { sched = "0 * * ? * * *" }
       else { sched = "2/${AutoInactive.toInteger()} * * ? * * *" }
       
       debugLog("updateAutoInactiveSchedule: Setting up schedule with settings: schedule(\"${sched}\",inactive)")
       try{
           
           schedule("${sched}","inactive")
       }
       catch(Exception e) {
           debugLog("updateAutoInactiveSchedule: Error - " + e)
       }
       
       debugLog("updateAutoInactiveSchedule: Scheduling Auto inactive complete")
   }
   else { debugLog("updateAutoInactiveSchedule: Auto Inactive disabled")  }
}
