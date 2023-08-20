package com.adp.ihcm.AdpAwsClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.MultipartUploadListing;

public class ListS3Multiparts {
	
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
		try (InputStream input = SamlSessionCredentials.class.getClassLoader().getResourceAsStream("AdpAwsConfig.properties")) {
		if (input == null) {
            System.out.println("Sorry, unable to find AdpAwsConfig.properties in ClassPath");
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
	
	public void s3ListMultiParts() {
		System.out.println("Setting the proxy configuration");
		clientConfig.setProxyHost(proxyHost);
		clientConfig.setProxyPort(proxyPort);
		clientConfig.withMaxConnections(maxPartConnection);
		region = Regions.fromName(awsRegion);
		pc = new ProfileCredentialsProvider(profileName);
		//region = Regions.AP_SOUTHEAST_1;
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(region).withClientConfiguration(clientConfig).withCredentials(pc).build();
		
		ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(bucketName);
		MultipartUploadListing response = s3Client.listMultipartUploads(request);
		List<MultipartUpload> multiParts = response.getMultipartUploads();
		
		for (MultipartUpload multiPartUpload: multiParts) {
			System.out.println("Key: "+multiPartUpload.getKey()+" UploadId: "+multiPartUpload.getUploadId()+" Initiated:  "+multiPartUpload.getInitiated());
		}
	}

}
