package emobot;

import battlecode.common.GameActionException;

public class TypeLaboratory extends Globals {
    public static void step() throws GameActionException {
        if(self.canTransmute())
        {
            self.transmute();
            self.disintegrate();
        }
    }
}
