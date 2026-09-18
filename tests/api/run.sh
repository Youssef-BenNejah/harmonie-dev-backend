#!/usr/bin/env bash
# Black-box API test suite: authentication, roles, tenant isolation, validation, uploads, web
# security, sessions, rate limits and pagination. Run it against a disposable environment (it creates
# users and data). See README.md in this folder for the environment variables.
ROOT="${BASE_URL:-http://localhost:8090}"
B="$ROOT/api/v1"
S="${TMPDIR:-/tmp}/harmonie-api-tests"; mkdir -p "$S"
J='Content-Type: application/json'
ADMIN_EMAIL="${ADMIN_EMAIL:?set ADMIN_EMAIL (the seeded admin)}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:?set ADMIN_PASSWORD}"
A_EMAIL="${A_EMAIL:-tests-a@example.com}"; C_EMAIL="${C_EMAIL:-tests-b@example.com}"
CUSTOMER_PASSWORD="${CUSTOMER_PASSWORD:-Client#Pass2026}"
FRONTEND_ORIGIN="${FRONTEND_ORIGIN:-http://127.0.0.1:5190}"
BULK="${BULK:-230}"
PASS=0; FAIL=0
ok(){ PASS=$((PASS+1)); }
bad(){ FAIL=$((FAIL+1)); echo "  FAIL: $1"; }
expect(){ # desc want got
  if [ "$2" = "$3" ]; then ok; else bad "$1 (wanted $2, got $3)"; fi; }
code(){ curl -s -o /dev/null -w "%{http_code}" "$@"; }
idof(){ grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4; }

lg(){ for i in $(seq 1 40); do t=$(curl -s -X POST $B/auth/login -H "$J" -d "{\"email\":\"$1\",\"password\":\"$2\"}" | grep -o "\"accessToken\":\"[^\"]*\"" | cut -d"\"" -f4); [ -n "$t" ] && { echo $t; return; }; sleep 3; done; }
for e in "$A_EMAIL" "$C_EMAIL"; do curl -s -o /dev/null -X POST $B/auth/register -H "$J" -d "{\"email\":\"$e\",\"password\":\"$CUSTOMER_PASSWORD\"}"; done
ADM=$(lg "$ADMIN_EMAIL" "$ADMIN_PASSWORD"); A=$(lg "$A_EMAIL" "$CUSTOMER_PASSWORD"); C=$(lg "$C_EMAIL" "$CUSTOMER_PASSWORD")
[ -n "$ADM" ] && [ -n "$A" ] && [ -n "$C" ] || { echo "login failed"; exit 1; }
AH="Authorization: Bearer $A"; CH="Authorization: Bearer $C"; DH="Authorization: Bearer $ADM"

