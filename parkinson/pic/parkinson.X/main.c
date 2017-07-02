/*
 * File:   main.c
 * Author: pfreyerm
 * chip: PIC16F1829
 * Created on June 11, 2017, 4:36 PM
 */


#include <xc.h>
#include <stdio.h>
#include <stdlib.h>

// PIC16F1829 Configuration Bit Settings
// #pragma config statements should precede project file includes.
// Use project enums instead of #define for ON and OFF.

// CONFIG1
#pragma config FOSC = INTOSC    // Oscillator Selection (INTOSC oscillator: I/O function on CLKIN pin)
#pragma config WDTE = SWDTEN    // Watchdog Timer Enable (WDT enabled)
#pragma config PWRTE = OFF      // Power-up Timer Enable (PWRT disabled)
#pragma config MCLRE = OFF      // MCLR Pin Function Select (MCLR/VPP pin function is digital input)
#pragma config CP = OFF         // Flash Program Memory Code Protection (Program memory code protection is disabled)
#pragma config CPD = OFF        // Data Memory Code Protection (Data memory code protection is disabled)
#pragma config BOREN = ON       // Brown-out Reset Enable (Brown-out Reset enabled)
#pragma config CLKOUTEN = OFF   // Clock Out Enable (CLKOUT function is disabled. I/O or oscillator function on the CLKOUT pin)
#pragma config IESO = OFF       // Internal/External Switchover (Internal/External Switchover mode is disabled)
#pragma config FCMEN = ON       // Fail-Safe Clock Monitor Enable (Fail-Safe Clock Monitor is enabled)

// CONFIG2
#pragma config WRT = OFF        // Flash Memory Self-Write Protection (Write protection off)
#pragma config PLLEN = ON       // PLL Enable (4x PLL enabled)
#pragma config STVREN = ON      // Stack Overflow/Underflow Reset Enable (Stack Overflow or Underflow will cause a Reset)
#pragma config BORV = LO        // Brown-out Reset Voltage Selection (Brown-out Reset Voltage (Vbor), low trip point selected.)
#pragma config LVP = ON         // Low-Voltage Programming Enable (Low-voltage programming enabled)

// CPU freq
#define _XTAL_FREQ  (32000000UL)

// LED
#define LED     RB6

// button
#define BUTTON  RC6

// SPI CS pins
#define CS_gyro     RC0
#define CS_flash    RA4

//
#define INT_gyro    RA2
#define HOLD_flash  RC2

//proto
void putch(unsigned char byte);
void init();
void gyro_spiRW(unsigned char address, unsigned char * data, int dataLength);
void flash_spiRW(unsigned char address, unsigned char * data, int dataLength);
void serialCharOut(unsigned char toSend);
void serialOut(unsigned char * string, int length);
void flash_read_page(unsigned short long page_address);
void flash_block_erase(unsigned short long page_address);
unsigned char dump_gyro_fifo_to_flash(unsigned short long page_address);
void read_flash();
void record();

//global var
unsigned char payload[32];        //general purpose payload 
unsigned short long flash_page_address = 0x000000;      // 24bit

