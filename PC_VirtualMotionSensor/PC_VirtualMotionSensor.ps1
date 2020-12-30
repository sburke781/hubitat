# Import Configuration File PS Module
#    Downloaded from https://github.com/alekdavis/ConfigFile
$modulePath = Join-Path (Split-Path -Path $PSCommandPath -Parent) 'ConfigFile.psm1'
Import-Module $modulePath -ErrorAction Stop -Force

# Configuration Settings Variables
$heIPAddress = ""
$heMakerAPIAppNo = ""
$heMakerAPIToken = ""
$heMotionSensorDeviceId = ""
$inactiveSecsThreshold = 1000
$logFile = ""
$logLevel = ""

#Other Variables used throughout the script
$idleSeconds = 0
$motionStatus = ""

# Load configuration settings
#   Loads from the default .ini file with the same file name
#      as this powershell script (except for the extension of course!)
#   Settings can be passed in at the command line
#   Config items must have the same name as variables in this script
#   See more at https://github.com/alekdavis/ConfigFile/blob/master/README.md
Import-ConfigFile -Ini -DefaultParameters $PSBoundParameters

# Log File Output Function
#   Taken from Stack Overflow: https://stackoverflow.com/a/38738942

Function Write-Log {
    [CmdletBinding()]
    Param(
    [Parameter(Mandatory=$False)]
    [ValidateSet("INFO","WARN","ERROR","FATAL","DEBUG")]
    [String]
    $Level = "INFO",

    [Parameter(Mandatory=$True)]
    [string]
    $Message,

    [Parameter(Mandatory=$False)]
    [string]
    $logfile
    )

    $Stamp = (Get-Date).toString("yyyy/MM/dd HH:mm:ss")
    $Line = "$Stamp $Level $Message"
    If($logfile -And $logLevel.contains($Level)) {
        Add-Content $logfile -Value $Line
    }
    Else {
        Write-Output $Line
    }
}

# Last User Input Code
#   Taken from Stack Overflow: https://stackoverflow.com/a/39319540

Add-Type @'
using System;
using System.Diagnostics;
using System.Runtime.InteropServices;

namespace PInvoke.Win32 {

    public static class UserInput {

        [DllImport("user32.dll", SetLastError=false)]
        private static extern bool GetLastInputInfo(ref LASTINPUTINFO plii);

        [StructLayout(LayoutKind.Sequential)]
        private struct LASTINPUTINFO {
            public uint cbSize;
            public int dwTime;
        }

        public static DateTime LastInput {
            get {
                DateTime bootTime = DateTime.UtcNow.AddMilliseconds(-Environment.TickCount);
                DateTime lastInput = bootTime.AddMilliseconds(LastInputTicks);
                return lastInput;
            }
        }

        public static TimeSpan IdleTime {
            get {
                return DateTime.UtcNow.Subtract(LastInput);
            }
        }

        public static int LastInputTicks {
            get {
                LASTINPUTINFO lii = new LASTINPUTINFO();
                lii.cbSize = (uint)Marshal.SizeOf(typeof(LASTINPUTINFO));
                GetLastInputInfo(ref lii);
                return lii.dwTime;
            }
        }
    }
}
'@


# Determine the number of seconds since the last user input
$idleSeconds = ([TimeSpan]::Parse([PInvoke.Win32.UserInput]::IdleTime)).TotalSeconds

# Determine the motion status based on the time since last user input and the
#   threshold configured, reporting as either active or inactive

if( $idleSeconds -lt $inactiveSecsThreshold) { $motionStatus = "active" } else { $motionStatus = "inactive" }
Write-Log "INFO" "$motionStatus, Last Activity $idleSeconds seconds ago" "$logFile"

# Construct the Hubitat Maker API URI and make the web request to update the motion sensor status
$makerURI = "http://$heIPAddress/apps/api/$heMakerAPIAppNo/devices/$heMotionSensorDeviceId/" + $motionStatus + "?access_token=$heMakerAPIToken"
Write-Log "DEBUG" "Maker API URI: $makerURI" "$logFile"
Invoke-WebRequest -Uri "$makerURI" -UseBasicParsing



