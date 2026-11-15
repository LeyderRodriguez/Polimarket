import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { SesionServicio } from './core/auth/sesion.servicio';
import { erroresInterceptor } from './core/http/errores.interceptor';
import { TemaServicio } from './core/tema/tema.servicio';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // Rutas relativas: la cookie de sesión y el token CSRF viajan solos (ver proxy.conf.json).
    provideHttpClient(withInterceptors([erroresInterceptor])),
    // Antes de pintar nada, aplica el tema que la persona eligió la última vez (así no hay un destello del otro tema).
    provideAppInitializer(() => {
      inject(TemaServicio);
    }),
    // Antes de mostrar nada, recupera la sesión si la cookie sigue viva (así recargar la página no cierra la sesión).
    provideAppInitializer(() => inject(SesionServicio).restaurar()),
  ],
};
