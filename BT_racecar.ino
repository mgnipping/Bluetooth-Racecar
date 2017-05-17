#include <Servo.h>
#include <SimpleTimer.h>

/*
 *BT_racecar.ino 
 *by Malin Gnipping, 2017-05-04
 *Written for Arduino Nano board 
 *Bluetooth controlled model race car with Electronic Speed Controller S8A RTR and servo,
 *light/darkness detection activating night mode for lights, light sensing using photoresistors on voltage dividers with 10KOhm pull-down
 *automatic reverse mode triggering when pulse goes below 1440 microseconds
 */

//pins
#define MPIN 10 //output signal to ESC motor speed controller
#define SPIN 9  //output signal to servo motor
#define LPINF 6 //front lights
#define LPINR 5 //rear lights

//values for auto reverse mode 
#define MICROS_TRESH_FWD 1540  //starting pulse width to trigger ESC forward mode
#define MICROS_TRESH_REV_H 1490 //high treshold pulse width to trigger ESC reverse mode
#define MICROS_TRESH_REV_L 1440 //low treshold pulse width to trigger ESC reverse mode
#define MILLIS_HOLD_TRESH_H 40  //time for keeping high treshold pulse to trigger ESC rev. mode
#define MILLIS_HOLD_TRESH_L 100 //time for keeping below low treshold pulse to trigger ESC rev. mode

//pulse width constants for motor
#define MICROS_PULSE_M_MAX 2000 
#define MICROS_PULSE_M_DEF 1500 
#define MICROS_PULSE_M_MIN 1000 

//pulse width constants for servo
#define MICROS_PULSE_S_MAX 2000 
#define MICROS_PULSE_S_DEF 1500 
#define MICROS_PULSE_S_MIN 1000 

//light/darkness detection
#define DARK 380

Servo esc;
Servo servo;
SimpleTimer timer;

byte buffer[4];

unsigned short pulse_usM = MICROS_PULSE_M_DEF ; //default motor pulse width
unsigned short pulse_usS = MICROS_PULSE_S_DEF ; //center servo pulse width
unsigned short pulse_targetM = MICROS_PULSE_M_DEF ; 
unsigned short pulse_targetS = MICROS_PULSE_S_DEF ; 

unsigned short dcon_cntr = 300;

unsigned short in_rev_mode = 0;

boolean in_night_mode = false;

unsigned short light_val1, light_val2;

void setup() {
  
  esc.attach(10);
  servo.attach(9);

  /*Motor-ESC and servo control signals*/
  pinMode(MPIN, OUTPUT);
  pinMode(SPIN, OUTPUT);

  /*Front and rear lights*/
  pinMode(LPINF, OUTPUT);
  pinMode(LPINR, OUTPUT);

  /*Bluetooth communication*/
  Serial.begin(9600);

  timer.setInterval(100, readLightSetLEDs);      //set up light/dakness detection

  esc.writeMicroseconds(MICROS_PULSE_M_DEF);
  servo.writeMicroseconds(MICROS_PULSE_S_DEF);
  
}

void loop() {

             timer.run(); 
             readIncomingData();
             
             //Start reverse mode? If not already in rev mode and pulse deliberatly set below low treshold
             if(in_rev_mode==0 && pulse_usM<MICROS_TRESH_REV_L && pulse_targetM<MICROS_TRESH_REV_L){

                //scedule a check of pulse width later, set the high treshold if still kept below low treshold
                timer.setTimeout(MILLIS_HOLD_TRESH_L, setTreshPulse);
                
              }

               updateServo(); 
               setMotorPulses();
}

void setTreshPulse(){

   if(pulse_usM < MICROS_TRESH_REV_L && pulse_targetM < MICROS_TRESH_REV_L){
   
         //pulse kept below low treshold, now set high treshold
         esc.writeMicroseconds(MICROS_TRESH_REV_H);

         //scedule when to release the value from fixed high treshold 
         timer.setTimeout(MILLIS_HOLD_TRESH_H, releaseTreshPulse); 
         in_rev_mode = 1; 
   }
   else{
         //abort reverse mode
         in_rev_mode = 0;
         pulse_usM = MICROS_PULSE_M_DEF; //set pulse back to default
    }
   
  }


