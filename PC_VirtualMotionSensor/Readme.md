# PC Virtual Motion Sensor

## Description

When working at a desk in a room with Motion Lighting, the traditional motion sensors are not always going to be triggered.  The aim of this script is to capture activity on a PC as a way to keep motion lighting rules active, i.e. lights remaining turned on, while someone is working on the PC.

## How Does it Work
The PC Virtual Motion Sensor is essentially a Windows PowerShell script that can be configured to run periodically on the PC being monitored, detecting the number of seconds since the last user input was recorded, e.g. mouse movement or keyboard strokes.  This reading is compared to a threshold resulting in a status of active or inactive being recorded in a Virtual Motion Sensor device made available through the Maker API.

Once the Virtual Motion Sensor device is being updated on a regular basis, it can be used like any other motion sensor, incluiding in the built-in Motion Lighting app to keep a motion lighting rule active, or in a Rule Machine rule.

## Installation and Configuration

1. Hubitat Virtual Motion Sensor
   1. Open the Devices page for your Hubitat Hub and click Add Virtual Device
   2. Enter a Device Name
   3. Select the Virtual Motion Sensor driver in the Type drop-down list
   4. Optional - If you have multiple hubs and use Hub Mesh, you may want to turn on the Hub Mesh Enabled option
   5. Click Save Device to create the new motion sensor
   6. On the device setup page that is then displayed, note down the device id which can be obtained from the URL of the page
2. Maker API
   1. Add the new Virtual Motion Sensor to an existing Maker API install or setup a new Maker API install
   2. From the main page for the Maker API app, copy an example command URL 
3. PowerShell Script
   1. Download the 6 files from this repository to the PC to be monitored:
      * PC_VirtualMotionSensor.log
      * PC_VirtualMotionSensor.ps1.ini
      * PC_VirtualMotionSensor.vbs
      * PC_VirtualMotionSensor.ps1
      * ConfigFile.psm1
      * ConfigFile.psd1
   2. Open the **PC_VirtualMotionSensor.ps1.ini** file in Notepad or similar application
   3. Set the **heIPAddress**, **heMakerAPIAppNo** and **heMakerAPIToken** settings using the example commmand URL in step 2.2
   4. Set the **heMotionSensorDeviceId** from step 1.6
   5. The **inactiveSecsThreshold** can be left at 5 minutes (300 seconds) or adjusted as required.  In the example of 5 minutes, whilst ever there is user input less than 5 minutes ago, the motion sensor will report as active.  Once the last user input is more than 5 minutes in the past, the motion sensor will report inactive.
   6. The **logFile** setting is full path to the final location of the log file downloaded in step 3.1.  Tip - Hold down the shift key on the keyboard when right-clicking a file and select **Copy as path** to copy the full path to the file to the clipboard.
   7. The **logLevel** can be set with values of INFO, DEBUG or $null to adjust how much is logged to the **PC_VirtualMotionSensor.log** file
4. Task Scheduler Task   
   1. From the Windows Start Menu, type Task Scheduler and press Enter
   2. Right-Click on the Task Scheduler Library and select Create Task
   3. Give the task a Name, Description (optional) and select "Run only when user is logged on" and tick "Run with highest privileges"
   4. Click on the Triggers tab and click the "New..." button to setup the schedule
   5. The suggested schedule is once every minute, which requires choosing Daily, Recur every 1 days and Repeat Task Every 1 Minute (select 5 Minutes from the drop-down then change the 5 to a 1).  Also, adjust the start time at the top of the trigger settings to be whatever time each day the script should start looking for activity, e.g. 12:01:00 AM.  Click Ok to save the trigger schedule.
   6. Click the **Actions tab** and then the **"New..."** button.  For the **Action** setting choose **Start a Program**, for the **Program / Script** paste **C:\WINDOWS\System32\wscript.exe**.  In the **Add Arguments (Optional)** paste the location of the VB Script, e.g. **"C:\HomeAutomation\Hubitat\PC_VirtualMotionSensor\PC_VirtualMotionSensor.vbs"**.  Other settings can be adjusted as required.  Click Ok to save the task.
   7. Confirm the task runs as expected by reviewing the History for the task
4. Use the motion sensor in motion lighting or RM rules
