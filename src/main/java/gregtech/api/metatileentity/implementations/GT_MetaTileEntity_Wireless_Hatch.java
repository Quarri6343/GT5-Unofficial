package gregtech.api.metatileentity.implementations;

import static gregtech.api.enums.GT_Values.*;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IGlobalWirelessEnergy;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.IWirelessEnergyHatchInformation;
import gregtech.api.metatileentity.MetaTileEntity;
import java.math.BigInteger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class GT_MetaTileEntity_Wireless_Hatch extends GT_MetaTileEntity_Hatch_Energy
        implements IGlobalWirelessEnergy, IWirelessEnergyHatchInformation {

    private final BigInteger eu_transferred_per_operation =
            BigInteger.valueOf(2 * V[mTier] * ticks_between_energy_addition);
    private final long eu_transferred_per_operation_long = eu_transferred_per_operation.longValue();

    private String owner_uuid;
    private String owner_name;

    public GT_MetaTileEntity_Wireless_Hatch(String aName, byte aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    public GT_MetaTileEntity_Wireless_Hatch(int aID, String aName, String aNameRegional, int aTier) {
        super(aID, aName, aNameRegional, aTier, new String[] {""});
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            EnumChatFormatting.GRAY + "Stores energy globally in a network, up to 2^(2^31) EU.",
            EnumChatFormatting.GRAY + "Does not connect to wires. This block withdraws EU from the network.",
            AuthorColen
        };
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[] {aBaseTexture, Textures.BlockIcons.OVERLAYS_ENERGY_IN_MULTI_WIRELESS_ON[mTier]};
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] {aBaseTexture, Textures.BlockIcons.OVERLAYS_ENERGY_IN_MULTI_WIRELESS_ON[mTier]};
    }

    @Override
    public boolean isSimpleMachine() {
        return true;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    @Override
    public boolean isEnetInput() {
        return false;
    }

    @Override
    public boolean isInputFacing(byte aSide) {
        return aSide == getBaseMetaTileEntity().getFrontFacing();
    }

    @Override
    public boolean isValidSlot(int aIndex) {
        return false;
    }

    @Override
    public long getMinimumStoredEU() {
        return 2 * V[mTier];
    }

    @Override
    public long maxEUInput() {
        return V[mTier];
    }

    @Override
    public long maxEUStore() {
        return totalStorage(V[mTier]);
    }

    @Override
    public long maxAmperesIn() {
        return 2;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Wireless_Hatch(mName, mTier, new String[] {""}, mTextures);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return false;
    }

    @Override
    public void onPreTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {

        super.onPreTick(aBaseMetaTileEntity, aTick);

        if (aBaseMetaTileEntity.isServerSide()) {

            // On first tick find the player name and attempt to add them to the map.
            if (aTick == 1) {

                // UUID and username of the owner.
                owner_uuid = aBaseMetaTileEntity.getOwnerUuid().toString();
                owner_name = aBaseMetaTileEntity.getOwnerName();

                // Attempt to load in map from file.
                if (GlobalEnergy.size() == 0) loadGlobalEnergyInfo(aBaseMetaTileEntity.getWorld());

                strongCheckOrAddUser(owner_uuid, owner_name);

                if (addEUToGlobalEnergyMap(owner_uuid, eu_transferred_per_operation.negate()))
                    setEUVar(eu_transferred_per_operation_long);
            }

            // This is set up in a way to be as optimised as possible. If a user has a relatively plentiful energy
            // network
            // it should make no difference to them. Minimising the number of operations on BigInteger is essential.

            // Every ticks_between_energy_addition add eu_transferred_per_operation to internal EU storage from network.
            if (aTick % ticks_between_energy_addition == 0L) {
                long total_eu = this.getBaseMetaTileEntity().getStoredEU();

                // Can the machine store the EU being added?
                long new_eu_storage = total_eu + eu_transferred_per_operation_long;
                if (new_eu_storage <= maxEUStore()) {

                    // Attempt to remove energy from the network and add it to the internal buffer of the machine.
                    if (addEUToGlobalEnergyMap(owner_uuid, eu_transferred_per_operation.negate())) {
                        setEUVar(new_eu_storage);
                    }
                }
            }
        }
    }
}
