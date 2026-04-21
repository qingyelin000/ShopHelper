param(
    [string]$GatewayBaseUrl = "http://127.0.0.1:8080",
    [string]$AuthHealthUrl = "http://127.0.0.1:8081/actuator/health",
    [string]$UserHealthUrl = "http://127.0.0.1:8082/actuator/health",
    [int]$TimeoutSec = 20
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-ResponseText {
    param([Parameter(Mandatory = $true)]$Content)

    if ($Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Content)
    }

    return [string]$Content
}

function Invoke-JsonRequest {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Uri,
        [hashtable]$Headers,
        $Body
    )

    $params = @{
        Method          = $Method
        Uri             = $Uri
        UseBasicParsing = $true
        TimeoutSec      = $TimeoutSec
    }

    if ($Headers) {
        $params.Headers = $Headers
    }

    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = $Body | ConvertTo-Json -Depth 10 -Compress
    }

    try {
        $response = Invoke-WebRequest @params
        $responseText = Get-ResponseText -Content $response.Content
        return [pscustomobject]@{
            StatusCode = [int]$response.StatusCode
            RawBody    = $responseText
            Json       = $responseText | ConvertFrom-Json
        }
    } catch {
        $errorBody = ""
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            try {
                $errorBody = $reader.ReadToEnd()
            } finally {
                $reader.Dispose()
            }
        }
        throw "$Method $Uri failed. $errorBody"
    }
}

function Assert-BusinessCode {
    param(
        [Parameter(Mandatory = $true)][string]$StepName,
        [Parameter(Mandatory = $true)]$Response,
        [Parameter(Mandatory = $true)][int]$ExpectedCode
    )

    if ($Response.Json.code -ne $ExpectedCode) {
        throw "$StepName returned unexpected business code $($Response.Json.code). Raw response: $($Response.RawBody)"
    }
}

function Assert-Health {
    param(
        [Parameter(Mandatory = $true)][string]$StepName,
        [Parameter(Mandatory = $true)][string]$Uri
    )

    $response = Invoke-JsonRequest -Method "GET" -Uri $Uri
    if ($response.StatusCode -ne 200 -or $response.Json.status -ne "UP") {
        throw "$StepName is not healthy. Raw response: $($response.RawBody)"
    }
}

$gatewayHealthUrl = "$GatewayBaseUrl/actuator/health"

Assert-Health -StepName "gateway" -Uri $gatewayHealthUrl
Assert-Health -StepName "auth" -Uri $AuthHealthUrl
Assert-Health -StepName "user" -Uri $UserHealthUrl

$suffix = Get-Random -Minimum 10000000 -Maximum 99999999
$phone = "139$suffix"
$password = "Pass123"
$nickname = "smoke$suffix"

$registerResponse = Invoke-JsonRequest -Method "POST" -Uri "$GatewayBaseUrl/api/v1/users/register" -Body @{
    phone    = $phone
    password = $password
    nickname = $nickname
}
Assert-BusinessCode -StepName "register" -Response $registerResponse -ExpectedCode 200

$loginResponse = Invoke-JsonRequest -Method "POST" -Uri "$GatewayBaseUrl/api/v1/auth/login" -Body @{
    loginType = "phone"
    principal = $phone
    password  = $password
}
Assert-BusinessCode -StepName "login" -Response $loginResponse -ExpectedCode 200

$accessToken = [string]$loginResponse.Json.data.accessToken
$refreshToken = [string]$loginResponse.Json.data.refreshToken
$headers = @{
    Authorization = "Bearer $accessToken"
}

$profileResponse = Invoke-JsonRequest -Method "GET" -Uri "$GatewayBaseUrl/api/v1/users/me/profile" -Headers $headers
Assert-BusinessCode -StepName "profile" -Response $profileResponse -ExpectedCode 200

$createAddressResponse = Invoke-JsonRequest -Method "POST" -Uri "$GatewayBaseUrl/api/v1/users/me/addresses" -Headers $headers -Body @{
    receiverName  = "Smoke User"
    receiverPhone = $phone
    province      = "Shanghai"
    city          = "Shanghai"
    district      = "Pudong"
    detailAddress = "Century Avenue 100"
    postalCode    = "200120"
    isDefault     = $true
}
Assert-BusinessCode -StepName "create-address" -Response $createAddressResponse -ExpectedCode 200

$listAddressResponse = Invoke-JsonRequest -Method "GET" -Uri "$GatewayBaseUrl/api/v1/users/me/addresses" -Headers $headers
Assert-BusinessCode -StepName "list-address" -Response $listAddressResponse -ExpectedCode 200
if (@($listAddressResponse.Json.data).Count -lt 1) {
    throw "list-address returned no addresses."
}

$recommendationResponse = Invoke-JsonRequest -Method "GET" -Uri "$GatewayBaseUrl/api/v1/recommendations/me?scene=homepage&pageSize=5" -Headers $headers
Assert-BusinessCode -StepName "recommendations" -Response $recommendationResponse -ExpectedCode 200

$logoutResponse = Invoke-JsonRequest -Method "POST" -Uri "$GatewayBaseUrl/api/v1/auth/logout" -Headers $headers
Assert-BusinessCode -StepName "logout" -Response $logoutResponse -ExpectedCode 200

$refreshResponse = Invoke-JsonRequest -Method "POST" -Uri "$GatewayBaseUrl/api/v1/auth/refresh" -Body @{
    refreshToken = $refreshToken
}
Assert-BusinessCode -StepName "refresh-after-logout" -Response $refreshResponse -ExpectedCode 40101

[pscustomobject]@{
    gatewayBaseUrl      = $GatewayBaseUrl
    smokePhone          = $phone
    profileCode         = $profileResponse.Json.code
    addressId           = [string]$createAddressResponse.Json.data.id
    addressCount        = @($listAddressResponse.Json.data).Count
    recommendationCode  = $recommendationResponse.Json.code
    recommendationItems = @($recommendationResponse.Json.data.list).Count
    refreshAfterLogout  = $refreshResponse.Json.code
    result              = "PASS"
} | ConvertTo-Json -Depth 10
