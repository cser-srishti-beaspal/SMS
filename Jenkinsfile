// ============================================================
// Stationery Management System - Jenkins Pipeline (Windows)
// ============================================================

pipeline {

    agent any

    environment {
        IMAGE_TAG       = "${env.BUILD_NUMBER}"  // Har build ka unique tag — rollback ke liye useful
        DOCKER_REGISTRY = 'local'                // Setup stage mein override hoga
        HAS_REGISTRY    = 'false'                // Registry URL mili ya nahi
        HAS_CREDS       = 'false'                // Docker credentials hain ya nahi
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10')) // Sirf last 10 builds rakho
        timestamps()                                   // Logs mein time dikhao
        timeout(time: 30, unit: 'MINUTES')             // 30 min baad auto-fail
        disableConcurrentBuilds()                      // Ek waqt mein sirf ek build
    }

    stages {

        // ── Stage 1: Source Code Checkout ─────────────────────────
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        // ── Stage 2: Docker Credentials Setup ─────────────────────
        // Credentials na mile toh pipeline fail nahi hogi — Push stage skip hogi
        stage('Setup Credentials') {
            steps {
                script {
                    try {
                        withCredentials([string(credentialsId: 'docker-registry-url', variable: 'REG_URL')]) {
                            env.DOCKER_REGISTRY = REG_URL
                            env.HAS_REGISTRY    = 'true'
                            echo "Registry URL set: ${env.DOCKER_REGISTRY}"
                        }
                    } catch (Throwable t) {
                        echo "WARNING: 'docker-registry-url' not found. Using local."
                    }

                    try {
                        withCredentials([usernamePassword(
                                credentialsId   : 'docker-registry-creds',
                                usernameVariable: 'DOCKER_CREDS_USR',
                                passwordVariable: 'DOCKER_CREDS_PSW')]) {
                            env.HAS_CREDS = 'true'
                            echo "Docker credentials verified."
                        }
                    } catch (Throwable t) {
                        echo "WARNING: 'docker-registry-creds' not found. Push stage will be skipped."
                    }
                }
            }
        }

        // ── Stage 3: Build All Backend Services ───────────────────
        // Saare services parallel build hote hain — time bachta hai
        // -DskipTests: tests alag stage mein chalenge
        // -B: batch mode, CI ke liye suitable
        stage('Build Backend Services') {
            parallel {
                stage('Config Server')    { steps { dir('config-server')    { bat 'mvn clean package -DskipTests -B' } } }
                stage('Eureka Server')    { steps { dir('eureka-server')    { bat 'mvn clean package -DskipTests -B' } } }
                stage('API Gateway')      { steps { dir('api-gateway')      { bat 'mvn clean package -DskipTests -B' } } }
                stage('Auth Service')     { steps { dir('auth-service')     { bat 'mvn clean package -DskipTests -B' } } }
                stage('Inventory Service'){ steps { dir('inventory-service'){ bat 'mvn clean package -DskipTests -B' } } }
                stage('Request Service')  { steps { dir('request-service')  { bat 'mvn clean package -DskipTests -B' } } }
            }
        }

        // ── Stage 4: Run Backend Tests ────────────────────────────
        // Test results XML Jenkins dashboard mein dikhte hain
        stage('Run Tests') {
            parallel {
                stage('Auth Service Tests') {
                    steps { dir('auth-service') { bat 'mvn test -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'auth-service/target/surefire-reports/*.xml' } }
                }
                stage('Inventory Service Tests') {
                    steps { dir('inventory-service') { bat 'mvn test -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'inventory-service/target/surefire-reports/*.xml' } }
                }
                stage('Request Service Tests') {
                    steps { dir('request-service') { bat 'mvn test -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'request-service/target/surefire-reports/*.xml' } }
                }
            }
        }

        // ── Stage 5: Build React Frontend ─────────────────────────
        // npm ci: package-lock.json strictly follow karta hai (npm install se better for CI)
        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    bat 'npm ci'
                    bat 'npm run build'
                }
            }
        }

        // ── Stage 6: Run Frontend Tests ───────────────────────────
        // --watchAll=false: ek baar chalao aur band karo
        // --ci: test fail hone pe non-zero exit code do
        stage('Frontend Tests') {
            steps {
                dir('frontend') {
                    bat 'npm test -- --watchAll=false --ci'
                }
            }
        }

        // ── Stage 7: Build Docker Images ──────────────────────────
        // Har image ko do tags milte hain:
        //   :BUILD_NUMBER → specific version (rollback ke liye)
        //   :latest       → sabse nayi stable image
        stage('Docker Build') {
            parallel {
                stage('Config Server')    { steps { script { dockerBuild('sms-config-server',    'config-server')    } } }
                stage('Eureka Server')    { steps { script { dockerBuild('sms-eureka-server',    'eureka-server')    } } }
                stage('API Gateway')      { steps { script { dockerBuild('sms-api-gateway',      'api-gateway')      } } }
                stage('Auth Service')     { steps { script { dockerBuild('sms-auth-service',     'auth-service')     } } }
                stage('Inventory Service'){ steps { script { dockerBuild('sms-inventory-service','inventory-service') } } }
                stage('Request Service')  { steps { script { dockerBuild('sms-request-service',  'request-service')  } } }
                stage('Frontend')         { steps { script { dockerBuild('sms-frontend',          'frontend')         } } }
            }
        }

        // ── Stage 8: Push Docker Images ───────────────────────────
        // Sirf tab chalega jab dono credentials aur registry URL available ho
        stage('Docker Push') {
            when {
                expression { env.HAS_CREDS == 'true' && env.HAS_REGISTRY == 'true' }
            }
            steps {
                script {
                    withCredentials([usernamePassword(
                            credentialsId   : 'docker-registry-creds',
                            usernameVariable: 'DOCKER_CREDS_USR',
                            passwordVariable: 'DOCKER_CREDS_PSW')]) {

                        bat "docker login -u ${DOCKER_CREDS_USR} -p ${DOCKER_CREDS_PSW} ${env.DOCKER_REGISTRY}"

                        def services = [
                            'sms-config-server',
                            'sms-eureka-server',
                            'sms-api-gateway',
                            'sms-auth-service',
                            'sms-inventory-service',
                            'sms-request-service',
                            'sms-frontend'
                        ]

                        services.each { svc ->
                            bat "docker push ${env.DOCKER_REGISTRY}/${svc}:${env.IMAGE_TAG}"
                            bat "docker push ${env.DOCKER_REGISTRY}/${svc}:latest"
                        }
                    }
                }
            }
        }

        // ── Stage 9: Deploy ───────────────────────────────────────
        // Purane containers band karo, naye start karo
        // 60 seconds wait: services ko startup time dena zaroori hai
        stage('Deploy') {
            steps {
                bat 'docker-compose down --remove-orphans'
                bat 'docker-compose up -d --build'
                bat 'timeout /t 60 /nobreak'
                bat 'docker-compose ps'
            }
        }
    }

    // ── Post Build ─────────────────────────────────────────────────
    post {
        always {
            script {
                try {
                    if (env.HAS_CREDS == 'true' && env.HAS_REGISTRY == 'true') {
                        bat "docker logout ${env.DOCKER_REGISTRY} & exit 0"
                    }
                    cleanWs() // Workspace delete karo — disk space bachao
                } catch (Throwable t) {
                    echo "WARNING: Cleanup failed - ${t.getMessage()}"
                }
            }
        }
        success  { echo "Pipeline SUCCEEDED. Build #${env.BUILD_NUMBER} deployed." }
        failure  { echo "Pipeline FAILED. Check logs for build #${env.BUILD_NUMBER}." }
        unstable { echo "Pipeline completed with test failures. Review before deploying to production." }
    }
}

// ── Helper: Docker Build + Tag ─────────────────────────────────────
// Ek jagah se saari docker build commands handle hoti hain
// imageName: docker image ka naam   e.g. 'sms-auth-service'
// contextDir: source folder ka path e.g. 'auth-service'
def dockerBuild(String imageName, String contextDir) {
    bat "docker build -t ${env.DOCKER_REGISTRY}/${imageName}:${env.IMAGE_TAG} ./${contextDir}"
    bat "docker tag  ${env.DOCKER_REGISTRY}/${imageName}:${env.IMAGE_TAG} ${env.DOCKER_REGISTRY}/${imageName}:latest"
}