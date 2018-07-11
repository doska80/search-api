node {
    stage 'Checkout from Github'

    checkout scm

    stage "${env.JOB_NAME}"

    withCredentials([
        string(credentialsId: 'AWS_ACCESS_KEY_ID', variable: 'AWS_ACCESS_KEY_ID'),
        string(credentialsId: 'AWS_SECRET_ACCESS_KEY', variable: 'AWS_SECRET_ACCESS_KEY'),
        string(credentialsId: 'SLK_TOKEN', variable: 'SLK_TOKEN')
    ]){
        def environment = env.ENV
        def imageName = env.IMAGE_NAME
        def hostname = env.ES_HOSTNAME.replace("*env*", env.ENV).replace("*clustername*", env.ES_CLUSTER_NAME)
        def logstash = env.LOGSTASH_HOST.replace("*env*", env.ENV)
        def command = "make ENV=${env.ENV} AWS_DEFAULT_REGION=${env.AWS_DEFAULT_REGION} IMAGE_NAME=${env.IMAGE_NAME} STACK_ALIAS='${env.ES_CLUSTER_NAME}-${env.STACK_ALIAS}' SLK_TOKEN=${SLK_TOKEN} ES_CLUSTER_NAME=${env.ES_CLUSTER_NAME} NEWRELIC_ENABLED=${env.NEWRELIC_ENABLED} DOCKER_REGISTRY_DOMAIN=${env.DOCKER_REGISTRY_DOMAIN} ES_HOSTNAME=${hostname} LOGSTASH_HOST=${logstash} deploy-with-notification"
        def result = sh(script: command, returnStatus: true)
        sendToSlack(result, imageName, environment)
    }
}

def sendToSlack(result, imageName, environment) {
    def color = 'good'
    def status = 'SUCCESS'

    if (result != 0) {
        color = 'danger'
        status = 'ERROR'
    }

    def message = "Job <${env.BUILD_URL}/console|#${env.BUILD_NUMBER}> *${env.JOB_NAME}*, in *${env.AWS_DEFAULT_REGION}* of *${environment}* with *${status}* using image_name: *${imageName}*"
    slackSend channel: '#alerts-search-ranking', color: color, message: "${message}", teamDomain: 'vivareal'
}
