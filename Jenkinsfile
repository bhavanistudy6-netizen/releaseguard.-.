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
    }

    post {
        success {
            echo 'ReleaseGuard tests and builds passed.'
        }

        failure {
            echo 'Build failed. Open Console Output for the failing command.'
        }
    }
}