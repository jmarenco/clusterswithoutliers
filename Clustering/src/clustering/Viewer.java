package clustering;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

public class Viewer
{
	private JFrame _frame;
	
	public Viewer(Instance instance, Master master)
	{
		XYSeriesCollection dataset = createDataset(instance, master);
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
		
		_frame = new JFrame();
		_frame.setBounds(100, 100, 622, 640);
		_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		_frame.getContentPane().add(chartPanel);
		_frame.setVisible(true);
	}
	
	private XYSeriesCollection createDataset(Instance instance, Master master)
	{
		XYSeriesCollection dataset = new XYSeriesCollection();
		XYSeries outliers = new XYSeries("Outliers");
		dataset.addSeries(outliers);
		
		Set<Point> clustered = new HashSet<Point>();
		for(int i=0, j=0; i<master.getClusters().size(); ++i) if( master.getPrimal(i) > 0.9 )
		{
			XYSeries series = new XYSeries("Cluster " + (++j));
			for(Point point: master.getClusters().get(i).asSet())
			{
				series.add(point.get(0), point.get(1));
				clustered.add(point);
			}
			
			dataset.addSeries(series);
		}
		
		for(int i=0; i<instance.getPoints(); ++i) if( clustered.contains(instance.getPoint(i)) == false )
			outliers.add(instance.getPoint(i).get(0), instance.getPoint(i).get(1));

		return dataset;
	}
}
