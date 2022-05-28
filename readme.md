# Uptimer

An application that monitors the status of your servers / sites and, if necessary, will write to you in the telegram channel about the fall.

### Features
- Telegram channel notifications;
- Ping servers by ip, ip:port, sites;
- Setting up messages in the config and full customization;
- Writing messages only after three unsuccessful ping attempts (can be configured in the config).

### Requirements
- JDK 17

### Setup
1. Launching the application to create configs;
2. Create a telegram bot;
3. Fill in information about the bot in the config;
4. Specify the id of the telegram channel where notifications will be sent;
5. If you want to use the status update feature, run the application with the --dev flag;
> java -jar uptimer-1.0.0.jar --dev
6. After that, fill in the id of status message in the config;
7. Specify after what time all servers will be pinged with pingEvery. The default is 5 minutes.

## Adding servers
1. Fill in the servers section:
```
 { 
 "ip": "your ip or ip:port or website",
 "serverName": "your server name",
 "services": "your services running on this server"
 }
```
2. If necessary, add additional servers. Note that this is a JsonArray!;
3. If you need to specify upMessage and downMessage messages for a specific server, if they are not specified, messages for all servers from the config will be used.

## Edit status messages
If you are not satisfied with the standard status message, then you can replace it.
To do this, you need to change the messages in the config:
```
allOnline - Will display a message in the status if all servers are running.
allOffline - Will display a message in the status if all servers are down.
someOffline - Will display a message in the status if some servers are down.
```
You can also change the serverPattern string that is displayed in the status.
For example:
> "serverPattern": "{status} - {services}"

You can also output the server name via {serverName}.

### Edit status lines
This configuration is responsible for configuring what will be listed in the status message line by line.
Available placeholders:

>{status} - Displays an availability message according to the current state of the servers. Lines from allOnline, allOffline, someOffline will be substituted here
><br><br>{servers} - Displays the state of the server according to the serverPattern
