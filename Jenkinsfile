node {
    stage 'Checkout from Github'

    checkout scm

    stage "${env.JOB_NAME}"

    withCredentials([
        string(credentialsId: 'SLK_TOKEN', variable: 'SLK_TOKEN'),
        string(credentialsId: 'K8S_SEARCH_TOKEN_' + env.ENV.toUpperCase(), variable: 'K8S_TOKEN')
    ]){
        def environment = env.ENV
        def imageName = env.IMAGE_NAME
        def k8s_cluster = env.K8S_CLUSTER.replace("*env*", env.ENV)
        def hostname = env.ES_HOSTNAME.replace("*env*", env.ENV).replace("*clustername*", env.ES_CLUSTER_NAME)
        def command = "make ENV=${env.ENV} DEPLOY_GROUP=${env.DEPLOY_GROUP} APP=${env.APP} IMAGE_NAME=${env.IMAGE_NAME} K8S_CLUSTER=${k8s_cluster} K8S_TOKEN=${K8S_TOKEN} STACK_ALIAS=${env.STACK_ALIAS} SLK_TOKEN=${SLK_TOKEN} ES_CLUSTER_NAME=${env.ES_CLUSTER_NAME} ES_HOSTNAME=${hostname} ${env.OPERATION}-with-notification"
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

    def message = "Job <${env.BUILD_URL}/console|#${env.BUILD_NUMBER}> *${env.JOB_NAME}*, in *${environment}* with *${status}* using image_name: *${imageName}*"
    slackSend channel: '#alerts-search-ranking', color: color, message: "${message}", teamDomain: 'grupozap'
}
