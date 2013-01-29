TARGET = myagent
CC = gcc

# linux config
JDK = /usr/local/share/jdk1.6.0_20
CFLAGS=-Wall -I$(IDIR) -shared -DLINUX -I$(JDK)/include/linux -I$(JDK)/include


COMPILE = $(CC) $(CFLAGS) -c
OBJDIR = obj
SOURCES := $(wildcard *.c)
DEPS := $(wildcard *.h)
OBJFILES := $(addprefix $(OBJDIR)/, $(patsubst %.c, %.o, $(SOURCES)))

exe: $(OBJFILES)
    $(CC) $(OBJFILES) -shared -o $(TARGET)

$(OBJDIR)/%.o : %.c $(DEPS)
$(COMPILE) -o $@ $<