// footballrobot.pde - Footballrobot for cellphone control 
// (c) Kimmo Karvinen & Tero Karvinen http://BotBook.com

#include <Servo.h>

// Keep track of how far along we are reading a message
const int READY = 1;                // Ready to receive a message
const int RECEIVED_START = 2;       // Received the start character: 'S'
const int RECEIVED_LEFT_SPEED = 3;  // Received the left speed
const int RECEIVED_RIGHT_SPEED = 4; // Received the right speed
const int RECEIVED_KICK = 5;        // Received the kick indicator

int state = READY;

// Define the pins and declare the servo objects.
int servoRightPin=2;
int servoLeftPin=3;
int servoKickPin=4;

Servo kickerServo;
Servo servoRight;
Servo servoLeft;

// Various positions and settings for the kicker.
int kickerNeutral = 130;
int kickerKick    = 10;
long kickerWait   = 750;

// Limit our speed; this needs to be a value between 0 and 90
int maxSpeed = 25;

// Current speeds/kicker setting
int kickNow=0; 
int leftSpeed  = 90;
int rightSpeed = 90;

// Temporary speed variables used while we are processing a command.
int newLeftSpeed;
int newRightSpeed;

int ledPin=13;  // LED output pin

void kick()
{
  kickerServo.write(kickerKick);
  delay(kickerWait);
  kickerServo.write(kickerNeutral);
  //Serial.println("Kicking!");
}

void move()
{
  servoLeft.write(leftSpeed);
  servoRight.write(rightSpeed);
}

void stopMoving()
{
  leftSpeed = 90;
  rightSpeed = 90;
}

void setup()
{

//  pinMode(rxPin, INPUT);
//  pinMode(txPin, OUTPUT);
//  mySerial.begin(1200);


  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, HIGH);

  servoRight.attach(servoRightPin);
  servoLeft.attach(servoLeftPin);

  kickerServo.attach(servoKickPin);
  kickerServo.write(kickerNeutral);

  stopMoving();

  Serial.begin(57600);
  digitalWrite(ledPin, LOW);
}

void loop()
{

  if (Serial.available()) {

    int ch = Serial.read(); // Read a character
    
    switch (state) {
    case READY:
      if ('S' == ch) {
        state = RECEIVED_START; // We'll be in this state 
                                // next time through loop()
      } 
      else if ('?' == ch) {
        Serial.print("L"); // Let the phone know we are listening
      }
      break;
      
    case RECEIVED_START:
      if (ch >= 0 && ch <= 10) { // Make sure the values are in range
        state = RECEIVED_LEFT_SPEED;
        
        // Set the temporary left speed value
        newLeftSpeed = map(int(ch), 0, 10, 90-maxSpeed, 90+maxSpeed); 
      } 
      else { // Invalid input--go back to the ready state
        state = READY;
      }
      break;
      
    case RECEIVED_LEFT_SPEED:
      if (ch >= 0 && ch <= 10) { // Make sure the values are in range
        state = RECEIVED_RIGHT_SPEED;

        // Set the temporary right speed value
        newRightSpeed = 180 - map(int(ch), 0, 10, 90-maxSpeed, 90+maxSpeed); 
      } 
      else { // Invalid input--go back to the ready state
        state = READY;
      }
      break;
      
    case RECEIVED_RIGHT_SPEED:
      if ('k' == ch) { // 'k' for kick
        kickNow = 1;
      } 
      else { // anything else means don't kick
        kickNow = 0;
      }
      state = RECEIVED_KICK;
      break;

    case RECEIVED_KICK:
      if ('U' == ch) { // Reached the end of the message
      
        leftSpeed = newLeftSpeed;   // Set the speeds
        rightSpeed = newRightSpeed;
        
        if (kickNow) { // Are we supposed to kick now?
          kick();
        } 
        
        // Return to the ready state, clear the kick flag
        state = READY;
        kickNow = 0;
        break;
      }
    }
  }
  
  if (state == READY) {
    move();
  }
  delay(10); // Give the microcontroller a brief rest
}
