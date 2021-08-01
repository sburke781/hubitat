# Summary

The Unified Thermostat drivers enable air conditioning units to be integrated into a Hubitat Elevation system, providing details on capabilities of each unit, status information, as well as control of the unit to adjust operating mode and temperature settings.  Current platforms supported are:

* Mitsubishi Electric's Kumo Cloud in the United States
* Mitsubishi Electric's MELCloud in Europe
* Mitsubishi Electric's MELView in Australia and New Zealand

The drivers have been designed to allow for easy expansion to other platforms outside of Mitsubishi's if users of the Hubitat Elevation system request it.

More details and discussion can be found on the Hubitat Community forum:

https://community.hubitat.com/t/release-unified-thermostat-driver-alpha-melcloud-melview-kumo-cloud-and-more-if-you-want

# Installation
For those already familiar with using Community developed drivers, there are three main steps to getting up and running with the the Unified Thermostat:

1. Install the 2 drivers onto the Hubitat Elevation hub, either manually or through Hubitat Package Manager
1. Creat a virtual (parent) device
1. Configure the parent device, nominating the platform to use and entering authentication credentials

That's it!!  You now have the ability to control your air conditioning units through dashboards, Rule Machine rules and other applications.

For more detailed instructions, please read on...

There are aso some notes on additional configuration settings at the end of this article that may be of interest.

## Installing Unified Thermostat Drivers

The drivers can be installed using one of two methods:

1. Manually importing the two drivers into the **Drivers Code** section of the Hubitat Elevation web interface, using the links below:
    1. https://raw.githubusercontent.com/sburke781/hubitat/master/UnifiedThermostat/UnifiedThermostatParent_Driver.groovy
    1. https://raw.githubusercontent.com/sburke781/hubitat/master/UnifiedThermostat/UnifiedThermostatUnitChild_Driver.groovy

1. Using the Community developed application **Hubitat Package Manager (HPM)**
    1. Details on installing this custom application and drivers such as these can be found in the GitHub repository, plus additional discussion and notes can be found at the HPM Hubitat Community forum topic, both linked below:

        1. Documentation: https://github.com/dcmeglio/hubitat-packagemanager
        1. Community Discussion: https://community.hubitat.com/t/beta-hubitat-package-manager
    1. When searching for the Unified Thermostat drivers in HPM, they have been tagged under:
        1. Climate Control
        1. Temperature and Humidity
        1. Cloud
        1. LAN

## Creating / Configuring Virtual Thermostat Devices

  1. Open the **Devices** page in the Hubitat web interface
  1. Click the **Add Virtual Device** button at the top right of the page
  1. Type in a **Device Name**, a suggestion would be to name this device after the platform being used, e.g. Kumo Cloud, MELView or MELCloud, but the name used does not impact the functioning of the device.
  1. For the **Type** select the parent driver, i.e. **Unified Thermostat Parent Driver**
  1. Click the **Save Device** button
  1. The **Device Edit** page will be displayed
  1. In the **Preferences** section, select the **Platform** and enter in the **Username** and **Password** used for Authenticating with the platform selected
      1. For those using the MELCloud platform in Europe, also select the appropriate **Language**.
  1. If needed, select the temperature scale used by the platform
  1. Click the **Save Preferences** button to save the changes
  1. At the bottom of the page each air conditioning unit detected will be displayed in the Component Devices section of the table, along with a Scheduled Job to initialize each child device in 30 seconds
  1. Wait approximately 60 seconds and refresh the browser window
  1. The Scheduled Job should have disappeared
  
You can now click on one of the child devices and start interacting with it, changing it's mode, set temperature, etc.  You can also add the thermostat devices to a dashboard or make use of them in Rule Machine Rules and other applications.


## Optional Configuration Settings

### Automatic Status Polling
Each air conditioning unit device will automatically update it's status every 10 minutes.  This can be turned on or off using the **Automatic Status Polling** Preference or the frequency changed using the **Status Polling Interval** Preference.  Although not shown as a listing, the frequencies supported are 1, 2, 5, 10, 30 and 60 minutes.

### Fan Modes - Text or Numbers?
This option toggles Fan Modes / Speeds between being displayed as numbers 1-5 (or whatever range is appropriate for the unit) or as text-based settings of Low, Low-Medium, Medium, Medium-High and High.  This allows for a similar experience to the different native mobile applications provided by each platform.  The choice of how to display Fan Modes does not impact the integration with the unit, it is purely for display purposes, and doesn't even need to match what is displayed in the native mobile application.

### Logging
The default logging will capture warnings and errors that occur, however more detail information logging can be turned on to capture details such as changes in temperature settings, mode and other state changes.  In addition, debug logging can be turned on to provide a much larger amount of information about what is happening.  Debug logging is only intended for troubleshooting purposes and should not be left on for an extended period of time.  There is also potentially sensitive information logged with this setting turned on, so please get in touch with the developer if you have any concerns.

Information and Debugging logging can be turned on or off in the Device Edit page of the parent virtual device.

### Override Hubitat Elevation Hub Temperature Scale
Each HE hub is configured with a default temperature scale, typically based on the location nominated to represent the phsical location of the hub.  This hub setting is used in the unified thermostat drivers to determine whether temperatures need to be converted when reading them in or setting them on the MEL / Kumo platforms.  This HE Hub temperature scale can be overridden on the parent thermostat device, without needing to adjust the temperature scale used across the rest of the HE hub.  The Override HE Temp Scale command can be run, passing in "C" (Celsius) or "F" (Fahrenheit), or no value can be passed in to revert to reading from the HE hub setting.  When overriden, a heTempScale attribute will appear in the Current States section of the Device Edit page, with a value of F or C.

## Limitations / Future Enhancements

* Local Control

Currently all communication is performed through the appropriate cloud services.  There are plans shortly after the official release of these drivers to start looking at local control of the air conditioning units.

* Thermostat Scheduling

The Hubitat Elevation system does allow for scheduling of thermostat changes.  This is not currently supported, but may be looked at in the future if there is enough interest from users.

* Thermostat Commands

The Emergency Heat, Fan Auto and Fan Circulate commands that are part of the Thermostat definition in Hubitat are not currently supported.

* Other Enhancements

Other suggestions can be made and tracked under the Unified Thermostat Drivers project (https://github.com/sburke781/hubitat/projects/3) in this repository.
