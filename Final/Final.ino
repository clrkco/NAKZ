#include <DynamixelSerial1.h>
#define TIME_MSG_LEN 11
#define TIME_HEADER 255

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
//      Dynamixel.moveRW(14,random(200,600));
      Dynamixel.torqueStatus(PAN_SERVO,ON);
      Dynamixel.torqueStatus(TILT_SERVO,ON);
      Dynamixel.torqueStatus(RIGHT_SHOULDER,ON);
      Dynamixel.torqueStatus(LEFT_SHOULDER,ON);
      Dynamixel.torqueStatus(RIGHT_ARM,ON);
      Dynamixel.torqueStatus(LEFT_ARM,ON);
      Dynamixel.torqueStatus(RIGHT_ELBOW,ON);
      Dynamixel.torqueStatus(LEFT_ELBOW,ON);
      delay(500);
      Dynamixel.moveRW(PAN_SERVO,512);
      Dynamixel.action();
      delay(1000);
      Dynamixel.moveRW(TILT_SERVO,512);
      Dynamixel.action();
      delay(800);
      Dynamixel.move(RIGHT_ARM,512);
      Dynamixel.move(RIGHT_ELBOW,512);
      Dynamixel.move(RIGHT_SHOULDER,512);
      delay(600);
      Dynamixel.move(LEFT_ARM,512);
      Dynamixel.move(LEFT_SHOULDER,512);
      Dynamixel.move(LEFT_ELBOW,512);
      delay(600);
    }
    else if(command_arr[0] == 'y'){
      Dynamixel.moveRW(PAN_SERVO,512);
      Dynamixel.action();
      delay(500);
      Dynamixel.moveRW(TILT_SERVO,512);
      Dynamixel.action();
      delay(500);
      Dynamixel.move(RIGHT_ARM, 512);
      delay(500);
      Dynamixel.move(LEFT_ARM,200);
      Dynamixel.move(LEFT_ELBOW,230);
      Dynamixel.move(LEFT_SHOULDER,612);
      delay(800);
      Dynamixel.move(RIGHT_ARM,780);
      Dynamixel.move(RIGHT_ELBOW,770);
      Dynamixel.move(RIGHT_SHOULDER,412);
      delay(600);
    }
    else if(command_arr[0] == 'x'){
//      Dynamixel.moveRW(PAN_SERVO,512);
//      Dynamixel.action();
//      delay(500);
//      Dynamixel.moveRW(TILT_SERVO,512);
//      Dynamixel.action();
//      delay(500);
      Dynamixel.move(RIGHT_SHOULDER,412);
      Dynamixel.move(RIGHT_ARM,780);
      Dynamixel.move(RIGHT_ELBOW,770);
      delay(300);
      Dynamixel.move(LEFT_ELBOW,230);
      Dynamixel.move(LEFT_ARM,200);
      Dynamixel.move(LEFT_SHOULDER,612);
      delay(600);
    }
    else if(command_arr[0] == '~'){
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
        delay(500);
        Dynamixel.torqueStatus(RIGHT_ARM,ON);
        Dynamixel.torqueStatus(RIGHT_ELBOW,ON);
        Dynamixel.torqueStatus(RIGHT_SHOULDER,ON);
        delay(300);
        Dynamixel.moveRW(RIGHT_ARM, 710);
        Dynamixel.action();
        Dynamixel.moveRW(RIGHT_ELBOW,612);
        Dynamixel.action();
        delay(300);
        Dynamixel.moveRW(RIGHT_SHOULDER,t_position.toInt()+300);
        Dynamixel.action();
        
      } else if(command_arr[inLength]=='g'){ //gestures
        inLength=3;
        if(command_arr[inLength]=='a'){ //hello
          Dynamixel.moveSpeed(RIGHT_SHOULDER,812,500);
          Dynamixel.moveSpeed(RIGHT_ARM,512,300);
          Dynamixel.moveSpeed(RIGHT_ELBOW,512,300);
          delay(300);
          Dynamixel.moveSpeed(RIGHT_ELBOW,800,500);
          Dynamixel.moveSpeed(RIGHT_ARM,700,300);
          delay(300);
          Dynamixel.moveSpeed(RIGHT_ARM,512,300);
          Dynamixel.moveSpeed(RIGHT_ELBOW,512,300);
          delay(300);
          Dynamixel.move(RIGHT_ARM,780);
          Dynamixel.move(RIGHT_ELBOW,770);
          Dynamixel.move(RIGHT_SHOULDER,412);
          delay(800);
        } else if(command_arr[inLength]=='1'){ //welcome arms
          Dynamixel.moveSpeed(RIGHT_SHOULDER,552,300);
          Dynamixel.moveSpeed(LEFT_SHOULDER,492,300);
          delay(300);
          Dynamixel.moveSpeed(RIGHT_SHOULDER,472,200);
          Dynamixel.moveSpeed(LEFT_SHOULDER,572,200);
          delay(500);
          Dynamixel.moveSpeed(RIGHT_ARM,662,200);
          Dynamixel.moveSpeed(RIGHT_ELBOW,512,200);
          Dynamixel.moveSpeed(LEFT_ARM,362,200);
          Dynamixel.moveSpeed(LEFT_ELBOW,512,200);
          delay(700);
          Dynamixel.moveSpeed(RIGHT_ARM,780,200);
          Dynamixel.moveSpeed(RIGHT_ELBOW,770,200);
         Dynamixel.moveSpeed(LEFT_ELBOW,230,200);
          Dynamixel.moveSpeed(LEFT_ARM,200,200);
        }else if(command_arr[inLength]=='2'){ //left arm
          Dynamixel.move(LEFT_SHOULDER,532);
          delay(200);
          Dynamixel.move(LEFT_ARM,362);
          Dynamixel.move(LEFT_ELBOW,512);
          delay(500);
          Dynamixel.move(LEFT_ELBOW,230);
          Dynamixel.move(LEFT_ARM,200);
        }else if(command_arr[inLength]=='3'){ //right arm
          Dynamixel.move(RIGHT_SHOULDER,472);
          delay(300);
          Dynamixel.move(RIGHT_ARM,662);
          Dynamixel.move(RIGHT_ELBOW,512);
          delay(500);
          Dynamixel.move(RIGHT_ARM,780);
          Dynamixel.move(RIGHT_ELBOW,770);
        }else if(command_arr[inLength]=='4'){ //looking afar
           Dynamixel.moveSpeed(RIGHT_SHOULDER,412,250);
           Dynamixel.moveSpeed(RIGHT_ARM,600,250);
           delay(100);
           Dynamixel.moveSpeed(RIGHT_ELBOW,512,250);
           delay(400);
           Dynamixel.moveSpeed(RIGHT_SHOULDER,742,250);
           Dynamixel.moveSpeed(RIGHT_ARM, 692,250);
           Dynamixel.moveSpeed(RIGHT_ELBOW, 742,250);
           delay(400);
           Dynamixel.moveSpeed(RIGHT_ELBOW,740,250);
           Dynamixel.moveSpeed(RIGHT_ARM,694,250);
           delay(1000);
           Dynamixel.moveSpeed(RIGHT_ELBOW,512,250);
           delay(100);
           Dynamixel.moveSpeed(RIGHT_SHOULDER,412,250);
           delay(300);
           Dynamixel.moveSpeed(RIGHT_ARM,780,250);
           Dynamixel.moveSpeed(RIGHT_ELBOW,770,250);
        }else if(command_arr[inLength]=='5'){ //dance
          Dynamixel.moveSpeed(TILT_SERVO,280,300);
           Dynamixel.moveSpeed(PAN_SERVO,512,300);
           Dynamixel.moveSpeed(RIGHT_SHOULDER,512,300);
           Dynamixel.moveSpeed(RIGHT_ARM,512,300);
           Dynamixel.moveSpeed(RIGHT_ELBOW,800,400);
           delay(100);
           Dynamixel.moveSpeed(LEFT_SHOULDER,512,300);
           Dynamixel.moveSpeed(LEFT_ARM,512,300);
           Dynamixel.moveSpeed(LEFT_ELBOW,512,300);
           delay(600);
           Dynamixel.moveSpeed(PAN_SERVO,300,90);
           Dynamixel.moveSpeed(RIGHT_ARM,612,500);
           Dynamixel.moveSpeed(LEFT_ARM,412,500);
           delay(400);
           Dynamixel.moveSpeed(RIGHT_ARM,512,500);
           Dynamixel.moveSpeed(LEFT_ARM,512,500);
           delay(500);
           Dynamixel.moveSpeed(RIGHT_ARM,672,500);
           Dynamixel.moveSpeed(LEFT_ARM,372,500);
           delay(200);
           Dynamixel.moveSpeed(RIGHT_ARM,512,500);
           Dynamixel.moveSpeed(LEFT_ARM,512,500);
           delay(500);
           Dynamixel.moveSpeed(PAN_SERVO,700,90);
           Dynamixel.moveSpeed(RIGHT_ELBOW,512,500);
           Dynamixel.moveSpeed(LEFT_ELBOW,152,500);
           delay(300);
           Dynamixel.moveSpeed(RIGHT_ARM,612,500);
           Dynamixel.moveSpeed(LEFT_ARM,412,500);
           delay(500);
           Dynamixel.moveSpeed(RIGHT_ARM,512,500);
           Dynamixel.moveSpeed(LEFT_ARM,512,500);
           delay(400);
           Dynamixel.moveSpeed(RIGHT_ARM,672,500);
           Dynamixel.moveSpeed(LEFT_ARM,372,500);
           delay(200);
           Dynamixel.moveSpeed(RIGHT_ARM,512,500);
           Dynamixel.moveSpeed(LEFT_ARM,512,500);
           delay(500);
           Dynamixel.moveSpeed(RIGHT_SHOULDER,812,300);
           Dynamixel.moveSpeed(LEFT_SHOULDER,212,300);
           delay(300);
           //next phase
           Dynamixel.moveSpeed(PAN_SERVO,512,90);
           Dynamixel.moveSpeed(RIGHT_ARM,742,500);
           Dynamixel.moveSpeed(RIGHT_ELBOW,600,500);
           delay(300);
           Dynamixel.moveSpeed(RIGHT_ARM,512,500);
           Dynamixel.moveSpeed(RIGHT_ELBOW,800,500);
           Dynamixel.moveSpeed(LEFT_ARM,312,500);
           Dynamixel.moveSpeed(LEFT_ELBOW,400,500);
           delay(300);
           Dynamixel.moveSpeed(RIGHT_ARM,742,500);
           Dynamixel.moveSpeed(RIGHT_ELBOW,600,500);
           Dynamixel.moveSpeed(LEFT_ARM,512,500);
           Dynamixel.moveSpeed(LEFT_ELBOW,152,500);
           delay(300);
           Dynamixel.moveSpeed(RIGHT_ARM,512,500);
           Dynamixel.moveSpeed(RIGHT_ELBOW,800,500);
           Dynamixel.moveSpeed(LEFT_ARM,312,500);
           Dynamixel.moveSpeed(LEFT_ELBOW,400,500);
           delay(500);
           Dynamixel.moveSpeed(RIGHT_ARM,742,500);
           Dynamixel.moveSpeed(RIGHT_ELBOW,600,500);
           Dynamixel.moveSpeed(LEFT_ARM,512,500);
           Dynamixel.moveSpeed(LEFT_ELBOW,152,500);
           delay(300);
           Dynamixel.moveSpeed(RIGHT_ARM,512,500);
           Dynamixel.moveSpeed(RIGHT_ELBOW,800,500);
           Dynamixel.moveSpeed(LEFT_ARM,312,500);
           Dynamixel.moveSpeed(LEFT_ELBOW,400,500);
           delay(500);
           Dynamixel.moveSpeed(RIGHT_ARM,742,500);
           Dynamixel.moveSpeed(RIGHT_ELBOW,600,500);
           Dynamixel.moveSpeed(LEFT_ARM,512,500);
           Dynamixel.moveSpeed(LEFT_ELBOW,152,500);
           delay(500);
           //Y
           Dynamixel.moveSpeed(RIGHT_ARM,702,600);
           Dynamixel.moveSpeed(RIGHT_ELBOW,512,600);
           Dynamixel.moveSpeed(LEFT_ARM,342,600);
           Dynamixel.moveSpeed(LEFT_ELBOW,512,600);
           delay(1000);
           //M
           Dynamixel.moveSpeed(RIGHT_ARM,752,600);
           Dynamixel.moveSpeed(RIGHT_ELBOW,800,600);
           Dynamixel.moveSpeed(LEFT_ARM,292,600);
           Dynamixel.moveSpeed(LEFT_ELBOW,182,600);
           delay(1000);
           //C
           Dynamixel.moveSpeed(RIGHT_ARM,852,600);
           Dynamixel.moveSpeed(RIGHT_ELBOW,700,600);
           Dynamixel.moveSpeed(LEFT_ARM,712,600);
           Dynamixel.moveSpeed(LEFT_ELBOW,182,600);
           delay(300);
           //A
           Dynamixel.moveSpeed(RIGHT_ARM,852,600);
           Dynamixel.moveSpeed(RIGHT_ELBOW,600,600);
           Dynamixel.moveSpeed(LEFT_ARM,212,600);
           Dynamixel.moveSpeed(LEFT_ELBOW,382,600);
           delay(1000);
           Dynamixel.move(RIGHT_ARM,512);
           Dynamixel.move(RIGHT_ELBOW,512);
           Dynamixel.move(LEFT_ARM,512);
           Dynamixel.move(LEFT_ELBOW,512);
           delay(1000);
           Dynamixel.move(RIGHT_SHOULDER,412);
           Dynamixel.move(LEFT_SHOULDER,612);
           delay(500);
           Dynamixel.move(RIGHT_ARM,780);
           Dynamixel.move(RIGHT_ELBOW,770);
           Dynamixel.move(LEFT_ELBOW,230);
           Dynamixel.move(LEFT_ARM,200);
        }
        
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
        Dynamixel.moveSpeed(PAN_SERVO,p_position.toInt(),100);
//        Dynamixel.moveRW(PAN_SERVO,p_position.toInt());
//        Dynamixel.action();
        Dynamixel.torqueStatus(TILT_SERVO,ON);
        Dynamixel.moveSpeed(TILT_SERVO,t_position.toInt(),100);
//        Dynamixel.moveRW(TILT_SERVO,t_position.toInt());
//        Dynamixel.action();
        delay(300);
      }
    }
  }
}
