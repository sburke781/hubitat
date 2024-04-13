/*
 * Australian BoM Weather Radar Images Data File Driver
 *
 * Retrieves URLs for radar images from the Australian Bureau of Meteorology (BOM) and stores them in a local file
 *   that is linked to a separate device to display the radar images
 *
 */
metadata {
    definition(name: 'BoM Radar Images Data File', namespace: 'simnet', author: 'sburke781') {
	    capability 'Actuator'

		attribute 'lastupdate', 'string'

        command 'refresh'
    }
}

preferences {
		input (name: 'idr',          type: 'text',   title: 'Observation ID number (e.g. IDR043)',                      required: true, defaultValue: '')
        input (name: 'dataFileName', type: 'text',   title: 'Data File Name (inc. file extension, e.g. BoMRadar.json)', required: true, defaultValue: 'BoMRadar.json')

        input (name: 'locations',    type: 'bool',   title:'Locations Image', description: 'Include Locations Image?', defaultValue: true,  required: true )
        input (name: 'range',        type: 'bool',   title:'Range Image', description: 'Include Range Image?',     defaultValue: false, required: true )
        input (name: 'topography',   type: 'bool',   title:'Topography Image', description: 'Include Topography?',      defaultValue: true,  required: true )
        
		input (name: 'AutoPolling',     type: 'bool',   title:'Automatic Polling', description: 'Enable / Disable automatic polling',          defaultValue: true, required: true )
        input (name: 'PollingInterval', type: 'string', title:'Polling Interval',  description: 'Number of minutes between automatic updates', defaultValue: 15,   required: true )

        input (name: 'DebugLogging', type: 'bool',   title:'Enable Debug Logging',                   defaultValue: false)
        input (name: 'WarnLogging',  type: 'bool',   title:'Enable Warning Logging',                 defaultValue: true )
        input (name: 'ErrorLogging', type: 'bool',   title:'Enable Error Logging',                   defaultValue: true )
        input (name: 'InfoLogging',  type: 'bool',   title:'Enable Description Text (Info) Logging', defaultValue: false)
}

// Standard device methods
void installed() {
    debugLog('installed: BoM Radar Images device installed')
}

void updated() {
    debugLog('updated: update process called')
    refresh()
    updateAutoPolling()
}

void refresh() {
    debugLog('refresh: Refreshing radar images')
    retrieveImageURLs()
    debugLog('refresh: Refresh complete')
}

// Preference setting management methods
void setIdr(String pidr) {
    idr = pidr
    infoLog("IDR set to ${pidr}")
}

void setDataFileName(String pdataFileName) {
    dataFileName = pdataFileName
    infoLog("Data file name set to ${pdataFileName}")
}

// General driver-specific methods
void retrieveImageURLs() {
    debugLog('retrieveImageURLs: updating radar image data')
    String lastUpdate = null
    def getParams = [
        uri: "http://www.bom.gov.au/products/${idr}.loop.shtml",
        headers: ['User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36'],
        contentType: 'text/plain',
        textParser: true,
        body : ''
	]

	try {
        asynchttpGet('retrieveImageURLsCallback', getParams) 
	} catch (Exception e) {
		errorLog "retrieveImageURLs: call to update radar images failed: ${e}"
	}
    debugLog('retrieveImageURLs: process complete')
}

