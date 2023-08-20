package com.sp91.AwsClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
//import java.util.logging.Logger;
//import org.apache.cxf;
import java.util.Properties;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.transfer.TransferManager;

public class S3ObjectUpload {

   // private static final Logger logger = LogUtil.getLogger();

    public static long DEFAULT_FILE_PART_SIZE = 25; // 25MB
    public static long FILE_PART_SIZE;

    private static AmazonS3 s3Client;
    private static TransferManager transferManager;
	private static String awsRegion;
	private static String proxyHost;
	private static int proxyPort;
	private static String nonProxyHosts;
	private static String profileName;
	private static int maxPartConnection;
	private static String bucketName;
	private static int socketTimeOut;
	private static int connectionTimeOut;

    static {
            init();
    }

    public S3ObjectUpload() {

    }

    public static void init() {
            // ...
    	
    	Properties properties = new Properties();
		try (InputStream input = SamlSessionCredentials.class.getClassLoader().getResourceAsStream("AwsConfig.properties")) {
		if (input == null) {
            System.out.println("Sorry, unable to find AwsConfig.properties in ClassPath");
            return;
        }
		try {
			properties.load(input);
		
		awsRegion = properties.getProperty("awsRegion");

		if (!properties.getProperty("defaultPartSize").isEmpty()) {
			DEFAULT_FILE_PART_SIZE = Integer.parseInt(properties.getProperty("defaultPartSize"));
		}
		
		FILE_PART_SIZE = DEFAULT_FILE_PART_SIZE * 1024 * 1024;
		
		proxyHost = properties.getProperty("proxyHost");
		proxyPort = Integer.parseInt(properties.getProperty("proxyPort"));
		nonProxyHosts = properties.getProperty("nonProxyHosts");
		profileName = properties.getProperty("profileName");
		maxPartConnection = Integer.parseInt(properties.getProperty("maxPartConnection"));
		bucketName = properties.getProperty("bucketName");
		socketTimeOut = Integer.parseInt(properties.getProperty("socketTimeOut"));
		connectionTimeOut = Integer.parseInt(properties.getProperty("socketTimeOut"));
		
		ClientConfiguration clientConfiguration = new ClientConfiguration()
				.withProxyHost(proxyHost)
				.withProxyPort(proxyPort)
				.withConnectionTimeout(connectionTimeOut*1000)
				.withMaxErrorRetry(15)
				.withTcpKeepAlive(true)
				.withClientExecutionTimeout(600*1000)	
				.withRequestTimeout(600*1000)
				.withSocketTimeout(socketTimeOut*1000)
				.withMaxConnections(maxPartConnection);
		AWSCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(profileName);
        s3Client = new AmazonS3Client(credentialsProvider.getCredentials(),clientConfiguration);
        transferManager = new TransferManager(credentialsProvider.getCredentials());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}                        
    }

    // ...

    public static void putObjectAsMultiPart(File file) {
            putObjectAsMultiPart(bucketName, file, FILE_PART_SIZE);
    }
 
    public static void putObjectAsMultiPart(String bucketName, File file, long partSize) {  
    		System.setProperty("http.proxyHost", proxyHost);
    		System.setProperty("http.nonProxyHosts ", nonProxyHosts);
    		System.setProperty("http.proxyPort", "3128");
            List<PartETag> partETags = new ArrayList<PartETag>();  
            List<MultiPartFileUploader> uploaders = new ArrayList<MultiPartFileUploader>();  
               
            // Step 1: Initialize.  
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, file.getName());  
            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);  
            long contentLength = file.length();  
               
