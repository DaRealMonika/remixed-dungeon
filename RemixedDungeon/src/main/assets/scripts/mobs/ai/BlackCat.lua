---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by mike.
--- DateTime: 23.08.18 22:51
---

local RPD = require "scripts/lib/commonClasses"

local ai = require "scripts/lib/ai"

local edible = {
    FriedFish = true,
    ChargrilledMeat = true
}

local raw = {
    MysteryMeat = true,
    RawFish = true,
}

return ai.init{

    act       = function(self, ai, me)
        local level = RPD.Dungeon.level

        -- already have something tasty?
        local heap = level:getHeap(me:getPos())
        if heap then
            local item = heap:peek()

            if edible[item:getClassName()] then
                heap:pickUp()
                RPD.Sfx.SpellSprite:show(me, RPD.Sfx.SpellSprite.FOOD)
                RPD.playSound("snd_eat.mp3")
                me:spend(1)
                return
            end
        end

        -- look for something tasty
        local heaps = level:allHeaps()

        local iterator = heaps:iterator()

        while iterator:hasNext() do
            local heap = iterator:next()
            local itemPos = heap.pos

            if level.fieldOfView[itemPos] then --visible heap
                local item = heap:peek()
                if edible[item:getClassName()] then

                    if RPD.Actor:findChar(itemPos) then
                        RPD.Wands.wandOfTelekinesis:mobWandUse(me, itemPos)
                    else
                        RPD.blinkTo(me, itemPos)
                    end
                    break
                end

                if raw[item:getClassName()] then

                    if level:adjacent(itemPos, me:getPos()) then
                        RPD.Wands.wandOfFirebolt:mobWandUse(me, itemPos)
                        break
                    else
                        local tgt = level:getEmptyCellNextTo(itemPos)
                        if level:cellValid(tgt) then
                            RPD.blinkTo(me, tgt)
                            break
                        end
                    end
                end

            end
        end

        me:spend(1)
    end,

    gotDamage = function(self, ai, me, src, dmg)

    end
}