void retrieveImageURLsCallback(resp, data) {
    
    String shtmlResponse = groovy.xml.XmlUtil.escapeXml(resp.getData())

            String[] lines = shtmlResponse.split('\\r\\n|\\n|\\r')
            def images = lines.findAll { it.startsWith('theImageNames[') }

            String staticImagesJson = '{\n'
            int b = 1 // background image count
            int f = 0 // foreground image count
            staticImagesJson += "\"background0${b}\": \"http://www.bom.gov.au/products/radar_transparencies/${idr}.background.png\"\n"
		    if (topography) {
                b++
                staticImagesJson += ",\"background0${b}\": \"http://www.bom.gov.au/products/radar_transparencies/${idr}.topography.png\"\n"
            }
            if (locations) {
                f++
                staticImagesJson += ",\"foreground0${f}\": \"http://www.bom.gov.au/products/radar_transparencies/${idr}.locations.png\"\n"
            }
            if (range) {
                f++
                staticImagesJson += ",\"foreground0${f}\": \"http://www.bom.gov.au/products/radar_transparencies/${idr}.range.png\"\n"
            }
            staticImagesJson += '}'
            debugLog("retrieveImageURLsCallback: background images JSON = ${staticImagesJson}")

            String imagesJson = '{\n'
            int i = 1
            images.each {
                imagesJson += "\"image${i}\": \"http://www.bom.gov.au/radar/${it.substring(32).substring(0,it.substring(32).length() - 7)}\"";
                if (i != images.size()) { imagesJson += ',' }
                imagesJson += '\n'
                i++
            }
            imagesJson += '}'
            debugLog("retrieveImageURLsCallback: radar images JSON = ${imagesJson}");

            updateDataFile(staticImagesJson,imagesJson);
            lastUpdate = new Date().format('YYYY-MM-dd HH:mm:ss');
            device.sendEvent(name: 'lastupdate', value: "${lastUpdate}")
        
}

void updateDataFile(String pbackgrounds, String pimages) {
    def imageCycle = findChildDevice('radar','ImageCycle');
    if (imageCycle == null) {
              createChildDevice('Image Cycle', 'radar', 'BOM Radar', 'ImageCycle')
              imageCycle = findChildDevice('radar','ImageCycle');
    }
    imageCycle.setFullImageList(pbackgrounds,pimages)
}

// Child Device methods

String deriveChildDNI(String childDeviceId, String childDeviceType) {
    return "${device.deviceNetworkId}-id${childDeviceId}-type${childDeviceType}"
}

def findChildDevice(String childDeviceId, String childDeviceType) {
	getChildDevices()?.find { it.deviceNetworkId == deriveChildDNI(childDeviceId, childDeviceType)}
}

void createChildDevice(String childDeviceDriver, String childDeviceId, String childDeviceName, String childDeviceType) {
    debugLog("createChildDevice: Creating Child Device: ${childDeviceId}, ${childDeviceName}, ${childDeviceType}")

	def childDevice = findChildDevice(childDeviceId, childDeviceType)

    if (childDevice == null) {
        childDevice = addChildDevice('simnet', childDeviceDriver, deriveChildDNI(childDeviceId, childDeviceType), [label: "${device.displayName} - ${childDeviceName}"])
        infoLog("createChildDevice: New ${childDeviceDriver} created -  ${device.displayName} - ${childDeviceName}")
	}
    else {
      debugLog("createChildDevice: child device ${childDevice.deviceNetworkId} already exists")
	}
}

//Automatic Polling methods

void poll() {
    refresh()
}

void updateAutoPolling() {
   String sched
   debugLog('updateAutoPolling: Update Automatic Polling called, about to unschedule polling')
   unschedule('poll')
   debugLog('updateAutoPolling: Unscheduling of automatic polling is complete')

   if (AutoPolling == true) {
       sched = "0 0/${PollingInterval} * ? * * *"

       debugLog("updateAutoPolling: Setting up scheduled refresh with settings: schedule(\"${sched}\",poll)")
       try {
           schedule("${sched}",'poll')
           infoLog("Refresh scheduled every ${PollingInterval} minutes")
       }
       catch (Exception e) {
           errorLog('updateAutoPolling: Error - ' + e)
       }
   }
   else { infoLog('Automatic polling disabled')  }
}

void getSchedule() { }

//Logging methods
void debugLog(String debugMessage) {
	if (DebugLogging == true) { log.debug(debugMessage) }
}

void errorLog(String errorMessage) {
    if (ErrorLogging == true) { log.error(errorMessage) }
}

void infoLog(String infoMessage) {
    if (InfoLogging == true) { log.info(infoMessage) }
}

void warnLog(String warnMessage) {
    if (WarnLogging == true) { log.warn(warnMessage) }
}
