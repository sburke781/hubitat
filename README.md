# hubitat

This respository includes device drivers and applications I have developed for the Hubitat Elevation (HE) platform and wish to make available to the broader hubitat community.  Descriptions and installation instructions for these pieces of code are outlined below.

MELCloud Device Drivers

Description

The MELCloud mobile application and web-based interface is offered to customers of Mitsubishi Electric who purchase wi-fi adaptors for their air conditioners and heat pumps across Europe.  In addition to their own user interface, Mitsubishi Electric have also made a REST API available for third-party developers to connect and interact with Mitsubishi Electric devices that have registered with this service.

The MELCloud Parent and Child drivers in this repository allow owners of a HE hub to
* View the status of their Mitsubishi Eletric devices
* Control their devices through the HE platform, including within the Rule Machine engine and HE dashboards

Installation Instructions

Step 1 - Create Parent MELCloud Device

1. Open the devices page on your HE hub, e.g. http:// *** HE-Hub-IP *** /device/list
2. Click the Add Virtual Device button
3. In the Device Information section, enter a Device Name, e.g. "MELCloud".  Note, this can be changed at a later point if necessary
4. In the Type drop-down list select the MelCloud Parent Driver - Note, user drivers are listed towards the end of the drop-down list.
5. Click the Save Device button

The device edit screen for the MELCloud device should now be displayed

Step 2 - Configuration of the MELCloud Service Device

1. Locate the MELCloud parent device from within the HE device listing, i.e. http://~HE-Hub-IP~/device/list, left clicking on the device to open the device edit screen
2. In the device preferences, enter the MELCloud Username / Email and MELCloud Account Password used to login to the MELCloud service
3. Click Save Preferences

Step 3 - Creation of Child AC and Heat Pump Units

1. In the MELCloud Parent Device edit screen, click the Refresh command button
2. Wait for 15-20 seconds and click the refresh button on your browser tab / window
3. Towards the botton of the Device edit screen, review the Device Details table, in particular the Component Devices entry, ensuring a child device appears representing each air conditioner or heat pummp configured in the MELCloud mobile application and web UI.

You are now setup in terms of the devices. You can open the device edit screen for the child device and start adjusting modes, temperature, etc. You can also add the child device to a dashboard as a tile with the thermostat template.
