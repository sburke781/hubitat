/*
 * BoMWeather Radar Images driver
 *
 * Gets radar imaged from the Australian Bureau of Meteorology (BOM)
 * 
 */
metadata {
    definition(name: "BoM Radar Images", namespace: "SIMNET", author: "sburke781") {
	    capability "Actuator"
		
		attribute "lastupdate", "date"
		attribute "area", "string"
		attribute "radarJson", "string"
		command "refresh"
		
    }
}

preferences {
    section("URIs") {
		input "idr", "text", title: "Observation ID number (eg. IDR043)", required: true
        
		input "autoPoll", "bool", required: true, title: "Enable Auto Poll", defaultValue: false
        input "pollInterval", "text", title: "Poll interval (which minutes of the hour to run on, eg. 5,35)", required: true, defaultValue: "5,35"
		input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def installed() { }

def updated() { }

def poll(){
    refresh()
}

def refresh() {
    if (logEnable) log.debug "refresh: Refreshing radar images"
	
	    
    def getParams = [
        uri: "http://www.bom.gov.au/products/${idr}.loop.shtml",
        headers: ["User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"],
        contentType: "text/plain",
        textParser: true,
        body : ""
	]
    
	try {
        
        httpGet(getParams) { resp ->
            String shtmlResponse = groovy.xml.XmlUtil.escapeXml(resp.data.text);
            if (logEnable) log.debug(shtmlResponse)
            def lines = "${shtmlResponse}".split("\\r\\n|\\n|\\r");
            def images = lines.findAll { it.startsWith('theImageNames[') }
            
            def imagesJson = "{"
            def i = 1
            images.each { 
                imagesJson += "\"image${i}\": \"${it.value.toString().substring(32).substring(0,it.value.toString().substring(32).length() - 7)}\"";
                if (i != images.size()) imagesJson += ",";
                imagesJson += "\n";
                i++
            }
            imagesJson += "}"
            if (logEnable) log.debug("refresh: radar images JSON = ${imagesJson}")
            device.sendEvent(name: "radarJson", value: "${imagesJson}")
        }
        
	} catch (Exception e) {
		log.warn "refresh: call to update radar images failed: ${e}"
	}
}

