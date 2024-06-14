# Discordian

Yet another easy to set up Fabric mod that connects Minecraft and Discord chats and provides you with most stuff you
need when chatting, as well as a more advanced feature for linking Minecraft and Discord accounts.

# Setting up

1. Make a bot at https://discord.com/developers/applications. Create an Application, then a bot for it, and find a Bot
   Token. In the Bot section, scroll down and enable Presence Intent and Server Members Intent. There are a lot of
   tutorials on doing this on YouTube.
2. Invite your bot to your server using the link: https://discord.com/oauth2/authorize?scope=bot&client_id=12345 where
   you put your bot Application ID instead of 12345.
3. Make sure you have Fabric installed and put the mod .jar into your mods folder, then run the server.
4. Upon running first time a config file will be created under `config/mantevian/discordian.json5`. Open it and put in
   your bot token and the ID of the channel in your Discord server that should be linked to Minecraft.
5. Restart the server, if you see the bot go online that means you've done everything right! You can edit the config
   file and run `/discord reload` to apply the changes.

# What it does

When settings are left by default, the Discord bot will send messages in your Discord channel about various events, such
as players joining, leaving, chatting, getting advancements and dying.

Players can use the `/discord` command in-game to link their Discord account to their Minecraft player. Account linking
is used for several things:

* if the mod is configured to use a webhook, Minecraft messages of linked players will be displayed using a webhook (
  with that player's Discord name and avatar);
* if the mod is configured to restrict access for unlinked players, players will not be able to interact with the world
  until they link their account (this puts them below the world, sets their gamemode to spectator and doesn't allow them
  to leave).

Additionally, in-game commands can be run using the Discord chat, including commands added by other mods. In order to
use operator commands from Discord, assign your Discord user ID to
an [OP level](https://minecraft.wiki/w/Permission_level) in the configuration file.

Other than `/discord` used for linking accounts, Discordian also adds 2 utility commands which are intended to use in
Discord, but can be used in-game as well:

* `/tps` to see the server's ticks per second;
* `/players` to see who is online at the moment.

# Download

Please see the [Releases](https://github.com/mantevian/discordian/releases) page to see available downloads.

**IMPORTANT:** Since Discordian 2.0, you need to
have [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) installed on your server to run
Discordian.