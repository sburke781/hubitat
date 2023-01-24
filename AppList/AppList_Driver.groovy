/**
 *  App List
 *
 *  Copyright 2022 Simon Burke
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
 *    Date            Who            What
 *    ----            ---            ----
 *    2023-01-25      Simon Burke    Initial Beta Release
 * 
 */
metadata {
	definition (name: "App List", namespace: "simnet", author: "Simon Burke") {
        capability "Refresh"
        
	}


}

def refresh() {
    
    log.debug("refresh: Refresh Process called");
    def params = [
		uri: "http://127.0.0.1:8080/installedapp/list",
        textParser: true,
        timeout: 60,
		headers: [:]
	  ]
	
	def allAppsList = [:]
	try {
		httpGet(params) { resp ->     
			
            def matcherText = resp.data.text.replace("\n","").replace("\r","")
            
            def grid = matcherText.find(/(<div class=\"mdl-grid\"> .*?<div id=\"list-view\">)/) { match,f -> return f }
            
            def parentMatcher = grid.findAll(/(<div class=\"app-grid grid-parentArea .*?<\/div)/).each {
                
                def id = it.find(/configure\/([^"]+)\"/) { match,i -> return i.trim() };
                def title = it.find(/>([^"]+)<\/a>/) { match,t -> 
                                                        t = t.replaceAll(/<span(.*?)<\/span>/,"");
                                                        return t.trim() };
                allAppsList.putAll([(id):[parentId:null,title:(title)]]);
            }
            
            def childMatcher = grid.findAll(/<div class=\"grid-childArea(.*?)<\/div/).each {
                
                def parentId = it.find(/childOf([^"]+)\"/) { match,i -> return i.trim() };
                def id = it.find(/configure\/([^"]+)\"/) { match,i -> return i.trim() };
                def childTitle;
                it.findAll(/app-row(.*?)<\/div/).each { children ->
                    childTitle = children.find(/>([^"]+)<\/a>/) { match,t -> 
                                                                     t = t.replaceAll(/<span(.*?)<\/span>/,"");
                                                                     return t.trim() };
                };
                
                allAppsList.putAll([(id):[parentId:(parentId),title:(childTitle)]]);
            }

		}
	} catch (e) {
		log.error "Error retrieving installed apps: ${e}"
        log.error(getExceptionMessageWithLine(e))
	}
    state.allAppsList = allAppsList
    log.debug("refresh: Refresh process finished");
}
    
    
