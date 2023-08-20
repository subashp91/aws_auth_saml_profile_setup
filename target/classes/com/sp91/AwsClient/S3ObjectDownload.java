package com.sp91.AwsClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Builder;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;

public class S3ObjectDownload {
	
	private static String awsRegion;
	private static String proxyHost;
	private static int proxyPort;
	private static String nonProxyHosts;
	private static String profileName;
	private static int maxPartConnection;
	private static String bucketName;

	ClientConfiguration clientConfig = new ClientConfiguration();
	Regions region;
	private static ProfileCredentialsProvider pc;
	String key,dest;
	
	static {
		init();
	}
	
	public static void init() {
		Properties properties = new Properties();
		try (InputStream input = SamlSessionCredentials.class.getClassLoader().getResourceAsStream("AwsConfig.properties")) {
		if (input == null) {
            System.out.println("Sorry, unable to find AwsConfig.properties in ClassPath");
            return;
        }
		try {
			properties.load(input);
		System.out.println("Reading the properties from properties file");
		awsRegion = properties.getProperty("bucketRegion");

		proxyHost = properties.getProperty("proxyHost");
		proxyPort = Integer.parseInt(properties.getProperty("proxyPort"));
		nonProxyHosts = properties.getProperty("nonProxyHosts");
		profileName = properties.getProperty("profileName");
		maxPartConnection = Integer.parseInt(properties.getProperty("maxPartConnection"));
		bucketName = properties.getProperty("bucketName");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void s3Download(String objectKey, String destination) {
		region = Regions.fromName(awsRegion);
		key=objectKey;
		dest=destination;
		File fileName = new File(dest);
		System.out.println("Setting the proxy configuration");
		clientConfig.setProxyHost(proxyHost);
		clientConfig.setProxyPort(proxyPort);
		clientConfig.withMaxConnections(maxPartConnection);
		pc = new ProfileCredentialsProvider(profileName);
		System.out.println("Download the Object from S3: " +pc.getCredentials().getAWSAccessKeyId());
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withClientConfiguration(clientConfig).withCredentials(pc).withRegion(region).build();
		GetObjectRequest gor = new GetObjectRequest(bucketName, key);
//		ObjectMetadata getS3Object = s3Client.getObject(gor, fileName);
		s3Client.getObject(gor, fileName);
		//ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
		//ListObjectsV2Result res = new ListObjectsV2Result();
		
	}
	
	public void s3Transfer(String objectKey, String destination) {
		key=objectKey;
		dest=destination;
		System.out.println("Setting the proxy configuration");
		clientConfig.setProxyHost(proxyHost);
		clientConfig.setProxyPort(proxyPort);
		clientConfig.withMaxConnections(maxPartConnection);
		region = Regions.fromName(awsRegion);
		pc = new ProfileCredentialsProvider(profileName);
		//region = Regions.AP_SOUTHEAST_1;
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(region).withClientConfiguration(clientConfig).withCredentials(pc).build();
		//AmazonS3 s3 = AWSS3Factory.
		
		TransferManager transferManager = new TransferManager(s3Client);
		//TransferManagerBuilder tx = TransferManagerBuilder.standard().withS3Client(s3Client);
		File fileName = new File(dest);
		
		System.out.println("Download Object from S3 ");
		Download dx = transferManager.download(bucketName, key, fileName);
		try {
			while(!dx.isDone()) {
				System.out.println(dx.getDescription()+" is "+dx.getState()+" and "+dx.getProgress().getPercentTransferred()+" percentage is completed");
				TimeUnit.SECONDS.sleep(30);
			}
			dx.waitForCompletion();
			transferManager.shutdownNow();
		System.out.println("Download completed");
			
		} catch (AmazonServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} catch (AmazonClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
		
	}

}
