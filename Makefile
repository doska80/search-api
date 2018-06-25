include make/git/Makefile

ORG:=vivareal
PROJECT_NAME:=search-api-v2
PROCESS:=api
PRODUCT:=search
include make/pro/Makefile

AWS_DEFAULT_REGION?=us-east-1

NEWRELIC_ENABLED?=false

ENV:=dev
include make/env/Makefile

ARTIFACT_NAME:=$(ORG)-$(PROJECT_NAME)-$(VERSION).jar
include make/gra/Makefile

DOCKER_REGISTRY_DOMAIN=prod-search-docker-registry.vivareal.com

CONTAINER_ID:=$(ENV)-$(VERSION)
ARTIFACT:=build/libs/$(ARTIFACT_NAME)
include make/doc/Makefile
DOCKER_NET_CONFIG?=$(if $(filter prod,$(ENV)),--net host,-p 8482:8482 -p 4000:4000)

LOG:=/var/log/$(CONTAINER_NAME)
CONTAINER_LOG:=/logs
include make/log/Makefile

RUN_MEMORY:=$(if $(filter prod,$(ENV)),3000,900)
PORT:=8482
ES_PORT?=9300

RUN_OPTS+=-Dspring.profiles.active=$(ENV)
RUN_OPTS+=-server -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=65 -XX:MaxMetaspaceSize=256m
RUN_OPTS+=-Xmx$(shell echo "$(RUN_MEMORY) * 0.9" | bc | grep -E -o ^[0-9]+)m -Xms$(shell echo "$(RUN_MEMORY) * 0.9" | bc | grep -E -o ^[0-9]+)m
RUN_OPTS+=-Dnewrelic.config.agent_enabled=$(NEWRELIC_ENABLED)
RUN_OPTS+=-javaagent:/usr/local/newrelic.jar

# Elasticsearch
RUN_OPTS+=-Des.hostname=$(ENV)-search-es-api-$(ES_CLUSTER_NAME).vivareal.com
RUN_OPTS+=-Des.port=$(ES_PORT)
RUN_OPTS+=-Des.cluster.name=$(ES_CLUSTER_NAME)

include make/jmx/Makefile

RUN_CMD= docker run \
		$(DOCKER_NET_CONFIG) \
		-v $(LOG):$(CONTAINER_LOG) \
		-e JAVA_OPTS='"$(RUN_OPTS)"' \
		$(REMOVE_CONTAINER_FLAG) --name $(CONTAINER_NAME) \
		$(DAEMON_FLAG) -m $(RUN_MEMORY)M -ti $(IMAGE_NAME)

run: log es_cluster_name aws_default_region image
	$(shell echo $(RUN_CMD))

EXTRA_USER_DATA=

ifeq ($(ENV),prod)
	override EXTRA_USER_DATA+=docker run -d --name=filebeat \
		-e APPLICATION="$(PROJECT_NAME)" \
		-e ENV="$(ENV)" \
		-e INSTANCEID='$$(/usr/bin/curl -s --connect-timeout 2 curl http://169.254.169.254/latest/meta-data/local-ipv4)' \
		-e LOGSTASH_HOST="gateway.logs.platform.private.$(ENV).us-east-1.vivareal.io" \
		-e LOGSTASH_PORT="5044" \
		-e PROCESS="$(PROCESS)" \
		-e PRODUCT="$(PRODUCT)" \
		-v /var/lib/docker/containers:/var/lib/docker/containers \
		--log-opt max-size=200m \
		vivareal/docker-filebeat:v1.4.2
endif

user-data-setup:
include make/usr/Makefile

VARIABLES?=$(ENV)
REGION_VARIABLES?=$(AWS_DEFAULT_REGION)/$(ENV)
TEMPLATE?=$(if $(filter prod,$(ENV)),asg-with-double-elb,simple-asg-with-elb)
STACK_ALIAS?=$(COMMIT_HASH)
STACK_NAME?=$(ENV)-search-$(PROJECT_NAME)-$(STACK_ALIAS)
DEPLOY_NAME?=$(STACK_NAME)
stack-variables-setup: user-data
include make/asn/Makefile

deploy: aws_default_region es_cluster_name deploy-stack set-cfn-stack-id
SLK_DEPLOY_URL=https://console.aws.amazon.com/cloudformation/home?region=$(AWS_DEFAULT_REGION)\#/stack/detail?stackId=$(CFN_STACK_ID)

teardown: destroy-stack

# Notifications config
SLK_CHANNEL=alerts-search-ranking
SLK_USER_GROUP=search-ranking
SLK_CD_URL=http://jenkins.vivareal.com/view/SEARCH-API-V2/job/SEARCH_API_V2_$(ENV_CAPS)/parambuild?IMAGE_NAME=$(IMAGE_NAME)&STACK_ALIAS=$(STACK_ALIAS)&delay=0sec
include make/slk/Makefile

push-with-notification: push notify-success-build

deploy-with-notification: deploy notify-success-deploy

aws_default_region:
	$(if $(value AWS_DEFAULT_REGION),,$(error "AWS_DEFAULT_REGION is required for Makefile"))

es_cluster_name:
	$(if $(value ES_CLUSTER_NAME),,$(error "ES_CLUSTER_NAME is required for Makefile"))

benchmark:
	./gradlew clean jmh --no-daemon
