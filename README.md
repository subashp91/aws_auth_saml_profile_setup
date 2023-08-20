# aws_auth_saml_profile_setup
 AWS Profile and credentials setup based on SAML authentication and S3 Object operations

## **Description:** 
Provide your SSO SAML credentials which will generate the temporary session token and perform the required profile setup

Also, performs S3 object upload and download using Multipart operations


### **Usage**
#### *Compile*
```
mvn -Dbuild.number=${env.BUILD_NUMBER} clean package
```
#### *Step 1: SAML Authentication Credentials generation*
```
java -cp "<ZipExtractedPath>\lib\* <ZipExtractedPath>\AwsClient-${VERSION}.jar;.;" com.sp1.AwsClient.SamlAuthenticator <ADDomain> <awsUser> <awsUserPassword> <AWSRole>
```
#### *Step 2: Download files from S3*
```
java -cp "<ZipExtractedPath>\lib\*;<ZipExtractedPath>\AwsClient-${VERSION}.jar;.;" com.sp91.AwsClient.S3ObjectOperations S3ObjectKey DestFile <DOWNLOAD/UPLOAD>
```

### **Properties and their usage**
-----------------------------------------

| S.No.| Variable | Allowed Values |  Default   | Required  | Remarks |
|------|----------|----------------|------------|-----------|-------|
|1|awsRegion|String|us-east-1|No| # AWS Region|
|2|urlStr|String|https://ssov3.com/adfs/ls/IdpInitiatedSignOn?loginToRp=urn:amazon:webservices|Yes| # IDP provider URL|
|3|proxyHost|String|internal-default-proxy-lb1-3432532.us-east-1.elb.amazonaws.com|No| # PROXY details|
|4|proxyPort|Integer|3128|No| # PROXY details|
|5|nonProxyHosts|String|sso.com|No| # PROXY details|
|6|credentialsOutput|String|FILE|No| # Output file to be stored in FILE or should be returned. Options [FILE|RETURN]|
|7|profileName|String|saml|No|	# Name of the AWS profile if the credentials is stored in a file|
|8|maxPartConnection|Integer|500|No| # Maximum number of parts connection for the parallel upload|
|9|defaultPartSize|Integer|25|No| # Default Part Size for Mutlipart Upload. In MBs,|
|10|bucketName|String|s3bucket|No| # S3 Bucket Name|
|11|bucketRegion|String|ap-southeast-1|No||
|12|connectionTimeOut|Integer|60|No|	# Connection and Socket Timeout for Upload Object in Seconds|
|13|socketTimeOut|Integer|60|No|	# Connection and Socket Timeout for Upload Object in Seconds|
