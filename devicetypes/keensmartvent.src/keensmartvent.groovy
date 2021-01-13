import physicalgraph.zigbee.zcl.DataType

// keen home smart vent
// http://www.keenhome.io
// SmartThings Device Handler v1.0.0

metadata {
    definition (name: "My Keen Home Smart Vent", namespace: "yracine", author: "Keen Home", ocfDeviceType: "x.com.st.d.vent") {
        capability "Switch Level"
        capability "Switch"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "Temperature Measurement"
        capability "Battery"
        capability "Health Check"
        capability "Atmospheric Pressure Measurement"

        fingerprint profileId: "0104", inClusters: "0000,0001,0003,0004,0005,0006,0008,0020,0402,0403,0B05,FC01,FC02", outClusters: "0019", deviceJoinName: "Keen Home Vent"
    }

    // simulator metadata
    simulator {
        // status messages
        status "on": "on/off: 1"
        status "off": "on/off: 0"

        // reply messages
        reply "zcl on-off on": "on/off: 1"
        reply "zcl on-off off": "on/off: 0"
    }
	attribute "pressure", "number"
    // UI tile definitions
    tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on", action: "switch.off", icon: "st.vents.vent-open-text", backgroundColor: "#00a0dc"
            state "off", action: "switch.on", icon: "st.vents.vent-closed", backgroundColor: "#ffffff"
            state "obstructed", action: "clearObstruction", icon: "st.vents.vent-closed", backgroundColor: "#ff0000"
            state "clearing", action: "", icon: "st.vents.vent-closed", backgroundColor: "#ffff33"
        }
        controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 2, inactiveLabel: false) {
            state "level", action:"switch level.setLevel"
        }
        standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        valueTile("temperature", "device.temperature", inactiveLabel: false) {
            state "temperature", label:'${currentValue}°',
                    backgroundColors:[
                            // Celsius
                            [value: 0, color: "#153591"],
                            [value: 7, color: "#1e9cbb"],
                            [value: 15, color: "#90d2a7"],
                            [value: 23, color: "#44b621"],
                            [value: 28, color: "#f1d801"],
                            [value: 35, color: "#d04e00"],
                            [value: 37, color: "#bc2323"],
                            // Fahrenheit
                            [value: 40, color: "#153591"],
                            [value: 44, color: "#1e9cbb"],
                            [value: 59, color: "#90d2a7"],
                            [value: 74, color: "#44b621"],
                            [value: 84, color: "#f1d801"],
                            [value: 95, color: "#d04e00"],
                            [value: 96, color: "#bc2323"]
                    ]
        }
        valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
            state "battery", label: 'Battery \n${currentValue}%', backgroundColor:"#ffffff"
        }
        main "switch"
        details(["switch","refresh","temperature","levelSliderControl","battery"])
    }
}

def getPRESSURE_MEASUREMENT_CLUSTER() {0x0403}
def getMFG_CODE() {0x115B}

def parse(String description) {
    log.debug "description: $description"
    def event = zigbee.getEvent(description)
    if (!event) {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.attrInt == 0x0021) {
            event = getBatteryPercentageResult(Integer.parseInt(descMap.value, 16))
        } else if (descMap?.clusterInt == PRESSURE_MEASUREMENT_CLUSTER && descMap.attrInt == 0x0020) {
            // manufacturer-specific attribute
            event = getPressureResult(Integer.parseInt(descMap.value, 16))
            sendEvent([name: "pressure", value: event.value, unit : "Pa"])  // for backward compatibility
            
        }
    } else if (event.name == "level") {
        if (event.value > 0 && device.currentValue("switch") == "off") {
            sendEvent([name: "switch", value: "on"])
        }
	    if (event.value == 255) {
    	    log.debug "${linkText} is obstructed"
	        // Just return here. Once the vent is power cycled
    	    // it will go back to the previous level before obstruction.
	        // Therefore, no need to update level on the display.
            sendEvent([name: "switch", value: "obstructed", descriptionText: "${linkText} is obstructed. Please power cycle."])
    	}
        
    }

    log.debug "parsed event: $event"
    createEvent(event)
}

def getBatteryPercentageResult(rawValue) {
    // reports raw percentage, not 2x
    def result = [:]

    if (0 <= rawValue && rawValue <= 100) {
        result.name = 'battery'
        result.translatable = true
        result.descriptionText = "${device.displayName} battery was ${rawValue}%"
        result.value = Math.round(rawValue)
    }

    return result
}

