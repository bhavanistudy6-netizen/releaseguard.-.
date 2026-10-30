pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                checkout scm
            }
        }

        stage('Verify Tools') {
            steps {
                script {
                    if (isUnix()) {
                        error 'This pipeline requires a Windows agent.'
                    }
                }

                bat 'java -version'
                bat 'git --version'
                bat 'call node --version'
                bat 'call npm --version'
                bat 'docker version'
            }
        }

        stage('Backend Tests and Package') {
            steps {
                dir('backend') {
                    bat 'call mvnw.cmd -B -ntp clean verify'
                }
            }

            post {
                always {
                    junit(
                        testResults: 'backend/target/surefire-reports/TEST-*.xml',
                        allowEmptyResults: false
                    )
                }

                success {
                    archiveArtifacts(
                        artifacts: 'backend/target/*.jar',
                        fingerprint: true
                    )
                }
            }
        }

        stage('Verify Backend Coverage') {
            steps {
                script {
                    if (!fileExists(
                            'backend/target/site/jacoco/jacoco.xml')) {
                        error(
                            'JaCoCo XML report is missing. ' +
                            'Ensure backend/pom.xml contains the ' +
                            'JaCoCo configuration and is committed.'
                        )
                    }
                }

                archiveArtifacts(
                    artifacts: 'backend/target/site/jacoco/**',
                    fingerprint: true
                )
            }
        }

        stage('Frontend Tests') {
            steps {
                dir('frontend') {
                    bat 'call npm ci'
                    bat 'call npm test'
                }
            }
        }

        stage('Frontend Production Build') {
            steps {
                dir('frontend') {
                    bat 'call npm run build'
                }

                archiveArtifacts(
                    artifacts: 'frontend/dist/**',
                    fingerprint: true
                )
            }
        }

        stage('SonarQube Analysis and Quality Gate') {
            steps {
                script {
                    def scannerHome = tool 'SonarScanner'

                    withEnv([
                        "SONAR_SCANNER_HOME=${scannerHome}"
                    ]) {
                        withSonarQubeEnv('SonarQube') {
                            withCredentials([
                                string(
                                    credentialsId: 'sonarqube-token',
                                    variable: 'SONAR_TOKEN'
                                )
                            ]) {
                                bat '''
                                    @call "%SONAR_SCANNER_HOME%\\bin\\sonar-scanner.bat" -Dsonar.qualitygate.wait=true -Dsonar.qualitygate.timeout=300
                                '''
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'ReleaseGuard builds, tests, and SonarQube quality gate passed.'
        }

        unstable {
            echo 'Pipeline is unstable. Review the published test results.'
        }

        failure {
            echo 'Pipeline failed. Check the first failing stage in Console Output.'
        }

        aborted {
            echo 'Pipeline was cancelled or exceeded its timeout.'
        }
    }
}