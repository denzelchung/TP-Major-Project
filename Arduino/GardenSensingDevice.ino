#include <SPI.h>//wifishield
#include <WiFi.h> //wifishield
#include <DHT.h>//dht11 sensor
#include <TimerOne.h>//interrupt
#include <Servo.h>//servo motor
//#include <Wire.h>//rfid
//#include <Adafruit_NFCShield_I2C.h>//rfid

#define IRQ (2)
//DHT DEFINE
#define DHTPIN A5 //gray color
#define DHTTYPE DHT11

DHT dht(DHTPIN, DHTTYPE);
Servo servoMain;

int pos = 0;//servor motor position
int redLED    = 46;//blue color
int yellowLED = 58;//purple color
int greenLED  = 50; //gray color
int sensorValue;
int newSensorValue;
int temperature;
int newTemperatureValue;
int humidity;
int newHumidityValue;
int heatIndex;
int newHeatIndex;
int f;
boolean updateSensor;
boolean updateTemperature;
boolean updateHumidity;
boolean updateHeatIndex;
volatile int speaker = 45;
int tempo=350;
int count=0;


int fan            =  A13 ;// blue color if digital test 10 a4
int bulb           =  A12;// green color 9 a3
int photoresistor  =  A11;//yellow color 8 a2
int motionsensor   =  A9;//black color 7 a0
int lm35           =  A10; // brown color 6 a1
float temp;




char ssid[] = "DENZEL";      //  your network SSID (name) 
char pass[] = "denzelchung";   // your network password                                     

int status = WL_IDLE_STATUS;

//char server[] = "192.168.1.103";
//char server[] = "72.14.204.104";
//byte server[] = {72, 14, 204, 104};
//char server[] = "192.168.1.103";
char server[] = "www.google.com";


int interval = 1000;//wait between dumps
WiFiClient client;
boolean notConnected;

void setup() {

  //Serial SETUP  
  Serial.begin(9600);  //Serial Baud rate
    pinMode(redLED        ,     OUTPUT );
    pinMode(yellowLED     ,     OUTPUT );
    pinMode(greenLED      ,     OUTPUT );
    pinMode( motionsensor, INPUT);
    pinMode(photoresistor, INPUT);
    pinMode( lm35, INPUT);
    pinMode( fan   , OUTPUT);
    pinMode(bulb, OUTPUT);

  //Adafruit DHT11/22/21 setup
  dht.begin();
  
  sensorValue = analogRead(8); //read from analog 3 moisture sensor
  humidity = dht.readHumidity();
  temperature = dht.readTemperature();
  f = dht.readTemperature(true);//read temperature as Fahrenheit
  heatIndex = dht.computeHeatIndex(f, humidity);//compute HeatIndex value from library file
  //Serial.println(sensorValue);
  //delay(5000);
  updateSensor = false;
  updateTemperature = false;
  updateHumidity = false;
  updateHeatIndex = false;

  //WiFi.begin
//  if (WiFi.status() == WL_NO_SHIELD){
//      
//      Serial.println("WiFi Shield not present");
//      while(false);  //don't continue
//  
//  }
//    Serial.print(F("Firmware Version:"));
//    Serial.println(WiFi.firmwareVersion());
//    
//      if ( status != WL_CONNECTED) { 
//      Serial.print("Attempting to connect to Network named: ");
//      Serial.println(ssid);                   // print the network name (SSID);
//  
//      // Connect to WPA/WPA2 network. Change this line if using open or WEP network:    
//       status = WiFi.begin(ssid, pass);
//       // wait 10 seconds for connection:
//       delay(10000);
//      notConnected = true;
//    }

  //Timer1
  Timer1.initialize(500000);         // initialize timer1, and set a 8.3 second period
  Timer1.pwm(9, 512);                // setup pwm on pin 9, 50% duty cycle
  Timer1.attachInterrupt(callback);  // attaches callback() as a timer overflow interrupt
 

    servoMain.attach(9);
    servoMain.write(90);  // set servo to mid-point

}

