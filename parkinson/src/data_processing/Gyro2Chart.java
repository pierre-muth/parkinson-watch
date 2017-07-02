package data_processing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import cern.jdve.Axis;
import cern.jdve.Chart;
import cern.jdve.ChartInteractor;
import cern.jdve.Style;
import cern.jdve.data.DefaultDataSet;
import cern.jdve.data.DefaultDataSet3D;
import cern.jdve.graphic.AbstractMarker;
import cern.jdve.renderer.ContourChartRenderer;
import cern.jdve.renderer.PolylineChartRenderer;
import cern.jdve.renderer.ScatterChartRenderer;

public class Gyro2Chart extends JPanel {
	private static final String CONFPATH = "C:\\Pierre\\parkinson\\";
	private static final String RAW_FILE = "test005.txt";
	private static final String OUT_FILE = "test005.csv";

	private DefaultDataSet gxDataset = new DefaultDataSet("Ax");
	private DefaultDataSet gyDataset = new DefaultDataSet("Ay");
	private DefaultDataSet gzDataset = new DefaultDataSet("Az");
	private DefaultDataSet axDataset = new DefaultDataSet("Gx");
	private DefaultDataSet ayDataset = new DefaultDataSet("Gy");
	private DefaultDataSet azDataset = new DefaultDataSet("Gz");
	
	private DefaultDataSet3D axFFTDataset = new DefaultDataSet3D("Ax FFT");
	private DefaultDataSet3D ayFFTDataset = new DefaultDataSet3D("Ay FFT");
	private DefaultDataSet3D azFFTDataset = new DefaultDataSet3D("Az FFT");
	private DefaultDataSet3D gxFFTDataset = new DefaultDataSet3D("Gx FFT");
	private DefaultDataSet3D gyFFTDataset = new DefaultDataSet3D("Gy FFT");
	private DefaultDataSet3D gzFFTDataset = new DefaultDataSet3D("Gz FFT");

	private DefaultDataSet temperatureDataset = new DefaultDataSet("temperature");
	
	public Gyro2Chart(){
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
//					doFFT(axDataset, axFFTDataset);
//					doFFT(ayDataset, ayFFTDataset);
//					doFFT(azDataset, azFFTDataset);
//					doFFT(gxDataset, gxFFTDataset);
//					doFFT(gyDataset, gyFFTDataset);
//					doFFT(gzDataset, gzFFTDataset);
//					writeFile();
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
		Gyro2Chart main = new Gyro2Chart();
		frame.add(main, BorderLayout.CENTER );
		frame.pack();
		frame.setVisible(true);
	}

	private void buildGUI() {
		Font font = new Font(Font.DIALOG, Font.PLAIN, 10);

		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(1200, 800));
		
		JPanel axisPanel = new JPanel(new GridLayout(3, 4));
		axisPanel.setPreferredSize(new Dimension(1200, 600));
		
		JPanel temperaturePanel = new JPanel(new BorderLayout());
		temperaturePanel.setPreferredSize(new Dimension(1200, 200));
		
		add(axisPanel, BorderLayout.CENTER);
		add(temperaturePanel, BorderLayout.SOUTH);

		Chart chartAx = new Chart();
		Chart chartAy = new Chart();
		Chart chartAz = new Chart();
		Chart chartGx = new Chart();
		Chart chartGy = new Chart();
		Chart chartGz = new Chart();
		
		Chart chartAxFFT = new Chart();
		Chart chartAyFFT = new Chart();
		Chart chartAzFFT = new Chart();
		Chart chartGxFFT = new Chart();
		Chart chartGyFFT = new Chart();
		Chart chartGzFFT = new Chart();
		
		Chart chartTemperature = new Chart();
		
		axisPanel.add(chartAx);
		axisPanel.add(chartAxFFT);
		axisPanel.add(chartGx);
		axisPanel.add(chartGxFFT);
		axisPanel.add(chartAy); 
		axisPanel.add(chartAyFFT);
		axisPanel.add(chartGy); 
		axisPanel.add(chartGyFFT);
		axisPanel.add(chartAz); 
		axisPanel.add(chartAzFFT);
		axisPanel.add(chartGz); 
		axisPanel.add(chartGzFFT);
		
