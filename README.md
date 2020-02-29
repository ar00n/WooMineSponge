# WooMineSponge - Self-hosted Minecraft Donations

WooMineSponge is a port of free Minecraft Donation plugin from Bukkit to Sponge's API.  By leveraging a well known eCommerce plugin on the
WordPress side of things, we allow you to specify commands for product variations, general product commands, and resending of donations at any time.   
![WooMinecraft Logo](https://raw.githubusercontent.com/WooMinecraft/WooMinecraft/master/src/main/resources/wmc-logo.jpg)

## IMPORTANT
This build supports only **Minecraft Sponge**

If in your config you are using the **/shop** path as your URL, you must NOT use that going forward. Your host MUST support
access to the WordPress Rest API. If they do not, you should consider changing hosts.

## Config
Your config should look like the below section.
```
# You must set this to your WordPress site URL.  If you installed WordPress in a
# subdirectory, it should point there.
url = "https://ar0n.com"

# This must match the WordPress key in your admin panel for WooMinecraft
# This is a key that YOU set, both needing to be identical in the admin panel
# and in this config file
# For security purposes, you MUST NOT leave this empty.
key = ""

# This is how often, in seconds, the server will contact your WordPress installation
# to see if there are donations that need made.
interval = 300

# Set to true in order to toggle debug information
debug = false
```

## How does it work?
This bridges the gap between PHP, and Java by leveraging both the bukkit/spigot API ( java ) and the WordPress API with WooCommerce support ( php ). It stores commands
per order, per player, per command ( yes you read that right ) in the WordPress database.  This plugin, either when an op requests it, or on a timer, sends a request to
the WordPress server to check for donations for all online players.

If online players have commands waiting to be processed, then all necessary commands are ran.  There is NO LIMIT to the type of commands you can set, `give`, `i`, `tp`, etc... all commands are ran
by the console sender, and not a player.

## Mojang Guidelines
Since this plugin is GPL and entirely opensource, we cannot be sure how you will use this. However, when providing 'donation' options, you are still considered a 
`commercial` entity and therefore are bound to the [Mojang Commercial Usage Guidelines](https://account.mojang.com/terms#commercial)

### WordPress Plugin
You'll need the WordPress plugin for this MC Plugin to work - you can [get it here](https://github.com/WooMinecraft/woominecraft-wp).

## Changelog

## 1.2.0-SPONGE
* Ported over to Sponge API. World whitelisting + language change is disabled.

## 1.2.0
* Tested on Spigot 1.12.2
* Cleaned up a lot of internals by simplifying a ton of requests.
* Now building with okHTTP3 library for ease of use.
* Move to REST API.

## 1.1.2
* Tested on Spigot 1.12.1
* Cleaned up a lot of pointless code.
* Privatized a lot of methods.
* Make OrderID more visible in the sea of text.

### 1.1.1
* Fixed - [#137](https://github.com/WooMinecraft/WooMinecraft/issues/137) - Removed missing commands from help message.
* Fixed - [#130](https://github.com/WooMinecraft/WooMinecraft/issues/130) - Commands not processing due to new white-list worlds config being commented out.

### 1.1.0
* Added - Redirect Exceptions for sending/receiving data from the server. You will now get an exception if your host is redirecting the requests in most cases.
* Added - Debug logging specifically for HTTP requests. Just set `debug: true` in your config.
* Added - Exception handling for sending order updates to server. Will now throw exceptions if plugin receives invalid data.
* Added - World white-listing, props [FabioZumbi12](https://github.com/WooMinecraft/WooMinecraft/pull/117) - disabled by default
* Added - Clarification around server key, props [spannerman79](https://github.com/WooMinecraft/WooMinecraft/pull/119)
* Updated - HTTP Requests now use `CloseableHttpClient` and `CloseableHttpResponse` so connections will now close, not sure if they weren't before.
* Removed - Support for MC 1.7.10

### 1.0.10
* Updated public suffix list, required by HTTP client

### 1.0.9
* Change key sent to server, fixes WooCommerce compatibility.

### 1.0.8
* Better error handling from WordPress
* Make use of the `debug: true` flag in the config.
* Code cleanup, removed unused libs, removed commented code.
* Added config validation for users coming from older versions - will now throw exceptions if your config is not setup properly and will stop the check.
* Fixed player online check, props [@FailPlayDE](https://github.com/FailPlayDE) - [#108](https://github.com/WooMinecraft/WooMinecraft/pull/108)
* **REMOVED** Reload & Register commands - more problems than their worth.

### 1.0.7
* Prints stacktraces on JSON error to log.
* Updated Readme.md file to reflect supported bukkit versions.

### 1.0.6
* Refactored all HTTP connections to work on a single thread
* Cleaned up a TON of code
* Removed internal JSON library, used maven deps instead
* Fixed [#88](https://github.com/WooMinecraft/WooMinecraft/issues/88), [#85](https://github.com/WooMinecraft/WooMinecraft/issues/85), [#48](https://github.com/WooMinecraft/WooMinecraft/issues/48), [#60](https://github.com/WooMinecraft/WooMinecraft/issues/60)

### 1.0.5
* Added debug option for more straight forward debug options.

### 1.0.4
* Too much to detail

### 1.0.0
* First official release
