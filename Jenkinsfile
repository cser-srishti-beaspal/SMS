// ============================================================
// Stationery Management System - Declarative Jenkins Pipeline
// ============================================================
// Prerequisites:
//   - Jenkins with Pipeline plugin
//   - Maven 3.9+ configured as 'Maven-3.9' in Global Tool Config
//   - JDK 17 configured as 'JDK-17' in Global Tool Config
//   - Node.js 18 configured as 'NodeJS-18' via NodeJS plugin
//   - Docker & Docker Compose installed on Jenkins agent
//   - Docker registry credentials stored as 'docker-registry-creds'
// ============================================================

pipeline {
    agent any

    /*
    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
        nodejs 'NodeJS-18'
    }
    */

    environment {
        IMAGE_TAG        = "${env.BUILD_NUMBER}"
        COMPOSE_PROJECT  = 'sms'
        DOCKER_REGISTRY  = 'local'
        HAS_REGISTRY     = 'false'
        HAS_CREDS        = 'false'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {

        // ──────────────────────────────────────────────────
        // Stage 1: Checkout Source Code
        // ──────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                echo '🔄 Checking out source code...'
                checkout scm
            }
        }

        // ──────────────────────────────────────────────────
        // Stage 1.5: Setup Credentials & Registry
        // ──────────────────────────────────────────────────
        stage('Setup Credentials') {
            steps {
                script {
                    echo '⚙️ Resolving registry credentials...'
                    try {
                        withCredentials([string(credentialsId: 'docker-registry-url', variable: 'REG_URL')]) {
                            env.DOCKER_REGISTRY = REG_URL
                            env.HAS_REGISTRY = 'true'
                            echo "✅ Docker registry URL resolved: ${env.DOCKER_REGISTRY}"
                        }
                    } catch (Throwable t) {
                        env.DOCKER_REGISTRY = 'local'
                        env.HAS_REGISTRY = 'false'
                        echo "⚠️ 'docker-registry-url' credential not found. Using default local registry: ${env.DOCKER_REGISTRY}"
                    }

                    try {
                        withCredentials([usernamePassword(credentialsId: 'docker-registry-creds', usernameVariable: 'DOCKER_CREDS_USR', passwordVariable: 'DOCKER_CREDS_PSW')]) {
                            env.HAS_CREDS = 'true'
                            echo "✅ Docker registry credentials verified."
                        }
                    } catch (Throwable t) {
                        env.HAS_CREDS = 'false'
                        echo "⚠️ 'docker-registry-creds' credential not found. Docker push stage will be skipped."
                    }
                }
            }
        }

        // ──────────────────────────────────────────────────
        // Stage 2: Build All Backend Services (Parallel)
        // ──────────────────────────────────────────────────
        stage('Build Backend Services') {
            parallel {
                stage('Config Server') {
                    steps {
                        dir('config-server') {
                            echo '🔨 Building Config Server...'
                            runCmd 'mvn clean package -DskipTests -B'
                        }
                    }
                }
                stage('Eureka Server') {
                    steps {
                        dir('eureka-server') {
                            echo '🔨 Building Eureka Server...'
                            runCmd 'mvn clean package -DskipTests -B'
                        }
                    }
                }
                stage('API Gateway') {
                    steps {
                        dir('api-gateway') {
                            echo '🔨 Building API Gateway...'
                            runCmd 'mvn clean package -DskipTests -B'
                        }
                    }
                }
                stage('Auth Service') {
                    steps {
                        dir('auth-service') {
                            echo '🔨 Building Auth Service...'
                            runCmd 'mvn clean package -DskipTests -B'
                        }
                    }
                }
                stage('Inventory Service') {
                    steps {
                        dir('inventory-service') {
                            echo '🔨 Building Inventory Service...'
                            runCmd 'mvn clean package -DskipTests -B'
                        }
                    }
                }
                stage('Request Service') {
                    steps {
                        dir('request-service') {
                            echo '🔨 Building Request Service...'
                            runCmd 'mvn clean package -DskipTests -B'
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────────────────
        // Stage 3: Run Unit & Integration Tests (Parallel)
        // ──────────────────────────────────────────────────
        stage('Run Tests') {
            parallel {
                stage('Auth Service Tests') {
                    steps {
                        dir('auth-service') {
                            echo '🧪 Running Auth Service tests...'
                            runCmd 'mvn test -B'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true,
                                  testResults: 'auth-service/target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Inventory Service Tests') {
                    steps {
                        dir('inventory-service') {
                            echo '🧪 Running Inventory Service tests...'
                            runCmd 'mvn test -B'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true,
                                  testResults: 'inventory-service/target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Request Service Tests') {
                    steps {
                        dir('request-service') {
                            echo '🧪 Running Request Service tests...'
                            runCmd 'mvn test -B'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true,
                                  testResults: 'request-service/target/surefire-reports/*.xml'
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────────────────
        // Stage 4: Build React Frontend
        // ──────────────────────────────────────────────────
        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    echo '🌐 Installing frontend dependencies...'
                    runCmd 'npm ci'

                    echo '🌐 Building frontend production bundle...'
                    runCmd 'npm run build'
                }
            }
        }

        // ──────────────────────────────────────────────────
        // Stage 5: Run Frontend Tests
        // ──────────────────────────────────────────────────
        stage('Frontend Tests') {
            steps {
                dir('frontend') {
                    echo '🧪 Running frontend tests...'
                    runCmd 'npm test -- --watchAll=false --ci'
                }
            }
        }

        // ──────────────────────────────────────────────────
        // Stage 6: Build Docker Images (Parallel)
        // ──────────────────────────────────────────────────
        stage('Docker Build') {
            parallel {
                stage('Docker: Config Server') {
                    steps {
                        echo '🐳 Building Config Server image...'
                        runCmd "docker build -t ${env.DOCKER_REGISTRY}/sms-config-server:${env.IMAGE_TAG} ./config-server"
                        runCmd "docker tag ${env.DOCKER_REGISTRY}/sms-config-server:${env.IMAGE_TAG} ${env.DOCKER_REGISTRY}/sms-config-server:latest"
                    }
                }
                stage('Docker: Eureka Server') {
                    steps {
                        echo '🐳 Building Eureka Server image...'
                        runCmd "docker build -t ${env.DOCKER_REGISTRY}/sms-eureka-server:${env.IMAGE_TAG} ./eureka-server"
                        runCmd "docker tag ${env.DOCKER_REGISTRY}/sms-eureka-server:${env.IMAGE_TAG} ${env.DOCKER_REGISTRY}/sms-eureka-server:latest"
                    }
                }
                stage('Docker: API Gateway') {
                    steps {
                        echo '🐳 Building API Gateway image...'
                        runCmd "docker build -t ${env.DOCKER_REGISTRY}/sms-api-gateway:${env.IMAGE_TAG} ./api-gateway"
                        runCmd "docker tag ${env.DOCKER_REGISTRY}/sms-api-gateway:${env.IMAGE_TAG} ${env.DOCKER_REGISTRY}/sms-api-gateway:latest"
                    }
                }
                stage('Docker: Auth Service') {
                    steps {
                        echo '🐳 Building Auth Service image...'
                        runCmd "docker build -t ${env.DOCKER_REGISTRY}/sms-auth-service:${env.IMAGE_TAG} ./auth-service"
                        runCmd "docker tag ${env.DOCKER_REGISTRY}/sms-auth-service:${env.IMAGE_TAG} ${env.DOCKER_REGISTRY}/sms-auth-service:latest"
                    }
                }
                stage('Docker: Inventory Service') {
                    steps {
                        echo '🐳 Building Inventory Service image...'
                        runCmd "docker build -t ${env.DOCKER_REGISTRY}/sms-inventory-service:${env.IMAGE_TAG} ./inventory-service"
                        runCmd "docker tag ${env.DOCKER_REGISTRY}/sms-inventory-service:${env.IMAGE_TAG} ${env.DOCKER_REGISTRY}/sms-inventory-service:latest"
                    }
                }
                stage('Docker: Request Service') {
                    steps {
                        echo '🐳 Building Request Service image...'
                        runCmd "docker build -t ${env.DOCKER_REGISTRY}/sms-request-service:${env.IMAGE_TAG} ./request-service"
                        runCmd "docker tag ${env.DOCKER_REGISTRY}/sms-request-service:${env.IMAGE_TAG} ${env.DOCKER_REGISTRY}/sms-request-service:latest"
                    }
                }
                stage('Docker: Frontend') {
                    steps {
                        echo '🐳 Building Frontend image...'
                        runCmd "docker build -t ${env.DOCKER_REGISTRY}/sms-frontend:${env.IMAGE_TAG} ./frontend"
                        runCmd "docker tag ${env.DOCKER_REGISTRY}/sms-frontend:${env.IMAGE_TAG} ${env.DOCKER_REGISTRY}/sms-frontend:latest"
                    }
                }
            }
        }

        // ──────────────────────────────────────────────────
        // Stage 7: Push Docker Images to Registry
        // ──────────────────────────────────────────────────
        stage('Docker Push') {
            when {
                expression { env.HAS_CREDS == 'true' && env.HAS_REGISTRY == 'true' }
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-registry-creds', usernameVariable: 'DOCKER_CREDS_USR', passwordVariable: 'DOCKER_CREDS_PSW')]) {
                        echo '📤 Pushing Docker images to registry...'
                        runCmd "docker login -u ${DOCKER_CREDS_USR} -p ${DOCKER_CREDS_PSW} ${env.DOCKER_REGISTRY}"

                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-config-server:${env.IMAGE_TAG}"
                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-config-server:latest"

                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-eureka-server:${env.IMAGE_TAG}"
                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-eureka-server:latest"

                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-api-gateway:${env.IMAGE_TAG}"
                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-api-gateway:latest"

                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-auth-service:${env.IMAGE_TAG}"
                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-auth-service:latest"

                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-inventory-service:${env.IMAGE_TAG}"
                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-inventory-service:latest"

                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-request-service:${env.IMAGE_TAG}"
                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-request-service:latest"

                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-frontend:${env.IMAGE_TAG}"
                        runCmd "docker push ${env.DOCKER_REGISTRY}/sms-frontend:latest"
                    }
                }
            }
        }

        // ──────────────────────────────────────────────────
        // Stage 8: Deploy with Docker Compose
        // ──────────────────────────────────────────────────
        stage('Deploy') {
            steps {
                echo '🚀 Deploying Stationery Management System...'
                runCmd 'docker-compose down --remove-orphans'
                runCmd 'docker-compose up -d --build'

                echo '⏳ Waiting for services to become healthy...'
                sleepSec(60)

                echo '✅ Verifying deployment...'
                runCmd 'docker-compose ps'
            }
        }
    }

    // ──────────────────────────────────────────────────────
    // Post-Build Actions
    // ──────────────────────────────────────────────────────
    post {
        always {
            script {
                try {
                    echo '🧹 Cleaning up workspace...'
                    if (env.HAS_CREDS == 'true' && env.HAS_REGISTRY == 'true') {
                        runCmd "docker logout ${env.DOCKER_REGISTRY} || exit 0"
                    }
                    cleanWs()
                } catch (Throwable t) {
                    echo "⚠️ Could not perform workspace cleanup: ${t.getMessage()}"
                }
            }
        }
        success {
            echo '✅ ========================================='
            echo '✅  Pipeline SUCCEEDED!'
            echo '✅  Build #${BUILD_NUMBER} deployed.'
            echo '✅ ========================================='
        }
        failure {
            echo '❌ ========================================='
            echo '❌  Pipeline FAILED!'
            echo '❌  Check logs for build #${BUILD_NUMBER}.'
            echo '❌ ========================================='
        }
        unstable {
            echo 'Pipeline completed with warnings. Review test results.'
        }
    }
}

// ──────────────────────────────────────────────────────────
// Helper Methods for OS-Independent Executions
// ──────────────────────────────────────────────────────────
def runCmd(cmd) {
    if (isUnix()) {
        sh cmd
    } else {
        bat cmd
    }
}

def sleepSec(seconds) {
    if (isUnix()) {
        sh "sleep ${seconds}"
    } else {
        bat "timeout /t ${seconds} /nobreak"
    }
}
