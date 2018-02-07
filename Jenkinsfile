node {
    stage 'Checkout from Github'
    checkout scm

    stage 'Deploy'
    withCredentials([string(credentialsId: 'AWS_ACCESS_KEY_ID', variable: 'AWS_ACCESS_KEY_ID'),
                     string(credentialsId: 'AWS_SECRET_ACCESS_KEY', variable: 'AWS_SECRET_ACCESS_KEY'),
                     string(credentialsId: 'SLK_TOKEN', variable: 'SLK_TOKEN')]) {
        def result = sh(script: "make ENV=prod NEWRELIC_AGENT=true AWS_DEFAULT_REGION=${env.AWS_DEFAULT_REGION} IMAGE_NAME=${env.IMAGE_NAME} STACK_ALIAS=${env.STACK_ALIAS} SLK_TOKEN=${SLK_TOKEN} ES_CLUSTER_NAME=${env.ES_CLUSTER_NAME} deploy-with-notification", returnStatus: true)
        if (result) {
            slackSend channel: '#squad-search-ranking', color: 'danger', message: "Build failure - ${env.JOB_NAME} <${env.BUILD_URL}|#${env.BUILD_NUMBER}>", teamDomain: 'vivareal'
            error('make deploy error')
        }
    }
}
