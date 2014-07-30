package edu.utexas.clm.archipelago.plugin;

import edu.utexas.clm.archipelago.Cluster;
import edu.utexas.clm.archipelago.FijiArchipelago;
import edu.utexas.clm.archipelago.ijsupport.bottle.PointBottler;
import ij.IJ;
import ij.plugin.PlugIn;
import mpicbg.models.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 *
 */
public class Stress_Point_Test implements PlugIn
{
    public static class PointCallable implements Callable<ArrayList<Point>>, Serializable
    {
        private final ArrayList<Point> points;

        public PointCallable(final List<Point> points)
        {
            this.points = new ArrayList<Point>(points);
        }


        public ArrayList<Point> call() throws Exception
        {
            ArrayList<Point> outputPoints = new ArrayList<Point>(points.size());

            for (final Point point : points)
            {
                final float[] l = point.getL(), w = point.getW();
                for (final Point other : outputPoints)
                {
                    final float[] ol = other.getL(), ow = other.getW();
                    for (int i = 0; i < l.length; ++i)
                    {
                        l[i] += ol[i];
                        w[i] += ow[i];
                    }
                }

                outputPoints.add(point);
            }

            return outputPoints;
        }
    }

    private Random random;

    private ArrayList<Point> generateRandomPoints(final int n)
    {
        final ArrayList<Point> points = new ArrayList<Point>(n);

        for (int i = 0; i < n; ++i)
        {
            final float[] l = new float[2];
            final Point pt;

            l[0] = random.nextFloat();
            l[1] = random.nextFloat();

            pt = new Point(l);

            points.add(pt);
        }

        return points;
    }

    public void run(String s)
    {
        final Cluster cluster;
        final int n = 1024 * 16;
        final int m = 1024;
        int count = 0;
        long tLast, tElapse;

        final ArrayList<Future<ArrayList<Point>>> pointFutures;
        final ExecutorService service;

        if (Cluster.initializedCluster())
        {
            cluster = Cluster.getCluster();
            if (cluster.numRegisteredUIs() <= 0)
            {
                if (!FijiArchipelago.runClusterGUI())
                {
                    return;
                }
            }
        }
        else
        {
            if (!FijiArchipelago.runClusterGUI())
            {
                return;
            }
            cluster = Cluster.getCluster();
        }

        cluster.addBottler(new PointBottler());
        random = new Random(System.currentTimeMillis());
        pointFutures = new ArrayList<Future<ArrayList<Point>>>(m);

        service = cluster.getService(1);


        try
        {
            while (true)
            {
                IJ.log("Submitting futures...");

                ++count;

                for (int i = 0; i < m; ++i)
                {
                    pointFutures.add(service.submit(new PointCallable(generateRandomPoints(n))));
                }

                tLast = System.currentTimeMillis();
                for (final Future<ArrayList<Point>> future : pointFutures)
                {
                    future.get();
                }

                tElapse = System.currentTimeMillis() - tLast;

                IJ.log("Pass " + count + " took " + tElapse + "ms...");

                pointFutures.clear();
            }
        }
        catch (InterruptedException ie)
        {
            IJ.error("" + ie);
        }
        catch (ExecutionException ee)
        {
            IJ.error("" + ee);
            ee.printStackTrace();
        }

    }
}
