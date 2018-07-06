## CyclosThePostureCoach
Preventing Low Back Pain among cyclists using the Autoregressive Model
# Inspiration
As an endurance road cyclist, I, myself, suffer low back pain on rides that last more than a 100 miles. I realized apart from buying the correct bike, interestingly researchers show that muscle fatigue among cyclists worsens their spine posture. An inappropriate spine posture could lead to a Chronic Low Back Pain or a Herniated Fibrous Disc causing pain. Well, numbers speak for themselves; of the 85% of cyclists who report injury every year, 30.3% require medical treatment for low back pain (The American Journal of Sports Medicine & The International Journal of Sports Medicine)
# What it does
Cyclos tracks the cyclist's pelvic flexion in real-time using motion sensors placed on the first vertebrae of the lumbar (L1). The sensor values were correlated to the pelvic flexion on a 3D Motion Capture System. An Autoregressive Model (AR) combined with the least square estimate of order 20 is used to eliminate white noise due to the terrain. The model uses the same sensor input to analyze the parameters and variance that are re-calibrated with a moving average to generate a personalized threshold, to warn the cyclist and avoid false alarms
![summary](https://user-images.githubusercontent.com/40699541/42361981-d0a67918-810e-11e8-8ae7-c07ec6ddb1f3.png)
# How we built it
To build the hardware, I used a triaxial Accelerometer, a triaxial Gyroscope and a magnetometer which wirelessly connected to a smartphone via Bluetooth to store and compute the data. The raw sensor values processed it with an ATmega328 microcontroller which ran on a 3.7v 1000mAh Lithium ion Polymer battery. The sensors collect data at a frequency approximately of 6Hz. 
