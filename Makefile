.SILENT:
include make/git/Makefile

ORG:=vivareal
PROJECT_NAME:=search-api
PROCESS:=api
PRODUCT:=search
include make/pro/Makefile

DATADOG_ENABLED?=false
APP?=$(PROJECT_NAME)

ENV:=dev
include make/env/Makefile

ARTIFACT_NAME:=$(ORG)-$(PROJECT_NAME)-$(VERSION).jar
include make/gra/Makefile

CONTAINER_ID:=$(ENV)-$(VERSION)
ARTIFACT:=build/libs/$(ARTIFACT_NAME)
include make/doc/Makefile
DOCKER_NET_CONFIG?=$(if $(filter prod,$(ENV)),--net host, $(if $(filter qa,$(ENV)), --net host -p 8482:8482 -p 4000:4000, -p 8482:8482 -p 4000:4000))

LOG:=/var/log/$(CONTAINER_NAME)
CONTAINER_LOG:=/logs
include make/log/Makefile

RUN_MEMORY:=$(if $(filter prod,$(ENV)),2560,1024)
PORT:=8482

RUN_OPTS+=-Djava.security.egd=file:/dev/./urandom
RUN_OPTS+=-Dspring.profiles.active=$(ENV)
RUN_OPTS+=-server -XX:+PrintFlagsFinal -Xss256k
RUN_OPTS+=-Xmx$(shell expr $(RUN_MEMORY) - 512)m -Xms$(shell expr $(RUN_MEMORY) - 512)m

ifeq ($(DATADOG_ENABLED), true)
	RUN_OPTS+=-javaagent:/usr/local/datadog.jar
	RUN_OPTS+=-Ddd.service.name=$(APP)
	RUN_OPTS+=-Ddd.jmxfetch.enabled=true
	RUN_OPTS+=-Ddd.service.mapping=elasticsearch:$(APP)-elasticsearch
endif

# Elasticsearch
RUN_OPTS+=-Des.hostname=$(ES_HOSTNAME)
RUN_OPTS+=-Des.cluster.name=$(ES_CLUSTER_NAME)

include make/jmx/Makefile

RUN_CMD= docker run \
		$(DOCKER_NET_CONFIG) \
		-v $(LOG):$(CONTAINER_LOG) \
		-e JAVA_OPTS='"$(RUN_OPTS)"' \
		$(REMOVE_CONTAINER_FLAG) --name $(CONTAINER_NAME) \
		$(DAEMON_FLAG) -m $(RUN_MEMORY)M -ti $(IMAGE_NAME)

run: log check-es_cluster_name image
	$(shell echo $(RUN_CMD))

MAX_SURGE:=1
ifeq ($(ONDEMAND_REPLICAS),)
	override ONDEMAND_REPLICAS:=$(if $(filter prod,$(ENV)),2,0)
endif
ifeq ($(SPOT_REPLICAS),)
	override SPOT_REPLICAS:=$(if $(filter prod,$(ENV)),2,1)
endif
MIN_SPOT_REPLICAS:=$(SPOT_REPLICAS)
MAX_SPOT_REPLICAS:=10
ifeq ($(FRIENDLY_DNS),)
	override FRIENDLY_DNS:=$(if $(filter prod,$(ENV)),,$(ENV)-)$(PROJECT_NAME).grupozap.io
endif
ifeq ($(LEGACY_FRIENDLY_DNS),)
	override LEGACY_FRIENDLY_DNS:=$(if $(filter prod,$(ENV)),,$(ENV)-)$(PROJECT_NAME).vivareal.com
endif

INTERNAL_DNS:=$(if $(filter prod,$(ENV)),,$(ENV)-)$(PROJECT_NAME).internal.$(ENV).grupozap.io

DEPLOY_GROUP:=test

