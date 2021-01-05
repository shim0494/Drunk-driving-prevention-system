import RPi.GPIO as GPIO
import spidev
import time
import socket

spi=spidev.SpiDev()
spi.open(0,0)
spi.max_speed_hz=500000

pin=18

GPIO.setwarnings(False)

light_on=False
 
GPIO.setmode(GPIO.BCM)                          #gpio 모드 셋팅
GPIO.setup(pin,GPIO.OUT)                        #모터동작
GPIO.setup(12,GPIO.IN,pull_up_down=GPIO.PUD_UP) #버튼 입력
p=GPIO.PWM(pin,50)                              #펄스폭 변조 핀,주파수
p.start(0)

def read_spi_adc(adcChannel):
    adcValue=0
    buff=spi.xfer2([1,(8+adcChannel)<<4,0])
    adcValue=((buff[1]&3)<<8)+buff[2]
    return adcValue

def valueTomL(adcValue):
    R2=2000
    adc_volt=(adcValue)/1024*5.0
    gas=((5.0*R2)/adc_volt)-R2
    R0 = 16000
    ratio = gas/R0
    x = 0.4*ratio
    BAC= x**(-1.431)
    return BAC

HOST = ""
PORT = 8888
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
print ('Socket created')
s.bind((HOST, PORT))
print ('Socket bind complete')
s.listen(1)
print ('Socket now listening')

#파이 컨트롤 함수
def do_some_stuffs_with_input(input_string):
   """#라즈베리파이를 컨트롤할 명령어 설정
   if input_string == "on":
      GPIO.output(led_pin,1)
      print("LED ON!")
      #파이 동작 명령 추가할것
   elif input_string == "off":
      GPIO.output(led_pin,0)
      print("LED OFF!")
   elif input_string == "single":
      input_string = "사진을 찍습니다."
   else :
      input_string = input_string + " 없는 명령어 입니다."
      """
   input_string = BAC
   return input_string

try:
    a=True                                  #초기화
    i = 0
    SUM = 0
    AVER = 0
    #알콜센서가 감지
    while True:
        max_time_end = time.time() + 5
        #접속 승인
        conn, addr = s.accept()
        print("Connected by ", addr)
        while True:
            adcChannel=0
            adcValue=read_spi_adc(adcChannel)
            BAC = valueTomL(adcValue)
            print("%.6f"%(BAC*0.0001))
            
            i += 1
            SUM += BAC*0.0001
            AVER = SUM/i
        
            #수신한 데이터로 파이를 컨트롤 
            res = "%.4f"%(AVER)
            print("파이 동작 :" + res)
            #클라이언트에게 답을 보냄

            if time.time() > max_time_end:
                break
        conn.sendall(res.encode("utf-8"))
        time.sleep(2.5)
        print("aver: %.4f"%(AVER))
        max_time_end2 = time.time() + 5
        while True:
            if AVER <= 0.03:
                button_state=GPIO.input(12)
                if button_state==False:
                    a=False if a else True
                    print("시동")
                if a:
                    p.ChangeDutyCycle(9.5)
                else:
                    p.ChangeDutyCycle(2.5)
                time.sleep(0.2)
            else:
                print("음주운전")
            if time.time() > max_time_end2:
                break
        #연결 닫기
        conn.close()

except KeyboardInterrupt:
    spi.close()
    p.stop()