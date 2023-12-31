#  Powertools for AWS Lambda (Java) - Cloudformation Custom Resource Example

This project contains an example of Lambda function using the CloudFormation module of Powertools for AWS Lambda in Java. For more information on this module, please refer to the [documentation](https://awslabs.github.io/aws-lambda-powertools-java/utilities/custom_resources/).

## Deploy the sample application

This sample can be used either with the Serverless Application Model (SAM) or with CDK.

### Deploy with SAM CLI
To deploy it using the SAM CLI, check out the instructions for getting started in [the examples directory](../README.md)

### Deploy with CDK
To use CDK you need the following tools.

* CDK - [Install CDK](https://docs.aws.amazon.com/cdk/v2/guide/getting_started.html)
* Java 8 - [Install Java 8](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html)
* Maven - [Install Maven](https://maven.apache.org/install.html)
* Docker - [Install Docker community edition](https://hub.docker.com/search/?type=edition&offering=community)

To build and deploy this application for the first time, run the following in your shell:

```bash
cd infra/cdk
mvn package
cdk synth
cdk deploy -c BucketNameParam=my-unique-bucket-20230718
```