EXTRA_K8S_ARGS?=RUN_MEMORY=$(RUN_MEMORY) APP=$(APP) PROCESS=$(PROCESS) PRODUCT=$(PRODUCT) ES_CLUSTER_NAME=$(ES_CLUSTER_NAME) DEPLOY_GROUP=$(DEPLOY_GROUP) MAX_SURGE=$(MAX_SURGE) ONDEMAND_REPLICAS=$(ONDEMAND_REPLICAS) SPOT_REPLICAS=$(SPOT_REPLICAS) MIN_SPOT_REPLICAS=$(MIN_SPOT_REPLICAS) MAX_SPOT_REPLICAS=$(MAX_SPOT_REPLICAS) FRIENDLY_DNS=$(FRIENDLY_DNS) LEGACY_FRIENDLY_DNS=$(LEGACY_FRIENDLY_DNS) INTERNAL_DNS=$(INTERNAL_DNS)
ifeq ($(STACK_ALIAS),)
	override STACK_ALIAS?=$(COMMIT_HASH)
endif
DEPLOY_NAME?=$(PROJECT_NAME)-$(ES_CLUSTER_NAME)-$(STACK_ALIAS)
include make/k8s/Makefile

deploy: check-es_cluster_name deploy-k8s check-deploy-with-rollback
deploy-full: check-es_cluster_name deploy-full-k8s check-deploy-with-rollback
SLK_DEPLOY_URL=https://dashboard.k8s.$(if $(filter prod,$(ENV)),,qa.)vivareal.io/#!/deployment/$(K8S_NAMESPACE)/$(DEPLOY_NAME)?namespace=$(K8S_NAMESPACE)

check-deploy-with-rollback:
	$(KUBECTL_CMD) rollout status deployment.v1.apps/$(DEPLOY_NAME)-ondemand || { \
	$(KUBECTL_CMD) get ev | grep $(DEPLOY_NAME)-ondemand ; \
	$(KUBECTL_CMD) rollout undo deployment.v1.apps/$(DEPLOY_NAME)-ondemand; exit 1; } \
	&& \
	$(KUBECTL_CMD) rollout status deployment.v1.apps/$(DEPLOY_NAME)-spot || { \
	$(KUBECTL_CMD) get ev | grep $(DEPLOY_NAME)-spot ; \
	$(KUBECTL_CMD) rollout undo deployment.v1.apps/$(DEPLOY_NAME)-spot; exit 1; }

teardown:
	$(KUBECTL_CMD) delete deploy ${DEPLOY_NAME}-ondemand && \
	$(KUBECTL_CMD) delete deploy ${DEPLOY_NAME}-spot && \
	$(KUBECTL_CMD) delete hpa ${DEPLOY_NAME}-spot && \
	$(KUBECTL_CMD) delete pdb ${DEPLOY_NAME} && \
	$(KUBECTL_CMD) delete service ${DEPLOY_NAME} && \
	$(KUBECTL_CMD) delete ingress ${DEPLOY_NAME}

# Notifications config
SLK_CHANNEL=notifs-search-ranking
SLK_USER_GROUP=search-ranking
SLK_CD_URL=$(CD_URL)ENV=$(ENV)&IMAGE_NAME=$(IMAGE_NAME)&STACK_ALIAS=$(STACK_ALIAS)&BRANCH=$(GIT_BRANCH)&delay=0sec
include make/slk/Makefile

push-with-notification: push notify-success-build

deploy-with-notification: deploy notify-success-deploy

teardown-with-notification: teardown notify-success-teardown

check-es_cluster_name-arg:
	$(if $(value ES_CLUSTER_NAME),,$(error "ES_CLUSTER_NAME is required for Makefile"))

check-es_cluster_name: check-es_cluster_name-arg
	curl -s -f "http://$(ENV)-search-es-api-$(ES_CLUSTER_NAME).vivareal.com:9200" || (echo "Unable to find ES cluster '$(ES_CLUSTER_NAME)'"; exit 1)

benchmark:
	./gradlew clean jmh --no-daemon

sonarqube:
	./gradlew sonarqube
