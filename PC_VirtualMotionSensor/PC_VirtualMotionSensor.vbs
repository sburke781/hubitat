Set objShell = CreateObject("Wscript.shell")
objShell.run "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -executionpolicy bypass -File " & Chr(34) & "PC_VirtualMotionSensor.ps1" & Chr(34), 0
Set objShell = Nothing
