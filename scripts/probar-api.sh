#!/usr/bin/env bash
# Recorrido manual de RSI-1 contra el backend en marcha (perfil dev).
#   docker compose up -d
#   cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
#   bash scripts/probar-api.sh          (en Git Bash, WSL o Linux/macOS)
BASE="${BASE:-http://localhost:8080}"
CLAVE="${CLAVE:-Demo-1234}"            # clave ficticia del perfil dev
JAR="$(mktemp)"
trap 'rm -f "$JAR"' EXIT

xsrf() { awk '$6=="XSRF-TOKEN"{print $7}' "$JAR" | tail -1; }
paso() { printf '\n== %s\n' "$1"; }
pedir() { # metodo ruta [cuerpo]
  local m="$1" r="$2" b="${3:-}"
  curl -s -b "$JAR" -c "$JAR" -X "$m" "$BASE$r" \
    -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $(xsrf)" \
    ${b:+-d "$b"} -w '\n[HTTP %{http_code}]\n'
}

DOC="$RANDOM$RANDOM"

paso "1. Sin sesión: debe dar 401"
pedir GET /api/vendedores

paso "2. Login como Recursos Humanos"
pedir POST /api/auth/login "{\"correo\":\"rh@polimarket.test\",\"contrasena\":\"$CLAVE\"}"

paso "3. Registrar un tercero (persona)"
TERCERO=$(pedir POST /api/terceros "{\"tipo\":\"NATURAL\",\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"$DOC\",\"nombres\":\"Ana\",\"apellidos\":\"Perez\"}")
echo "$TERCERO"
TID=$(echo "$TERCERO" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)

paso "4. Crear su cuenta de vendedor (aval queda PENDIENTE)"
CUENTA=$(pedir POST /api/usuarios "{\"terceroId\":$TID,\"correo\":\"ana$DOC@polimarket.test\",\"contrasenaInicial\":\"$CLAVE\",\"rol\":\"VENDEDOR\"}")
echo "$CUENTA"
VID=$(echo "$CUENTA" | sed -n 's/.*"vendedorId":\([0-9]*\).*/\1/p')

paso "5. Consultar su autorización (autorizado: false)"
pedir GET "/api/vendedores/$VID/autorizacion"

paso "6. Aprobar el aval"
pedir POST "/api/vendedores/$VID/aval/aprobar" '{"observacion":"Documentacion revisada"}'

paso "7. Consultar de nuevo (autorizado: true)"
pedir GET "/api/vendedores/$VID/autorizacion"

paso "8. Aprobar otra vez: debe dar 409"
pedir POST "/api/vendedores/$VID/aval/aprobar" '{}'

paso "9. Listar vendedores"
pedir GET /api/vendedores

paso "10. Cerrar sesión y comprobar que ya no entra (401)"
pedir POST /api/auth/logout
pedir GET /api/auth/me
