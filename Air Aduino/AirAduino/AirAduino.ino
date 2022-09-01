//YWROBOT
//Compatible with the Arduino IDE 1.0
//Library version:1.1
#include <SoftwareSerial.h> //시리얼통신 라이브러리 호출
#include <LiquidCrystal_I2C.h> //LCD화면 처리 라이브러리
#include <Timer.h> // 타이머 라이브러리
#include "TinyDHT.h" //DHT(온, 습도)센서 라이브러리
#define DHTPIN 8
#define DHTTYPE DHT22 //DHT타입 (DHT11, DHT22)등이 있다.

//GP2Y1010AU0F모듈은 내부의 적외선 LED로 미세먼지양을 측정한다. 정확도는 GP2Y1014AU0F보다 떨어지지만, 미세먼지(PM10), 초미세먼지(PM2.5)를 따로 측정가능하다.
int measurePin = 0; //Connect dust sensor to Arduino A0 pin (A0)핀(아날로그)을 미세먼지 측정으로 사용함
int ledPower = 2;   //Connect 3 led driver pins of dust sensor to Arduino D2 (D2)핀 (디지털)을 미세먼지의 내부 센서 의 led를 연결하는데 사용함 
int blueTx=3;   //Tx (보내는핀 설정)at
int blueRx=4;   //Rx (받는핀 설정)
#define FanPin 9 //팬 출력 핀 9번핀 사용
SoftwareSerial bluetooth(blueTx, blueRx); //블루투스 대부분 (Rx, Tx)로 하고 교차로 연결하나, (Tx, Rx)로 블루투스 함수 선언하고, 그대로 연결하는 것이 편함 
Timer Ts; // 타이머 변수
int samplingTime = 280; //미세먼지 모듈 측정된것 샘플링 하는 시간
int deltaTime = 40; //시간차
unsigned long duration;   //지속 시간
unsigned long sampleDusttime_ms = 5000;   //먼지 샘플시간 5초 마다 업데이트
unsigned long sampleTemtime_ms = 5000;   //온도 샘플시간 10초 마다 업데이트
unsigned long lowpulseoccupancy = 0;   //Low 신호가 지속된 시간을 초기화

float voMeasured = 0; //미세먼지 모듈의 측정값 (전압)
float calcVoltage = 0; //아두이노에서 실질적으로 읽어 들이는 전압 값
int dustDensity = 0; //먼지 밀도
float dustDensityAvg = 0; // 먼지센서값 평균값을 구하기 위한 임시 저장 공간


LiquidCrystal_I2C lcd(0x27,20,4);  // set the LCD address to 0x27 for a 16 chars and 2 line display
DHT dht(DHTPIN, DHTTYPE); //DHT 초기화 

void setup()
{
  Serial.begin(9600); //시리얼 통신 (시리얼 값)출력 디버깅 용도로 사용
  bluetooth.begin(9600); //블루투스 시리얼 //9600은 아두이노에서 기본적으로 사용하는 보드레이트임
  dht.begin(); //dht측정 시작
  pinMode(ledPower,OUTPUT); //미세먼지 값을 읽게해 줄 LED 빛을 출력으로 지정함
  pinMode(FanPin, OUTPUT); //팬을 출력으로 함
  lcd.init();                      // initialize the lcd 
  // Print a message to the LCD.
  lcd.backlight(); //LCD 백라이트 밝게 조절
    lcd.clear();  //LCD 초기화
  lcd.print("Aduino DUST");    
  lcd.setCursor(0, 1);  //두 번째 줄로 커서 이동
  lcd.print("AND TEMPERTURE");
  lcd.setCursor(0, 2); //세 번째 줄로 커서 이동
  lcd.print("AND HUMINITY");
  delay(5000);  // 5초 대기
    if (dustDensity == 0) {    //만약 결과값이 0보다 작으면 아래를 LCD에 출력한다.
    lcd.clear();
    lcd.print("Analysing Data");
    lcd.setCursor(0, 1);
    lcd.print("................");
    lowpulseoccupancy = 0;
  }
  Ts.every(sampleDusttime_ms, doDust); //미세먼지 측정
  Ts.every(sampleTemtime_ms, doDHT_22); //온, 습도 측정
}

