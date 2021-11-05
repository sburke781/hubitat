/*
 * Logitech G-Hub Battery Sensor Parent Driver
 *
 * Gets battery levels for Logitech G-Hub Devices connected to a PC / laptop
 *
 */
metadata {
    definition(name: 'G-Hub Battery Parent Driver', namespace: 'SIMNET', author: 'sburke781') {
	    capability 'Actuator'

        attribute 'lastupdate', 'date'
		command 'refresh'
        command 'poll'
    }
}

preferences {
	input 'IP', 'text', title: 'IP Address of PC / Laptop', required: true
    input 'Port', 'number', title: 'Port configured in LGSTrayBattery', required: true, defaultValue: 12321
	input 'autoPoll', 'bool', required: true, title: 'Enable Auto Poll', defaultValue: false
    input 'pollInterval', 'text', title: 'Poll interval (minutes)', required: true, defaultValue: '10'
	input(name: 'DebugLogging', type: 'bool', title:'Enable Debug Logging', displayDuringSetup: true, defaultValue: false)
    input(name: 'WarnLogging', type: 'bool', title:'Enable Warning Logging', displayDuringSetup: true, defaultValue: true)
    input(name: 'ErrorLogging', type: 'bool', title:'Enable Error Logging', displayDuringSetup: true, defaultValue: true)
    input(name: 'InfoLogging', type: 'bool', title:'Enable Description Text (Info) Logging', displayDuringSetup: true, defaultValue: false)
}

//void installed() { }

void updated() {
    debugLog("updated: AutoPolling = ${autoPoll}, StatusPollingInterval = ${pollInterval}")
    updatePolling()
}

void poll(){
    getBatteryReadings()
}

void refresh() {
    retrieveBatterySensorDevices()
    poll()
}

void retrieveBatterySensorDevices() {
    debugLog('retrieveBatterySensorDevices: Refreshing G-Hub Battery Sensor List')

	def getParams = [
        uri: "http://${IP}:${Port}/devices",
        headers: ['User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36', 'Accept': 'text/xml'],
        contentType: 'text/plain',
        textParser: true
	]

	try {
        httpGet(getParams) { resp ->
            String htmlResponse = groovy.xml.XmlUtil.escapeXml(resp.data.text)
            htmlResponse = htmlResponse.replace('&lt;html&gt;', '').replace('&lt;/html&gt;', '').replace('&lt;br&gt;', '')

            String[] devices = htmlResponse.split('&lt;/a&gt;')
            for (int i = 0; i < devices.length; i++) {
                String deviceName = "${devices[i].split(' : ')[0]}";
                String deviceId = "${devices[i].split(' : ')[1].replace('&lt;a href=\'', '').split('&gt;')[1]}"
                debugLog("retrieveBatterySensorDevices: Id = ${deviceId}, Name = ${deviceName}")

                childDevice = findChildDevice("ghub_${deviceId}")
                if (childDevice == null) {
                    childDevice = addChildDevice('simnet', 'Virtual Battery Sensor', "ghub_${deviceId}", [label: "${deviceName} Battery Sensor"])
                }
            }
        }
	} catch (Exception e) {
		warnLog("refresh: call to update G-Hub device battery levels failed: ${e}")
	}
}

void getBatteryReadings() {
 for (device in getChildDevices()) { 
     def getParams = [
         uri: "http://${IP}:${Port}/device/${device.deviceNetworkId.replace('ghub_', '')}",
         headers: ['User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36', 'Accept': 'text/xml'],
         contentType: 'text/xml'
	]

	try {
            httpGet(getParams) { resp ->
                String batteryPC = resp.data.battery_percent.text()
                String deviceId = resp.data.device_id.text()
                String deviceName = resp.data.device_name.text()
                debugLog("getBatteryReadings: ${deviceName}(${deviceId}) - ${batteryPC}%")
                if (batteryPC != 'NaN') { device.setBattery(new BigDecimal(batteryPC)) }
            }
    } catch (Exception e) {
		log.warn "getBatteryReadings: call to update G-Hub device battery levels failed: ${e}"
	}
 }
}

def findChildDevice(childDeviceId) {
    getChildDevices()?.find { it.deviceNetworkId == "${childDeviceId}" }
}

//Utility methods
void debugLog(String debugMessage) {
	if (DebugLogging == true) { log.debug(debugMessage) }
}

void errorLog(String errorMessage) {
    if (ErrorLogging == true) { log.error(errorMessage) }
}

void infoLog(String infoMessage) {
    if(InfoLogging == true) { log.info(infoMessage) }
}

void warnLog(String warnMessage) {
    if (WarnLogging == true) { log.warn(warnMessage) }
}

void updatePolling() {
   String sched = ''
   debugLog('updatePolling: Updating Polling called, about to unschedule poll')
   unschedule('poll')
   debugLog('updatePolling: Unscheduleing poll complete')

   if (autoPoll == true) {
       sched = "0 0/${pollInterval} * ? * * *"

       debugLog("updatePolling: Setting up schedule with settings: schedule(\"${sched}\",poll)")
       try {
           schedule("${sched}", 'poll')
       }
       catch (Exception e) {
           errorLog('updatePolling: Error - ' + e)
       }

       infoLog('updatePolling: Scheduled poll set')
   }
   else { infoLog('updatePolling: Automatic polling disabled')  }
}
void getSchedule() { }