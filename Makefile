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

MAX_SURGE:=1
MIN_AVAILABLE:=1
MIN_REPLICAS:=$(if $(filter prod,$(ENV)),3,1)
MAX_REPLICAS:=$(if $(filter prod,$(ENV)),6,1)
RUN_MEMORY:=$(if $(filter prod,$(ENV)),1152,384)
JVM_MEMORY:=$(if $(filter prod,$(ENV)),512,128)
PORT:=8482

RUN_OPTS+=-server -XX:+PrintFlagsFinal -XX:+UseG1GC -Xss256k
RUN_OPTS+=-Xms$(JVM_MEMORY)m -Xmx$(JVM_MEMORY)m
RUN_OPTS+=-Djava.security.egd=file:/dev/./urandom
RUN_OPTS+=-Dspring.profiles.active=$(ENV)
RUN_OPTS+=-Dapplication.version=$(VERSION)

ifeq ($(DATADOG_ENABLED), true)
	RUN_OPTS+=-javaagent:/usr/local/datadog.jar
	RUN_OPTS+=-Ddd.service.name=$(APP)
	RUN_OPTS+=-Ddd.jmxfetch.enabled=true
	RUN_OPTS+=-Ddd.service.mapping=elasticsearch:$(APP)-elasticsearch
endif

# Elasticsearch
RUN_OPTS+=-Des.hostname=$(ES_HOSTNAME)
RUN_OPTS+=-Des.cluster.name=$(ES_CLUSTER_NAME)

ifneq ($(ENV), prod)
	include make/jmx/Makefile
endif

RUN_CMD= docker run \
		$(DOCKER_NET_CONFIG) \
		-v $(LOG):$(CONTAINER_LOG) \
		-e JAVA_OPTS='"$(RUN_OPTS)"' \
		$(REMOVE_CONTAINER_FLAG) --name $(CONTAINER_NAME) \
		$(DAEMON_FLAG) -m $(RUN_MEMORY)M -ti $(IMAGE_NAME)

run: log check-es_cluster_name image
	$(shell echo $(RUN_CMD))

ifeq ($(FRIENDLY_DNS),)
	override FRIENDLY_DNS:=$(if $(filter prod,$(ENV)),,$(ENV)-)$(PROJECT_NAME).grupozap.io
endif
ifeq ($(LEGACY_FRIENDLY_DNS),)
	override LEGACY_FRIENDLY_DNS:=$(if $(filter prod,$(ENV)),,$(ENV)-)$(PROJECT_NAME).vivareal.com
endif

INTERNAL_DNS:=$(if $(filter prod,$(ENV)),,$(ENV)-)$(PROJECT_NAME).internal.$(ENV).grupozap.io

DEPLOY_GROUP:=test

EXTRA_K8S_ARGS?=RUN_MEMORY=$(RUN_MEMORY) APP=$(APP) PROCESS=$(PROCESS) PRODUCT=$(PRODUCT) ES_CLUSTER_NAME=$(ES_CLUSTER_NAME) DEPLOY_GROUP=$(DEPLOY_GROUP) MAX_SURGE=$(MAX_SURGE) MIN_REPLICAS=$(MIN_REPLICAS) MAX_REPLICAS=$(MAX_REPLICAS) FRIENDLY_DNS=$(FRIENDLY_DNS) LEGACY_FRIENDLY_DNS=$(LEGACY_FRIENDLY_DNS) INTERNAL_DNS=$(INTERNAL_DNS) MIN_AVAILABLE=${MIN_AVAILABLE}
ifeq ($(STACK_ALIAS),)
	override STACK_ALIAS?=$(COMMIT_HASH)
endif
DEPLOY_NAME?=$(PROJECT_NAME)-$(ES_CLUSTER_NAME)-$(STACK_ALIAS)
include make/k8s/Makefile

deploy: check-es_cluster_name deploy-k8s deploy-pdb-and-hpa check-deploy-with-rollback
deploy-full: check-es_cluster_name deploy-full-k8s check-deploy-with-rollback
SLK_DEPLOY_URL=https://dashboard.k8s.$(if $(filter prod,$(ENV)),,qa.)vivareal.io/#!/deployment/$(K8S_NAMESPACE)/$(DEPLOY_NAME)?namespace=$(K8S_NAMESPACE)

deploy-pdb-and-hpa:
ifeq ($(ENV), prod)
	echo $(ENVSUBST_RUN)
	echo $(K8S_TMPL_DIR)
	echo $(K8S_DIST_DIR)
	$(ENVSUBST_RUN) < $(K8S_TMPL_DIR)/pdb.yaml.tmpl > $(K8S_DIST_DIR)/pdb.yaml
	$(ENVSUBST_RUN) < $(K8S_TMPL_DIR)/hpa.yaml.tmpl > $(K8S_DIST_DIR)/hpa.yaml
	$(KUBECTL_CMD) apply --record -f $(K8S_DIST_DIR)/pdb.yaml
	$(KUBECTL_CMD) apply --record -f $(K8S_DIST_DIR)/hpa.yaml
endif

check-deploy-with-rollback:
	$(KUBECTL_CMD) rollout status deployment.v1.apps/$(DEPLOY_NAME) || { \
	$(KUBECTL_CMD) get ev | grep $(DEPLOY_NAME) ; \
	$(KUBECTL_CMD) rollout undo deployment.v1.apps/$(DEPLOY_NAME); exit 1; }

teardown:
	$(KUBECTL_CMD) delete deploy ${DEPLOY_NAME}
	$(KUBECTL_CMD) delete service ${DEPLOY_NAME}
	$(KUBECTL_CMD) delete ingress ${DEPLOY_NAME}
ifeq ($(ENV), prod)
	$(KUBECTL_CMD) delete hpa ${DEPLOY_NAME}
	$(KUBECTL_CMD) delete pdb ${DEPLOY_NAME}
endif

# Notifications config
SLK_CHANNEL=notifs-matching
SLK_USER_GROUP=matching
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
