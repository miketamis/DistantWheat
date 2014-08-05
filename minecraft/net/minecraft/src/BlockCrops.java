package net.minecraft.src;

import java.util.Random;

public class BlockCrops extends BlockFlower
{
    private static final int MAX_LEVEL = 7;
    private static final int DAY_LENGTH = 24000;
    private static final int MAX_SUNLIGHT_LEVEL = 15;
    private Icon[] iconArray;
    private int growthStage;

    protected BlockCrops(int par1)
    {
        super(par1);
        float var2 = 0.5F;
        this.setBlockBounds(0.5F - var2, 0.0F, 0.5F - var2, 0.5F + var2, 0.25F, 0.5F + var2);
        this.setCreativeTab((CreativeTabs)null);
        this.setHardness(0.0F);
        this.setStepSound(soundGrassFootstep);
        this.disableStats();
    }

    /**
     * Gets passed in the blockID of the block below and supposed to return true if its allowed to grow on the type of
     * blockID passed in. Args: blockID
     */
    protected boolean canThisPlantGrowOnThisBlockID(int par1)
    {
        return par1 == Block.tilledField.blockID;
    }

    int averageTimeTakenForRandomTick = 1365;
    public int getAverageGrowthTime(World world, int i, int j, int k) {
        return  averageTimeTakenForRandomTick * (int)(25.0F / getGrowthRate(world, i, j, k)) + 1;

    }

    private void randomBasedGrowth(World world, int i, int j, int k, Random rnd,
                                          int averageGrowthTime, int timeSinceUpdate) {
        if (rnd.nextInt(averageGrowthTime/timeSinceUpdate) == 0)
            grow(world, i,  j, k);
    }

    public void updateTimeTick(World world, int i, int j, int k, Random rnd, long timeSinceUpdate, int distanceToPlayer) {
        if(getGrowthStage(world, i, j, k) >= MAX_LEVEL) {
            return;
        }
        int averageGrowthTime = getAverageGrowthTime(world, i, j, k);

        if (canQuickUpdate(averageGrowthTime, timeSinceUpdate)) {
            if(world.getBlockLightValue(i, j + 1, k) >= getRequiredLight())
                randomBasedGrowth(world, i, j,k , rnd,averageGrowthTime, (int) timeSinceUpdate);
            return;
        }

        if(!canGrowWithorWithoutSun(world, i, j, k)) {
           return;
        }

        if(canGrowWithoutSun(world, i, j, k)) {
            timeBasedGrowth(world, i, j, k, rnd, averageGrowthTime, (int) timeSinceUpdate);
        } else {

            int startTime = getStartTime(world.getWorldTime(), timeSinceUpdate);
            int timeGrowable = getTimeGrowable(world, i, j, k);
            int timeNotGrowable = getTimeNotGrowable(world, i, j, k);
            int lengthOfUngrowablePeriod = timeGrowable - timeNotGrowable;

            timeSinceUpdate -= jumpToGrowablePeriod(world, i, j, k, rnd, startTime, averageGrowthTime, timeSinceUpdate);
            while (timeSinceUpdate > 0) {
                int growthLength = DAY_LENGTH - lengthOfUngrowablePeriod;
                if (growthLength > timeSinceUpdate) {
                    growthLength = (int) timeSinceUpdate;
                }
                timeBasedGrowth(world, i, j, k, rnd, averageGrowthTime, growthLength);
                timeSinceUpdate -= DAY_LENGTH;
            }

        }
    }

    private boolean canGrowWithorWithoutSun(World world, int i, int j, int k) {
        return getLightValueAtFullSun(world,i, j, k) >= getRequiredLight();
    }

    private boolean canGrowWithoutSun(World world, int i, int j, int k) {
        return getLightValueWithoutSun(world, i, j, k) >= getRequiredLight();
    }

