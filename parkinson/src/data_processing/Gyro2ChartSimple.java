package data_processing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import cern.jdve.Axis;
import cern.jdve.Chart;
import cern.jdve.ChartInteractor;
import cern.jdve.Style;
import cern.jdve.data.DefaultDataSet;
import cern.jdve.graphic.AbstractMarker;
import cern.jdve.renderer.ScatterChartRenderer;

public class Gyro2ChartSimple extends JPanel {
	private static final String CONFPATH = "C:\\Pierre\\parkinson\\";
	private static final String RAW_FILE = "test003.txt";

	private DefaultDataSet gxDataset = new DefaultDataSet("Ax");
	private DefaultDataSet gyDataset = new DefaultDataSet("Ay");
	private DefaultDataSet gzDataset = new DefaultDataSet("Az");
	private DefaultDataSet axDataset = new DefaultDataSet("Gx");
	private DefaultDataSet ayDataset = new DefaultDataSet("Gy");
	private DefaultDataSet azDataset = new DefaultDataSet("Gz");

	public Gyro2ChartSimple(){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				buildGUI();
			}
		});

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					readFiles();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();

	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Csv 2 Chart");
		frame.setLayout(new BorderLayout());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Gyro2ChartSimple main = new Gyro2ChartSimple();
		frame.add(main, BorderLayout.CENTER );
		frame.pack();
		frame.setVisible(true);
	}

	private void buildGUI() {
		setPreferredSize(new Dimension(800, 800));
		setLayout(new GridLayout(3, 2));

		Chart chartAx = new Chart();
		Chart chartAy = new Chart();
		Chart chartAz = new Chart();
		Chart chartGx = new Chart();
		Chart chartGy = new Chart();
		Chart chartGz = new Chart();
		
		add(chartAx); 
		add(chartGx); 
		add(chartAy); 
		add(chartGy); 
		add(chartAz); 
		add(chartGz); 
		
		ScatterChartRenderer scrAx = new ScatterChartRenderer(new AlfaMarker());
		ScatterChartRenderer scrAy = new ScatterChartRenderer(new AlfaMarker());
		ScatterChartRenderer scrAz = new ScatterChartRenderer(new AlfaMarker());
		ScatterChartRenderer scrGx = new ScatterChartRenderer(new AlfaMarker());
		ScatterChartRenderer scrGy = new ScatterChartRenderer(new AlfaMarker());
		ScatterChartRenderer scrGz = new ScatterChartRenderer(new AlfaMarker());
		
		scrAx.setDataSet(axDataset);
		scrAy.setDataSet(ayDataset);
		scrAz.setDataSet(azDataset);
		scrGx.setDataSet(gxDataset);
		scrGy.setDataSet(gyDataset);
		scrGz.setDataSet(gzDataset);
		
		chartAx.addRenderer(scrAx);
		chartAy.addRenderer(scrAy);
		chartAz.addRenderer(scrAz);
		chartGx.addRenderer(scrGx);
		chartGy.addRenderer(scrGy);
		chartGz.addRenderer(scrGz);
		
		chartAx.addInteractor(ChartInteractor.ZOOM);
		chartAy.addInteractor(ChartInteractor.ZOOM);
		chartAz.addInteractor(ChartInteractor.ZOOM);
		chartGx.addInteractor(ChartInteractor.ZOOM);
		chartGy.addInteractor(ChartInteractor.ZOOM);
		chartGz.addInteractor(ChartInteractor.ZOOM);
		chartAx.addInteractor(ChartInteractor.DATA_PICKER);
		chartAy.addInteractor(ChartInteractor.DATA_PICKER);
		chartAz.addInteractor(ChartInteractor.DATA_PICKER);
		chartGx.addInteractor(ChartInteractor.DATA_PICKER);
		chartGy.addInteractor(ChartInteractor.DATA_PICKER);
		chartGz.addInteractor(ChartInteractor.DATA_PICKER);

		chartAy.synchronizeAxis(chartAx, Axis.X_AXIS, true);
		chartAz.synchronizeAxis(chartAx, Axis.X_AXIS, true);
		chartGx.synchronizeAxis(chartAx, Axis.X_AXIS, true);
		chartGy.synchronizeAxis(chartAx, Axis.X_AXIS, true);
		chartGz.synchronizeAxis(chartAx, Axis.X_AXIS, true);
	}

	private void readFiles() throws IOException{
		char[] cbuf4 = new char[4];
		char[] cbuf2 = new char[2];
		char[] timeStampbuf = new char[6];
		int read = 0;
		short y = 0 ;
		short rawTemperature = 0;
		int bytesNumber = 0;
		double timeStamp = 0;
		double time = 0;
		double seconds = 0;
		short[] samples = new short[2048];

		BufferedReader br = new BufferedReader( new FileReader(CONFPATH+RAW_FILE) );

		while (read != -1) {
			
			for (int i = 0; i < samples.length; i++) {
				read = br.read(cbuf4);
				samples[i] = (short) Integer.parseInt(new String(cbuf4), 16);
			}
			
			read = br.read(cbuf4);
			bytesNumber =  Integer.parseInt(new String(cbuf4), 16);
			
			read = br.read(timeStampbuf);
			timeStamp = Integer.parseInt(new String(timeStampbuf), 16);
			timeStamp *= 6400.0;
			timeStamp /= 1000.0; // milliseconds
			
			read = br.read(cbuf4);
			rawTemperature =  (short) Integer.parseInt(new String(cbuf4), 16);
			
			for (int i = 0; i < 4224-4103; i++) {
				read = br.read(cbuf2);
			}

			if (bytesNumber > 0 && bytesNumber < 4096){
			
				System.out.println("bytesNumber: "+bytesNumber+
						", timeStamp: "+timeStamp+
						", rawTemperature:"+rawTemperature);

				for (int i = 0; i < bytesNumber/2; i++) {
					y = samples[i];
					time = timeStamp - ((((bytesNumber/2)-i) * 6400.0)/1000.0);
					seconds = time / 1000.0;
					
					if (i%6 == 0) axDataset.add(seconds, y);
					if (i%6 == 1) ayDataset.add(seconds, y);
					if (i%6 == 2) azDataset.add(seconds, y);
					if (i%6 == 3) gxDataset.add(seconds, y);
					if (i%6 == 4) gyDataset.add(seconds, y);
					if (i%6 == 5) gzDataset.add(seconds, y);
				}
			}
			
		}
		
		br.close();

	}
	
	private class AlfaMarker extends AbstractMarker {

		public void draw(Graphics2D g2, int x, int y, int size, Style style, boolean selected) {
			// check if the size and style were defined in the AbstractMarker
			if (super.size >= 0) {
				size = super.size;
			}
			if (super.style != null) {
				style = super.style;
			}

//			style.renderOval(g2, x - size / 2, y - size / 2, size, size);
			style.drawRect(g2, x, y, 1, 1);
		}


		@Override
		public int getType() {
			return 222;
		}

	}

}
