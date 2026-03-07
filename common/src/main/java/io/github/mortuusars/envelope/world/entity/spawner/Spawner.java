package io.github.mortuusars.envelope.world.entity.spawner;

import net.minecraft.server.level.ServerLevel;

public abstract class Spawner {
    protected final int interval;
    protected final int id;

    static int counter;

    public Spawner(int interval) {
        this.interval = interval;
        this.id = counter++;
    }

    public final void tick(ServerLevel level) {
        if (!worksIn(level)) return;
        if ((level.getGameTime() + id) % interval != 0) return;
        spawn(level);
    }

    public abstract boolean worksIn(ServerLevel level);
    public abstract void spawn(ServerLevel level);
}