void releaseTreshPulse(){
      //release theshold pulse width for reverse mode
      pulse_usM = MICROS_TRESH_REV_L; //set instantly back to low value, to skip delay before reverse acceleration begins
      in_rev_mode = 2;
  }

void readIncomingData(){
     if(!Serial.available()){
      dcon_cntr--;

      if(dcon_cntr == 0){ 
         //stop motor if disconnected
         pulse_targetM = MICROS_PULSE_M_DEF;
         pulse_targetS = MICROS_PULSE_S_DEF;
        }
    }

  else if(Serial.available()>=5){

        if(dcon_cntr ==0){  
             dcon_cntr = 300; //reconnected
        }
        while(Serial.read() != '+'){
          ;//read until start of next message
        }
       
        if(Serial.readBytes(buffer, 4) == 4){

        //read speed to temporary variable
        pulse_targetM = (buffer[0]<<8);  //high byte
        pulse_targetM |= buffer[1];      //low byte

        //ensure value is within limits
        if (pulse_targetM < MICROS_PULSE_M_MIN)
                  pulse_targetM = MICROS_PULSE_M_MIN;
        else if (pulse_targetM > MICROS_PULSE_M_MAX)
                  pulse_targetM = MICROS_PULSE_M_MAX;
             
        //read servo value
        pulse_targetS = (buffer[2]<<8);  //high byte
        pulse_targetS |= buffer[3]; //add low byte

        //ensure value is within limits
        if (pulse_targetS < MICROS_PULSE_S_MIN)
              pulse_targetS = MICROS_PULSE_S_MIN;
        else if (pulse_targetS > MICROS_PULSE_S_MAX)
              pulse_targetS = MICROS_PULSE_S_MAX; 
        }
      }
 }

void readLightSetLEDs(){
    
    //read light value from both sensors
    light_val1 = analogRead(6);
    light_val2 = analogRead(7);

    
      //set or clear night mode
      if(!in_night_mode && light_val1< DARK && light_val2< DARK){

              in_night_mode = true;
      }
      else if(in_night_mode && light_val1>DARK && light_val2>DARK){

              in_night_mode = false;
      }
       
    //front lights, turn on in night mode
    if(in_night_mode){
        digitalWrite(LPINF, HIGH);
      }
    else{
        digitalWrite(LPINF, LOW);
      }

    //rear lights, fully lit in reverse mode, dimmed in night mode
    if(in_rev_mode == 2){
        digitalWrite(LPINR, HIGH);
    }
    else if(in_night_mode && !in_rev_mode){
        analogWrite(LPINR, 150);
      }
    else{
        digitalWrite(LPINR, LOW);
      }
      
}

 void setMotorPulses(){     
            
            //if not keeping signal at fixed treshold for reverse mode triggering  
            if(in_rev_mode!= 1){
           
              if(pulse_usM != pulse_targetM){   //adjust closer to target motor pulse     
                   if (pulse_usM < pulse_targetM){
                        pulse_usM++;
                        if(in_rev_mode && pulse_targetM >= MICROS_TRESH_FWD){ //quit reverse mode
                          in_rev_mode = 0;
                          pulse_usM = MICROS_TRESH_FWD; //set pulse directly to forward mode treshold to skip delay before acceleration begins
                        }
                   }
                   else if (pulse_usM > pulse_targetM)
                        pulse_usM--; 
              
              
                   esc.writeMicroseconds(pulse_usM);  //set new pulse
                 }
              }
      }
      
  void updateServo(){

         if(pulse_usS != pulse_targetS){   
                    //adjust closer to target servo pulse
                   if (pulse_usS < pulse_targetS)
                        pulse_usS++;
                   else if (pulse_usS > pulse_targetS)
                        pulse_usS--; 
                                 
               servo.writeMicroseconds(pulse_usS); //set new pulse              
              }
    }


