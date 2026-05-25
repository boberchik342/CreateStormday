package org.boberchik342.CreateStormday.wind;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface AirflowProvider {
    Vec3 getWind(Level level, Vec3 pos);
    double getMaxWindSpeed();
}
