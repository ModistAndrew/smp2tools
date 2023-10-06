Advancement

 - Use %parent% as a criteria with impossible trigger to make parent advancement as required.
 - Use %execute%:[your command] as a criteria with tick trigger to execute the command when the advancement is made. You may use %player% to replace the player's name. Notice that you should never use @s as the command is executed directly on the server.

Loot Penalty

 - specify the name of the loot table to make loot penalty take effect, e.g. minecraft:chests/buried_treasure. use global to modify global settings. use namespace to modify all loot tables under it
 - rangeMin/rangeMax: the min/max range of teleport. rangeMax must be larger than rangeMin
 - function: a math expression to get the penalty count. use %level% to represent how many times the player has already opened a chest with the same loot
 - fail_info/success_info(optional): message sent to the player when they can/cannot open the chest. you can use %level%, %count% and %loot_table%
 - protection: whether to prevent the container from being broken, otherwise the container will be destroyed with nothing left!
