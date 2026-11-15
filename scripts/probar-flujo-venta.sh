#!/usr/bin/env bash
# Recorrido manual de RSI-2 a RSI-6 con los datos de demostración (perfil dev, base recién creada).
#   docker compose up --build            (todo en Docker; no hace falta JDK)
#   o bien: docker compose up -d postgres  y  cd backend && ./mvnw spring-boot:run
#   bash scripts/probar-flujo-venta.sh          (en Git Bash, WSL o Linux/macOS)
# Muestra qué ve cada rol: vendedora, bodega y entregas, cada uno con su propia sesión.
BASE="${BASE:-http://localhost:8080}"
CLAVE="${CLAVE:-Demo-1234}"            # clave ficticia del perfil dev
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

xsrf() { awk '$6=="XSRF-TOKEN"{print $7}' "$TMP/$1" | tail -1; }
paso() { printf '\n== %s\n' "$1"; }
# pedir <sesión> <método> <ruta> [cuerpo]
pedir() {
  local s="$1" m="$2" r="$3" b="${4:-}"
  curl -s -b "$TMP/$s" -c "$TMP/$s" -X "$m" "$BASE$r" \
    -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $(xsrf "$s")" \
    ${b:+-d "$b"} -w '\n[HTTP %{http_code}]\n'
}
entrar() { pedir "$1" POST /api/auth/login "{\"correo\":\"$2\",\"contrasena\":\"$CLAVE\"}"; }

paso "1. Ana (vendedora con aval) inicia sesión y consulta la disponibilidad"
entrar ana ana@polimarket.test
PRODUCTOS=$(pedir ana GET /api/productos)
echo "$PRODUCTOS"
ARROZ=$(echo "$PRODUCTOS" | sed -n 's/.*"id":\([0-9]*\),"codigo":"ARR-01".*/\1/p')

paso "2. Busca a su cliente y registra una venta de 6 unidades de arroz (hay 10)"
CLIENTES=$(pedir ana GET /api/clientes)
echo "$CLIENTES"
CLIENTE=$(echo "$CLIENTES" | sed -n 's/.*{"id":\([0-9]*\),"terceroId":[0-9]*,"nombre":"María Pérez".*/\1/p')
VENTA=$(pedir ana POST /api/ventas "{\"clienteId\":$CLIENTE,\"direccionEntrega\":\"Calle 10 # 20-30\",\"productos\":[{\"productoId\":$ARROZ,\"cantidad\":6}]}")
echo "$VENTA"
VID=$(echo "$VENTA" | sed -n 's/.*"ventaId":\([0-9]*\).*/\1/p')

paso "3. Bodega ve 10 en físico, 6 reservados y 4 disponibles; el arroz quedó bajo su mínimo"
entrar bodega bodega@polimarket.test
pedir bodega GET /api/productos
pedir bodega GET /api/inventario/bajo-stock

paso "4. Otra venta de 5 unidades: no alcanza (409) y no queda nada a medias"
pedir ana POST /api/ventas "{\"clienteId\":$CLIENTE,\"direccionEntrega\":\"Calle 10\",\"productos\":[{\"productoId\":$ARROZ,\"cantidad\":5}]}"

paso "5. Ana cancela la venta: se libera la reserva y el físico no cambia"
pedir ana POST "/api/ventas/$VID/cancelacion"
pedir ana POST "/api/ventas/$VID/cancelacion"   # segunda vez: 409

paso "6. Nueva venta de 6, ahora Entregas la ve pendiente y la despacha"
VENTA2=$(pedir ana POST /api/ventas "{\"clienteId\":$CLIENTE,\"direccionEntrega\":\"Calle 10 # 20-30\",\"productos\":[{\"productoId\":$ARROZ,\"cantidad\":6}]}")
VID2=$(echo "$VENTA2" | sed -n 's/.*"ventaId":\([0-9]*\).*/\1/p')
entrar entregas entregas@polimarket.test
pedir entregas GET /api/ventas/pendientes-entrega
pedir entregas POST "/api/ventas/$VID2/despacho"
pedir entregas POST "/api/ventas/$VID2/despacho"   # segundo despacho: 409

paso "7. Bodega: el físico bajó de 10 a 4 y la reserva quedó consumida"
pedir bodega GET /api/productos
