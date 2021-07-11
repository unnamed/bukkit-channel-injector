# BukkitChannelInjector
A simple bukkit netty channel injector, allowing to inject new handlers to every netty channel of a bukkit server.

## Usage
You have 2 ways of using it, as a plugin which you depend of, or as a library.
### As a plugin
Just add a depends in your `plugin.yml` and use `ServerInjector#getOrCreate(Plugin)` to get an instance of ServerInjector on your onLoad.
With that instance you can add injectors for every channel.
### As a library
First of all things, relocate the library into another package to avoid incompatibilites.
Then, at your onEnable you do exactly the same as when using this api as a plugin, and after that you call `ServerInjector#injectServer()`.
Also at your onDisable you should call `ServerInjector#removeServerInjection()`
