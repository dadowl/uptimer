# Uptimer

An application that monitors the status of your servers / sites and, if necessary, will write to you in the telegram channel about the fall.

### Features
- Telegram channel notifications;
- Ping servers by ip, ip:port, sites;
- Setting up messages in the config and full customization.

### Requirements
- JDK 17

### Setup

1. Run the application so that it creates a config;
> java -jar uptimer.jar
2. Create a telegram bot via [@BotFather](https://t.me/BotFather);
3. Specify the username and token from the bot in the config;
4. Specify the id of the channel where all notifications about pigs will be sent;
5. [Add servers](https://github.com/dadowl/uptimer#adding-servers)
6. If you want to use the message-status function, then start the application with the dev flag, then specify the message-status id in the config;
> java -jar uptimer.jar --dev
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

## Example config

```
{
  "other": {
    // How often to ping the server?
    "pingEvery": 1,
    // Standard message if the server is online after a crash. One for all servers.
    "upMessage": "Server {serverName}({ip}) is UP!", 
    // Standard message if the server is down. One for all servers.
    "downMessage": "Server {serverName}({ip}) is DOWN!", 
    // After how many failed pings will the failure notification be displayed?
    "downTryes": 1 
  },
  "Telegram": {
    // Your Telegram bot token
    "token": "???",
    // Your Telegram bot username
    "username": "???",
    // Your Telegram channel for notifications 
    "channel": -1, 
    "status": {
      // Status message ID. Can be obtained if run with the --dev flag. If -1, then disabled.
      "msgId": 51,
      // Line-by-line format of the status message
      "lines": [
        // The current status of all servers is substituted from the config from the "status" section. 
        "{status}", 
        "",
        "Servers:",
        // List of servers added to the project. Filled in according to the serverPattern below. 
        "{servers}" 
      ],
       // Server template for a list of servers in state.
       // {status} - if it works, then 🟢, if not, then - 🔴, if it is pending, that is, downTryes != downTryes in the config, then - 🟡
       // {serverName} - server name from config
       // {services} - services that run on this server, specified in the config
      "serverPattern": "{status} - {serverName} - {services}", 
      "status": {
        // Will display a message in the status if all servers are running.
        "allOnline": "🟢 All servers are online!",
        // Will display a message in the status if all servers are down.
        "allOffline": "🔴 All servers are offline!", 
        // Will display a message in the status if some servers are down.
        "someOffline": "🟡 Some servers are offline!" 
      }
    }
  },
  "servers": [
    {
      // Server ip or ip:port or domain name
      "ip": "8.8.8.8",
      // Server name  
      "serverName": "Example server",
      // Server services 
      "services": "Example server", 
      // This server-only message about being online after the failed pings specified in downTryes and after the server is considered offline.
      // You can use {downTime} placeholder to show downtime seconds
      // If this message is not specified, as in server 3, then the message from the configuration file will be used.
      "upMessage": "Server {serverName}({ip}) is UP!  It was offline {downTime} seconds!" 
    },
    {
      // Server ip or ip:port or domain name
      "ip": "8.8.4.4",
      // Server name 
      "serverName": "Example server 2", 
      // Server services
      "services": "Example server 2", 
      // If the server crashes after the number of failed pings is set to downTryes in the config, then this message will be displayed.
      // If this message is not specified, as in "Example server 3", then the message from the configuration file will be used.
      "downMessage": "Server {serverName}({ip}) is DOWN!" 
    },
    {
      // Server ip or ip:port or domain name
      "ip": "8.8.8.8",
      // Server name 
      "serverName": "Example server 3",
      // Server services 
      "services": "Example server 3" 
    }
  ]
}
```