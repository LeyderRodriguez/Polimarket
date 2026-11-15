import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { iniciarComo, proveedoresDePrueba } from '../testing/ayudas';
import { autenticado, invitado } from './autenticado.guard';
import { conRol } from './rol.guard';

describe('guardias de sesión y de rol', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: proveedoresDePrueba() }));

  const ejecutar = (guardia: CanActivateFn) =>
    TestBed.runInInjectionContext(() => guardia({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
  const direccion = (resultado: unknown) => TestBed.inject(Router).serializeUrl(resultado as UrlTree);

  describe('autenticado', () => {
    it('manda al login a quien no inició sesión', () => {
      expect(direccion(ejecutar(autenticado))).toBe('/login');
    });

    it('deja pasar a quien ya inició sesión', async () => {
      await iniciarComo('VENDEDOR');
      expect(ejecutar(autenticado)).toBe(true);
    });
  });

  describe('invitado', () => {
    it('deja ver el login a quien no tiene sesión', () => {
      expect(ejecutar(invitado)).toBe(true);
    });

    it('lleva a su pantalla de inicio a quien ya está dentro', async () => {
      await iniciarComo('ENTREGAS');
      expect(direccion(ejecutar(invitado))).toBe('/entregas');
    });
  });

  describe('conRol', () => {
    it('deja pasar al rol permitido', async () => {
      await iniciarComo('VENDEDOR');
      expect(ejecutar(conRol('VENDEDOR'))).toBe(true);
    });

    it('devuelve a su inicio a quien tiene otro rol', async () => {
      await iniciarComo('ENTREGAS');
      expect(direccion(ejecutar(conRol('VENDEDOR')))).toBe('/entregas');
    });

    it('manda al login si no hay sesión', () => {
      expect(direccion(ejecutar(conRol('VENDEDOR')))).toBe('/login');
    });
  });
});
