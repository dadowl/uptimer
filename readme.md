# Uptimer

Application for tracking the status of servers

### Features
- Ping servers by ip, ip:port, websites
- Notifications in the telegram channel and to the mail
- Web server and API
- Server groups
- Configuring messages in the config
- Placeholders for messages
- Full customization

### Requirements
- JDK 17

## Launch

When you turn on the program for the first time, it will generate the necessary configs to work, launch the web server on port 9000 and immediately start working.

Configuration help:
1. [General config](https://github.com/dadowl/uptimer#general-config)
2. [Web server](https://github.com/dadowl/uptimer#web-server)
3. [Adding Servers](https://github.com/dadowl/uptimer#adding-servers)
4. [Noticers setup](https://github.com/dadowl/uptimer#noticers-setup)
5. [Placeholders for messages](https://github.com/dadowl/uptimer#placeholders-for-messages)

## General config

The general parameters for the operation of the application are specified in the config.json file.

Let's go through the list:
1. pingEvery - indicates how often to ping the server. The value is specified in the format "5m", where 5 is the number, and m is the minutes, that is, the servers will ping every 5 minutes. You can also specify s, h, for seconds and hours respectively
2. downTryes - indicates after how many unsuccessful pings the server will be considered offline
3. upMessage and downMessage - messages that will be sent to the mail or telegram when the server appears online or goes offline. You can also use [placeholders](https://github.com/dadowl/uptimer#placeholders-for-messages)

## Web server

The web server is configured in the config file.json in the Web Server section.

Here:
1. enable - determines whether the web server is turned on or off
2. port - the port of the web server
3. hide Ip - is it necessary to hide the ip address of the server? It will be useful if you bring monitoring to the site and you do not need to show the ip addresses of your servers.

When accessing the server, a response in json format will be output in the response object:
The status item will indicate the current servers level
In the items array, servers grouped by server group.

Example of a response from the server:

![](https://dadowl.dev/files/uptimer/request_example.jpg)

## Adding Servers

The servers are configured in the servers.json file.

Each server has required parameters:
1. ip - the ip address of the server that will be pinged. You can only specify an ip address or ip:port or domain.
2. serverName - the name of the server. However, you can specify anything here, this parameter is just used for the convenience of messages.
3. services - services that are running on this server. However, you can specify anything here, this parameter is just used for the convenience of messages.

Also, you can register custom upMessage and downMessage messages on each server. You can also use [placeholders]((https://github.com/dadowl/uptimer#placeholders-for-messages)).

Also, the server can be added to the group. This is done through the group parameter. For example, "group": "minecraft".

## Noticers setup

All noticers are configured in the noticers.json file.

### Telegram setup

In the file above, there is a block for configuring Telegram.

Let's go through the list of its parameters:
1. enabled - determines whether the Telegram noticer is enabled or not
2. token - the telegram bot token that is used for notifications. You can get it when creating a bot via [@BotFather](https://t.me/BotFather )
3. username - the username of the bot, it is also obtained through [@BotFather](https://t.me/BotFather )
4. channel - the channel where ping status messages will be sent. For example, the server is offline or has become online again.
5. deleteAfter - determines after how long to delete messages. Accepts in the same form as [pingEvery](https://github.com/dadowl/uptimer#general-config)
6. status - status message settings

Status message is a unique feature of this application.
It will allow you to create a message in your telegram channel and pin it. In this case, after each ping, information about the servers will be updated in the status message, if the status has not changed after the previous ping, then nothing will happen.

Parameters:
1. msgId - the id of the status message, it is this message that will be updated. You can either specify it manually, or by running the application with the --dev flag
2. lines - status message lines, placeholders here:
   {status} - the general status of the servers, depending on the current state, is set in the statuses section below
   {group:group_name} - server group. Instead of group_name, the name of the server group is specified. When generating a status message, all servers with the specified group will be displayed here. If there are no servers with such a group, then this line will not change.
3. serverPattern - server string that will be output when calling servers in {group:group_name}. You can also specify any message using placeholders here
4. statuses - here you specify what will be displayed in the status message lines in the placeholder {status}
   
There are three parameters here:
- allOnline - if all servers are online
- allOffline - if all servers are offline
- someOffline - if some servers are turned off

Example of a status message:

![](https://dadowl.dev/files/uptimer/status_example.jpg)

### Email setup

In the file listed above, there is a block for configuring Mail.

Let's go through the list of its parameters:
1. enabled - determines whether mail noticer is enabled or not
2. smtp - smtp mail server
3. port - smtp server port
4. username - the name of the user from whom the email will be sent
5. password - the password of the user from whom the email will be sent
6. address - the email from which the email will be sent
7. senderName - the sender's name
8. sendTo - indicates which mail the status messages will be sent to

### Placeholders for messages

Placeholders for messages:
1. ip - ip address of the server;
2. serverName - the name of the server;
3. services - services running on this host;
4. downTime - offline time;
5. errorCode - error code if the website is pinged, otherwise 0;
6. status - status icon according to the current status:
 - ONLINE(🟢)
 - OFFLINE(🔴)
 - PENDING(🟡)
