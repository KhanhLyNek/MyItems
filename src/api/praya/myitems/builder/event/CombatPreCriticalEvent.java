// 
// Decompiled by Procyon v0.5.36
// 

package api.praya.myitems.builder.event;


import com.praya.myitems.modern.Paper26StatsMath;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class CombatPreCriticalEvent extends Event implements Cancellable
{
    private static final HandlerList handlers;
    private final LivingEntity attacker;
    private final LivingEntity victims;
    private boolean cancel;
    private double chance;
    
    static {
        handlers = new HandlerList();
    }
    
    public CombatPreCriticalEvent(final LivingEntity attacker, final LivingEntity victims, final double chance) {
        this.cancel = false;
        this.attacker = attacker;
        this.victims = victims;
        this.chance = Paper26StatsMath.chance(chance);
    }
    
    public final LivingEntity getAttacker() {
        return this.attacker;
    }
    
    public final LivingEntity getVictims() {
        return this.victims;
    }
    
    public final double getChance() {
        return this.chance;
    }
    
    public final boolean isCritical() {
        return Paper26StatsMath.rollChance(this.chance, Math.random());
    }
    
    public final void setChance(final double chance) {
        this.chance = Paper26StatsMath.chance(chance);
    }
    
    public HandlerList getHandlers() {
        return CombatPreCriticalEvent.handlers;
    }
    
    public static final HandlerList getHandlerList() {
        return CombatPreCriticalEvent.handlers;
    }
    
    public boolean isCancelled() {
        return this.cancel;
    }
    
    public void setCancelled(final boolean cancel) {
        this.cancel = cancel;
    }
}
