package fr.univavignon.transpolosearch.data.event;

import java.util.List;
import java.util.concurrent.ExecutorService;

import jsat.linear.Vec;
import jsat.linear.distancemetrics.DistanceMetric;

/**
 * Minimal implementation of a distance, used to compare events 
 * and cluster them with {@link MyPam}.
 *  
 * @author Vincent Labatut
 */
@SuppressWarnings({ "javadoc", "serial" })
public class PredefinedDistanceMetric implements DistanceMetric
{	double[][] dist;
	
	private PredefinedDistanceMetric()
	{	
		//
	}
	public PredefinedDistanceMetric(List<Event> events)
	{	dist = new double[events.size()][events.size()];
		for(int i=0;i<events.size()-1;i++)
		{	Event event1 = events.get(i);
			for(int j=i+1;j<events.size();j++)
			{	Event event2 = events.get(j);
				float sim = event1.processJaccardSimilarity(event2);
				dist[i][j] = 1 - sim;
				dist[j][i] = 1 - sim;
			}
		}
	}
	
	@Override
	public boolean supportsAcceleration()
	{	return false;
	}
	
	@Override
	public double metricBound()
	{	return 1;
	}
	
	@Override
	public boolean isSymmetric()
	{	return false;
	}
	
	@Override
	public boolean isSubadditive()
	{	return false;
	}
	
	@Override
	public boolean isIndiscemible()
	{	return true;
	}
	
	@Override
	public List<Double> getQueryInfo(Vec q)
	{	return null;
	}
	
	@Override
	public List<Double> getAccelerationCache(List<? extends Vec> vecs, ExecutorService threadpool) 
	{	return null;
	}
	
	@Override
	public List<Double> getAccelerationCache(List<? extends Vec> vecs)
	{	return null;
	}
	
	@Override
	public double dist(int a, Vec b, List<Double> qi, List<? extends Vec> vecs, List<Double> cache) 
	{	throw new IllegalArgumentException();
	}
	
	@Override
	public double dist(int a, Vec b, List<? extends Vec> vecs, List<Double> cache) 
	{	throw new IllegalArgumentException();
	}
	
	@Override
	public double dist(int a, int b, List<? extends Vec> vecs, List<Double> cache) 
	{	return dist[a][b];
	}
	
	@Override
	public double dist(Vec a, Vec b)
	{	throw new IllegalArgumentException();
	}
	
    @Override
    public PredefinedDistanceMetric clone()
    {	PredefinedDistanceMetric res = new PredefinedDistanceMetric();
    	res.dist = dist;
		return res;
    }
}
