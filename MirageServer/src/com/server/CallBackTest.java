package com.server;

import java.util.concurrent.*;

public class CallBackTest {
	private static int NUM_OF_TASKS = 1;
	Object result;
	int cnt = 0;
	long begTest, endTest;
	int targets = 0;
	ExecutorService es;

	public CallBackTest() {
		begTest = new java.util.Date().getTime();
		targets = Matcher.fetch();
		
			
		run();
		
	}
	
	public static long fib(int n) {
        if (n <= 1) return n;
        else return fib(n-1) + fib(n-2);
    }	

	public void callBack(Object result) {
		// System.out.println("result "+result);
		this.result = result;

		int sizeResult = (int) result;
		if (sizeResult > 0) {
			es.shutdown();
		}
		if (++cnt == NUM_OF_TASKS) {
			Double secs = new Double((new java.util.Date().getTime() - begTest) * 0.001);
			System.out.println("run time " + secs + " secs");
			System.exit(0);
		}
	}

	public void run() {
		int nrOfProcessors = Runtime.getRuntime().availableProcessors();
		es = Executors.newFixedThreadPool(nrOfProcessors);

		int targetsPerCore = (int) targets / nrOfProcessors;
		int extLastCore = 0;

		if ((targetsPerCore * nrOfProcessors) != targets) {
			extLastCore = targets - (targetsPerCore * nrOfProcessors);
		}

		NUM_OF_TASKS = nrOfProcessors;

		int beginThisCore = 0;
		int endThisCore = beginThisCore + targetsPerCore - 1;

		for (int i = 0; i < NUM_OF_TASKS; i++) {

			if (i == (NUM_OF_TASKS - 1)) {
				extLastCore = endThisCore - targets;
				endThisCore -= extLastCore + 1;
			}
			System.out.println("CORE "+(i+1));
			System.out.println("BEGIN: "+beginThisCore+"   END: "+endThisCore);

//			CallBackTask task = new CallBackTask(i, beginThisCore, endThisCore);
			//task.setCaller(this);
//			es.submit(task);

			beginThisCore = endThisCore + 1;
			endThisCore = beginThisCore + targetsPerCore;

		}
	}

	public static void main(String[] args) {
		new CallBackTest();
	}
}