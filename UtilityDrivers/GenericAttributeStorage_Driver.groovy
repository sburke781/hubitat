/**
 *  Generic Attribute Storage Driver
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
 *    2021-03-07  Simon Burke    Original Creation
 *
 */
metadata {
	definition (name: "Generic Attribute Storage Driver", namespace: "simnet", author: "Simon Burke") {
        attribute "stringAttribute", "string"
        attribute "integerAttribute", "integer"
        attribute "floatAttribute", "number"
        attribute "dateAttribute", "date"
        
        
        command "setStringAttribute", [[name:"stringValue", type: "STRING", description: "Enter the String value to be stored" ] ]
        command "setIntegerAttribute", [[name:"integerValue", type: "INTEGER", description: "Enter the Integer value to be stored" ] ]
        command "setFloatAttribute", [[name:"floatValue", type: "NUMBER", description: "Enter the Float value to be stored" ] ]
        command "setDateAttribute", [[name:"dateValue", type: "DATE", description: "Enter the Date value to be stored" ] ]
        
	}

}


def updated() {}

def setStringAttribute(stringValue) {
    try {
        sendEvent(name: "stringAttribute", value : stringValue);
    }
    catch(Exception e) {
        log.error("setStringAttribute: Error storing string value ${stringValue}, the error is ${e}")
    }
    
}

def setIntegerAttribute(integerValue) {
    try {
        sendEvent(name: "integerAttribute", value : integerValue);
    }
    catch(Exception e) {
        log.error("setIntegerAttribute: Error storing integer value ${integerValue}, the error is ${e}")
    }
    
}

def setFloatAttribute(floatValue) {
    try {
        sendEvent(name: "floatAttribute", value : floatValue);
    }
    catch(Exception e) {
        log.error("setFloatAttribute: Error storing float value ${floatValue}, the error is ${e}")
    }
    
}

def setDateAttribute(dateValue) {
    try {
        sendEvent(name: "dateAttribute", value : dateValue);
    }
    catch(Exception e) {
        log.error("setDateAttribute: Error storing date value ${dateValue}, the error is ${e}")
    }
    
}