void doDust() {
  for (int i = 0; i < 5; i++) {
    duration = pulseIn(measurePin, LOW); //먼지센서의 LOW 지속시간 읽어오기 
    digitalWrite(ledPower,LOW); // LED파워를 킴
  delayMicroseconds(samplingTime);
  voMeasured = analogRead(measurePin); // read the dust value
    delayMicroseconds(deltaTime);
    digitalWrite(ledPower,HIGH); // turn the LED off
    lowpulseoccupancy = lowpulseoccupancy+duration;
      // 0 - 5V mapped to 0 - 1023 integer values
  // recover voltage
  calcVoltage = voMeasured * (5.0 / 1024.0);

  // linear eqaution taken from http://www.howmuchsnow.com/arduino/airquality/
  // Chris Nafis (c) 2012
  dustDensity = (0.17*calcVoltage-0.1)*1000;

    dustDensityAvg += dustDensity;
   }
   dustDensity = dustDensityAvg / 5; //5초동안 읽어들인 미세먼지 평균값을 미세먼지 측정값으로 출력함
   
  if (dustDensity > 0.01 ) {   // 미세먼지를 측정했으면 출력하라
      String dust = String(dustDensity);
      dust.concat("#"); //concat를 통해 "dust#" ex)110# 으로 만들어, #을 제거하고, 110을 읽게 구분시켜준다.
      bluetooth.println(dust); //블루투스를 통해 어플로 dust값을 전달함
      lcd.clear(); //화면을 클리어함
      lcd.print("Dust:"); 
      lcd.print(dustDensity);
      lcd.print("ug/m3");
      lowpulseoccupancy = 0;
      dustDensityAvg = 0;
    }
  
    if (dustDensity > 0.01 && dustDensity <= 30) {   //만약 미세먼지 값이 0.01 보다 크고 30이랑 같거나 작으면 아래를 출력 (좋음)
      lcd.setCursor(0, 1);
      lcd.print("Good! ^v^");
      DHT22_LCD(); //샘플링 속도가 미세먼지 센서가 온습도 센서 대비 빠르기 때문에 여기다 LCD 화면 업데이트 처리해서 화면 업데이트로 인한 공백 최소화
      analogWrite(9, 0); //팬 돌아가는 속도 (0~255) (0)이므로, OFF
    }
    else if (dustDensity > 30 && dustDensity <= 80) {  //만약 미세먼지 값이 30보다 크고 80이랑 같거나 작으면 아래를 출력 (보통)
      lcd.setCursor(0, 1);
      lcd.print("SoSo! '.';");
      DHT22_LCD();
      analogWrite(9, 150); 
    }
    else if (dustDensity > 80 && dustDensity <= 150) {  //만약 미세먼지 값이 80보다 크고 150이랑 같거나 작으면 아래를 출력 (나쁨)
      lcd.setCursor(0, 1);
      lcd.print("Bad! T.T");
      DHT22_LCD();
      analogWrite(9, 200);
    }
    else if (dustDensity > 150) {  //만약 미세먼지 값이 150 보다 크면 아래를 출력 (매우나쁨)
      lcd.setCursor(0, 1);
      lcd.print("Be Careful @.@");
      DHT22_LCD();
      analogWrite(9, 250);
    }
}

void DHT22_LCD() { //온, 습도 화면 업데이트 함수
  lcd.setCursor(0, 2);
  lcd.print("Humi: ");
  lcd.setCursor(0, 3);
  lcd.print("Temp: ");
}

void doDHT_22() { //온, 습도 측정 함수
      int8_t h = dht.readHumidity(); //습도 읽어들임
      int16_t t = dht.readTemperature(); //온도 읽어들임
      lcd.setCursor(6, 2); //커서를 통해 알맞는 위치에 온, 습도 표시
      lcd.print(h);
      lcd.print(" %");
      lcd.setCursor(6, 3);
      lcd.print(t);
      lcd.write(223); //°를 표시함
      lcd.print("C"); 
      
      
      String humistr = String(h);
      humistr.concat("%"); //concat를 통해 "humistr" ex)45% 으로 만들어, %을 제거하고, 45을 읽게 구분시켜준다.

      String tempstr = String(t);
      tempstr.concat("!"); //concat를 통해 "tempstr" ex)28! 으로 만들어, !을 제거하고, 28을 읽게 구분시켜준다.

      bluetooth.print(tempstr); //블루투스를 통해 어플로 tempstr값을 전달함
      delay(100);
      bluetooth.print(humistr); //블루투스를 통해 어플로 humistr값을 전달함
      Serial.print("temp"); //디버깅용도로 시리얼 값 출력
      Serial.println(tempstr);
      Serial.print("humi");
      Serial.println(humistr);
}

void loop() {
  // Reading temperature or humidity takes about 250 milliseconds!
  // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
  Ts.update(); // dustTs변수에 지정했던 every함수에서 설정했던 값을 실행
  bluetoothConnectView(); // 안드로이드와 아두이노가 연결 성공했을 때 먼지농도, 온도 값을 넘겨줌
  FanAction(); //팬에 동작할 수 있게함
  
}
 

void bluetoothConnectView() { //아두이노 블루투스 전송
  String dust = String(dustDensity);
  dust.concat("#"); //concat를 통해 토크나이저로 끊어 읽기 가능
  bluetooth.println(dust);
  
  int8_t h = dht.readHumidity();
  int16_t t = dht.readTemperature();

  String humistr = String(h);
  humistr.concat("%");
  bluetooth.print(humistr);
  String tempstr = String(t);
  tempstr.concat("!");
  bluetooth.print(tempstr);
}


void FanAction(){
 
  String strInput ="";

  while(bluetooth.available() > 0){ //블루투스로 값을 읽어들이면
    strInput += (char) bluetooth.read(); //char형으로 블루투스 값 일어 들임
    Serial.print((char) bluetooth.read());
    delay(50);
  }

  if( strInput !=""){
    int intVal = strInput.toInt(); //char형으로 읽어들인 값을 int형으로 변환
    analogWrite(FanPin, intVal); //int값을 FanPin에 전송
    Serial.println(strInput); //값이 보내졌는지 시리얼로 확인
    delay(50);
  }
}
