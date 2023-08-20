package com.sp91.AwsClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

import net.dongliu.requests.RawResponse;
import net.dongliu.requests.RequestBuilder;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithSAMLRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithSAMLResult;

public class SamlSessionCredentials {
	
	private static String awsRegion;
	private static Regions region;
	private Session session;
	private String domain;
	private String userName;
	private String password;
	private String userpass;
	//private String userpass = domain+"\\"+userName + ":" + password;
	private RequestBuilder requestBuild;
	private URL url;
	private static String urlStr;
	private String samlAssertion=null;
	private String decodedSamlAssertion;
	private static String proxyHost;
	static int proxyPort;
	private static String nonProxyHosts;
	private String aws_access_key;
	private String aws_secret_key;
	private String aws_session_token;
	private static int connectionTimeOut;
	private static String profileName;
	private static String fileSeparator;
	
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
			System.out.println("Reading the temporary Credentials");
			properties.load(input);
			awsRegion = properties.getProperty("awsRegion");
			urlStr = properties.getProperty("urlStr");
			proxyHost = properties.getProperty("proxyHost");
			proxyPort = Integer.parseInt(properties.getProperty("proxyPort"));
			nonProxyHosts = properties.getProperty("nonProxyHosts");
			region = Regions.fromName(awsRegion);
			profileName = properties.getProperty("profileName");
			fileSeparator = System.getProperty("file.separator");
			connectionTimeOut = Integer.parseInt(properties.getProperty("connectionTimeOut"));
			
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getSamlAssertion(String domain, String userName, String password) throws IOException {
		// TODO Auto-generated method stub
		this.userName = userName;
		this.password = password;
		this.domain = domain;
		//userpass = userName + ":" + password;
		System.out.println("Authentication Set for "+domain+"\\"+userName);
		Authenticator.setDefault(new Authenticator() {
			@Override
		    public PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication(domain + "\\" + userName, password.toCharArray());
		        //return new PasswordAuthentication(userName, password.toCharArray());
		    }
		});

