#!/usr/bin/python

from java.io import FileInputStream
import java.lang
import os
import string
 
propInputStream = FileInputStream("domain.properties")
configProps = Properties()
configProps.load(propInputStream)

Username = configProps.get("username")
Password = configProps.get("password")
Host = configProps.get("host")
adminPort = configProps.get("admin.port")
nmUser = configProps.get("nm.user")
nmPassword = configProps.get("nm.password")
nmPort = configProps.get("nm.port")
nmHome = configProps.get("nm.home")
domainName = configProps.get("domain.name")
domainDir = configProps.get("domain.dir")
repServer = configProps.get("rep.server")
 
startNodeManager(verbose='true', NodeManagerHome=nmHome, ListenAddress=Host, ListenPort=nmPort)

print ''
print '============================================='
print ' NODE MANAGER started successfully...!!!'
print '============================================='
print ''

# Connect to Node Manager
time.sleep(5)
nmConnect(nmUser, nmPassword, Host, nmPort, domainName, domainDir, 'ssl')


# Start the Admin Server
nmStart('AdminServer')

print ''
print '============================================='
print ' Admin Server started successfully...!!!'
print '============================================='
print ''


# Status of Admin Server
nmServerStatus('AdminServer')

# Connect to Admin Server
connect(Username, Password, url='t3://' + Host + ':' + adminPort);

# Start all Managed Server
try:
	print 'Loop through the managed servers and start all servers ';
	domainConfig()
	svrs = cmo.getServers()
	domainRuntime()
	for server in svrs:
		# Do not start the adminserver, it's already running
		if server.getName() != 'AdminServer':
			# Get state and machine
			print "Server " + server.getName();
			slrBean = cmo.lookupServerLifeCycleRuntime(server.getName())
			serverState = slrBean.getState()
			print '======================================================================================'
			print server.getName() + " is " + serverState
			print '======================================================================================'
			if (serverState == "SHUTDOWN") or (serverState == "FAILED_NOT_RESTARTABLE"):
				print "Starting " + server.getName();
				start(server.getName(),'Server')
				slrBean = cmo.lookupServerLifeCycleRuntime(server.getName())
				serverState = slrBean.getState()
			print '======================================================================================'
			print server.getName() + " is " + serverState
			print '======================================================================================'

except:
	print 'Exception while starting managed servers !';
	dumpStack();

# Start Reports Server
nmStart(serverName=repServer, serverType='ReportsServerComponent')

# Disconnect from  Admin Server
print 'Disconnect from the Admin Server...'
disconnect()
exit()
 
