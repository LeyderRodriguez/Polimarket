/**
 * Capa HTTP: controllers y {@code FachadaPoliMarket} (patrón Facade).
 *
 * <p>No contiene reglas de negocio. Recibe la petición, toma la identidad de la sesión y delega
 * en las interfaces de los componentes. Es lo único que ven los clientes (IPoliMarket, API REST).
 */
package polimarket.fachada;