            try {  
                    // Step 2: Upload parts.  
                    long filePosition = 0;  
                    for (int i = 1; filePosition < contentLength; i++) {  
                            // Last part can be less than part size. Adjust part size.  
                            partSize = Math.min(partSize, (contentLength - filePosition));  

                            // Create request to upload a part.  
                            UploadPartRequest uploadRequest =   
                                    new UploadPartRequest().  
                                            withBucketName(bucketName).withKey(file.getName()).  
                                            withUploadId(initResponse.getUploadId()).withPartNumber(i).  
                                            withFileOffset(filePosition).
                                            withFile(file).
                                            withPartSize(partSize).
                                            withSdkClientExecutionTimeout(600*1000).
                                            withSdkRequestTimeout(600*1000);
                                            

                            uploadRequest.setGeneralProgressListener(new UploadProgressListener(file, i, partSize));  
                   
                            // Upload part and add response to our list.  
                            MultiPartFileUploader uploader = new MultiPartFileUploader(uploadRequest); 
                            uploader.upload();
                            uploaders.add(uploader);
                      
                            

                            filePosition += partSize;  
                    }  
                 
                    for (MultiPartFileUploader uploader : uploaders) {  
                            uploader.join();  
                            partETags.add(uploader.getPartETag());  
                    }  

                    // Step 3: complete.  
                    CompleteMultipartUploadRequest compRequest = 
                            new CompleteMultipartUploadRequest(bucketName,   
                                                               file.getName(),   
                                                               initResponse.getUploadId(),   
                                                               partETags);  

                    s3Client.completeMultipartUpload(compRequest);
                    s3Client.shutdown();
                    System.out.println("Object Upload completed");
            }   
            catch (Throwable t) {  
            		//System.out.println("Unable to put object as multipart to Amazon S3 for file " + file.getName(), t);  
                    s3Client.abortMultipartUpload(  
                            new AbortMultipartUploadRequest(  
                                    bucketName, file.getName(), initResponse.getUploadId()));  
            } 
    }  

    //public static void main(String[] args) {
    	//File file = new File("C:\\Users\\sparam1\\WHRM-S150-3.zip");
    	//AmazonS3Util.init();
    	//AmazonS3Util.putObjectAsMultiPart("ihcmapac", file);
//    	AmazonS3Util amazonS3Util = new AmazonS3Util();
  //  	amazonS3Util.init();
    //	amazonS3Util.putObjectAsMultiPart("ihcmapac", file);
//    }

    private static class UploadProgressListener implements ProgressListener {

            File file;
            int partNo;
            long partLength;

            UploadProgressListener(File file) {
                    this.file = file;
            }

            @SuppressWarnings("unused")
            UploadProgressListener(File file, int partNo) {
                    this(file, partNo, 0);
            }

            UploadProgressListener(File file, int partNo, long partLength) {
                    this.file = file;
                    this.partNo = partNo;
                    this.partLength = partLength;
            }

            @Override
            public void progressChanged(ProgressEvent progressEvent) {
                    switch (progressEvent.getEventCode()) {
                            case ProgressEvent.STARTED_EVENT_CODE:
                                    System.out.println("Upload started for file " + "\"" + file.getName() + "\"");
                                    break;
                            case ProgressEvent.COMPLETED_EVENT_CODE:
                            		System.out.println("Upload completed for file " + "\"" + file.getName() + "\"" + 
                                                    ", " + file.length() + " bytes data has been transferred");
                                    break;
                            case ProgressEvent.FAILED_EVENT_CODE:
                            		System.out.println("Upload failed for file " + "\"" + file.getName() + "\"" + 
                                                    ", " + progressEvent.getBytesTransferred() + " bytes data has been transferred");
                                    break;
                            case ProgressEvent.CANCELED_EVENT_CODE:
                            		System.out.println("Upload cancelled for file " + "\"" + file.getName() + "\"" + 
                                                    ", " + progressEvent.getBytesTransferred() + " bytes data has been transferred");
                                    break;
                            case ProgressEvent.PART_STARTED_EVENT_CODE:
                            		System.out.println("Upload started at " + partNo + ". part for file " + "\"" + file.getName() + "\"");
                                    break;
                            case ProgressEvent.PART_COMPLETED_EVENT_CODE:
                            		System.out.println("Upload completed at " + partNo + ". part for file " + "\"" + file.getName() + "\"" + 
                                                    ", " + (partLength > 0 ? partLength : progressEvent.getBytesTransferred())  + 
                                                    " bytes data has been transferred");
                                    break;
                            case ProgressEvent.PART_FAILED_EVENT_CODE:
                            		System.out.println("Upload failed at " + partNo + ". part for file " + "\"" + file.getName() + "\"" +
                                                    ", " + progressEvent.getBytesTransferred() + " bytes data has been transferred");
                                    break;
                    }
            }

    }

    private static class MultiPartFileUploader extends Thread {  
               
            private UploadPartRequest uploadRequest;  
            private PartETag partETag;
			//private AmazonS3 s3Client;  
               
            MultiPartFileUploader(UploadPartRequest uploadRequest) { 
                 //   this.s3Client = s3Client;
                    this.uploadRequest = uploadRequest;  
            }  
               
            @Override  
            public void run() {  
                    partETag = s3Client.uploadPart(uploadRequest).getPartETag();
            }  
               
            private PartETag getPartETag() {  
                    return partETag;  
            }  
               
            private void upload() {  
                    start();  
            }  
               
    }  

}