def getPressureResult(rawValue) {
    def kpa = rawValue  // reports are in Pascals
    return [name: "atmosphericPressure", value: kpa, unit: "Pa"]
}

/**** COMMAND METHODS ****/



// takes a level from 0 to 100 and translates it to a ZigBee move to level with on/off command
private def makeLevelCommand(level) {
    def rangeMax = 254
    def scaledLevel = Math.round(level * rangeMax / 100)
    log.debug "scaled level for ${level}%: ${scaledLevel}"

    // convert to hex string and pad to two digits
    def hexLevel = new BigInteger(scaledLevel.toString()).toString(16).padLeft(2, '0')

    "st cmd 0x${device.deviceNetworkId} 1 8 4 {${hexLevel} 0000}"
}

/**** COMMAND METHODS ****/
def on() {
    def linkText = getLinkText(device)
    log.debug "open ${linkText}"

    // only change the state if the vent is not obstructed
    if (device.currentValue("switch") == "obstructed") {
        log.error("cannot open because ${linkText} is obstructed")
        return
    }

	setLevel(100)	
}

def off() {
    def linkText = getLinkText(device)
    log.debug "close ${linkText}"

    // only change the state if the vent is not obstructed
    if (device.currentValue("switch") == "obstructed") {
        log.error("cannot close because ${linkText} is obstructed")
        return
    }

	setLevel(0)
}

def clearObstruction() {
    def linkText = getLinkText(device)
    log.debug "attempting to clear ${linkText} obstruction"

    sendEvent([
        name: "switch",
        value: "clearing",
        descriptionText: "${linkText} is clearing obstruction"
    ])

    // send a move command to ensure level attribute gets reset for old, buggy firmware
    // then send a reset to factory defaults
    // finally re-configure to ensure reports and binding is still properly set after the rtfd
    [
        makeLevelCommand(device.currentValue("level")), "delay 500",
        "st cmd 0x${device.deviceNetworkId} 1 0 0 {}", "delay 5000"
    ] + configure()
}

def setLevel(value, rate = null) {
    log.debug "setting level: ${value}"
    def linkText = getLinkText(device)

    // only change the level if the vent is not obstructed
    def currentState = device.currentValue("switch")

    if (currentState == "obstructed") {
        log.error("cannot set level because ${linkText} is obstructed")
        return
    }

    sendEvent(name: "level", value: value)
    if (value > 0) {
        sendEvent(name: "switch", value: "on", descriptionText: "${linkText} is on by setting a level")
    }
    else {
        sendEvent(name: "switch", value: "off", descriptionText: "${linkText} is off by setting level to 0")
    }

    makeLevelCommand(value)
}

def refresh() {
    zigbee.onOffRefresh() +
    zigbee.levelRefresh() +
    zigbee.readAttribute(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, 0x0000) +
    zigbee.readAttribute(PRESSURE_MEASUREMENT_CLUSTER, 0x0020, [mfgCode: MFG_CODE]) +
    zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    zigbee.levelRefresh()
}

def configure() {
    log.debug "CONFIGURE"

    // Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
    // enrolls with default periodic reporting until newer 5 min interval is confirmed
    sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

    def cmds = [
            zigbee.temperatureConfig(30, 300) +
            zigbee.onOffConfig() +
            zigbee.addBinding(zigbee.LEVEL_CONTROL_CLUSTER) +
            zigbee.addBinding(PRESSURE_MEASUREMENT_CLUSTER) +
            zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021, DataType.UINT8, 600, 21600, 0x01) + // battery precentage
        // Yves Racine 2015/09/10: temp and pressure reports are preconfigured, but
        //   we'd like to override their settings for our own purposes
        // temperature - type: int16s, change: 0xA = 10 = 0.1C, 0x32=50=0.5C
        "zcl global send-me-a-report 0x0402 0 0x29 300 600 {3200}", "delay 200" +
        "send 0x${device.deviceNetworkId} 1 1", "delay 1500" +

        // Yves Racine 2015/09/10: use new custom pressure attribute
        // pressure - type: int32u, change: 1 = 0.1Pa, 500=50 PA
        "zcl mfg-code 0x115B", "delay 200" +
        "zcl global send-me-a-report 0x0403 0x20 0x22 300 600 {01F400}", "delay 200" +
        "send 0x${device.deviceNetworkId} 1 1", "delay 1500" 
	]
   return refresh() + delayBetween(cmds)
}


