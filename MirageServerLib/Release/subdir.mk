################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../LoadInitialTargetsImages.cpp \
../Matcher.cpp 

OBJS += \
./LoadInitialTargetsImages.o \
./Matcher.o 

CPP_DEPS += \
./LoadInitialTargetsImages.d \
./Matcher.d 


# Each subdirectory must supply rules for building sources it contributes
%.o: ../%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I/usr/lib/jvm/java-1.6.0-openjdk/include -I/usr/local/include -I/usr/include/include -I/usr/include -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<" -fPIC
	@echo 'Finished building: $<'
	@echo ' '


