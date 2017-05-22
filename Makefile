include make/git/Makefile

ORG:=vivareal

PROJECT_NAME:=search-api-v2
include make/pro/Makefile

ENV:=dev
include make/env/Makefile

ARTIFACT_NAME:=$(ORG)-$(PROJECT_NAME)-$(VERSION).jar
include make/gra/Makefile

CONTAINER_ID:=$(ENV)-$(VERSION)
ARTIFACT:=build/libs/$(ARTIFACT_NAME)
include make/doc/Makefile
DOCKER_NET_CONFIG?=$(if $(filter prod,$(ENV)),--net host,-p 8482:8482 -p 4000:4000)

LOG:=logs
CONTAINER_LOG:=/var/log/$(CONTAINER_NAME)
include make/log/Makefile

RUN_MEMORY:=$(if $(filter prod,$(ENV)),3500,900)
PORT:=8482

RUN_OPTS+=-Dspring.profiles.active=$(ENV)
# TODO - Use DefaultAWSCredentialsProviderChain
RUN_OPTS+=-Daws.access.key=$(AWS_ACCESS_KEY_ID) -Daws.secret.key=$(AWS_SECRET_ACCESS_KEY)
RUN_OPTS+=-server -XX:+UseConcMarkSweepGC -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=80
RUN_OPTS+=-Xmx$(shell expr $(RUN_MEMORY) - 100)m -Xms$(shell expr $(RUN_MEMORY) - 100)m
#ifneq ($(NEWRELIC_AGENT),)
#  RUN_OPTS+=-javaagent:/opt/apache-tomcat-8.0.35/webapps/ROOT/WEB-INF/lib/newrelic.jar
#endif

include make/jmx/Makefile

RUN_CMD= docker run \
		$(DOCKER_NET_CONFIG) \
		-v $(LOG):$(CONTAINER_LOG) \
		-e CATALINA_OPTS='"$(RUN_OPTS)"' \
		-e AWS_ACCESS_KEY_ID=$(AWS_ACCESS_KEY_ID) \
		-e AWS_SECRET_ACCESS_KEY=$(AWS_SECRET_ACCESS_KEY) \
		$(REMOVE_CONTAINER_FLAG) --name $(CONTAINER_NAME) \
		$(DAEMON_FLAG) -m $(RUN_MEMORY)M -ti $(IMAGE_NAME)

run: log image aws_default_region
	$(shell echo $(RUN_CMD))

user-data-setup:
include make/usr/Makefile

VARIABLES?=$(ENV)
REGION_VARIABLES?=$(AWS_DEFAULT_REGION)/$(ENV)
TEMPLATE?=simple-asg-with-elb
STACK_ALIAS?=$(COMMIT_HASH)
STACK_NAME?=$(ENV)-search-$(PROJECT_NAME)-$(STACK_ALIAS)
stack-variables-setup: user-data
include make/asn/Makefile

deploy: aws_default_region deploy-stack set-cfn-stack-id
SLK_DEPLOY_URL=https://console.aws.amazon.com/cloudformation/home?region=$(AWS_DEFAULT_REGION)\#/stack/detail?stackId=$(CFN_STACK_ID)

teardown: destroy-stack

# Notifications config
SLK_CHANNEL=squad-search
SLK_USER_GROUP=search
SLK_CD_URL=http://jenkins.vivareal.com/view/SEARCH-API/job/SEARCH_API_V2_$(ENV_CAPS)/parambuild?IMAGE_NAME=$(IMAGE_NAME)&STACK_ALIAS=$(STACK_ALIAS)&delay=0sec
include make/slk/Makefile

push-with-notification: push notify-success-build

deploy-with-notification: deploy notify-success-deploy

aws_default_region:
	$(if $(value AWS_DEFAULT_REGION),,$(error "AWS_DEFAULT_REGION is required for Makefile"))