    private int jumpToGrowablePeriod(World world, int i, int j, int k, Random rnd,
                                     int startTime, int averageGrowthTime, long timeSinceUpdate) {

        int timeGrowable = getTimeGrowable(world, i, j, k);
        int timeNotGrowable = getTimeNotGrowable(world, i, j, k);
        int lengthOfUngrowablePeriod = timeGrowable - timeNotGrowable; //aka night
        if(timeGrowable > startTime && startTime > timeNotGrowable) //in night period
        {
            return timeGrowable - startTime;
        }
        int timeTillNotGrowable = timeNotGrowable - startTime;
        if (timeTillNotGrowable < 0) {
            timeTillNotGrowable += DAY_LENGTH;
        }
        if (timeTillNotGrowable > timeSinceUpdate) {
            timeTillNotGrowable = (int) timeSinceUpdate;
        }
        timeBasedGrowth(world, i, j, k, rnd, averageGrowthTime, timeTillNotGrowable);
        return timeTillNotGrowable + lengthOfUngrowablePeriod;
    }

    private void timeBasedGrowth(World world, int i, int j, int k, Random rnd,
                                 int averageGrowthTime, int time) {
        int timeTillNextGrowth = getTimeTillNextGrowth(rnd, averageGrowthTime);
        if (time > timeTillNextGrowth) {
            grow(world, i, j, k);
            timeBasedGrowth(world, i, j, k, rnd, averageGrowthTime, time - timeTillNextGrowth);
        } else if(rnd.nextFloat() < time/timeTillNextGrowth) {
            grow(world, i, j, k);
        }
    }

    private int getTimeTillNextGrowth(Random rnd, int averageGrowthTime) {
        int timeTillNextGrowth = (int) (Math.log(1-rnd.nextFloat())* -averageGrowthTime);
        return timeTillNextGrowth > 0 ? timeTillNextGrowth : 1;
    }

    private int getStartTime(long worldTime, long timeSinceUpdate) {

        int startTime = (int) (worldTime - timeSinceUpdate % DAY_LENGTH);
        if (startTime < 0) {
            startTime += DAY_LENGTH;
        }
        return startTime % DAY_LENGTH;
    }

    private boolean canQuickUpdate(int averageGrowthTime, long timeSinceUpdate) {
        return timeSinceUpdate < averageGrowthTime && timeSinceUpdate < 400;
    }

    private void grow(World world, int i, int j, int k) {
        int growthStage = getGrowthStage(world, i, j, k);
        if (growthStage < MAX_LEVEL) {
            ++growthStage;
            SetGrowthStage(world, i, j, k, growthStage);
        }
    }

    private void SetGrowthStage(World world, int i, int j, int k, int growthStage) {
        world.setBlockMetadataWithNotify(i, j, k, growthStage, 2);
    }

    /**
     * Apply bonemeal to the crops.
     */
    public void fertilize(World par1World, int par2, int par3, int par4)
    {
        int var5 = par1World.getBlockMetadata(par2, par3, par4) + MathHelper.getRandomIntegerInRange(par1World.rand, 2, 5);

        if (var5 > 7)
        {
            var5 = 7;
        }

        par1World.setBlockMetadataWithNotify(par2, par3, par4, var5, 2);
    }

    /**
     * Gets the growth rate for the crop. Setup to encourage rows by halving growth rate if there is diagonals, crops on
     * different sides that aren't opposing, and by adding growth for every crop next to this one (and for crop below
     * this one). Args: x, y, z
     */
    private float getGrowthRate(World par1World, int par2, int par3, int par4)
    {
        float var5 = 1.0F;
        int var6 = par1World.getBlockId(par2, par3, par4 - 1);
        int var7 = par1World.getBlockId(par2, par3, par4 + 1);
        int var8 = par1World.getBlockId(par2 - 1, par3, par4);
        int var9 = par1World.getBlockId(par2 + 1, par3, par4);
        int var10 = par1World.getBlockId(par2 - 1, par3, par4 - 1);
        int var11 = par1World.getBlockId(par2 + 1, par3, par4 - 1);
        int var12 = par1World.getBlockId(par2 + 1, par3, par4 + 1);
        int var13 = par1World.getBlockId(par2 - 1, par3, par4 + 1);
        boolean var14 = var8 == this.blockID || var9 == this.blockID;
        boolean var15 = var6 == this.blockID || var7 == this.blockID;
        boolean var16 = var10 == this.blockID || var11 == this.blockID || var12 == this.blockID || var13 == this.blockID;

        for (int var17 = par2 - 1; var17 <= par2 + 1; ++var17)
        {
            for (int var18 = par4 - 1; var18 <= par4 + 1; ++var18)
            {
                int var19 = par1World.getBlockId(var17, par3 - 1, var18);
                float var20 = 0.0F;

                if (var19 == Block.tilledField.blockID)
                {
                    var20 = 1.0F;

                    if (par1World.getBlockMetadata(var17, par3 - 1, var18) > 0)
                    {
                        var20 = 3.0F;
                    }
                }

                if (var17 != par2 || var18 != par4)
                {
                    var20 /= 4.0F;
                }

                var5 += var20;
            }
        }

        if (var16 || var14 && var15)
        {
            var5 /= 2.0F;
        }

        return var5;
    }

