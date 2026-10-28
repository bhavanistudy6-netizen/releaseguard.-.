pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
        ansiColor('xterm')
    }

    parameters {
        booleanParam(
            name: 'PUSH_IMAGES',
            defaultValue: false,
            description: 'Whether to push built images to the remote container registry'
        )
        stringParam(
            name: 'DOCKER_REGISTRY',
            defaultValue: 'docker.io/releaseguard',
            description: 'Target container registry namespace'
        )
        stringParam(
            name: 'RELEASE_GUARD_API_URL',
            defaultValue: 'http://localhost:8080',
            description: 'URL of the ReleaseGuard server for release registration evidence'
        )
    }

    environment {
        JAVA_HOME = tool(name: 'Java-21', type: 'jdk')
        SHORT_SHA = "${env.GIT_COMMIT ? env.GIT_COMMIT.take(8) : 'local'}"
        IMAGE_BACKEND = "${params.DOCKER_REGISTRY}/releaseguard-backend:${env.GIT_COMMIT ?: 'local'}"
        IMAGE_FRONTEND = "${params.DOCKER_REGISTRY}/releaseguard-frontend:${env.GIT_COMMIT ?: 'local'}"
        IMAGE_BACKEND_TAGGED = "${params.DOCKER_REGISTRY}/releaseguard-backend:${env.BRANCH_NAME ?: 'main'}-${SHORT_SHA}"
        IMAGE_FRONTEND_TAGGED = "${params.DOCKER_REGISTRY}/releaseguard-frontend:${env.BRANCH_NAME ?: 'main'}-${SHORT_SHA}"
    }

    stages {
        stage('Prerequisites Verification') {
            steps {
                echo "Validating pipeline execution environment on agent..."
                sh 'java -version'
                sh 'docker version'
                sh 'node --version || echo "Node will be executed via container or agent tool"'
            }
        }

        stage('Backend Build & Coverage Test') {
            steps {
                dir('backend') {
                    echo "Executing Maven package with unit and integration tests (JaCoCo report)..."
                    // Testcontainers requires access to Docker daemon on the Jenkins agent
                    sh './mvnw -B clean test package'
                }
            }
            post {
                always {
                    junit testResults: 'backend/target/surefire-reports/*.xml', allowEmptyResults: false
                    archiveArtifacts artifacts: 'backend/target/site/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        stage('Frontend Build & Test') {
            steps {
                dir('frontend') {
                    echo "Executing frontend dependency install, Vitest suite, and production build..."
                    sh 'npm ci'
                    sh 'npm test'
                    sh 'npm run build'
                }
            }
        }

        stage('SonarQube Analysis & Quality Gate') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    echo "Running SonarQube Scanner linked to JaCoCo coverage report..."
                    sh 'sonar-scanner -Dproject.settings=sonar-project.properties'
                }
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        echo "Evaluating SonarQube Quality Gate result..."
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Pipeline aborted: SonarQube Quality Gate failed with status ${qg.status}"
                        }
                    }
                }
            }
        }

        stage('Container Image Build & Immutable Tagging') {
            steps {
                echo "Building container images tagged with immutable commit SHA ${env.GIT_COMMIT}..."
                sh "docker build -t ${IMAGE_BACKEND} -t ${IMAGE_BACKEND_TAGGED} ./backend"
                sh "docker build -t ${IMAGE_FRONTEND} -t ${IMAGE_FRONTEND_TAGGED} ./frontend"
            }
        }

        stage('Push Images to Registry') {
            when {
                expression { return params.PUSH_IMAGES }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker-registry-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
                    sh "docker push ${IMAGE_BACKEND}"
                    sh "docker push ${IMAGE_BACKEND_TAGGED}"
                    sh "docker push ${IMAGE_FRONTEND}"
                    sh "docker push ${IMAGE_FRONTEND_TAGGED}"
                }
            }
        }

        stage('Governance Release Registration (Boundary Demonstration)') {
            steps {
                script {
                    echo "Recording release audit record in ReleaseGuard..."
                    echo "Notice: Registering a release records governance evidence; it does not deploy to cloud infrastructure."
                    /*
                    Demonstration boundary:
                    When connecting to a live ReleaseGuard instance in your cluster:
                    sh """
                    curl -s -X POST ${params.RELEASE_GUARD_API_URL}/api/v1/applications/1/releases \\
                        -H 'Content-Type: application/json' \\
                        -d '{
                            "version": "v1.0.0-${SHORT_SHA}",
                            "gitCommitSha": "${env.GIT_COMMIT}",
                            "dockerImage": "${IMAGE_BACKEND}"
                        }'
                    """
                    */
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline run completed for commit ${env.GIT_COMMIT ?: 'local'}."
        }
        failure {
            echo "Pipeline failed. Check stage logs for details."
        }
    }
}
