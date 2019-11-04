#include <DynamixelSerial1.h>
#define BUFFERSIZE 127
#define PAN_SERVO 15
#define TILT_SERVO 14
#define RIGHT_SHOULDER 20
#define LEFT_SHOULDER 21
#define RIGHT_ARM 22
#define LEFT_ARM 16
#define RIGHT_ELBOW 11
#define LEFT_ELBOW 9
uint8_t inBuffer[BUFFERSIZE];
int inLength;

String p_position,t_position;

void setup() {
  Serial.begin(9600);
  Serial2.begin(9600);
  Dynamixel.begin(1000000,2); 
  Dynamixel.torqueStatus(PAN_SERVO,ON);
  Dynamixel.torqueStatus(TILT_SERVO,ON);
  Dynamixel.torqueStatus(RIGHT_SHOULDER,ON);
  Dynamixel.torqueStatus(LEFT_SHOULDER,ON);
  Dynamixel.torqueStatus(RIGHT_ARM,ON);
  Dynamixel.torqueStatus(LEFT_ARM,ON);
  Dynamixel.torqueStatus(RIGHT_ELBOW,ON);
  Dynamixel.torqueStatus(LEFT_ELBOW,ON);
  Dynamixel.setEndless(PAN_SERVO,OFF);
  Dynamixel.setEndless(TILT_SERVO,OFF);
  Dynamixel.setEndless(RIGHT_SHOULDER,OFF);
  Dynamixel.setEndless(LEFT_SHOULDER,OFF);
  Dynamixel.setEndless(RIGHT_ARM,OFF);
  Dynamixel.setEndless(LEFT_ARM,OFF);
  Dynamixel.setEndless(RIGHT_ELBOW,OFF);
  Dynamixel.setEndless(LEFT_ELBOW,OFF);
  delay(1000);
}

void loop() {
  // put your main code here, to run repeatedly:
  if(Serial2.available()){
    //proceed to parse into string
    String command = Serial2.readStringUntil('!');
    char command_arr[command.length()];
    command.toCharArray(command_arr, command.length());
    Serial.println(command);
    if(command_arr[0] == 'z'){  //init
      Dynamixel.moveRW(PAN_SERVO,512);
      Dynamixel.action();
      Dynamixel.moveRW(TILT_SERVO,512);
      Dynamixel.action();
      Dynamixel.moveRW(RIGHT_SHOULDER,512);
      Dynamixel.action();
      delay(300);
      Dynamixel.moveRW(RIGHT_ARM,512);
      Dynamixel.action();
      Dynamixel.moveRW(RIGHT_ELBOW,512);
      Dynamixel.action();
      Dynamixel.moveRW(LEFT_SHOULDER,512);
      Dynamixel.action();
      delay(300);
      Dynamixel.moveRW(LEFT_ARM,512);
      Dynamixel.action();
      Dynamixel.moveRW(LEFT_ELBOW,512);
      Dynamixel.action();
    }
    if(command_arr[0] == '~'){
      inLength = 1;
      if(command_arr[inLength]=='p'){ //pan-tilt
        inLength=3;
        p_position = "";
        t_position = "";
        while(command_arr[inLength] != '_'){
          p_position.concat(command_arr[inLength]);
          inLength++;
        }
        inLength++;
        while(command_arr[inLength] != '_'){
          t_position.concat(command_arr[inLength]);
          inLength++;
        }
        Dynamixel.torqueStatus(PAN_SERVO,ON);
        Dynamixel.moveRW(PAN_SERVO,p_position.toInt());
        Dynamixel.action();
        delay(1000);
        Dynamixel.torqueStatus(TILT_SERVO,ON);
        Dynamixel.moveRW(TILT_SERVO,t_position.toInt());
        Dynamixel.action(); 
        delay(300);
        Dynamixel.torqueStatus(RIGHT_SHOULDER,ON);
        Dynamixel.torqueStatus(RIGHT_ARM,ON);
        Dynamixel.torqueStatus(RIGHT_ELBOW,ON);
        Dynamixel.moveRW(RIGHT_ARM, 710);
        Dynamixel.action();
        Dynamixel.moveRW(RIGHT_ELBOW,612);
        Dynamixel.action();
        Dynamixel.moveRW(RIGHT_SHOULDER,t_position.toInt()+300);
        Dynamixel.action();
        
      } else if(command_arr[inLength]=='g'){ //gestures
        inLength=3;
        
      } else if(command_arr[inLength]=='d'){
        inLength=3;
        p_position = "";
        t_position = "";
        while(command_arr[inLength] != '_'){
          p_position.concat(command_arr[inLength]);
          inLength++;
        }
        inLength++;
        while(command_arr[inLength] != '_'){
          t_position.concat(command_arr[inLength]);
          inLength++;
        }
        Dynamixel.torqueStatus(PAN_SERVO,ON);
        Dynamixel.moveRW(PAN_SERVO,p_position.toInt());
        Dynamixel.action();
        Dynamixel.torqueStatus(TILT_SERVO,ON);
        Dynamixel.moveRW(TILT_SERVO,t_position.toInt());
        Dynamixel.action();
        delay(300);
      }
    }
  }
}