		try {
			url = new URL(urlStr);
			System.out.println("Obtaining HTTP Session");
			session=Requests.session();
			requestBuild = session.newRequest("GET", url);
			requestBuild.followRedirect(true);
			requestBuild.connectTimeout(connectionTimeOut*1000);
			requestBuild.timeout(connectionTimeOut*1000);
			requestBuild.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36");
			System.out.println("Obtaining HTTP Response");
			RawResponse response = requestBuild.send();
			if (response.getStatusCode()!=200) {
				System.out.println("Authentication Failed! "+response.getStatusCode()+" "+response.getStatusLine());
				System.exit(2);
			}
			Document doc = Jsoup.parse(response.readToText(),urlStr, Parser.htmlParser());
			Elements elements = doc.body().getElementsByTag("form").select("input[type=hidden]");
			for (Element element: elements) {
				if(!element.getElementsByAttributeValue("name", "SAMLResponse").isEmpty()) {
					System.out.println("Reading SAML Assertion");
					samlAssertion=element.val();
					//System.out.println("SAML Assertion: "+samlAssertion);
				}
			}
			
		}
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return samlAssertion;
		
	}
	
	public HashMap<String, String> getTempSessionCredentials(int selectedRole, String samlAssertion) {
			
			Decoder decoder = Base64.getDecoder();
			this.samlAssertion=samlAssertion;
			System.out.println("Decoding SAML Assertion");
			byte[] bytes = decoder.decode(samlAssertion);
			try {
				decodedSamlAssertion = new String(bytes, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println(decodedSamlAssertion);
			
			Document decodedDoc = Jsoup.parse(decodedSamlAssertion, urlStr, Parser.xmlParser());
			ArrayList<String> rolesArn= new ArrayList();
			System.out.println("Reading the AWS Roles and ARNs");
			HashMap<Integer,String> hashMap = new HashMap<Integer,String>();
			Elements xmlElements = decodedDoc.select("Attribute[Name=\"https://aws.amazon.com/SAML/Attributes/Role\"]").select("AttributeValue");
			int i=0;
			for (Element xmlElement: xmlElements) {
				rolesArn.add(xmlElement.text());
				hashMap.put(i, rolesArn.get(i));
				i++;
				//System.out.println("1: "+xmlElement.text());
			}
			
			for(int j=0;j<hashMap.size();j++) {
				System.out.println(j+" : "+hashMap.get(j));
			}
			
			int roleNo;
			if (selectedRole == -1) {
				Scanner sc = new Scanner(System.in);
				System.out.println("Choose the Account/Role..");
				roleNo = sc.nextInt();
			}
			else {
				roleNo = selectedRole;
			}
			
			String awsRoles = hashMap.get(roleNo);
			String roleArn = awsRoles.split(",")[1];
			String principalArn = awsRoles.split(",")[0];
			
			BasicAWSCredentials theAWSCredentials= new BasicAWSCredentials("","");
			AWSCredentialsProvider theAWSCredentialsProvider = new AWSStaticCredentialsProvider(theAWSCredentials);
			
			ClientConfiguration clientConfiguration = new ClientConfiguration();
			clientConfiguration.setProxyHost(proxyHost);
			clientConfiguration.setProxyPort(proxyPort);
			clientConfiguration.setNonProxyHosts(nonProxyHosts);
			
			System.out.println("Posting SAML Assertion with Role and Principal ARN");
			AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
					.withRegion(region)
					.withCredentials(theAWSCredentialsProvider)
					.withClientConfiguration(clientConfiguration)
					.build();
			
			AssumeRoleWithSAMLRequest assumeRoleWithSAMLRequest = new AssumeRoleWithSAMLRequest()
					.withPrincipalArn(principalArn)
					.withRoleArn(roleArn)
					.withSAMLAssertion(samlAssertion)
					.withDurationSeconds(3600);
			System.out.println("Obtaining AWS Temporary Session Credentials");
			AssumeRoleWithSAMLResult assumeRoleWithSAMLResult = stsClient.assumeRoleWithSAML(assumeRoleWithSAMLRequest);
			
			aws_access_key = assumeRoleWithSAMLResult.getCredentials().getAccessKeyId();
			aws_secret_key = assumeRoleWithSAMLResult.getCredentials().getSecretAccessKey();
			aws_session_token = assumeRoleWithSAMLResult.getCredentials().getSessionToken();
			HashMap credentialsMap= new HashMap();
			
			credentialsMap.put("aws_access_key", aws_access_key);
			credentialsMap.put("aws_secret_key", aws_secret_key);
			credentialsMap.put("aws_session_token", aws_session_token);
			return credentialsMap;		
			
		} 
	
	public void createAwsProfileFile(String aws_access_key, String aws_secret_key, String aws_session_token) {
		this.aws_access_key = aws_access_key;
		this.aws_secret_key = aws_secret_key;
		this.aws_session_token = aws_session_token;
		
		String awsDir = System.getProperty("user.home");
		File credentialsFilePath = new File(awsDir+fileSeparator+".aws");
		
		if(!credentialsFilePath.exists()) {
			credentialsFilePath.mkdirs();
		}
		FileWriter fileWriter,fileWriterConfig;
		try {
			System.out.println("Writing the credentials into credentials file");
			fileWriter = new FileWriter(credentialsFilePath+fileSeparator+"credentials");
			fileWriter.write("[saml]\n");
			fileWriter.append("aws_access_key_id = "+aws_access_key+"\n");
			fileWriter.append("aws_secret_access_key = "+aws_secret_key+"\n");
			fileWriter.append("aws_session_token = "+aws_session_token);
			fileWriter.close();
			
			
			System.out.println("Writing the config file");
			fileWriterConfig = new FileWriter(credentialsFilePath+fileSeparator+"config");
			fileWriterConfig.write("["+profileName+"]\n");
			fileWriterConfig.append("region = "+region.getName()+"\n");
			fileWriterConfig.append("output = json");
			fileWriterConfig.close();
			System.out.println("Credentials are stored in "+credentialsFilePath+"\\credentials file and valid for 1 hour ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}