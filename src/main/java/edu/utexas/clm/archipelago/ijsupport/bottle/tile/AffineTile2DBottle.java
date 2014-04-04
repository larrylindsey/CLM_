package edu.utexas.clm.archipelago.ijsupport.bottle.tile;

import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.network.translation.Bottle;
import ini.trakem2.display.Patch;
import mpicbg.models.AffineModel2D;
import mpicbg.trakem2.align.AbstractAffineTile2D;
import mpicbg.trakem2.align.AffineTile2D;

import java.io.IOException;

/**
 *
 */
public class AffineTile2DBottle implements Bottle<AbstractAffineTile2D<?>>
{
    private final AffineModel2D model;
    private final Patch patch;

    public AffineTile2DBottle(final AffineTile2D tile)
    {
        model = tile.getModel();
        patch = tile.getPatch();
    }

    public AbstractAffineTile2D<?> unBottle(final MessageXC xc) throws IOException
    {
        return new AffineTile2D(model, patch);
    }
}