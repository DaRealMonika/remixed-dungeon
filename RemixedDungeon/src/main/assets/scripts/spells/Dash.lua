---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by mike.
--- DateTime: 21.03.19 22:57
---

local RPD = require "scripts/lib/commonClasses"

local spell = require "scripts/lib/spell"

return spell.init{
    desc  = function ()
        return {
            image         = 1,
            imageFile     = "spellsIcons/warrior.png",
            name          = "DashSpell_Name",
            info          = "DashSpell_Info",
            magicAffinity = "Combat",
            targetingType = "cell",
            level         = 3,
            spellCost     = 10,
            cooldown      = 30,
            castTime      = 0.5
        }
    end,
    castOnCell = function(self, spell, caster, cell)

        local level = caster:level()

        local ownPos = caster:getPos()

        local dist = level:distance(ownPos, cell)

        if ownPos == cell then
            RPD.glogn("DashSpell_OnSelf")
            return false
        end

        if dist  > 2 then
            RPD.glogn("DashSpell_TooFar")
            return false
        end

        local dst = RPD.Ballistica:cast(ownPos, cell, false, true, true)

        local char = RPD.Actor:findChar(dst)

        if char then
            RPD.affectBuff(char, RPD.Buffs.Vertigo, caster:skillLevel())
            local newPos = char:getPos()
            if char:push(caster) then
                dst = newPos
            end
        end

        local object = level:getLevelObject(dst)

        if object then
            local newPos = object:getPos()
            if object:push(caster) then
                dst = newPos
            end
        end

        local items = caster:getBelongings()

        local function hitCell(cell)
            local victim = RPD.Actor:findChar(cell)
            if victim ~= nil then
                local dmg = items.weapon:damageRoll(caster)
                dmg = victim:defenseProc(caster, dmg)
                victim:damage(dmg, caster)
                RPD.Sfx.Wound:hit(victim)
            end
        end

        RPD.forCellsAround(dst, hitCell)

        RPD.playSound("dash.mp3")
        RPD.zapEffect(ownPos,dst,"dash")
        caster:getSprite():dash(ownPos,dst)
        caster:move(dst)

        return true
    end
}
