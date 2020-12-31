# PC Virtual Motion Sensor

## Description

When working at a desk in a room with Motion Lighting, the traditional motion sensors are not always going to be triggered.  The aim of this script is to capture activity on a PC as a way to keep motion lighting rules active, i.e. lights remaining turned on, while someone is working on the PC.

## How Does it Work
The PC Virtual Motion Sensor is essentially a Windows PowerShell script that can be configured to run periodically on the PC being monitored, detecting the number of seconds since the last user input was recorded, e.g. mouse movement or keyboard strokes.  This reading is compared to a threshold resulting in a status of active or inactive being recorded in a Virtual Motion Sensor device made available through the Maker API.

Once the Virtual Motion Sensor device is being updated on a regular basis, it can be used like any other motion sensor, incluiding in the built-in Motion Lighting app to keep a motion lighting rule active, or in a Rule Machine rule.

Requirements

At this stage it is understood that the user input being reported is for the account used to run the Windows Scheduled Task.  This user needs to be a member of the Local Administrators User Group in order to run the Windows Scheduled Task.

Initial development and testing was performed on a Windows 10 laptop, though this is not strictly a requirement.

## Installation and Configuration

Tip - Keep a Notepad window open during this setup process to paste in a handful of details used later in the configuration of the PowerShell Script

1. Hubitat Virtual Motion Sensor
   1. Open the **Devices** page for your Hubitat Hub and click **Add Virtual Device**
   2. Enter a **Device Name**
   3. In the **Type** drop-down list select the **Virtual Motion Sensor** driver
   4. Optional - If you have multiple hubs and use Hub Mesh, you may want to turn on the Hub Mesh Enabled option
   5. Click **Save Device** to create the new motion sensor
   6. The **Device Setup Page** is then displayed.  Note down the **Device Id**, included at the end of the URL for the page
   7. In the **Preferences** section make sure the **Auto-Inactive** setting is **Disabled** and the **Enable descriptionText Logging** is **turned on**.  If any changes were made click **Save Preferences**
   
2. Maker API
   1. In the Apps section of the Hubitat Hub, add the newly created Virtual Motion Sensor to an existing Maker API install or install and configure a new Maker API installation
   2. From Maker API app, take a copy of the example **Command URL**
   
3. PowerShell Script
   1. Create a folder on the PC being monitored, e.g. C:\HomeAutomation\Hubitat\PC_VirtualMotionSensor
   2. Download the 6 files listed below from this repository, saving them to the folder created in step 3.1:
      * PC_VirtualMotionSensor.log
      * PC_VirtualMotionSensor.ps1.ini
      * PC_VirtualMotionSensor.vbs
      * PC_VirtualMotionSensor.ps1
      * ConfigFile.psm1
      * ConfigFile.psd1
   3. Open the local copy of the **PC_VirtualMotionSensor.ps1.ini** file in Notepad or similar application
   4. Set the **heIPAddress**, **heMakerAPIAppNo** and **heMakerAPIToken** settings using the example commmand URL in step 2.2
   5. Set the **heMotionSensorDeviceId** from step 1.6
   6. The **inactiveSecsThreshold** can be left at 5 minutes (**300 seconds**) or adjusted as required.  In the example of 5 minutes, whilst ever there is user input less than 5 minutes ago, the motion sensor will report as active.  Once the last user input is more than 5 minutes in the past, the motion sensor will report inactive.
   7. If required, adjust the **logFile** setting to be the full path to the local copy of the log file downloaded in step 3.2.  Tip - Hold down the shift key on the keyboard when right-clicking a file and select **Copy as path** to copy the full path to the clipboard and paste this into the configuration INI file.
   8. The **logLevel** can be set with values of INFO, DEBUG or $null to adjust how much is logged to the **PC_VirtualMotionSensor.log** file.  Set this to INFO for the initial setup (this can be adjusted once setup is complete)
   
4. Task Scheduler Task   
   1. From the **Windows Start Menu**, type **Task Scheduler** and press Enter
   2. Right-Click on the **Task Scheduler Library** and select **Create Task**
   3. Give the task a **Name**, **Description (optional)**, select **Run only when user is logged on** and tick **Run with highest privileges**
   4. Click on the **Triggers** tab and click the **New...** button to setup the schedule
   5. The suggested schedule is once every minute, which requires choosing **Daily**, **Recur every 1 days** and **Repeat Task Every 1 Minute**.  To set 1 Minute, select 5 Minutes from the drop-down then change the 5 to a 1.  Adjust the **Start Time** at the top of the trigger settings to be whatever time each day the script should start looking for activity, e.g. **12:01:00 AM**.  Click **Ok** to save the trigger schedule.
   6. Click the **Actions tab** and the **"New..."** button.  For the **Action** setting choose **Start a Program**, for the **Program / Script** paste **C:\WINDOWS\System32\wscript.exe**.  In the **Add Arguments (Optional)** paste the location of the VB Script, e.g. **"C:\HomeAutomation\Hubitat\PC_VirtualMotionSensor\PC_VirtualMotionSensor.vbs"**.  Set the **Start in (Optional)** setting to the folder path where the scripts and other files were installed, e.g. C:\HomeAutomation\Hubitat\PC_VirtualMotionSensor\.  Click **Ok** to save the Action, then **Ok** to save the Task.
   7. Confirm the task runs at the scheduled time by reviewing details in the **History** tab for the task, **right-clicking** in the **Events** table and clicking **Refresh** to see updated list of results.
   8. If the Task Scheduler task is running successfully, review the contents of the **PC_VirtualMotionSensor.log** file, ensuring appropriate status results and time since last input are reported as expected.  If any errors are appearing in the History for the Task or it is not running, go back through the steps above to confirm nothing has been missed or setup incorrectly.  Review the Windows Event Log to see if there are any errors or other issues that may be affecting execution of the task.  Ensure the user account being used to run the task is part of the local administrators group.
   9. On the Hubitat Hub, open the **Logs** page and watch the Live Log to confirm the status of the Virstual Motion Sensor device is being updated with correct details at the expected time.  If logs for the device updates are not appearing, confirm the Virtual Motion Sensor device has the descriptionText Logging preference turned on.  Adjust the **logLevel** setting in the **PC_VirtualMotionSensor.ps1.ini** to include DEBUG, e.g. **logLevel=INFO,DEBUG**, which will result in the Maker API URI being logged.  Confirm the URI recorded in the log file has the correct details and review / update other settings in the **PC_VirtualMotionSensor.ps1.ini** file if required
   
5. Use the motion sensor in motion lighting or RM rules

6. Optional Cleanup of Settings
   1. Turn off the descriptionText Logging preference for the Virtual Motion Sensor device on the Hubitat hub to reduce the amount of logs being generated
   2. Set the **logLevel** in the **PC_VirtualMotionSensor.ps1.ini** to be either INFO or $null, where $null will mean no logs are generated
