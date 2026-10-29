param(
    [string]$ApiBase = 'http://localhost:8080',
    [string]$MailpitBase = 'http://localhost:8025',
    [string]$DatabaseContainer = 'ducart_resume_ready-mysql-1'
)

$ErrorActionPreference = 'Stop'
$dockerPath = (Get-Command docker -CommandType Application | Select-Object -First 1).Source
$testUsername = 'smoke' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$testEmail = $testUsername + '@example.test'
$originalPassword = 'Original!' + [guid]::NewGuid().ToString('N')
$replacementPassword = 'Replacement!' + [guid]::NewGuid().ToString('N')
$created = $null

try {
    $registration = @{
        name = 'Smoke Buyer'; username = $testUsername; email = $testEmail
        phone = ('353' + (Get-Random -Minimum 100000000 -Maximum 999999999))
        password = $originalPassword
    } | ConvertTo-Json
    $created = Invoke-RestMethod ($ApiBase + '/user') -Method Post -ContentType 'application/json' -Body $registration
    $requested = Invoke-WebRequest ($ApiBase + '/user/forgot-password') -Method Post -ContentType 'application/json' `
        -Body (@{ usernameOrEmail = $testUsername } | ConvertTo-Json)
    if ($requested.StatusCode -ne 202) { throw 'Reset request was not accepted' }

    # SMTP acceptance precedes indexing in the Mailpit search API.
    $mail = $null
    for ($attempt = 0; $attempt -lt 25; $attempt++) {
        $inbox = Invoke-RestMethod ($MailpitBase + '/api/v1/messages')
        $mail = $inbox.messages | Where-Object {
            @($_.To | ForEach-Object { $_.Address }) -contains $testEmail
        } | Select-Object -First 1
        if ($mail) { break }
        Start-Sleep -Milliseconds 200
    }
    if (-not $mail) { throw 'Reset email was not captured by Mailpit' }
    $message = Invoke-RestMethod ($MailpitBase + '/api/v1/message/' + $mail.ID)
    $match = [regex]::Match($message.Text, 'token=([A-Za-z0-9_-]{43})')
    if (-not $match.Success) { throw 'Reset link missing from captured email' }
    $payload = @{ token = $match.Groups[1].Value; newPassword = $replacementPassword } | ConvertTo-Json
    $reset = Invoke-WebRequest ($ApiBase + '/user/reset-password') -Method Post -ContentType 'application/json' -Body $payload
    if ($reset.StatusCode -ne 200) { throw 'Password reset failed' }
    $replay = Invoke-WebRequest ($ApiBase + '/user/reset-password') -Method Post -ContentType 'application/json' -Body $payload -SkipHttpErrorCheck
    if ($replay.StatusCode -ne 400) { throw 'Consumed token was accepted' }
    $oldLogin = Invoke-WebRequest ($ApiBase + '/user/login') -Method Post -ContentType 'application/json' `
        -Body (@{ username = $testUsername; password = $originalPassword } | ConvertTo-Json) -SkipHttpErrorCheck
    if ($oldLogin.StatusCode -ne 401) { throw 'Old password still accepted' }
    $login = Invoke-RestMethod ($ApiBase + '/user/login') -Method Post -ContentType 'application/json' `
        -Body (@{ username = $testUsername; password = $replacementPassword } | ConvertTo-Json)
    $profile = Invoke-RestMethod ($ApiBase + '/user/' + $created.userid) -Headers @{ Authorization = ('Bearer ' + $login.token) }
    if ($profile.PSObject.Properties.Name -contains 'password') { throw 'Password leaked from profile response' }
    Write-Output 'PASS: SMTP capture, password reset, replay rejection, old password rejection, new password login, password-free profile.'
} finally {
    if ($null -ne $created) {
        # Delete only this randomly named smoke-test account and its token; retain existing users.
        $sql = "DELETE FROM password_reset_tokens WHERE user_id IN (SELECT userid FROM users WHERE username='$testUsername'); DELETE FROM users WHERE username='$testUsername';"
        $sql | & $dockerPath exec -i $DatabaseContainer sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot "$MYSQL_DATABASE"'
        if ($LASTEXITCODE -ne 0) { throw 'Smoke-test account cleanup failed' }
    }
}
