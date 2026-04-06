package dfdyz.ef_anim_phy;

import dfdyz.ef_anim_phy.physics.NativeLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;


@Mod(value = EFAnimPhy.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = EFAnimPhy.MODID, value = Dist.CLIENT)
public class EFAnimPhyClient {
    public EFAnimPhyClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        NativeLoader.init();
    }
}
