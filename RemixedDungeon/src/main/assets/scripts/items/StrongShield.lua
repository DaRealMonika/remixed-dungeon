--
-- User: mike
-- Date: 29.01.2019
-- Time: 20:33
-- This file is part of Remixed Pixel Dungeon.
--

local RPD = require "scripts/lib/commonClasses"

local item = require "scripts/lib/item"

return item.init{
    desc  = function (self, item)

        RPD.glog("Created item with id:"..tostring(item:getId()))

        return {
            image         = 2,
            imageFile     = "items/shields.png",
            name          = "StrongShield_name",
            info          = "StrongShield_desc",
            price         = 20,
            equipable     = "left_hand"
        }
    end,

    activate = function(self, item, hero)
        RPD.permanentBuff(hero,"Shield"):level(3)
    end,

    deactivate = function(self, item, hero)
        RPD.removeBuff(hero,"Shield")
    end
}