void loop() {

  //check if any value is unreadable returns it.
  //isnan returns 1 if not a number
  if(isnan(humidity) || isnan(temperature) || isnan(f)){
    Serial.println("Failed to read from DHT sensor!");
    Serial.print("Humidity: ");
    Serial.print(humidity);
    Serial.print(" %\t");
    Serial.print("Temperature: ");
    Serial.print(temperature);
    Serial.print(" *C \t");
    Serial.print("Heat Index: ");
    Serial.print(heatIndex);
    Serial.println(" *F");
    return;
  }

  newSensorValue = analogRead(8); //read from analog 3 moisture sensor
  newHumidityValue = dht.readHumidity();
  newTemperatureValue = dht.readTemperature();
  f = dht.readTemperature(true);//read temperature as Fahrenheit
  newHeatIndex = dht.computeHeatIndex(f, newHumidityValue);//compute HeatIndex value from library file
  Serial.println(sensorValue);


  
  
  if(sensorValue<350)
    {
      digitalWrite(redLED    , HIGH );
      digitalWrite(yellowLED , LOW  );
      digitalWrite(greenLED  , LOW  );
    }
    else if(sensorValue>350||sensorValue<500)
    {
      digitalWrite(redLED    , LOW  );
      digitalWrite(yellowLED , HIGH );
      digitalWrite(greenLED  , LOW  );
    }
    else if (sensorValue>750)
    {
      digitalWrite(redLED    , LOW );
      digitalWrite(yellowLED , LOW );
      digitalWrite(greenLED  , HIGH);
    }
  else
  {
      digitalWrite(redLED    , HIGH );
      digitalWrite(yellowLED , HIGH );
      digitalWrite(greenLED  , HIGH);
  }

//motion sensor   
    if(digitalRead(motionsensor==HIGH ))//&&temperature>25) ROOM TEMPERATURE 22 DEGREE put t>23
    {   digitalWrite(fan, digitalRead(motionsensor));
   servoMain.write(45);  // Turn Servo Left to 45 degrees
   delay(1000);          // Wait 1 second
   servoMain.write(0);   // Turn Servo Left to 0 degrees
   delay(1000);          // Wait 1 second
   
//    Serial.println("fan");
//    Serial.println(digitalRead(motionsensor));
//BULB
 digitalWrite(bulb, !digitalRead(photoresistor)); 
 
 //LM35 check here
   temp = analogRead(lm35);           //read the value from the sensor
//   Serial.println("");
  

    temp = (5.0*temp*100.0)/2176;
  temp = temp / 10;
temperature = (temp+temperature)/2;
//    Serial.print("TEMPRATURE ");
//    Serial.print(temp);
//    Serial.print("*C");




}
  
  
  
}// end of void loop


void callback(){//interrupt Timer 1
  Serial.println("h");
  //Serial.println("Interrupt Timer 1 is working");
  // Send data every hour
  if(count==1){     //change to 720 demo change to 12
    //Serial.println("");
//    Serial.println("Sending Data");
    sendData();
    count=0;
  }//end of count==12
  
  // Read data every minute
  if (count%12) {
    Serial.println("Read Data");
    readData();
  }
  count=count+1;

}

void sendData(){
  if(client.connect(server, 80)){
    Serial.println("->Connected");
    client.print("GET /tree/arduino/MacPherson");
    client.print("/");
    client.print(sensorValue);
    client.print("/");
    client.print(temperature);
    client.print("/");
    client.print(humidity);
    client.print("/");
    client.print(heatIndex);
    client.println(" HTTP/1.1");
    client.println("Host: cenplusplus.appspot.com");
    client.println();
    notConnected = false;
    Serial.println(client.available());
  }
else {
//    Serial.println("Connection failed");
  }

//  while ( client.available()){
//    char c = client.read();
//    Serial.write(c);
//  }
//  
//  if(!client.connected()){
//    Serial.println();
//    Serial.println("Disconnected from Sever");
//    client.stop();
//    while(true); 
//  }
}


void readData() {



  
//  Serial.println(newSensorValue);
//    Serial.println(newHumidityValue);
//      Serial.println(newTemperatureValue);
//      Serial.println(sensorValue);
  if ((abs(sensorValue - newSensorValue) > 120) ||
      (abs(humidity - newHumidityValue) > 50) ||
      (abs(temperature - newTemperatureValue) > 10) ||
      (abs(heatIndex - newHeatIndex) > 25)) {
    sendData();
  }
  
  if (abs(sensorValue - newSensorValue) > 30) {  
    updateSensor = true;
    sensorValue=newSensorValue;
    
//    Serial.println(sensorValue);
  }
  
  if (abs(humidity - newHumidityValue)>5) {
    updateHumidity = true;
    humidity = newHumidityValue;
//    Serial.println(humidity);
  }
  if (abs(temperature - newTemperatureValue)>3) {
    updateTemperature = true;
    temperature = newTemperatureValue;
  //  Serial.println(temperature);
  }
  if (abs(heatIndex - newHeatIndex) > 3) {
    updateHeatIndex = true;
    heatIndex = newHeatIndex;
  }

}


