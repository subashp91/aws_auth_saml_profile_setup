package com.sp91.AwsClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class S3ObjectOperations {
	
	public static void main(String[] args) {
		if (args.length <2) {
			System.out.println("Usage: com.sp91.AwsClient.S3ObjectOperations localObjectPath ObjectKey Action(Upload|Download)");
			System.out.println("Ex: java -cp com.sp91.AwsClient.S3ObjectOperations $CLASSPATH /tmp/s3object/file bucket/object/file Upload");
			System.exit(2);
		}
		String fileName = args[0];
		String localObjectPath = args[1];
		String s3ObjectAction = args[2];
		File file = new File(fileName);		
		
		if (s3ObjectAction.equalsIgnoreCase("UPLOAD")) {
			S3ObjectUpload s3ObjectUpload = new S3ObjectUpload();
	    	//s3ObjectUpload.init();
	    	s3ObjectUpload.putObjectAsMultiPart(file);
		}
		
		else if(s3ObjectAction.equalsIgnoreCase("DOWNLOAD")) {
			S3ObjectDownload s3ObjectDownload = new S3ObjectDownload();
			//s3ObjectDownload.init();
			s3ObjectDownload.s3Transfer(fileName, localObjectPath);
			
		}
    	
    }

}
