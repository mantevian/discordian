# Discordian
Yet another easy to setup Fabric mod that connects Minecraft and Discord chats and provides you with most stuff you need when chatting.

# Setting up
1. Make a bot at https://discord.com/developers/applications. Create an Application, then a bot for it, and find a Bot Token. In the Bot section, scroll down and enable Presence Intent and Server Members Intent. There are a lot of tutorials on doing this on YouTube.
2. Invite your bot to your server using the link: https://discord.com/oauth2/authorize?scope=bot&client_id=12345 where you put your bot's Application ID instead of 12345.
3. Make sure you have Fabric installed and put the mod .jar into your mods folder, then run the server.
4. Upon running first time a config file will be created under `config/mante/discordian.json`. Open it and put in your bot's token and the ID of the channel in your Discord server that should be linked to Minecraft.
5. Restart the server, if you see the bot go online that means you've done everything right! You can edit the config file and run `/reload` to apply the changes.

# What can the mod do?
This mod, obviously, provides connection between your Minecraft and your Discord chats. And it does so in a fancy clean way.
## Minecraft -> Discord
* Unlike other mods, Discordian doesn't just listen to in-game events like a player dying or getting an advancement, it sends everything from the global chat. Any other mod that broadcasts stuff will have its messages sent to your Discord chat!
* Another feature that's useful for folks who use datapacks or command blocks: `/tellraw` messages also get transferred to Discord, as long as a message targets every player in the server (e.g. `@a`).
* Finally, you can click on someone's Discord name to mention them.

![alt text](https://cdn.discordapp.com/attachments/500368559084666890/853315065536184340/DiscordCanary_W2GYPmj6rW.png "What it looks like in Discord")
![alt text](https://cdn.discordapp.com/attachments/500368559084666890/853315070061576222/javaw_AvLR4ocMgJ.png "Hover over someone's name")
![alt text](https://cdn.discordapp.com/attachments/500368559084666890/853315080631353415/javaw_YWLSm3BkKG.png "Click on someone's name")

## Discord -> Minecraft
* When you type a message in Discord it gets sent to the Minecraft chat. Discordian respects a user's nickname color and will always show it in Minecraft!
* To avoid any confusion, the Minecraft players will also know when someone edits or replies to another message in Discord.

![alt text](https://cdn.discordapp.com/attachments/500368559084666890/853315101800005632/javaw_IGC3qm0r9B.png "What it looks like in Minecraft")

## Commands
Currently Discordian has 3 commands that are used in the Discord chat, and you can add a Discord role that will work as Operator access in-game. Users with this role can use any Minecraft command in the Discord chat.

Commands available for everyone:
> /players

Shows the player list.

> /tps

Shows server's TPS (ticks per second).

> /scores

Shows server's currently displayed scoreboard in the sidebar slot.

## Blacklist
In the config file, you can add words to be blacklisted, so that messages containing them will be blocked from being sent Minecraft -> Discord or vice-versa.