		temperaturePanel.add(chartTemperature);
		
		chartAx.getYScale().setLabelFont(font);
		chartAy.getYScale().setLabelFont(font);
		chartAz.getYScale().setLabelFont(font);
		chartGx.getYScale().setLabelFont(font);
		chartGy.getYScale().setLabelFont(font);
		chartGz.getYScale().setLabelFont(font);
		
		chartAx.getXScale().setLabelFont(font);
		chartAy.getXScale().setLabelFont(font);
		chartAz.getXScale().setLabelFont(font);
		chartGx.getXScale().setLabelFont(font);
		chartGy.getXScale().setLabelFont(font);
		chartGz.getXScale().setLabelFont(font);
		
		chartAxFFT.getYScale().setLabelFont(font);
		chartAyFFT.getYScale().setLabelFont(font);
		chartAzFFT.getYScale().setLabelFont(font);
		chartGxFFT.getYScale().setLabelFont(font);
		chartGyFFT.getYScale().setLabelFont(font);
		chartGzFFT.getYScale().setLabelFont(font);
		
		chartAxFFT.getXScale().setLabelFont(font);
		chartAyFFT.getXScale().setLabelFont(font);
		chartAzFFT.getXScale().setLabelFont(font);
		chartGxFFT.getXScale().setLabelFont(font);
		chartGyFFT.getXScale().setLabelFont(font);
		chartGzFFT.getXScale().setLabelFont(font);
		
		chartAx.setYScaleTitle("Accelerometer X (g)");
		chartAy.setYScaleTitle("Accelerometer Y (g)");
		chartAz.setYScaleTitle("Accelerometer Z (g)");
		chartGx.setYScaleTitle("Angle rate X (dps)");
		chartGy.setYScaleTitle("Angle rate Y (dps)");
		chartGz.setYScaleTitle("Angle rate Z (dps)");
		
		chartAxFFT.setYScaleTitle("Accelerometer X (Hz)");
		chartAyFFT.setYScaleTitle("Accelerometer Y (Hz)");
		chartAzFFT.setYScaleTitle("Accelerometer Z (Hz)");
		chartGxFFT.setYScaleTitle("Angle rate X (Hz)");
		chartGyFFT.setYScaleTitle("Angle rate Y (Hz)");
		chartGzFFT.setYScaleTitle("Angle rate Z (Hz)");
		
		chartAz.setXScaleTitle("Time (s)");
		chartGz.setXScaleTitle("Time (s)");
		
		chartAzFFT.setXScaleTitle("Time (s)");
		chartGzFFT.setXScaleTitle("Time (s)");
		
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
		chartAxFFT.addInteractor(ChartInteractor.ZOOM);
		chartAyFFT.addInteractor(ChartInteractor.ZOOM);
		chartAzFFT.addInteractor(ChartInteractor.ZOOM);
		chartGxFFT.addInteractor(ChartInteractor.ZOOM);
		chartGyFFT.addInteractor(ChartInteractor.ZOOM);
		chartGzFFT.addInteractor(ChartInteractor.ZOOM);
		chartAx.addInteractor(ChartInteractor.DATA_PICKER);
		chartAy.addInteractor(ChartInteractor.DATA_PICKER);
		chartAz.addInteractor(ChartInteractor.DATA_PICKER);
		chartGx.addInteractor(ChartInteractor.DATA_PICKER);
		chartGy.addInteractor(ChartInteractor.DATA_PICKER);
		chartGz.addInteractor(ChartInteractor.DATA_PICKER);
		
		ContourChartRenderer ccrAx = new ContourChartRenderer();
		ContourChartRenderer ccrAy = new ContourChartRenderer();
		ContourChartRenderer ccrAz = new ContourChartRenderer();
		ContourChartRenderer ccrGx = new ContourChartRenderer();
		ContourChartRenderer ccrGy = new ContourChartRenderer();
		ContourChartRenderer ccrGz = new ContourChartRenderer();

