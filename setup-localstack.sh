#!/bin/bash

awslocal kinesis create-stream --stream-name events --shard-count 5

#awslocal dynamodb create-table \
#--table-name spring-stream-lock-registry \
#--attribute-definitions AttributeName=lockKey,AttributeType=S AttributeName=sortKey,AttributeType=S \
#--key-schema AttributeName=lockKey,KeyType=HASH AttributeName=sortKey,KeyType=RANGE \
#--provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
#--tags Key=Owner,Value=localstack
#
#awslocal dynamodb create-table \
#--table-name spring-stream-metadata \
#--attribute-definitions AttributeName=KEY,AttributeType=S \
#--key-schema AttributeName=KEY,KeyType=HASH \
#--provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
#--tags Key=Owner,Value=localstack

#awslocal dynamodb list-tables
awslocal kinesis list-streams

check_stream_active() {
  awslocal kinesis describe-stream --stream-name events | grep '"StreamStatus": "ACTIVE"'
}

while [ -z "$(check_stream_active)" ]; do
  echo "Waiting for stream to become active..."
done

echo "KinesisStreamIsReady" # used by TestContainers to wait until container is ready