void init(){
    OSCCONbits.IRCF = 0b1110;   // 32MHz
    WDTCONbits.WDTPS = 0b01110; // watchdog delay 16s
    WDTCONbits.SWDTEN = 0;      // disable watchdog

    ANSELA = 0b00000000;    // no analog input on port A
    ANSELB = 0b00000000;    // no analog input on port b
    ANSELC = 0b00000000;    // no analog input on port C
    TRISA = 0b11111111;
    TRISB = 0b11111111;
    TRISC = 0b11111111;
    
    TRISB7 = 0;             // SPI2 CLK as output
    TRISB5 = 1;             // SPI2 SDI as input
    TRISA5 = 0;             // SPI2 SDO as output
    TRISA4 = 0;             // SPI2 CS flash command pin
    TRISC0 = 0;             // SPI2 CS gyro command pin
    TRISB6 = 0;             // LED outup
    TRISC6 = 1;             // Button input
    TRISA2 = 1;             // INT_gyro input
    TRISC2 = 0;             // HOLD_flash output

    //SPI conf
    APFCON1bits.SDO2SEL = 1;
    APFCON1bits.SS2SEL = 1;
    SSP2CON1bits.CKP = 0;                // SPI clk phase
    SSP2STATbits.CKE = 1;                // SPI clk default low
    SSP2CON1bits.SSPM0 = 0;              // SPI clock Fosc/64
    SSP2CON1bits.SSPM1 = 1;              // "
    SSP2CON1bits.SSPM2 = 0;              // "
    SSP2CON1bits.SSPM3 = 0;              // "
    SSP2CON1bits.SSPEN = 1;              // Enable SPI
    
    //Serial conf
    APFCON0bits.TXCKSEL = 1;    // TX on RC4
    TXSTAbits.SYNC = 0;         // async serial
    TXSTAbits.BRGH = 1;         // high freq bauds
    BAUDCONbits.BRG16 = 1;      // 16bit baud generator
    SPBRGL = 68;                // 115200 bauds
    SPBRGH = 0;
    RCSTAbits.SPEN = 1;         // enable serial
    TXSTAbits.TXEN = 1;         // enable serial TX

    //Interrupt conf
    INTCONbits.INTE = 1;        // enable Int pin (gyro fifo watermark)
    INTCONbits.GIE = 1;         // Enable general interrupt
    
    CS_gyro = 1;
    CS_flash = 1;
    HOLD_flash = 1;
    
    __delay_ms(1);
    
    // init gyro
    payload[0] = 0x00;
    gyro_spiRW(0x0A, payload, 1);   // fifo disable
    payload[0] = 0x21;
    gyro_spiRW(0x10, payload, 1);   // Accel output rate 26Hz, 2g scale
    payload[0] = 0x22;
    gyro_spiRW(0x11, payload, 1);   // Gyro output rate 26Hz, 125 dps scale
//    payload[0] = 0x10;
//    gyro_spiRW(0x15, payload, 1);   // Accel low power mode
//    payload[0] = 0x80;
//    gyro_spiRW(0x16, payload, 1);   // Gyro low power mode
    payload[0] = 0x20;
    gyro_spiRW(0x19, payload, 1);   // enable timer
    payload[0] = 0x64;
    gyro_spiRW(0x06, payload, 1);   // fifo watermark L
    payload[0] = 0x07;
    gyro_spiRW(0x07, payload, 1);   // fifo watermark H
    payload[0] = 0x09;
    gyro_spiRW(0x08, payload, 1);   // fifo no decimation
    payload[0] = 0x08;
    gyro_spiRW(0x0D, payload, 1);   // INT1 control - only fifo watermark
    __delay_ms(1);
    payload[0] = 0x16;
    gyro_spiRW(0x0A, payload, 1);   // fifo enable, 26Hz, continuous

    // init flash
    flash_spiRW(0xFF, payload, 0);  // reset flash
    __delay_ms(10);
}

// interrupt
void interrupt isr(void) {
    if (INTCONbits.INTF = 1){
        // woke-up from interrupt
        
        INTCONbits.INTF = 0;
    }
    
}

void serialCharOut(unsigned char toSend) {
    TXREG = toSend;
    while (!TXSTAbits.TRMT) {;}
}

void putch(unsigned char byte) {
    serialCharOut(byte);
}

void serialOut(unsigned char * data, int length) {
    for(unsigned int i=0; i<length; i++){
        TXREG = data[i];
        while (TXSTAbits.TRMT == 0) {;}
    }
}

void gyro_spiRW(unsigned char adress, unsigned char * data, int dataLength){

    CS_gyro = 0;
    SSP2BUF = adress;
    while (SSP2STATbits.BF == 0){;}

    for(unsigned int i=0; i<dataLength; i++){
        SSP2BUF = data[i];
        while (SSP2STATbits.BF == 0){;}
        data[i] = SSP2BUF;
    }
    CS_gyro = 1;
}