		ccrAx.setDataSet(axFFTDataset);
		ccrAy.setDataSet(ayFFTDataset);
		ccrAz.setDataSet(azFFTDataset);
		ccrGx.setDataSet(gxFFTDataset);
		ccrGy.setDataSet(gyFFTDataset);
		ccrGz.setDataSet(gzFFTDataset);
		
		chartAxFFT.addRenderer(ccrAx);
		chartAyFFT.addRenderer(ccrAy);
		chartAzFFT.addRenderer(ccrAz);
		chartGxFFT.addRenderer(ccrGx);
		chartGyFFT.addRenderer(ccrGy);
		chartGzFFT.addRenderer(ccrGz);
		
//		chartAy.synchronizeAxis(chartAx, Axis.X_AXIS, true);
//		chartAz.synchronizeAxis(chartAx, Axis.X_AXIS, true);
//		chartGx.synchronizeAxis(chartAx, Axis.X_AXIS, true);
//		chartGy.synchronizeAxis(chartAx, Axis.X_AXIS, true);
//		chartGz.synchronizeAxis(chartAx, Axis.X_AXIS, true);
		
		PolylineChartRenderer scrTemperature = new PolylineChartRenderer(new AlfaMarker());
		scrTemperature.setDataSet(temperatureDataset);
		chartTemperature.addRenderer(scrTemperature);
		chartTemperature.addInteractor(ChartInteractor.ZOOM);
		chartTemperature.addInteractor(ChartInteractor.DATA_PICKER);
		chartTemperature.getYScale().setLabelFont(font);
		chartTemperature.getXScale().setLabelFont(font);
		chartTemperature.setYScaleTitle("Temperature (C)");
		chartTemperature.setXScaleTitle("Time (s)");
		
	}
	
	private void writeFile() {
		BufferedWriter logFileWriter;
		try {
			Path dir = Paths.get(CONFPATH);
			logFileWriter = Files.newBufferedWriter(dir.resolve(OUT_FILE), Charset.defaultCharset());
			
			logFileWriter.write("time (s), Accelerometer X (g), "
					+ "time (s), Accelerometer Y (g), "
					+ "time (s), Accelerometer Z (g), "
					+ "time (s), Angle rate X (dps), "
					+ "time (s), Angle rate Y (dps), "
					+ "time (s), Angle rate Z (dps), "
					+ "time (s), Temperature (C)\n");
			
			String line = "";
			for (int i = 0; i < axDataset.getDataCount(); i++) {
				line = axDataset.getX(i)+", ";
				line += axDataset.getY(i)+", ";
				line += ayDataset.getX(i)+", ";
				line += ayDataset.getY(i)+", ";
				line += azDataset.getX(i)+", ";
				line += azDataset.getY(i)+", ";
				line += gxDataset.getX(i)+", ";
				line += gxDataset.getY(i)+", ";
				line += gyDataset.getX(i)+", ";
				line += gyDataset.getY(i)+", ";
				line += gzDataset.getX(i)+", ";
				line += gzDataset.getY(i)+"\n";
				
				logFileWriter.write(line);
				System.out.println("Will wrote: "+i+" lines");
			}
			
			logFileWriter.flush();
			logFileWriter.close();
			
			System.out.println("Wrote file");
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
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
		double temperature = 0;
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
			temperature = (rawTemperature/256.0)+25.0;
			
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
					
					if (i%6 == 0) gxDataset.add(seconds, y*(250.0/65536.0));
					if (i%6 == 1) gyDataset.add(seconds, y*(250.0/65536.0));
					if (i%6 == 2) gzDataset.add(seconds, y*(250.0/65536.0));
					if (i%6 == 3) axDataset.add(seconds, y*(4.0/65536.0));
					if (i%6 == 4) ayDataset.add(seconds, y*(4.0/65536.0));
					if (i%6 == 5) azDataset.add(seconds, y*(4.0/65536.0));
				}
				
				temperatureDataset.add(timeStamp/1000.0, temperature);
				
			}
			
		}
		
		br.close();
		
		System.out.println("GX points: "+gxDataset.getDataCount());
		System.out.println("GY points: "+gyDataset.getDataCount());
		System.out.println("GZ points: "+gzDataset.getDataCount());
		System.out.println("AX points: "+axDataset.getDataCount());
		System.out.println("AY points: "+ayDataset.getDataCount());
		System.out.println("AZ points: "+azDataset.getDataCount());

	}
	
	private void windowing(double[] realArray){
		double coef;
		// HAMMING
		for (int i = 0; i < realArray.length; i++) {
			coef = 0.54 - 0.46 * Math.cos( (2 * Math.PI * i) / (realArray.length - 1) );
			realArray[i] = realArray[i] * coef;
        }
		
		/*
		 
        HAMMING:
                    y[i] = 0.54 - 0.46 * Math.cos((2 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1));
 		HANN:
                    y[i] = 0.5 * (1 - Math.cos((2 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1)));

        BLACKMAN:
                    y[i] = 0.42 - 0.5 * (Math.cos((2 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1))) + 0.08
                            * (Math.cos((4 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1)));
        NUTTALL:
                    y[i] = 0.355768 - 0.487396 * (Math.cos((2 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1)))
                            + 0.144232 * (Math.cos((4 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1))) - 0.012604
                            * (Math.cos((6 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1)));
        BLACKMAN_HARRIS:
                    y[i] = 0.35875 - 0.48829 * (Math.cos((2 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1)))
                            + 0.14128 * (Math.cos((4 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1))) - 0.01168
                            * (Math.cos((6 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1)));
        BLACKMAN_NUTTALL:
                    y[i] = 0.3635819 - 0.4891775 * (Math.cos((2 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1)))
                            + 0.1365995 * (Math.cos((4 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1)))
                            - 0.0106411 * (Math.cos((6 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1)));
        FLAT_TOP:
                    y[i] = 1 - 1.93 * (Math.cos((2 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1))) + 1.29
                            * (Math.cos((4 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1))) - 0.388
                            * (Math.cos((6 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1))) + 0.032
                            * (Math.cos((8 * Math.PI * i * reducingFactor) / (acqNbOfTurns - 1)));
		 */
	}
	
	private void doFFT(DefaultDataSet datasetIn, DefaultDataSet3D datasetOut) {
		int lengthFFT = 128;
		int step = 128;
		double max = Math.pow(2, 32);
		double re, im;
		double[] yValues;
		double[] realArray = new double[lengthFFT];
		double[] imagArray = new double[lengthFFT];
		double[] magnitudeSpectrumArray = new double[lengthFFT/2];
		double[] freqArray = new double[lengthFFT/2];
		double[][] zMag;
		double[] timeArray;
		
		yValues = datasetIn.getYValues();
		int timeSize = (yValues.length - lengthFFT)/step;
		
		timeArray = new double[timeSize];
		zMag = new double[timeSize][];
		
		for (int i = 0; i < freqArray.length; i++) {
			freqArray[i] = (i/(double)lengthFFT) * 26.0;
		}
		for (int i = 0; i < timeArray.length; i++) {
			timeArray[i] = (i*step)/26;
		}
		
		
		for (int inIdx = 0; inIdx < timeSize; inIdx++) {
			
			realArray = Arrays.copyOfRange(yValues, inIdx*step, (inIdx*step)+lengthFFT);
			
			windowing(realArray);
			
			Arrays.fill(imagArray, 0.0);
			FastFourierTransform.fastFT(realArray, imagArray, true);
			
			for (int i = 0; i < magnitudeSpectrumArray.length; i++) {
				re = realArray[i];
				im = imagArray[i];
				magnitudeSpectrumArray[i] = (10 * Math.log10((re * re + im * im) / max));
			}
			
			zMag[inIdx] = magnitudeSpectrumArray.clone();
			
//			System.out.println(inIdx);
		}
		
		datasetOut.set(timeArray, freqArray, zMag, true, true);
		
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
//			style.drawRect(g2, x, y, 1, 1);
			style.drawLine(g2, x, y, x, y);
		}


		@Override
		public int getType() {
			return 222;
		}

	}

}