    /**
     * From the specified side and block metadata retrieves the blocks texture. Args: side, metadata
     */
    public Icon getIcon(int par1, int par2)
    {
        if (par2 < 0 || par2 > 7)
        {
            par2 = 7;
        }

        return this.iconArray[par2];
    }

    /**
     * The type of render function that is called for this block
     */
    public int getRenderType()
    {
        return 6;
    }

    /**
     * Generate a seed ItemStack for this crop.
     */
    protected int getSeedItem()
    {
        return Item.seeds.itemID;
    }

    /**
     * Generate a crop produce ItemStack for this crop.
     */
    protected int getCropItem()
    {
        return Item.wheat.itemID;
    }

    /**
     * Drops the block items with a specified chance of dropping the specified items
     */
    public void dropBlockAsItemWithChance(World par1World, int par2, int par3, int par4, int par5, float par6, int par7)
    {
        super.dropBlockAsItemWithChance(par1World, par2, par3, par4, par5, par6, 0);

        if (!par1World.isRemote)
        {
            if (par5 >= 7)
            {
                int var8 = 3 + par7;

                for (int var9 = 0; var9 < var8; ++var9)
                {
                    if (par1World.rand.nextInt(15) <= par5)
                    {
                        this.dropBlockAsItem_do(par1World, par2, par3, par4, new ItemStack(this.getSeedItem(), 1, 0));
                    }
                }
            }
        }
    }

    /**
     * Returns the ID of the items to drop on destruction.
     */
    public int idDropped(int par1, Random par2Random, int par3)
    {
        return par1 == 7 ? this.getCropItem() : this.getSeedItem();
    }

    /**
     * Returns the quantity of items to drop on block destruction.
     */
    public int quantityDropped(Random par1Random)
    {
        return 1;
    }

    /**
     * only called by clickMiddleMouseButton , and passed to inventory.setCurrentItem (along with isCreative)
     */
    public int idPicked(World par1World, int par2, int par3, int par4)
    {
        return this.getSeedItem();
    }

    /**
     * When this method is called, your block should register all the icons it needs with the given IconRegister. This
     * is the only chance you get to register icons.
     */
    public void registerIcons(IconRegister par1IconRegister)
    {
        this.iconArray = new Icon[8];

        for (int var2 = 0; var2 < this.iconArray.length; ++var2)
        {
            this.iconArray[var2] = par1IconRegister.registerIcon(this.getTextureName() + "_stage_" + var2);
        }
    }

    public int getGrowthStage(World world, int i,int j,int k) {
        return world.getBlockMetadata(i, j, k);
    }

    public int getTimeGrowable(World world, int i, int j, int k) {
        return DAY_LENGTH - (MAX_SUNLIGHT_LEVEL - getRequiredLight() - getLightSubtraction(world, i, j, k)) * 164;
    }

    public int getTimeNotGrowable(World world, int i, int j, int k) {
        return 12000 + (MAX_SUNLIGHT_LEVEL - getRequiredLight() - getLightSubtraction(world, i, j, k)) * 164;
    }

    public int getLightValueWithoutSun(World world, int i, int j, int k) {
        Chunk thisChunk = world.getChunkFromChunkCoords(i >> 4, k >> 4);
        return thisChunk.getBlockLightValue(i & 15, j + 1, k & 15, MAX_SUNLIGHT_LEVEL);
    }

    public int getLightValueAtFullSun(World world, int i, int j, int k) {
        Chunk thisChunk = world.getChunkFromChunkCoords(i >> 4, k >> 4);
        return thisChunk.getBlockLightValue(i & 15, j + 1, k & 15, 0);
    }

    public int getRequiredLight() {
        return 9;
    }

    public int getLightSubtraction(World world, int i, int j, int k) {
        return MAX_SUNLIGHT_LEVEL - getLightValueAtFullSun(world, i, j, k);
    }
}