void flash_spiRW(unsigned char adress, unsigned char * data, int dataLength){

    CS_flash = 0;
    SSP2BUF = adress;
    while (SSP2STATbits.BF == 0){;}

    for(unsigned int i=0; i<dataLength; i++){
        SSP2BUF = data[i];
        while (SSP2STATbits.BF == 0){;}
        data[i] = SSP2BUF;
    }
    CS_flash = 1;
}

void flash_read_page(unsigned short long page_address){
    
    payload[0] = (page_address & 0xFF0000) >> 16;   // address
    payload[1] = (page_address & 0x00FF00) >> 8;    // address
    payload[2] = (page_address & 0x0000FF);         // address
    flash_spiRW(0x13, payload, 3);                  // read cell array

    __delay_ms(1);

    CS_flash = 0;
    SSP2BUF = 0x03;                 // read buffer command
    while (SSP2STATbits.BF == 0){;}
    SSP2BUF = 0x00;                 // start address high
    while (SSP2STATbits.BF == 0){;}
    SSP2BUF = 0x00;                 // start address low
    while (SSP2STATbits.BF == 0){;}
    SSP2BUF = 0xFF;                 // dummy
    while (SSP2STATbits.BF == 0){;}
    
    for (unsigned int i = 0; i < 4224; i++){
        SSP2BUF = 0xFF;                  // for read
        while (SSP2STATbits.BF == 0){;}
        serialCharOut(SSP2BUF);          // send on serial port
    }
    CS_flash = 1;
}

void flash_block_erase(unsigned short long page_address) {
    printf("Erasing block %X\n", page_address);
    
    payload[0] = 0xA0;                  // lock feature
    payload[1] = 0x00;                  // all unlock
    flash_spiRW(0x1F, payload, 2);      // set features
    
    flash_spiRW(0x06, payload, 0);      // write enable
    
    payload[0] = (page_address & 0xFF0000) >> 16;   // address
    payload[1] = (page_address & 0x00FF00) >> 8;    // address
    payload[2] = (page_address & 0x0000FF);         // address
    flash_spiRW(0xD8, payload, 3);      // block erase
    
    __delay_ms(10);
    printf("Erasing done\n");
    
}
    
