node {
    stage 'Checkout from Github'
    checkout scm

    stage 'Deploy'

    sh(script: "make ENV=prod NEWRELIC_AGENT=true AWS_DEFAULT_REGION=${env.AWS_DEFAULT_REGION} IMAGE_NAME=${env.IMAGE_NAME} STACK_ALIAS=${env.STACK_ALIAS} ES_CLUSTER_NAME=${env.ES_CLUSTER_NAME} deploy-with-notification", returnStatus: true)
}
