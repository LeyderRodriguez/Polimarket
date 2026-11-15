import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { inicioDe } from './inicio';
import { SesionServicio } from './sesion.servicio';

/** Solo deja pasar a quien ya inició sesión; a los demás los manda al login. */
export const autenticado: CanActivateFn = () => {
  const router = inject(Router);
  return inject(SesionServicio).autenticado() ? true : router.createUrlTree(['/login']);
};

/** Para el login: quien ya está dentro no tiene por qué verlo, va a su pantalla de inicio. */
export const invitado: CanActivateFn = () => {
  const router = inject(Router);
  const usuario = inject(SesionServicio).usuario();
  return usuario ? router.createUrlTree([inicioDe(usuario.rol)]) : true;
};