unsigned char dump_gyro_fifo_to_flash(unsigned short long page_address) {
    
    unsigned int unread = 0x01;
    unsigned int byte_address = 0x0000;
    unsigned char pattern = 0xFF;
    
    gyro_spiRW(0xBA, payload, 2);       // read fifo status
    unread = (payload[1] << 8) + payload[0];
    
    if (unread <= 12) return;
    
    payload[0] = 0xA0;              // lock feature command
    payload[1] = 0x00;              // all unlocked
    flash_spiRW(0x1F, payload, 2);  // set features command
    
    flash_spiRW(0x06, payload, 0);  // write enable command

    payload[0] = 0x00;              // address high
    payload[1] = 0x00;              // address low
    payload[2] = 0xFF;              // dummy 
    flash_spiRW(0x02, payload, 3);  // program load
    
    while (unread > 12 && byte_address < 4090) {
        
        while(pattern != 0x00) {            // searching begining pattern: 0x00
            gyro_spiRW(0xBC, payload, 1);   // read pattern
            pattern = payload[0];
            if (pattern != 0x00){
                gyro_spiRW(0xBE, payload, 2);      // read L & H
            }
        }
        
        gyro_spiRW(0xBE, payload, 2);      // read L & H of axis 1
        payload[2] = payload[1];
        payload[3] = payload[0];
        gyro_spiRW(0xBE, payload, 2);      // read L & H of axis 2
        payload[4] = payload[1];
        payload[5] = payload[0];
        gyro_spiRW(0xBE, payload, 2);      // read L & H of axis 3
        payload[6] = payload[1];
        payload[7] = payload[0];
        gyro_spiRW(0xBE, payload, 2);      // read L & H of axis 4
        payload[8] = payload[1];
        payload[9] = payload[0];
        gyro_spiRW(0xBE, payload, 2);      // read L & H of axis 5
        payload[10] = payload[1];
        payload[11] = payload[0];
        gyro_spiRW(0xBE, payload, 2);      // read L & H of axis 6
        payload[12] = payload[1];
        payload[13] = payload[0];
        
        payload[0] = (byte_address & 0xFF00) >> 8;    // address
        payload[1] = (byte_address & 0x00FF);         // address
        flash_spiRW(0x84, payload, 14);      // program load random
        
        gyro_spiRW(0xBA, payload, 2);       // read fifo status
        unread = (payload[1] << 8) + payload[0];
        
        byte_address += 12;
    }
    
    // load byte number
    payload[2] = (byte_address & 0xFF00) >> 8;        // data
    payload[3] = (byte_address & 0x00FF);             // data
    payload[0] = (4096 & 0xFF00) >> 8;  // address
    payload[1] = (4096 & 0x00FF);       // address
    flash_spiRW(0x84, payload, 4);      // program load random
    printf("Bytes written: %u, ", byte_address);
    
    // load gyro timeStamp
    gyro_spiRW(0xC0, payload, 3);       // read timestamp 
    printf("TimeStamp: %2X %2X %2X, ", payload[2], payload[1], payload[0]);
    
    payload[4] = payload[0];                // data
    payload[3] = payload[1];                // data
    payload[2] = payload[2];                // data
    payload[0] = (4098 & 0xFF00) >> 8;      // address
    payload[1] = (4098 & 0x00FF);           // address
    flash_spiRW(0x84, payload, 5);          // program load random
    
    // load temperature
    gyro_spiRW(0xA0, payload, 2);       // read temperature
    printf("Temperature: %2X %2X, ", payload[1], payload[0]);
    
    payload[3] = payload[0];                // data
    payload[2] = payload[1];                // data
    payload[0] = (4101 & 0xFF00) >> 8;      // address
    payload[1] = (4101 & 0x00FF);           // address
    flash_spiRW(0x84, payload, 4);          // program load random
    
    // program excecute
    payload[0] = (page_address & 0xFF0000) >> 16;   // address
    payload[1] = (page_address & 0x00FF00) >> 8;    // address
    payload[2] = (page_address & 0x0000FF);         // address
    printf("Address: %2X %2X %2X, ", payload[0], payload[1], payload[2]);
    flash_spiRW(0x10, payload, 3);                  // program excecute
    
    __delay_ms(10);
    
    // read flash status
    payload[0] = 0xC0;              // status feature
    payload[1] = 0x00;
    flash_spiRW(0x0F, payload, 2);  // get features
    printf("Status: %2X\n",  payload[1]);
    
    return payload[1];
}

void read_flash(){
    while (flash_page_address < 0x020000){
        LED = 1;
        flash_read_page(flash_page_address);
        LED = 0;
        flash_page_address++;
        __delay_ms(200);
    }
}

void record(){
    unsigned char status = 0;
    
    while(flash_page_address < 0x020000) {
        WDTCONbits.SWDTEN = 0; // disable watchdog
        
        // writing on a new block, erasing.
        if (flash_page_address % 64 == 0) {
            flash_block_erase(flash_page_address);
        }

        // dumping data of gyro fifo
        LED = 1;
        status = dump_gyro_fifo_to_flash(flash_page_address);
        LED = 0;
        
        // flash writing status
        if (status == 0){           // OK
            flash_page_address++;
        } else {                    // faulty block ?
            flash_page_address += 64;
            flash_page_address = flash_page_address & 0xFFFFC0;
        }
        
        // watch dog enable
        WDTCONbits.SWDTEN = 1;
        SLEEP();
        NOP();
        
    }
}

void main(void) {
    init();

    for (int i = 0; i < 200; i++) {
        LED = !LED;
        __delay_ms(50);
    }

    if (!BUTTON) {
        record();
    } else {
        read_flash();
    }

    while (1) {
        LED = !LED;
         __delay_ms(100);
    }
}
