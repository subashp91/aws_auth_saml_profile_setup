package com.sp91.AwsClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

public class SamlAuthenticator {


	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		if(args.length <3) {
			System.out.println("Usage: java -cp com.sp91.AwsClient.SamlAuthenticator ADDomain UserName Password selectedRole(0|1|2)");
			System.out.println("<AD Domain> - Mandatory Parameter. Domain of the user e.g. addomain");
			System.out.println("<UserName> - Mandatory Parameter. Username e.g. sp91");
			System.out.println("<Password> - Mandatory Parameter. Password of the user");
			System.out.println("<selectedRole> - Optional Parameter. If the user has multiple AWS roles, they can choose the role to be used for credentials genenration");
			System.out.println("If the role is not chosen in parameter, it'll anyway prompt during the credentials generation");
			System.exit(2);
		}

		String domain = args[0];
		String userName = args[1];
		String password = args[2];
		
		String samlAssertion; 
		HashMap<String,String> awsSessionCredentials;
		int selectedRole;
		
		String credentialsOutput = null;
		String awsAccessKey = null, awsSecretKey = null, awsSessionToken = null;
		
		SamlSessionCredentials samlSessionCredentials = new SamlSessionCredentials();
		Properties properties = new Properties();
		try (InputStream input = SamlAuthenticator.class.getClassLoader().getResourceAsStream("AwsConfig.properties")) {
			if (input == null) {
                System.out.println("Sorry, unable to find AwsConfig.properties in ClassPath");
                return;
            }
			System.out.println("Loading the properties file");
			properties.load(input);
			credentialsOutput=properties.getProperty("credentialsOutput");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			System.out.println("Initiating the SAML Authentication");
			samlAssertion=samlSessionCredentials.getSamlAssertion(domain,userName,password);
			awsSessionCredentials = new HashMap<String,String>();
			if (args.length >=4) {
				selectedRole = Integer.parseInt(args[3]);
			}
			else {
				selectedRole = -1;
			}
			
			System.out.println("Reading the temporary Credentials");
			awsSessionCredentials = samlSessionCredentials.getTempSessionCredentials(selectedRole, samlAssertion);
			awsAccessKey = awsSessionCredentials.get("aws_access_key");
			awsSecretKey = awsSessionCredentials.get("aws_secret_key");
			awsSessionToken = awsSessionCredentials.get("aws_session_token");
			if(credentialsOutput.equalsIgnoreCase("FILE")) {
				System.out.println("Writing the temporary Credentials into AWS credentials file");
				samlSessionCredentials.createAwsProfileFile(awsAccessKey, awsSecretKey, awsSessionToken);
			}
			else {
				System.out.println("Invalid Credentials Output Return Type");
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
