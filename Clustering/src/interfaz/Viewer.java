package interfaz;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

import general.*;

public class Viewer
{
	private JFrame _frame;
	static int viewnumber = 0;
	
	public Viewer(Instance instance, Solution solution)
	{
		viewnumber++;
		XYSeriesCollection dataset = createDataset(instance, solution);
		JFreeChart xylineChart = ChartFactory.createXYLineChart("", "", "", dataset, PlotOrientation.VERTICAL, true, true, false);
		ChartPanel chartPanel = new ChartPanel(xylineChart);
		XYPlot plot = xylineChart.getXYPlot();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		
		for(int i=0; i<dataset.getSeriesCount(); ++i)
		{
			renderer.setSeriesLinesVisible(i, false);
			renderer.setSeriesShape(i, ShapeUtilities.createRegularCross(2, 2));
		}
		
		renderer.setSeriesPaint(0, Color.LIGHT_GRAY);
		plot.setRenderer(renderer);
		plot.setBackgroundPaint(Color.WHITE);
		xylineChart.removeLegend();
		
		if( solution != null )
		{
			for(Cluster cluster: solution.getClusters()) if( cluster instanceof RectangularCluster )
			{
				RectangularCluster rectCluster = (RectangularCluster)cluster;
				
				Shape rectangle = new Rectangle2D.Double(rectCluster.getMin(0), rectCluster.getMin(1), rectCluster.getMax(0) - rectCluster.getMin(0), rectCluster.getMax(1) - rectCluster.getMin(1));
				XYShapeAnnotation shapeAnnotation = new XYShapeAnnotation(rectangle, new BasicStroke(0.5f), Color.GRAY);
				plot.addAnnotation(shapeAnnotation);
			}
		}
		
		_frame = new JFrame();
		_frame.setTitle("View number: " + viewnumber);
		_frame.setBounds(100, 100, 622, 640);
		_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		_frame.getContentPane().add(chartPanel);
		_frame.setVisible(true);
	}
	
	private XYSeriesCollection createDataset(Instance instance, Solution solution)
	{
		XYSeriesCollection dataset = new XYSeriesCollection();
		XYSeries outliers = new XYSeries("Outliers");
		dataset.addSeries(outliers);
		
		Set<Point> clustered = new HashSet<Point>();
		
		if( solution != null )
		{
			for(Cluster cluster: solution.getClusters())
			{
				XYSeries series = new XYSeries("Cluster " + (dataset.getSeriesCount() + 1));
				for(Point point: cluster.asSet())
				{
					series.add(point.get(0), point.get(1));
					clustered.add(point);
				}
				
				dataset.addSeries(series);
			}
		}
		
		for(int i=0; i<instance.getPoints(); ++i) if( clustered.contains(instance.getPoint(i)) == false )
			outliers.add(instance.getPoint(i).get(0), instance.getPoint(i).get(1));

		return dataset;
	}
}
