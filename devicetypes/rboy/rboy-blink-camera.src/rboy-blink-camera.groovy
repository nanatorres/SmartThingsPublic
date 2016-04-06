/* **DISCLAIMER**
* THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
* HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
* Without limitation of the foregoing, Contributors/Regents expressly does not warrant that:
* 1. the software will meet your requirements or expectations;
* 2. the software or the software content will be free of bugs, errors, viruses or other defects;
* 3. any results, output, or data provided through or generated by the software will be accurate, up-to-date, complete or reliable;
* 4. the software will be compatible with third party software;
* 5. any errors in the software will be corrected.
* The user assumes all responsibility for selecting the software and for the results obtained from the use of the software. The user shall bear the entire risk as to the quality and the performance of the software.
*/ 

/**
* Blink Camera
* v3.4.0
*
* Copyright RBoy, redistribution of code is not allowed without permission
* Change log:
* 2016-3-22 - Added support to check for sync module going offline and reporting it to the user (don't send commands if sync module is offline)
* 2016-3-15 - Changed imageDataJpeg to a Base64 string instead of UTF-8 to make it compatible with SmartTiles
* 2016-3-15 - Fixed updated function to call refresh automatically after updating settings
* 2016-3-12 - Updated the icons to bring it more inline with the native app
* 2016-3-11 - Added support for imageDataJpeg attribute to report the JPEG picture as a UTF-8 String
* 2016-3-8 - Fix messages
* 2016-3-7 - Debugging stuff added
* 2016-3-4 - Added support to customize switch interface behavior to either control individual cameras or the entire system (sync module)
* 2016-3-3 - Added support for capability Battery
* 2016-3-2 - Switch/siren/default action is now to enable/disable camera. Motion must be enabled at global level 
* 2016-3-1 - Added support for Motion Event notifications through Motion Sensor Interface
* 2016-2-29 - Added support for forcing camera sensors update and showing last time they were updated
* 2016-2-27 - Added support for WiFi and LFR signal
* 2016-2-27 - Fixed issues with camera pictures and referred processing
* 2016-2-26 - Added support for controlling the camera through Rules Machines (monitorOn/monitorOff)
* 2016-2-26 - Added support for temperature
* 2016-2-23 - Initial release
*/

