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

        stage('Deploy') {
            steps {
                bat 'docker compose -p ducart_resume_ready --env-file .env.example up -d --no-build --wait --wait-timeout 180'
                bat 'docker compose -p ducart_resume_ready --env-file .env.example ps'
            }
        }
        
        stage('Seed Catalog') {
    steps {
        bat 'docker compose -p ducart_resume_ready --env-file .env.example exec -T mysql sh -c "mysql -u$MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE" < database/catalog-seed.sql'
    }
}

        stage('Smoke Test') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $targets = @(
                        'http://localhost:3000',
                        'http://localhost:8080/product'
                    )

                    foreach ($target in $targets) {
                        $ready = $false

                        for ($attempt = 1; $attempt -le 24; $attempt++) {
                            try {
                                $response = Invoke-WebRequest -Uri $target -UseBasicParsing -TimeoutSec 10
                                if ($response.StatusCode -eq 200) {
                                    Write-Host "$target returned HTTP 200"
                                    $ready = $true
                                    break
                                }
                            } catch {
                                Write-Host "$target is not ready (attempt $attempt of 24)"
                            }

                            Start-Sleep -Seconds 5
                        }

                        if (-not $ready) {
                            throw "$target did not return HTTP 200"
                        }
                    }
                '''
            }
        }

        
    }
}
