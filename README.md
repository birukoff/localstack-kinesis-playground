A simple example project that shows how to set up local tests that use 
Localstack with Kinesis and Testcontainers, as well as AWS SDK v2.

Note, Docker should be installed on your machine to run localstack.

Files that are of interest:
1) build.gradle.kts shows dependencies,
2) src/test/resources/logback.xml has hints how to configure logging,
3) src/test/java/org/example/KinesisTest.java shows how to set up a simple test.
