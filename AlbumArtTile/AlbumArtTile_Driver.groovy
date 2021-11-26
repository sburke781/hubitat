/**
 *  Album Art Tile
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
 *    2021-10-02  Simon Burke    Original Creation
 * 
 */
metadata {
	definition (name: "Album Art Tile", namespace: "simnet", author: "Simon Burke") {
        capability "Refresh"
        
        attribute "artist"           , "string"
        attribute "album"            , "string"
        attribute "albumMBId"        , "string"
        attribute "coverArtURL"  , "string"
        
        attribute "coverArtTile"     , "string"
        
        command "setCurrentAlbum" , [[name:"partist", type: "STRING", description: "Enter the Artist" ], [name:"palbum", type: "STRING", description: "Enter the Album" ] ]
        command "correctAlbumMBId", [[name:"partist", type: "STRING", description: "Enter the Artist" ], [name:"palbum", type: "STRING", description: "Enter the Album" ], [name:"pmbId", type: "STRING", description: "Enter the Music Brainz ID" ] ]
        command "addAlbum"        , [[name:"partist", type: "STRING", description: "Enter the Artist" ], [name:"palbum", type: "STRING", description: "Enter the Album" ], [name:"pimageURL", type: "STRING", description: "Album Cover image URL" ] ]
        command "removeAlbum"     , [[name:"partist", type: "STRING", description: "Enter the Artist" ], [name:"palbum", type: "STRING", description: "Enter the Album" ] ]
        command "removeArtist"    , [[name:"partist", type: "STRING", description: "Enter the Artist" ] ]
        command "clearAlbumList"
	}

	preferences {
		
        input(name: "country", type: "string", title:"Preferred country for searches (2 characters)", displayDuringSetup: true, defaultValue: "US")
        input(name: "DebugLogging", type: "bool", title:"Enable Debug Logging", displayDuringSetup: true, defaultValue: false)
        input(name: "WarnLogging", type: "bool", title:"Enable Warning Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "ErrorLogging", type: "bool", title:"Enable Error Logging", displayDuringSetup: true, defaultValue: true)
        input(name: "InfoLogging", type: "bool", title:"Enable Description Text (Info) Logging", displayDuringSetup: true, defaultValue: false)
    }
}

def addAlbum(String partist, String palbum, String pimageURL) {
    updateAlbumList(partist, palbum, '', pimageURL)
}



def removeAlbum(String partist, String palbum) {
    if(state.albumList?.get((partist)) != null) { 
        debugLog("removeAlbum: ${partist} found, removing ${palbum}")
        state.albumList.get((partist)).remove((palbum))
        if(state.albumList.get((partist)).isEmpty()) { state.albumList.remove((partist)) }
    }
    else { debugLog("removeAlbum: Unable to find ${partist}") }
}

def removeArtist(String partist) {
    if(state.albumList?.get((partist)) != null) { 
        debugLog("removeArtist: ${partist} found")
        state.albumList.remove((partist))
    }
    else { debugLog("removeArtist: Unable to find ${partist}") }
}


void clearAlbumList() {
     state.albumList = [:]
}

void installed() {
    debugLog("installed: device installed")
    state.albumList = [:];
}

void updated() { debugLog("updated: device updated") }


void refresh() { debugLog("refresh: device refreshed") }

void setCurrentAlbum(String partist, String palbum) {
    
    String[] mbIdList = new String[3];
    String mbId = null;
    String coverArtURL = null;
    
    String vartist = partist.replace('**comma**',',');
    String valbum  = palbum.replace('**comma**',',');
    
    if(vartist != partist) { debugLog("setCurrentAlbum: translated ${partist} to ${vartist}") }
    if(valbum != palbum) { debugLog("setCurrentAlbum: translated ${palbum} to ${valbum}") }
    setArtist(vartist);
    setAlbum(valbum);
    
    String urlEncoding = 'UTF-8';
    String bodyJson = '';
    def getParams = [:];
    def headers = [:];
    String uri = '';
    
    if(!findAlbum(vartist, valbum)) {
        String uriNoCountry = "https://musicbrainz.org/ws/2/release/?query=artistname:%22${URLEncoder.encode(vartist,urlEncoding)}%22%20AND%20release:%22${URLEncoder.encode(valbum,urlEncoding)}%22%20AND%20primarytype:%22Album%22%20AND%20status:%22official%22";
        
        uri = "${uriNoCountry}%20AND%20country:%22${country}%22";
        
        headers.put("accept", "application/json")
        getParams = [
            uri: uri,
            headers: headers,
            contentType: "application/json",
            requestContentType: "application/json",
	    	body : bodyJson
	    ]
               
	    try {
            httpGet(getParams)
            { resp -> 
                debugLog("setCurrentAlbum: resp = ${resp.data}")
                
                if(resp.data.releases[0]?.id?.value != null) { mbIdList[0] = "${resp.data.releases[0]?.id?.value}" }
                if(resp.data.releases[1]?.id?.value != null) { mbIdList[1] = "${resp.data.releases[1]?.id?.value}" }
                if(resp.data.releases[2]?.id?.value != null) { mbIdList[2] = "${resp.data.releases[2]?.id?.value}" }
                debugLog("setCurrentAlbum: mbIds = ${mbIdList[0]}, ${mbIdList[1]}, ${mbIdList[2]}")
            }
        }
        catch(Exception e)
        {
            errorLog("setCurrentAlbum: Error looking up MusicBrainz Id -  ${e}")   
        }
        if(mbIdList[0] == null) {
         
            getParams.uri = uriNoCountry;
            httpGet(getParams)
            { resp -> 
                debugLog("setCurrentAlbum: resp = ${resp.data}")
                
                if(resp.data.releases[0]?.id?.value != null) { mbIdList[0] = "${resp.data.releases[0]?.id?.value}" }
                if(resp.data.releases[1]?.id?.value != null) { mbIdList[1] = "${resp.data.releases[1]?.id?.value}" }
                if(resp.data.releases[2]?.id?.value != null) { mbIdList[2] = "${resp.data.releases[2]?.id?.value}" }
                debugLog("setCurrentAlbum: mbIds = ${mbIdList[0]}, ${mbIdList[1]}, ${mbIdList[2]}")
            }
        }
        boolean imageFound = false;
        for(int i = 0; i < 3 && coverArtURL == null && mbIdList[i] != null; i++) {
            mbId = mbIdList[i];
            uri = "https://ia600900.us.archive.org/14/items/mbid-${mbId}/index.json"
            getParams.uri = uri
            
            try {
                httpGet(getParams) { resp -> 
                    //log.debug("setCurrentAlbum: resp = ${resp.data.images}")
                    //log.debug("setCurrentAlbum: resp = ${resp.data.images?.get(0)}")
                    coverArtURL = "${resp.data.images?.get(0)?.thumbnails?.small}";
                    
                    debugLog("setCurrentAlbum: Cover Art URL = ${coverArtURL}")
                    
                }
            }
            catch(Exception e)
            {
                errorLog("setCurrentAlbum: Error looking up Cover Art Archive - ${e}")   
            }
        }
        if(coverArtURL != null) {
            debugLog("setCurrentAlbum: updating album list for Artist: ${vartist}, Album: ${valbum}, MB Id: ${mbId}, Cover Art URL: ${coverArtURL}");
            updateAlbumList(vartist, valbum, mbId, coverArtURL)
        }
    }
    
    setCoverArtTile(vartist, valbum)
}

boolean findAlbum(String partist, String palbum) {

    boolean result = false;
    def albumDetail = [:];
    
    if(state.albumList != null) {
        albumDetail = state.albumList.get(partist)?.get(palbum);
    }
    debugLog("findAlbum: Artist = ${partist}, Album = ${palbum}, Album Detail = ${albumDetail}")
    if (albumDetail != null) { result = true }
    return result
}

def getArtistAlbums(String partist) {

    def artistAlbums;
    
    if(state.albumList != null) {
        artistAlbums = state.albumList.get(partist);
    }
    if(artistAlbums == null){ artistAlbums = [:] }
    
    debugLog("getArtistAlbums: Artist = ${partist}, albums = ${artistAlbums}")
    return artistAlbums
}


void updateAlbumList(String partist, String palbum, String pmbId, String pcoverArtURL) {

    def artistAlbums = [:];
    
    debugLog("updateAlbumList: method called")
    if(!findAlbum(partist, palbum)) {
        
        debugLog("updateAlbumList: Album not found, about to add ${partist}, Album: ${palbum}, MB Id: ${pmbId}, Cover Art URL: ${pcoverArtURL}");
        
        if (state.albumList == null) { state.albumList = [:] }
        artistAlbums = getArtistAlbums(partist)
        
        if(artistAlbums.isEmpty()) { artistAlbums.putAll([(palbum): [(pmbId): (pcoverArtURL)]]) }
        else { artistAlbums.putAll([(palbum): [(pmbId): (pcoverArtURL)]]) }
        
        state.albumList.putAll([(partist): artistAlbums])
    }
}

void setAlbum(String palbum) {
    debugLog("setAlbum: album provided = ${palbum}")
    device.sendEvent(name: "album", value: palbum);
}

void setArtist(String partist) {
    debugLog("setArtist: artist provided = ${partist}")
    device.sendEvent(name: "artist", value: partist);
}

void setAlbumMBId(String pmbId) {
    
    debugLog("setAlbumMBId: Album MB Id provided = ${pmbId}")
    device.sendEvent(name: "albumMBId", value: pmbId);
}

void setCoverArtURL(pcoverArtURL) {
    
    debugLog("setCoverArtURL: Cover Art URL provided = ${pcoverArtURL}")
    device.sendEvent(name: "coverArtURL", value: pcoverArtURL);
}

void setCoverArtTile(String partist, String palbum) {
 
    debugLog("setCoverArtTile: Artist provided = ${partist}, Album provided = ${palbum}")
    
    String coverArtURL = '';
    if(findAlbum(partist, palbum)) {
        
        setAlbumMBId(coverArtURL = state.albumList.get(partist).get(palbum).entrySet().iterator().next().getKey());
        
        coverArtURL = state.albumList.get(partist).get(palbum).entrySet().iterator().next().getValue();
        device.sendEvent(name: "coverArtTile", value: "<img src=\"${coverArtURL}\" style=\"max-width: 250px\" />")
        setCoverArtURL(coverArtURL);
    }
}

void correctAlbumMBId(String partist, String palbum, String pmbId) {
    
    debugLog("correctAlbumMBId: Artist provided = ${partist}, Album provided = ${palbum}, MB Id = ${pmbId}")
    debugLog("correctAlbumMBId: Yet to be implemented")
}

//Utility methods
void debugLog(debugMessage) {
	if (DebugLogging == true) {log.debug(debugMessage)}	
}

void errorLog(errorMessage) {
    if (ErrorLogging == true) { log.error(errorMessage)}  
}

void infoLog(infoMessage) {
    if(InfoLogging == true) {log.info(infoMessage)}    
}

void warnLog(warnMessage) {
    if(WarnLogging == true) {log.warn(warnMessage)}    
}
