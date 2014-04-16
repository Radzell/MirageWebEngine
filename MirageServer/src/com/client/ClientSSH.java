package com.client;

public class ClientSSH {

	public ClientSSH() {


	   

	}
	public static void main(String[] args) {
		 String sshCommand = "ssh -l diego 184.106.134.110";
		    try
		    {
		      System.out.println( "createSSHTunnel() - Creating ssh tunnel with " + sshCommand);
		      Process process=Runtime.getRuntime().exec(sshCommand);
		    }catch(Exception e){
		    	e.getStackTrace();
		    }

	}
	
}