metadata {
    definition (name: "RBoy Blink Camera", namespace: "rboy", author: "RBoy") {
        capability "Polling"
        capability "Image Capture"
        capability "Alarm"
        capability "Relay Switch"
        capability "Switch"
        capability "Refresh"
        capability "Motion Sensor"
        capability "Sensor"
        capability "Temperature Measurement"
        capability "Battery"

        // Custom attributes
        attribute "monitorStatus", "string"
        attribute "enableStatus", "string"
        attribute "wifi", "number"
        attribute "lfr", "number"
        attribute "lastUpdate", "string"
        attribute "imageDataJpeg", "string"

        // Local commands
        command "toggleCamera"
        command "enableCamera"
        command "monitorOn"
        command "monitorOff"
        command "disableCamera"
        command "forceSensorsUpdate"

        // Calls from Parent to Child
        command "generateEvent", ["JSON_OBJECT"]
        command "log", ["string","string"]
        command "saveImage", ["string"]
        command "deferredLoopbackQueue", ["number","string","enum"]
        command "updateSwitchBehavior", ["enum"]
    }

    tiles(scale: 2) {
        multiAttributeTile(name:"summary", type: "generic", width: 6, height: 4){
            tileAttribute ("device.enableStatus", key: "PRIMARY_CONTROL") {
                attributeState "on", label: "On", action: "disableCamera", icon: "http://smartthings.rboyapps.com/images/CameraOn.png", backgroundColor: "#79B821", nextState:"..."
                attributeState "off", label: "Off", action: "enableCamera", icon: "http://smartthings.rboyapps.com/images/CameraOff.png", backgroundColor: "#FFFFFF", nextState:"..."
                attributeState "alarm", label: "Intruder", action: "toggleCamera", icon: "http://smartthings.rboyapps.com/images/CameraOn.png",  backgroundColor: "#FF3333", nextState:"..."
                attributeState "...", label: "...", action:"", nextState:"..."
            }
            tileAttribute ("device.temperature", key: "SECONDARY_CONTROL") {
                attributeState("temperature", label:'${currentValue}°',
                               backgroundColors:[
                                   [value: 31, color: "#153591"],
                                   [value: 44, color: "#1e9cbb"],
                                   [value: 59, color: "#90d2a7"],
                                   [value: 74, color: "#44b621"],
                                   [value: 84, color: "#f1d801"],
                                   [value: 95, color: "#d04e00"],
                                   [value: 96, color: "#bc2323"]
                               ]
                              )
            }
        }

        standardTile("monitorStatus", "device.monitorStatus", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
            state "offline", label: "OFFLINE", action: "refresh", icon: "http://smartthings.rboyapps.com/images/SystemOff.png", backgroundColor: "#FF3333"
            state "on", label: "System Active", action: "monitorOff", icon: "http://smartthings.rboyapps.com/images/SystemOn.png", backgroundColor: "#FFFFFF", nextState:"..."
            state "off", label: "System Inactive", action: "monitorOn", icon: "http://smartthings.rboyapps.com/images/SystemOff.png",  backgroundColor: "#FFFFFF", nextState:"..."
            state "...", label: "...", action:"", nextState:"..."
        }

        standardTile("enableStatus", "device.enableStatus", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
            state "on", label: "On", action: "disableCamera", icon: "http://smartthings.rboyapps.com/images/CameraOn.png", backgroundColor: "#FFFFFF", nextState:"..."
            state "off", label: "Off", action: "enableCamera", icon: "http://smartthings.rboyapps.com/images/CameraOff.png", backgroundColor: "#FFFFFF", nextState:"..."
            state "alarm", label: "Intruder", action: "toggleCamera", icon: "http://smartthings.rboyapps.com/images/CameraOn.png",  backgroundColor: "#FF3333", nextState:"..."
            state "...", label: "...", action:"", nextState:"..."
        }

        standardTile("wifi", "device.wifi", width: 2, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
            state "0", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_0.png", backgroundColor: "#FFFFFF"
            state "1", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_1.png", backgroundColor: "#FFFFFF"
            state "2", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_2.png", backgroundColor: "#FFFFFF"
            state "3", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_3.png", backgroundColor: "#FFFFFF"
            state "4", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_4.png", backgroundColor: "#FFFFFF"
            state "5", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_5.png", backgroundColor: "#FFFFFF"
        }

        standardTile("lfr", "device.lfr", width: 2, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
            state "0", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a1.png", backgroundColor: "#FFFFFF"
            state "1", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a2.png", backgroundColor: "#FFFFFF"
            state "2", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a3.png", backgroundColor: "#FFFFFF"
            state "3", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a4.png", backgroundColor: "#FFFFFF"
            state "4", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a5.png", backgroundColor: "#FFFFFF"
            state "5", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a6.png", backgroundColor: "#FFFFFF"
        }

        carouselTile("cameraDetails", "device.image", width: 6, height: 4) { }

        standardTile("take", "device.image", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
            state "take", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
            state "taking", label:'Taking', action: "", icon: "st.camera.take-photo", backgroundColor: "#53a7c0"
            state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
        }

        valueTile("lastUpdate", "device.lastUpdate", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state "lastUpdate", label:'Last Update\n ${currentValue}', action: "forceSensorsUpdate"
        }

        standardTile("forceUpdate", "device.lastUpdate", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
            state "", label:'Sensor Update', action: "forceSensorsUpdate", icon: "http://smartthings.rboyapps.com/images/sensors.png"
        }

        valueTile("battery", "device.battery", width: 2, height: 2, inactiveLabel: false) {
            state "battery", label:'Battery\n ${currentValue}%', unit: "", backgroundColors:[
                [value: 15, color: "#ff0000"],
                [value: 30, color: "#fd4e3a"],
                [value: 50, color: "#fda63a"],
                [value: 60, color: "#fdeb3a"],
                [value: 75, color: "#d4fd3a"],
                [value: 90, color: "#7cfd3a"],
                [value: 99, color: "#55fd3a"]
            ]
        }

        valueTile("temperature", "device.temperature", width: 2, height: 2) {
            state("temperature", label:'${currentValue}°',
                  backgroundColors:[
                      [value: 31, color: "#153591"],
                      [value: 44, color: "#1e9cbb"],
                      [value: 59, color: "#90d2a7"],
                      [value: 74, color: "#44b621"],
                      [value: 84, color: "#f1d801"],
                      [value: 95, color: "#d04e00"],
                      [value: 96, color: "#bc2323"]
                  ]
                 )
        }

        standardTile("refresh", "device.status", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "refresh", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        standardTile("blank", "device.image", width: 2, height: 1, canChangeIcon: false,  canChangeBackground: false, decoration: "flat") {
            state "blank", label: "", action: "", icon: "", backgroundColor: "#FFFFFF"
        }

        standardTile("blank2x", "device.image", width: 2, height: 2, canChangeIcon: false,  canChangeBackground: false, decoration: "flat") {
            state "blank", label: "", action: "", icon: "", backgroundColor: "#FFFFFF"
        }

        main "summary"
        details(["take", "monitorStatus", "enableStatus", "cameraDetails", "temperature", "refresh", "battery", "lfr", "lastUpdate", "wifi", "blank2x", "forceUpdate", "blank2x"])
    }
}

def initialize() {
    log.trace "Initialize called settings: $settings"
    try {
        if (!state.init) {
            state.init = true
        }
        refresh() // Get the updates
    } catch (e) {
        log.warn "updated() threw $e"
    }
}

def updated() {
    log.trace "Update called settings: $settings"
    try {
        if (!state.init) {
            state.init = true
        }
        response(refresh()) // Get the updates
    } catch (e) {
        log.warn "updated() threw $e"
    }
}

private getPictureName() {
    def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
    "image" + "_$pictureUuid" + ".jpg"
}

//TAKE PICTURE
def take() {
    log.debug("Taking Photo")
    def sent = parent.takePicture(this)
    log.trace "Take picture command sent successfully: $sent"
}
//END TAKE PICTURE

//SWITCH ACTIONS
def on() {
    log.debug "On requested, enabling camera motion alerts"
    log.trace "Checking Switch Interface behavior, Switch Controls System Enable/Disable: $state.switchControlsSystem"
    
    if (state.switchControlsSystem) {
        monitorOn()
    } else {
        enableCamera()
    }
}

def off() {
    log.debug "Off requested, disabling camera motion alerts"
    log.trace "Checking Switch Interface behavior, Switch Controls System Enable/Disable: $state.switchControlsSystem"
    
    if (state.switchControlsSystem) {
        monitorOff()
    } else {
        disableCamera()
    }
}
//END SWITCH ACTIONS

//ALARM ACTIONS
def both() {
    log.debug "Alarm both requested, enabling camera motion alerts and taking picture"
    take()
    log.trace "Checking Switch Interface behavior, Switch Controls System Enable/Disable: $state.switchControlsSystem"
    
    if (state.switchControlsSystem) {
        monitorOn()
    } else {
        enableCamera()
    }
}

def siren() {
    log.debug "Alarm siren requested, enabling camera motion alerts and taking picture"
    take()
    log.trace "Checking Switch Interface behavior, Switch Controls System Enable/Disable: $state.switchControlsSystem"
    
    if (state.switchControlsSystem) {
        monitorOn()
    } else {
        enableCamera()
    }
}

def strobe() {
    log.debug "Alarm strobe requested, enabling camera motion alerts and taking picture"
    take()
    log.trace "Checking Switch Interface behavior, Switch Controls System Enable/Disable: $state.switchControlsSystem"
    
    if (state.switchControlsSystem) {
        monitorOn()
    } else {
        enableCamera()
    }
}
//END ALARM ACTIONS

//GLOBAL MONITOR ACTIONS (SYSTEM ACTIVE/INACTIVE)
def toggleMonitor() {
    log.debug "Toggling Monitor"
    if(device.currentValue("monitorStatus") == "off") {
        monitorOn()
    } else {
        monitorOff()
    }
}

def monitorOn() {
    log.debug "Enabling Monitor"
    def sent = parent.monitorOn(this)
    log.trace "Enable monitor command sent successfully: $sent"
}

def monitorOff() {
    log.debug "Disabling Monitor"
    def sent = parent.monitorOff(this)
    log.trace "Disable monitor command sent successfully: $sent"
}
//END MONITOR ACTIONS

//CAMERA MOTION ALERTS ACTIONS
def toggleCamera() {
    log.debug "Toggling Camera"
    if(device.currentValue("enableStatus") == "off") {
        enableCamera()
    } else {
        disableCamera()
    }
}

def disableCamera() {
    log.debug "Disabling Camera Motion Alerts"
    def sent = parent.disableAlerts(this)
    log.trace "Disable Camera Motion Alerts sent successfully: $sent"
}

def enableCamera() {
    log.debug "Enabling Camera Motion Alerts"
    def sent = parent.enableAlerts(this)
    log.trace "Enable Camera Motion Alerts sent successfully: $sent"
}
//END ALERTS ACTIONS

def parse(String description) {
    log.trace "Parse: $description"
}

def refresh() {
    log.trace "Refresh called"
    def sent = parent.refresh(this)
    log.trace "Refresh command sent successfully: $sent"
}

def poll() {
    log.trace "Poll called"
    refresh()
    sendEvent(name: "enableStatus", value: device.currentValue("enableStatus"), descriptionText: "Keeping Poll Alive", displayed: false, isStateChange: false) // We need to send something otherwise Poll dies if nothing is done
    device.activity()  // workaround to keep polling from being shut off
    null
}

def forceSensorsUpdate() {
    log.trace "Force sensor update called"
    def sent = parent.forceCameraSensorUpdate(this)
    log.trace "Request to force camera sensors update sent successfully: $sent"
}

// Deferred action to call the parent with a pending action
def deferredAction() {
    log.trace "Deferred action called, pending actions: $state.queuedActions"
    Long delay = 99999999 // arbitrary large number to begin with
    def unprocessedActions = []
    def actions = state.queuedActions.clone() // make a copy instead of working on original
    //log.warn "BEFORE:$state.queuedActions"
    state.queuedActions.clear() // Clear it
    //log.warn "AFTER:$state.queuedActions"
    //log.warn "PENDING:$actions"
    for (action in actions) {
        //log.trace "Processing:$action"
        def now = now()
        if (now >= (action.time + (action.delay * 1000))) {
            log.trace "Calling parent action: $action.function"
            try {
                parent."${action.function}"(this) // Call the parent with the context
            } catch (e) { }
        } else {
            unprocessedActions.add(action)
            delay = Math.min(delay, ((((action.time + (action.delay * 1000)) - now) as Float)/1000).round()) // take smallest pending delay and lets use that
            log.trace "Waiting $delay seconds to process deferred action ${action.function}"
        }
    }

    if (unprocessedActions) { // If anything is pending
        log.trace "Adding unprocessed actions back to queue: $unprocessedActions"
        state.queuedActions = state.queuedActions + unprocessedActions // Add back any pending actions (since we are adding an array of maps, use + and not << or .add())
        //log.warn "END:$state.queuedActions"
        runIn(delay > 0 ? delay : 1, deferredAction) // defer the loopback action, check for boundary condition
    }
    log.trace "Deferred action finished, pending actions: $state.queuedActions"
}

// CHILD INTERFACES TO CALL FROM PARENT
// Register the event attributes with the device
def generateEvent(results) {
    log.trace "Generate Event called: ${results.inspect()}"

    results.each { event ->
        //log.trace "Sending event name: ${event.inspect()}"
        sendEvent(event)
    }

    return null // always end child interface calls with a return value
}

// Save the image to the S3 store to display
def saveImage(image) {
    log.trace "Saving image to S3"

    // Send the image to an App who wants to consume it via an event as a Base64 String
    def bytes = image.buf
    //log.debug "JPEG Data Size: ${bytes.size()}"
    String str = bytes.encodeBase64()
    sendEvent(name: "imageDataJpeg", value: str, displayed: false, isStateChange: true)

    // Now save it to the S3 cloud, so this in the end since it removes the data from the object leaving it empty
    storeImage(getPictureName(), image)

    return null
}

def updateSwitchBehavior(switchControlsSystem) {
    log.trace "Updating switch interface behavior, Switch Interface Controls System Enable/Disable: $switchControlsSystem"
    state.switchControlsSystem = switchControlsSystem
    
    return null
}

// Print log message from parent
def log(message, level = "trace") {
    switch (level) {
        case "trace":
        log.trace "LOG FROM PARENT>" + message
        break;

        case "debug":
        log.debug "LOG FROM PARENT>" + message
        break

        case "warn":
        log.warn "LOG FROM PARENT>" + message
        break

        case "error":
        log.error "LOG FROM PARENT>" + message
        break

        default:
            log.error "LOG FROM PARENT>" + message
        break;
    }            

    return null // always end child interface calls with a return value
}

// Call back the parent app function after a delay with an option to overwrite existing queued function calls
def deferredLoopbackQueue(delay, function, overwrite = false) {
    log.trace "Deferred loopback called with delay $delay seconds and function $function, overwrite $overwrite"

    // Save it in the deferral queue
    if (state.queuedActions == null) {
        log.debug "Initializing queued Actions"
        state.queuedActions = [] // initialize it
    }
    //state.queuedActions.clear() // DEBUG CLEAR
    //log.warn "QUEUED ACTIONS: $state.queuedActions" // DEBUG
    
    if (overwrite) { // Find if function exists and remove it
        log.trace "Requested to overwrite existing queued action, checking for existing action $function in queued actions $state.queuedActions"
        def existingAction = state.queuedActions.find { it.function == function }
        if (existingAction) { // We found it
            state.queuedActions.remove(existingAction) // Remove it
            log.trace "Found existing action $existingAction, removed from queued actions $state.queuedActions"
        }
    }
    state.queuedActions.add([function:function, time:now(), delay:(delay as Long)]) // Add if to the queue

    //state.queuedActions.clear() // DEBUG CLEAR
    //log.warn "QUEUED ACTIONS: $state.queuedActions" // DEBUG

    deferredAction() // defer the loopback action

    return null
}