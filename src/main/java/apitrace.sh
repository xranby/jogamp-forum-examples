export LD_LIBRARY_PATH=/home/pi/apitrace/wrappers:$LD_LIBRARY_PATH
export TRACE_LIBGL=/usr/lib/arm-linux-gnueabihf/libGL.so.1
/home/pi/apitrace/apitrace trace -v java -cp jogamp-fat.jar:. xranby/com.gudinna.JogAmpGraphAPINurbsDemo
