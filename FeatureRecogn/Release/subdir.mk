################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../FeatureExtractor.cpp 

OBJS += \
./FeatureExtractor.o 

CPP_DEPS += \
./FeatureExtractor.d 


# Each subdirectory must supply rules for building sources it contributes
%.o: ../%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -I/usr/include/c++/4.7 -I/usr/include/c++/4.7/x86_64-linux-gnu -I/usr/include/c++/4.7/backward -I/usr/lib/gcc/x86_64-linux-gnu/4.7/include -I/usr/local/include -I/usr/lib/gcc/x86_64-linux-gnu/4.7/include-fixed -I/usr/include/include -I/usr/include/x86_64-linux-gnu -I/usr/include -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


