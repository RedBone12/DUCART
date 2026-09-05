pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend Test') {
            steps {
                dir('Ducart-Backend') {
                    bat 'mvnw.cmd clean verify -q'
                }
            }
        }

        stage('Frontend Test') {
            steps {
                dir('Ducart-Frontend') {
                    bat 'npm.cmd ci'
                    withEnv(['CI=true']) {
                        bat 'npm.cmd test -- --watchAll=false'
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                bat 'docker version'
                bat 'docker compose version'
                bat 'docker compose --env-file .env.example build backend frontend'
            }
        }
    }
}
