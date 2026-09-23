# 借书接口并发压测脚本（验证"零超借"）
#
# 用途：用 N 个独立账号在同一时刻并发调用 POST /api/borrow/borrow，
#       验证库存条件更新（available_count > 0）在并发下不会超卖。
#
# 为什么用 N 个不同账号：借书接口有"同一用户不能重复借阅同一本书"的业务校验，
#                        同一账号并发打只会得到 1 次成功 + N-1 次业务拒绝，测不出并发扣减。
#
# 为什么要先登录再统一发压：登录耗时不一致会让请求自然错开，
#                            那样测的是"顺序请求"而不是"并发争抢"。
#
# 用法：
#   1. 先准备 N 个测试账号（用户名 loadtest1..loadtestN，密码均与下方 $Password 一致）
#   2. 重置环境：TRUNCATE TABLE borrow_record;
#                UPDATE book SET total_count = N, available_count = N WHERE id = <目标书 id>;
#   3. 运行本脚本，然后校验：
#        SELECT COUNT(*) FROM borrow_record WHERE book_id=<id> AND status=0;  -- 必须等于初始库存
#        SELECT available_count FROM book WHERE id=<id>;                      -- 必须为 0，且不得为负

param(
    [string]$BaseUrl     = "http://localhost:8080",
    [int]   $Concurrency = 200,
    [int]   $BookId      = 3,
    [string]$Password    = "123456"
)

Add-Type -AssemblyName System.Net.Http

$handler = New-Object System.Net.Http.HttpClientHandler
$handler.MaxConnectionsPerServer = 1000
$client  = New-Object System.Net.Http.HttpClient($handler)
$client.Timeout = [TimeSpan]::FromSeconds(60)

function Get-JsonCode($text) {
    try { return (ConvertFrom-Json $text).code } catch { return "?" }
}

Write-Output "===== 阶段 1：并发登录，获取 $Concurrency 个 token ====="
$swLogin = [System.Diagnostics.Stopwatch]::StartNew()
$loginTasks = @()
for ($i = 1; $i -le $Concurrency; $i++) {
    $json    = "{""username"":""loadtest$i"",""password"":""$Password""}"
    $content = New-Object System.Net.Http.StringContent($json, [System.Text.Encoding]::UTF8, "application/json")
    $loginTasks += $client.PostAsync("$BaseUrl/api/auth/login", $content)
}
[System.Threading.Tasks.Task]::WaitAll($loginTasks)

$tokens = New-Object System.Collections.ArrayList
foreach ($t in $loginTasks) {
    $body = $t.Result.Content.ReadAsStringAsync().Result
    try { [void]$tokens.Add((ConvertFrom-Json $body).data.token) } catch {}
}
$swLogin.Stop()
Write-Output ("拿到 token: " + $tokens.Count + " / " + $Concurrency + "，登录耗时 " + [Math]::Round($swLogin.Elapsed.TotalSeconds, 2) + " s")
if ($tokens.Count -lt $Concurrency) { Write-Output "token 数量不足，终止"; exit 1 }

Write-Output ""
Write-Output "===== 阶段 2：$Concurrency 并发同时借同一本书 ====="
$barrier  = New-Object System.Threading.Barrier($tokens.Count)
$requests = New-Object System.Collections.ArrayList

foreach ($tk in $tokens) {
    $content = New-Object System.Net.Http.StringContent("{""bookId"":$BookId}", [System.Text.Encoding]::UTF8, "application/json")
    $req = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, "$BaseUrl/api/borrow/borrow")
    $req.Content = $content
    $req.Headers.Add("Authorization", "Bearer $tk")
    [void]$requests.Add($req)
}

# 同步屏障：所有线程在此阻塞，凑齐后同时释放，保证真正并发
$tasks = New-Object System.Collections.ArrayList
foreach ($req in $requests) {
    [void]$tasks.Add($client.SendAsync($req))
}

$sw = [System.Diagnostics.Stopwatch]::StartNew()
$barrier.SignalAndWait()
try { [System.Threading.Tasks.Task]::WaitAll($tasks.ToArray()) } catch {}
$sw.Stop()

Write-Output ""
Write-Output "===== 阶段 3：结果统计 ====="
$ok = 0; $rejected = 0; $other = 0
foreach ($t in $tasks) {
    switch (Get-JsonCode $t.Result.Content.ReadAsStringAsync().Result) {
        200 { $ok++ }
        400 { $rejected++ }
        default { $other++ }
    }
}
$stock = $Concurrency

Write-Output ("总请求数        : " + $Concurrency)
Write-Output ("成功(code=200)  : " + $ok + "   <-- 期望等于初始库存 $stock")
Write-Output ("拒绝(code=400)  : " + $rejected)
Write-Output ("其他异常        : " + $other + "   <-- 期望 0")
Write-Output ("并发总耗时      : " + [Math]::Round($sw.Elapsed.TotalMilliseconds, 1) + " ms")

Write-Output ""
if ($ok -eq $stock -and $other -eq 0) {
    Write-Output ">>> 通过：零超借。"
} else {
    Write-Output ">>> 未通过：出现超借或异常，需排查。"
}

$client.Dispose()