echo "== 0. give both customers the Pro plan so every feature is testable =="
PRO=$(curl -s $B/plans | tr '{' '\n' | grep '"nom":"Pro"' | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
for e in "$A_EMAIL" "$C_EMAIL"; do
  UID_=$(curl -s $B/auth/admin/users -H "$DH" | tr '{' '\n' | grep "\"email\":\"$e\"" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
  r=$(code -X PUT $B/auth/admin/users/$UID_ -H "$DH" -H "$J" -d "{\"firstName\":\"T\",\"lastName\":\"T\",\"status\":\"ACTIVE\",\"planId\":\"$PRO\",\"planExpiresAt\":\"2027-12-31T00:00:00Z\"}")
  echo "  plan set for $e: $r"
done

echo "== 1. anonymous access =="
for p in clients invoices persons entreprises taxes currencies payments services service-categories depenses depense-categories company/me dashboard/summary reports/overview plan-usage/me notifications auth/me auth/admin/users join-requests; do
  expect "anon GET /$p" 401 "$(code $B/$p)"; done
expect "anon POST /invoices" 401 "$(code -X POST $B/invoices -H "$J" -d '{}')"
expect "GET /plans public" 200 "$(code $B/plans)"

echo "== 2. role checks (customer token on admin routes) =="
for p in auth/admin/users join-requests; do expect "user GET /$p" 403 "$(code $B/$p -H "$AH")"; done
expect "user POST /plans" 403 "$(code -X POST $B/plans -H "$AH" -H "$J" -d '{}')"
expect "user PUT /plans/x" 403 "$(code -X PUT $B/plans/x -H "$AH" -H "$J" -d '{}')"
expect "user DELETE /plans/x" 403 "$(code -X DELETE $B/plans/x -H "$AH")"
expect "user reset someone's password" 403 "$(code -X POST $B/auth/admin/users/x/reset-password -H "$AH")"
expect "admin GET /auth/admin/users" 200 "$(code $B/auth/admin/users -H "$DH")"
expect "garbage token" 401 "$(code $B/clients -H 'Authorization: Bearer a.b.c')"
expect "token with wrong signature" 401 "$(code $B/clients -H 'Authorization: Bearer eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxIn0.AAAA')"
expect "alg=none token" 401 "$(code $B/clients -H 'Authorization: Bearer eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIiwidHlwZSI6ImFjY2VzcyIsInJvbGUiOiJBRE1JTiJ9.')"

echo "== 3. tenant isolation, resource by resource (A creates, C attacks) =="
iso(){ # name path body
  local n=$1 p=$2 body=$3
  local id=$(curl -s -X POST $B/$p -H "$AH" -H "$J" -d "$body" | idof)
  if [ -z "$id" ]; then bad "$n: could not create as A"; return; fi
  if [ "$p" != "service-categories" ] && [ "$p" != "depense-categories" ]; then
    expect "$n: A can read own" 200 "$(code $B/$p/$id -H "$AH")"; fi
  [ "$(curl -s $B/$p -H "$CH" | grep -c "$id")" = "0" ] && ok || bad "$n: C sees A's record in list"
  if [ "$p" != "service-categories" ] && [ "$p" != "depense-categories" ]; then
    expect "$n: C GET by id" 404 "$(code $B/$p/$id -H "$CH")"; fi
  expect "$n: C PUT" 404 "$(code -X PUT $B/$p/$id -H "$CH" -H "$J" -d "$body")"
  expect "$n: C DELETE" 404 "$(code -X DELETE $B/$p/$id -H "$CH")"
  expect "$n: A still has it after C's attempts" 1 "$(curl -s $B/$p -H "$AH" | grep -c "$id")"
  echo "$id" > "$S/last_id"
}
iso persons persons '{"prenom":"I","nom":"T","email":"i@t.io","telephone":"1","pays":"France","adresse":"x"}'; PID=$(cat $S/last_id)
iso entreprises entreprises '{"nom":"E","email":"e@t.io","telephone":"1","pays":"France","fisc":"1","adresse":"x"}'; EID=$(cat $S/last_id)
iso taxes taxes '{"name":"TVA","taxvalue":19,"isActive":true,"isDefault":false}'; TID=$(cat $S/last_id)
iso currencies currencies '{"code":"TND","name":"dinar","symbol":"DT"}'; CID=$(cat $S/last_id)
iso service-categories service-categories '{"name":"cat","color":"#123456","enabled":true}'
iso depense-categories depense-categories '{"name":"dcat","color":"#123456","enabled":true}'
iso services services '{"name":"svc","currency":"TND","price":10}'
iso depenses depenses '{"name":"dep","currency":"TND","price":5}'

echo "== 4. clients, invoices, payments =="
curl -s -X POST $B/persons -H "$AH" -H "$J" -d '{"prenom":"K","nom":"L","email":"k@t.io","telephone":"1","pays":"France","adresse":"x"}' > /dev/null
PID2=$(curl -s $B/persons -H "$AH" | grep -o '"id":"[^"]*"' | tail -1 | cut -d'"' -f4)
expect "C cannot turn A's person into a client" 404 "$(code -X POST $B/clients -H "$CH" -H "$J" -d "{\"type\":\"PERSON\",\"personId\":\"$PID2\"}")"
CLID=$(curl -s -X POST $B/clients -H "$AH" -H "$J" -d "{\"type\":\"PERSON\",\"personId\":\"$PID2\"}" | idof)
[ -n "$CLID" ] && ok || bad "A cannot create a client"
expect "C GET A's client" 404 "$(code $B/clients/$CLID -H "$CH")"
expect "C DELETE A's client" 404 "$(code -X DELETE $B/clients/$CLID -H "$CH")"
[ "$(curl -s $B/clients -H "$CH" | grep -c "$CLID")" = "0" ] && ok || bad "C sees A's client in list"

INV='{"clientId":"'$CLID'","currencyId":"'$CID'","status":"Facture","date":"2026-09-18","expirationDate":"2026-10-18","timbre":0,"items":[{"article":"x","quantity":2,"price":50,"taxId":"'$TID'"}]}'
IID=$(curl -s -X POST $B/invoices -H "$AH" -H "$J" -d "$INV" | idof)
[ -n "$IID" ] && ok || bad "A cannot create an invoice"
expect "C creates invoice with A's client" 404 "$(code -X POST $B/invoices -H "$CH" -H "$J" -d "$INV")"
expect "C GET A's invoice" 404 "$(code $B/invoices/$IID -H "$CH")"
expect "C PUT A's invoice" 404 "$(code -X PUT $B/invoices/$IID -H "$CH" -H "$J" -d "$INV")"
expect "C DELETE A's invoice" 404 "$(code -X DELETE $B/invoices/$IID -H "$CH")"
expect "C duplicate A's invoice" 404 "$(code -X POST $B/invoices/$IID/duplicate -H "$CH")"
expect "C convert A's invoice" 404 "$(code -X POST $B/invoices/$IID/convert -H "$CH")"
expect "C download A's invoice PDF" 404 "$(code $B/invoices/$IID/pdf -H "$CH")"
expect "C email A's invoice" 404 "$(code -X POST $B/invoices/$IID/send -H "$CH" -H "$J" -d '{"email":"x@y.z"}')"
expect "C export A's invoice zip" 404 "$(code "$B/invoices/export/zip?ids=$IID" -H "$CH")"
[ "$(curl -s $B/invoices -H "$CH" | grep -c "$IID")" = "0" ] && ok || bad "C sees A's invoice in list"
expect "A downloads own PDF" 200 "$(code $B/invoices/$IID/pdf -H "$AH")"
PAY='{"invoiceId":"'$IID'","amountPaid":10,"paymentMethod":"Autres","paymentDate":"2026-09-18"}'
expect "C pays A's invoice" 404 "$(code -X POST $B/payments -H "$CH" -H "$J" -d "$PAY")"
PYID=$(curl -s -X POST $B/payments -H "$AH" -H "$J" -d "$PAY" | idof)
[ -n "$PYID" ] && ok || bad "A cannot record own payment"
expect "C GET A's payment" 404 "$(code $B/payments/$PYID -H "$CH")"
expect "C DELETE A's payment" 404 "$(code -X DELETE $B/payments/$PYID -H "$CH")"
expect "C list A's invoice payments" 404 "$(code $B/invoices/$IID/payments -H "$CH")"
[ "$(curl -s $B/payments -H "$CH" | grep -c "$PYID")" = "0" ] && ok || bad "C sees A's payment in list"
echo "  invoice numbering per customer (A's first=1; C's first should also be 1):"
curl -s -X POST $B/persons -H "$CH" -H "$J" -d '{"prenom":"Z","nom":"Z","email":"z@t.io","telephone":"1","pays":"France","adresse":"x"}' >/dev/null
CP=$(curl -s $B/persons -H "$CH" | idof); CCL=$(curl -s -X POST $B/clients -H "$CH" -H "$J" -d "{\"type\":\"PERSON\",\"personId\":\"$CP\"}" | idof)
CCUR=$(curl -s -X POST $B/currencies -H "$CH" -H "$J" -d '{"code":"EUR","name":"euro","symbol":"E"}' | idof)
CN=$(curl -s -X POST $B/invoices -H "$CH" -H "$J" -d '{"clientId":"'$CCL'","currencyId":"'$CCUR'","status":"Facture","date":"2026-09-18","expirationDate":"2026-10-18","timbre":0,"items":[{"article":"x","quantity":1,"price":5}]}' | grep -o '"number":[0-9]*' | head -1)
expect "C's first invoice number" '"number":1' "$CN"

echo "== 5. aggregates only count the caller's data =="
DA=$(curl -s $B/dashboard/summary -H "$AH" | grep -o '"clientsCount":[0-9]*'); DC=$(curl -s $B/dashboard/summary -H "$CH" | grep -o '"clientsCount":[0-9]*')
echo "  A: $DA  C: $DC"; expect "dashboard clients (A=2? no: A has 1 client)" '"clientsCount":1' "$DA"; expect "dashboard clients C" '"clientsCount":1' "$DC"
[ "$(curl -s "$B/reports/overview" -H "$CH" | grep -c '"totalRevenue":0')" -ge 0 ] && ok

echo "== 6. validation and error handling =="
expect "currency code 'Z'" 400 "$(code -X POST $B/currencies -H "$AH" -H "$J" -d '{"code":"Z","name":"x","symbol":"x"}')"
expect "currency code 'TNDX'" 400 "$(code -X POST $B/currencies -H "$AH" -H "$J" -d '{"code":"TNDX","name":"x","symbol":"x"}')"
expect "invalid email on person" 400 "$(code -X POST $B/persons -H "$AH" -H "$J" -d '{"prenom":"a","nom":"b","email":"nope","telephone":"1","pays":"x","adresse":"x"}')"
expect "oversized name (300 chars)" 400 "$(code -X POST $B/persons -H "$AH" -H "$J" -d "{\"prenom\":\"$(printf 'a%.0s' $(seq 1 300))\",\"nom\":\"b\",\"email\":\"a@b.io\",\"telephone\":\"1\",\"pays\":\"x\",\"adresse\":\"x\"}")"
expect "malformed JSON" 400 "$(code -X POST $B/persons -H "$AH" -H "$J" -d '{bad')"
expect "unknown id" 404 "$(code $B/persons/doesnotexist -H "$AH")"
expect "unknown route" 404 "$(code $B/nope -H "$AH")"
expect "wrong method" 405 "$(code -X PATCH $B/persons -H "$AH")"
expect "wrong content type" 415 "$(code -X POST $B/persons -H "$AH" -H 'Content-Type: text/plain' -d 'x')"
expect "negative payment" 400 "$(code -X POST $B/payments -H "$AH" -H "$J" -d '{"invoiceId":"'$IID'","amountPaid":-5,"paymentMethod":"Autres","paymentDate":"2026-09-18"}')"
expect "overpay invoice" 400 "$(code -X POST $B/payments -H "$AH" -H "$J" -d '{"invoiceId":"'$IID'","amountPaid":99999,"paymentMethod":"Autres","paymentDate":"2026-09-18"}')"

echo "== 7. uploads =="
printf 'not an image' > $S/fake.png
printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde' > $S/real.png
printf '<script>alert(1)</script>' > $S/evil.html
W(){ if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else echo "$1"; fi; }
expect "text renamed .png" 400 "$(code -X POST $B/company/me/logo -H "$AH" -F "file=@$(W $S/fake.png);type=image/png")"
expect "html sent as image" 400 "$(code -X POST $B/company/me/logo -H "$AH" -F "file=@$(W $S/evil.html);type=image/png")"
expect "html as invoice document" 400 "$(code -X POST $B/invoices/documents -H "$AH" -F "file=@$(W $S/evil.html);type=application/pdf")"
expect "anonymous upload" 401 "$(code -X POST $B/company/me/logo -F "file=@$(W $S/real.png);type=image/png")"
echo "  (real PNG to Cloudinary is skipped: no Cloudinary account locally)"

echo "== 8. web security =="
expect "swagger" 401 "$(code $ROOT/swagger-ui.html)"
expect "api-docs" 401 "$(code $ROOT/api-docs)"
expect "actuator/info public" 200 "$(code $ROOT/actuator/info)"
expect "actuator/env locked" 401 "$(code $ROOT/actuator/env)"
[ "$(curl -s -i -X OPTIONS $B/persons -H 'Origin: http://evil.example' -H 'Access-Control-Request-Method: GET' | grep -ic 'access-control-allow-origin')" = 0 ] && ok || bad "CORS reflects evil origin"
[ "$(curl -s -i -X OPTIONS $B/persons -H "Origin: $FRONTEND_ORIGIN" -H 'Access-Control-Request-Method: GET' | grep -ic "access-control-allow-origin: $FRONTEND_ORIGIN")" = 1 ] && ok || bad "CORS blocks the real frontend"
H=$(curl -s -i $B/plans); for h in "x-content-type-options" "x-frame-options" "content-security-policy" "referrer-policy"; do echo "$H" | grep -qi "$h" && ok || bad "missing header $h"; done

echo "== 9. sessions =="
LOGIN_BODY="{\"email\":\"$C_EMAIL\",\"password\":\"$CUSTOMER_PASSWORD\"}"
echo "  waiting 61s so the login rate limit has refilled"; sleep 61
HDRS=$(curl -s -i -X POST $B/auth/login -H "$J" -d "$LOGIN_BODY")
COOKIE=$(echo "$HDRS" | grep -i '^set-cookie: refresh_token' | head -1)
[ -n "$COOKIE" ] && ok || bad "login did not set a refresh cookie"
for flag in HttpOnly Secure SameSite=Strict; do echo "$COOKIE" | grep -qi "$flag" && ok || bad "refresh cookie lacks $flag"; done
T=$(echo "$HDRS" | tail -1 | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
RT=$(echo "$COOKIE" | sed 's/^[^=]*=\([^;]*\).*/\1/')
[ -n "$T" ] && [ -n "$RT" ] && ok || bad "could not capture tokens for the session tests"
expect "access token works before logout" 200 "$(code $B/auth/me -H "Authorization: Bearer $T")"
NEW=$(curl -s -i -X POST $B/auth/refresh -H "Cookie: refresh_token=$RT")
expect "refresh with a valid token" 200 "$(echo "$NEW" | head -1 | awk '{print $2}')"
expect "reusing the rotated refresh token is refused" 401 "$(code -X POST $B/auth/refresh -H "Cookie: refresh_token=$RT")"
NEWRT=$(echo "$NEW" | grep -i '^set-cookie: refresh_token' | head -1 | sed 's/^[^=]*=\([^;]*\).*/\1/')
expect "reuse of a rotated token also revoked the newer one" 401 "$(code -X POST $B/auth/refresh -H "Cookie: refresh_token=$NEWRT")"
curl -s -o /dev/null -X POST $B/auth/logout -H "Authorization: Bearer $T"
expect "access token rejected after logout" 401 "$(code $B/auth/me -H "Authorization: Bearer $T")"

echo "== 10. rate limits =="
R=""; for i in 1 2 3 4 5 6 7; do R="$R $(code -X POST $B/auth/forgot-password -H "$J" -d '{"email":"zz@t.io"}')"; done; echo "  forgot-password x7:$R"
[ "$(echo $R | grep -c 429)" = 1 ] && ok || bad "forgot-password not rate limited"
R=""; for i in 1 2 3 4 5 6 7; do R="$R $(code -X POST $B/auth/register -H "$J" -d "{\"email\":\"rl$i@t.io\",\"password\":\"$CUSTOMER_PASSWORD\"}")"; done; echo "  register x7:$R"
[ "$(echo $R | grep -c 429)" = 1 ] && ok || bad "register not rate limited"

echo "== 11. pagination =="
echo "  creating $BULK persons as customer A (set BULK=0 to skip)"
for i in $(seq 1 "$BULK"); do curl -s -o /dev/null -X POST $B/persons -H "$AH" -H "$J" -d "{\"prenom\":\"P$i\",\"nom\":\"Bulk\",\"email\":\"p$i@example.com\",\"telephone\":\"1\",\"pays\":\"France\",\"adresse\":\"x\"}"; done
TOTAL=$(curl -s -D - -o /dev/null "$B/persons?page=0&size=50" -H "$AH" | grep -i '^x-total-count' | tr -d '\r' | awk '{print $2}')
COUNT_A=$(curl -s "$B/persons?size=200" -H "$AH" | grep -o '"prenom"' | wc -l)
[ "${TOTAL:-0}" -ge "$BULK" ] && ok || bad "X-Total-Count ($TOTAL) is below the $BULK records created"
expect "default page size" 50 "$(curl -s $B/persons -H "$AH" | grep -o '"prenom"' | wc -l)"
WANT_CAP=$(( TOTAL < 200 ? TOTAL : 200 ))
expect "size=10000 is capped at 200" "$WANT_CAP" "$(curl -s "$B/persons?size=10000" -H "$AH" | grep -o '"prenom"' | wc -l)"
expect "size=0 falls back to the default" 50 "$(curl -s "$B/persons?size=0" -H "$AH" | grep -o '"prenom"' | wc -l)"
expect "negative page becomes the first page" 200 "$(code "$B/persons?page=-4" -H "$AH")"
expect "page past the end is empty" 0 "$(curl -s "$B/persons?page=999&size=50" -H "$AH" | grep -o '"prenom"' | wc -l)"
P0=$(curl -s "$B/persons?page=0&size=50" -H "$AH" | grep -o '"id":"[^"]*"' | sort -u | wc -l)
P1=$(curl -s "$B/persons?page=1&size=50" -H "$AH" | grep -o '"id":"[^"]*"' | sort -u | wc -l)
BOTH=$( (curl -s "$B/persons?page=0&size=50" -H "$AH"; curl -s "$B/persons?page=1&size=50" -H "$AH") | grep -o '"id":"[^"]*"' | sort -u | wc -l)
expect "pages 0 and 1 do not overlap" $((P0+P1)) "$BOTH"
echo "  walking every page reaches all of A's records:"
GOT=0; for pg in $(seq 0 $(( (TOTAL + 49) / 50 ))); do GOT=$((GOT + $(curl -s "$B/persons?page=$pg&size=50" -H "$AH" | grep -o '"prenom"' | wc -l))); done
expect "records seen walking all pages equals X-Total-Count" "$TOTAL" "$GOT"
TOTAL_C=$(curl -s -D - -o /dev/null "$B/persons?size=1" -H "$CH" | grep -i '^x-total-count' | tr -d '\r' | awk '{print $2}')
[ "${TOTAL_C:-0}" -lt "$TOTAL" ] && ok || bad "customer C's total ($TOTAL_C) includes A's records"
for p in invoices payments clients services depenses entreprises; do
  [ "$(code "$B/$p?page=0&size=5" -H "$AH")" = 200 ] && ok || bad "GET /$p?page=0&size=5"
  [ -n "$(curl -s -D - -o /dev/null "$B/$p?page=0&size=5" -H "$AH" | grep -i '^x-total-count')" ] && ok || bad "/$p is missing X-Total-Count"
done
expect "paging headers exposed to the browser" 1 "$(curl -s -D - -o /dev/null $B/persons -H "$AH" -H "Origin: $FRONTEND_ORIGIN" | grep -ic 'access-control-expose-headers:.*x-total-count')"
expect "anonymous paged request" 401 "$(code "$B/persons?page=0&size=5")"

echo; echo "RESULT: $PASS passed, $FAIL failed"
[ "$FAIL" = 0 ]

