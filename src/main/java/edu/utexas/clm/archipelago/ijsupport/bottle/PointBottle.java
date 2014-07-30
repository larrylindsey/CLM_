package edu.utexas.clm.archipelago.ijsupport.bottle;

import edu.utexas.clm.archipelago.network.MessageXC;
import edu.utexas.clm.archipelago.network.translation.Bottle;
import mpicbg.models.Point;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A PointBottle to support synchronized point objects across cluster nodes
 */
public class PointBottle implements Bottle<Point>
{
    public class IdPoint extends Point
    {
        private final long id;

        public IdPoint(final long id, final Point pt)
        {
            super(pt.getL(), pt.getW());
            this.id = id;
        }

        public long getID()
        {
            return id;
        }
    }

    private static final Map<Long, Point> idPointMap =
            Collections.synchronizedMap(new HashMap<Long, Point>());

    private static final Map<Point, Long> pointIdMap =
            Collections.synchronizedMap(new IdentityHashMap<Point, Long>());
    private static final AtomicLong idGenerator = new AtomicLong(1);
    private static final Map<Point, Integer> pointCountMap =
            Collections.synchronizedMap(new IdentityHashMap<Point, Integer>());

    private static boolean putPoint(final long id, final Point point)
    {
        System.out.println("putPoint");
        synchronized(pointIdMap)
        {
            System.out.println("putPoint: Inside synch 1");
            synchronized(idPointMap)
            {
                System.out.println("putPoint: Inside synch 2");
                if (increment(point))
                {
                    pointIdMap.put(point, id);
                    idPointMap.put(id, point);
                    System.out.println("putPoint done");
                    return true;
                }
                else
                {
                    System.out.println("putPoint done");
                    return false;
                }
            }
        }
    }

    private static long getId(final Point point, final long idDefault)
    {
        System.out.println("getId");
        synchronized(pointIdMap)
        {
            System.out.println("getId: inside synch");
            final Long id = pointIdMap.get(point);
            System.out.println("getId done");
            return id == null ? idDefault : id;
        }
    }

    private static boolean increment(final Point point)
    {
        System.out.println("increment");
        synchronized(pointCountMap)
        {
            System.out.println("increment: inside synch");
            if (pointCountMap.containsKey(point))
            {
                pointCountMap.put(point, pointCountMap.get(point) + 1);
                System.out.println("increment done");
                return false;
            }
            else
            {
                pointCountMap.put(point, 1);
                System.out.println("increment done");
                return true;
            }
        }
    }

    private static void decrement(final Point point)
    {
        // This function relies on the idLock being held by the current thread.
        // If this fails, this function will throw an assertion exception.

        boolean doRemove = false;

        System.out.println("decrement");

        synchronized (pointCountMap)
        {
            System.out.println("decrement: inside synch A");
            if (pointCountMap.containsKey(point))
            {
                int count = pointCountMap.get(point);
                if (count <= 1)
                {
                    doRemove = true;
                }
                else
                {
                    pointCountMap.put(point, count - 1);
                }
            }
            else
            {
                doRemove = true;
            }
        }

        System.out.println("decrement: left synch A");

        synchronized (pointIdMap)
        {
            System.out.println("decrement: inside synch 1");
            synchronized (idPointMap)
            {
                System.out.println("decrement: inside synch 2");
                if (doRemove)
                {
                    long id = pointIdMap.get(point);
                    pointIdMap.remove(point);
                    idPointMap.remove(id);
                }
            }
        }

        System.out.println("decrement: done");
    }

    public static Point getPoint(final long orig, final Point localPoint)
    {
        System.out.println("getPoint");
        synchronized (pointIdMap)
        {
            System.out.println("getPoint: inside synch 1");
            synchronized (idPointMap)
            {
                final Point point;
                System.out.println("getPoint: inside synch 2");

                point = idPointMap.get(orig);
                decrement(point);

                System.out.println("getPoint: done");

                return point == null ? localPoint : point;
            }
        }
    }

    private final float[] w, l;
    private final long id;
    private final boolean fromOrigin;

    /**
     * Creates a Bottle containing a Point
     * @param point the Point to bottle
     * @param isOrigin true if we're bottling from the root-node perspective, false if from the
     *                 client-node perspective.
     */
    public PointBottle(final Point point, boolean isOrigin)
    {
        // This constructor should only be called from PointBottler.bottle, which is sync'ed.

        // Assume identityHashCode does not change for a given Object
        int idHash = System.identityHashCode(point);

        w = point.getW();
        l = point.getL();
        fromOrigin = isOrigin;

        if (fromOrigin)
        {
            // We are executing on the origin
            final boolean contains;

            System.out.println("constructor");

            synchronized(pointIdMap)
            {
                System.out.println("constructor: inside synch");
                contains = pointIdMap.containsKey(point);
            }

            System.out.println("constructor: left synch");

            //Check to see if we've sent this point before.
            if (contains)
            {
                // If we have, just re-use the existing id.
                id = getId(point, idHash);
                // We're sending it again, so we increment the count
                increment(point);
            }
            else
            {
                // If we haven't, generate a new id...
                id = idGenerator.getAndIncrement();
                putPoint(id, point);
            }
        }
        else
        {
            // We are executing on the remote node.
            if (point instanceof IdPoint)
            {
                // We are ostensibly done with this point. Get its id and decrement the count.
                id = ((IdPoint)point).getID();
                decrement(point);
            }
            else
            {
                // We created a new point on this end. There is no count to decrement. Set the id
                // negative so that on the origin side, we'll know not to look for it in the cache.
                id = -1;
            }
        }
    }

    public Point unBottle(final MessageXC xc)
    {
        if (fromOrigin)
        {
            // This means we're operating on a remote node.
            final Point point = new IdPoint(id, new Point(l, w));
            if (!putPoint(id, point))
            {
                // We have already seen this point, so return the one we generated before.
                // putPoint will have incremented the count for us.
                return getPoint(id, point);
            }
            else
            {
                // We have not seen this point, yet. Return the newly generated one.
                return point;
            }
        }
        else
        {
            // We're operating on the origin node.
            if (id >= 0)
            {
                // We sent this point over a little while ago. When we sent it over, we stored the
                // original point on this end. Now, fetch that point and update its internal state
                // to reflect the changes that were done to it on the remote side.
                final Point point = new Point(l, w);
                final Point origPoint = getPoint(id, point);
                syncPoint(origPoint, point);
                // Decrement the count of the origPoint. If we don't need it any more, we'll clear
                // it from the cache.
                decrement(origPoint);

                return origPoint;
            }
            else
            {
                // This point was created on the remote side, just return a copy.
                return new Point(l, w);
            }
        }
    }

    private static synchronized void syncPoint(final Point to, final Point from)
    {
        if (from != null && to != from)
        {
            final float[] wTo = to.getW(), wFrom = from.getW(),
                    lTo = to.getL(), lFrom = from.getL();

            for (int j = 0; j < wTo.length; ++j)
            {
                wTo[j] = wFrom[j];
            }
            for (int j = 0; j < lTo.length; ++j)
            {
                lTo[j] = lFrom[j];
            }
        }
    }
}
