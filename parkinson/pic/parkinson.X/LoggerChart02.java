package logging;

//sudo java -cp ".:/home/pi/AccelTest/lib/*" logger.LoggerChart01

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

import cern.jdve.Chart;
import cern.jdve.data.DataSet;
import cern.jdve.data.DefaultDataSource;
import cern.jdve.data.ShiftingDataSet;
import cern.jdve.renderer.PolylineChartRenderer;

public class LoggerChart02 extends JPanel {
	public static final int LOGGED_LENGTH = 600;
	public static Font labelFont = new Font("Dialog", Font.PLAIN, 8);
	private ShiftingDataSet xgDataSet = new ShiftingDataSet("GX", LOGGED_LENGTH, false);
	private ShiftingDataSet ygDataSet = new ShiftingDataSet("GY", LOGGED_LENGTH, false);
	private ShiftingDataSet zgDataSet = new ShiftingDataSet("GZ", LOGGED_LENGTH, false);
	private ShiftingDataSet xaDataSet = new ShiftingDataSet("AX", LOGGED_LENGTH, false);
	private ShiftingDataSet yaDataSet = new ShiftingDataSet("AY", LOGGED_LENGTH, false);
	private ShiftingDataSet zaDataSet = new ShiftingDataSet("AZ", LOGGED_LENGTH, false);
	public static final int LSM6DSL_I2C_ADDR  = 0x6A;
	
	public LoggerChart02() {
		setBackground(Color.black);
		setLayout(new BorderLayout());
		add(getChart(), BorderLayout.CENTER);
	}
	
	private void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(500);
					
					final I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
					I2CDevice Lsm6dslDevice = bus.getDevice(LSM6DSL_I2C_ADDR);
					
					int readByte = Lsm6dslDevice.read(0x11);
					byte readBytes[] = new byte[12];
					System.out.println("read at 0x11 : "+ Utils.byteToHex((byte)readByte));
					
					Lsm6dslDevice.write(0x0A, (byte) 0x00); // fifo disable

					Lsm6dslDevice.write(0x10, (byte) 0x21);	// Accel output rate 26Hz, 2g scale
					Lsm6dslDevice.write(0x11, (byte) 0x22); // Gyro output rate 26Hz, 125 dps scale
					
					Lsm6dslDevice.write(0x19, (byte) 0x20); // enable timer

					Lsm6dslDevice.write(0x06, (byte) 0xFF); // fifo watermark
					Lsm6dslDevice.write(0x07, (byte) 0x07); // fifo watermark 
					
					Lsm6dslDevice.write(0x08, (byte) 0x09); // fifo no decimation
					
					
					Thread.sleep(100);
					Lsm6dslDevice.write(0x0A, (byte) 0x16); // fifo enable, 26Hz, continuous
					
					int fifoStatus1, fifoStatus2, fifoStatus3, fifoOutH, fifoOutL;  
					int loop = 0, unread = 0;
					
					while(true) {
						fifoStatus1 = Lsm6dslDevice.read(0x3A);
						fifoStatus2 = Lsm6dslDevice.read(0x3B);
						
						unread = (fifoStatus2 << 8) + fifoStatus1;
						
						if (unread <= 12) {
							System.out.print(loop+"\n\n");
							loop = 0;
							Thread.sleep(10000);
							continue;
						}
						
						fifoStatus3 = Lsm6dslDevice.read(0x3C);
						fifoOutL = Lsm6dslDevice.read(0x3E);
						fifoOutH = Lsm6dslDevice.read(0x3F);
						
						if (loop%6 == 0) System.out.print('.');
						
//						System.out.println("l:"+loop+
//								", U:"+ Utils.byteToHex((byte)fifoStatus1)+
//								", P:"+ Utils.byteToHex((byte)fifoStatus3)+
//								", FL:"+ Utils.byteToHex((byte)fifoOutL)+
//								", FH:"+ Utils.byteToHex((byte)fifoOutH));
//						
						loop++;
					}					
					
					
				} catch (InterruptedException | UnsupportedBusNumberException | IOException e) {
					e.printStackTrace();
				}
				
			}
		}).start();
	}
	
	private Chart chart;
	private Chart getChart() {
		if(chart == null) {
			chart = new Chart();
			
			DefaultDataSource dataSource = new DefaultDataSource(new DataSet[] {
					xgDataSet, ygDataSet, zgDataSet, xaDataSet, yaDataSet, zaDataSet
			});
			
			PolylineChartRenderer lineRenderer0 = new PolylineChartRenderer();
			lineRenderer0.setDataSource(dataSource);
			
			chart.addRenderer(lineRenderer0);
			
			chart.getYScale().setLabelFont(labelFont);
			chart.getXScale().setVisible(false);
			chart.setXGridVisible(false);
			chart.setAntiAliasing(true);
			
		}
		return chart;
	}

	
	public static void main(String[] args) {

		LoggerChart02 l = new LoggerChart02();
//		JFrame f = new JFrame("Gyro");
//		f.getContentPane().add(l);
//        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        f.setExtendedState(JFrame.MAXIMIZED_BOTH); 
//        f.setUndecorated(true);
//        f.setVisible(true);
//        f.pack();
        
        l.start();
		
		
	}

}
