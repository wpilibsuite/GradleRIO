'Create File System Object for working with directories
Set fso = WScript.CreateObject("Scripting.FileSystemObject")

'Get the script name, and from that the jar name
jarName = fso.GetBaseName(WScript.ScriptName) + ".jar"

'Get the folder of this script
toolsFolder = fso.GetParentFolderName(WScript.ScriptFullName)

'Get the full path to the Jar
fullJarPath = fso.BuildPath(toolsFolder, jarName)

'Get the full path to the JDK
jdkDir = fso.GetParentFolderName(toolsFolder)
jdkDir = fso.BuildPath(jdkDir, "jdk")
jdkDir = fso.BuildPath(jdkDir, "bin")
jdkDir = fso.BuildPath(jdkDir, "javaw.exe")


' Get the full shell to run
shellScript = """" + jdkDir + """ -jar """ + fullJarPath + """"


'Create Shell Object
Set objShell = WScript.CreateObject( "WScript.Shell" )
dim runObject
' Allow us to catch a script run failure
On Error Resume Next
Set runObj = objShell.Exec(shellScript)
If Err.Number <> 0 Then
	' If script failed, try getting java home
	javaHomeEnv = "%JAVA_HOME%"
	javaHome = objShell.ExpandEnvironmentStrings(javaHomeEnv)
	If (javaHome = javaHomeEnv) Then
		'Java Home not found
		shellScript = "javaw -jar """ + fullJarPath + """"
	Else
		'Java Home found
		shellScript = """" + javaHome + "\\bin\\javaw.exe"" -jar """ + fullJarPath + """"
	End If
	Err.Clear
	Set runObj = objShell.Exec(shellScript)
	If Err.Number <> 0 Then
		If WScript.Arguments.Count > 0 Then
			If (WScript.Arguments(0) <> "silent") Then
				WScript.Echo "Error Launching Tool" + vbCrLf + Err.Description
			Else
				WScript.StdOut.Write("Error Launching Tool")
				WScript.StdOut.Write(Error.Description)
			End If
		Else
			WScript.Echo "Error Launching Tool"  + vbCrLf + Err.Description
		End If
		Set runObj = Nothing
		Set objShell = Nothing
		Set fso = Nothing
		WScript.Quit(1)
	End If
End If

WScript.Sleep 3000
If (runObj.Status <> 0) Then
	outputStd = runObj.StdOut.ReadAll()
	outputErr = runObj.StdErr.ReadAll()
	If WScript.Arguments.Count > 0 Then
		If (WScript.Arguments(0) <> "silent") Then
			WScript.Echo "Tool Failed To Start" + vbCrLf + outputStd + vbCrLf + outputErr
		Else
			WScript.StdOut.Write(output)
			WScript.StdErr.Write(outputErr)
		End If
	Else
		WScript.Echo "Tool Failed To Start" + vbCrLf + outputStd + vbCrLf + outputErr
	End If
	Set runObj = Nothing
	Set objShell = Nothing
	Set fso = Nothing
	WScript.Quit(2)
End If

Set runObj = Nothing
Set objShell = Nothing
Set fso = Nothing
WScript.Quit(0)
