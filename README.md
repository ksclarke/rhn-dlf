# RHN Download Facilitator

## Introduction

This is really just a scratch-my-own-itch project, but I'll add some documentation in case anyone else might find it useful as a starting point for scratching their own itches.

I use [Packer.io](https://www.packer.io/) to build RHEL virtual machines in an automated fashion. When I run the build on my local machine, I can reference previously downloaded ISOs. When I want to run the build on a hosted architecture that doesn't have a persistent file system, though, I need to download the ISOs as a part of the build. This is complicated by the fact that Red Hat wants you to authenticate to do the download and that Packer only supports Basic authentication for ISO downloads.

To make Red Hat and Packer happy, I've added an additional negotiation layer using [AWS Lambda](https://aws.amazon.com/lambda/) and the [AWS API Gateway](https://aws.amazon.com/api-gateway/). It handles Packer's download request and RHN's authentication process. Basically, it provides a URL through which Packer can download a RHEL ISO using your Red Hat Network developer authentication keys. That URL, once the API Gateway is configured and deployed, will look something like:

    https://[RHN_USERNAME]:[RHN_PASSWORD]@pi0yu3xvrh.execute-api.us-east-1.amazonaws.com/rhn_dlf
    
Since Basic authentication passes information in clear text, this should only be used behind HTTPS (which is the default on AWS API Gateway, anyway). There may still be some risk here, but it's acceptable given I just use my free RHN developer account to do the build (and then use real RHEL keys if the build goes anywhere other than my local machine).

Running this Lambda script on AWS costs money, but it doesn't pass the ISO itself through Lambda. It just gives Packer an authenticated download URL via a 302 "Location" response header so the costs are really minimal.

## Packer Configuration

Since I'd prefer to use local copies rather than download a fresh ISO with each build, I usually configure the VirtualBox and VMWare Builders in my Packer build with something like the following:

    "iso_urls": [
      "iso/rhel-server-{{ user `el-6x-version` }}-x86_64-dvd.iso",
      "{{ user `rhel-6x-download-url` }}"
    ], 

This will check the local file system first and only fall back to the download facilitator service if a local version isn't found first. The `rhel-6x-download-url` then just needs to be supplied at the point that the build is initiated. Those `user` calls are, of course, supplied via [Packer variables](https://www.packer.io/docs/templates/user-variables.html). And, of course, you can name those variables whatever you want to (e.g., `rhel-7x-download-url`), but if you want to download something other than the RHEL 6.8 ISO you'll also need to change the hard-coded `DOWNLOAD_URL` and `DOWNLOAD_ISO` variables in the code, too. I'll work to make everything more configurable in the future.

## AWS Lambda and API Gateway Configuration

I suppose to be really useful this document could also describe loading the function to AWS Lambda and configuring the API Gateway, but for now (since I imagine this project is really only going to be used by myself) I'll just forgo that.

## Building the RHN Download Facilitator

AWS Lambda has a nice Eclipse plugin [that can be used](http://docs.aws.amazon.com/toolkit-for-eclipse/v1/user-guide/lambda-tutorial.html) to build and upload the project. The project can also be built without Eclipse from the command line using Maven. To use Maven, check out the project and run the Maven build command (in the process, supplying the required RHN variables):

    git clone https://github.com/ksclarke/rhn-dlf.git
    cd rhn-dlf
    mvn -Drhn.username="MY_RHN_USERNAME" -Drhn.password="MY_RHN_PASSWORD" clean install
    
If you don't want to have to supply the `rhn.username` and `rhn.password` variables on the command line each time, you can alternatively put them in your [Maven settings](http://maven.apache.org/ref/3.5.0/maven-settings/settings.html) file. Since the test runs against the Red Hat download site (instead of being mocked), the supplied variables need to be valid and have the right permissions for the tests to pass. Running the tests locally only sends your login information to the Red Hat download site (not to AWS or any other location). To use the project for real, you'd want to setup your AWS Lambda and API Gateway environments. To run the test in Eclipse using the native JUnit tools, you'll need to supply the variables as system properties in your JUnit test configuration.

## License

There is just a wee bit of code here, but I'll include a license on it in case someone finds it useful. It's licensed under the 3-Clause BSD license.

## Contact

If you have any questions about the RHN Download Facilitator repository, feel free to [email me](mailto:ksclarke@ksclarke.io) or file an issue in the project's [issue queue](https://github.com/ksclarke/rhn-dlf/issues).