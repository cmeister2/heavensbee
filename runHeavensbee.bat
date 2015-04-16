:loop
if exist e:\stoploop.txt ( goto :end )

java -XX:+UseG1GC -Xms4g -Xmx10g -jar heavensbee.jar 9147 HeavensbeeMCTSMPPropnet

goto loop

:end