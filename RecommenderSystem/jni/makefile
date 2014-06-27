# Define a variable for classpath
CLASS_PATH = ../bin

JNI_FILEDIR = -I/Library/Java/JavaVirtualMachines/jdk1.8.0_05.jdk/Contents/Home/include/

# Define a virtual path for .class in the bin directory
vpath %.class $(CLASS_PATH)

all : libgraphGenerator.dylib

libgraphGenerator.dylib :
	gcc -o libgraphGenerator.dylib -shared $(JNI_FILEDIR) -I/usr/local/include/igraph -I/usr/local/include -L/usr/local/lib GraphGenerator.c -ligraph -lgsl -lc

# $* matches the target filename without the extension
RatingGenerator.h :
	javah -classpath $(CLASS_PATH) generator.RatingGenerator

clean :
	rm RatingGenerator.h graphGenerator.o graphGenerator.dylib