import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import type { ErrorApi } from '../../core/api/tipos';
import { inicioDe } from '../../core/auth/inicio';
import { SesionServicio } from '../../core/auth/sesion.servicio';
import { aErrorApi } from '../../core/http/errores.interceptor';
import { AvisoError } from '../../shared/aviso-error/aviso-error';
import { CLAVE_DEMO, CUENTAS_DEMO, type CuentaDemo } from './cuentas-demo';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, AvisoError],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly sesion = inject(SesionServicio);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', Validators.required],
  });
  protected readonly enCurso = signal(false);
  protected readonly error = signal<ErrorApi | null>(null);

  protected readonly cuentasDemo = CUENTAS_DEMO;

  protected usarCuenta(cuenta: CuentaDemo): void {
    this.form.setValue({ correo: cuenta.correo, contrasena: CLAVE_DEMO });
    this.error.set(null);
  }

  protected invalido(campo: 'correo' | 'contrasena'): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  protected async entrar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enCurso.set(true);
    this.error.set(null);
    try {
      const { correo, contrasena } = this.form.getRawValue();
      const perfil = await this.sesion.iniciarSesion(correo.trim(), contrasena);
      await this.router.navigateByUrl(inicioDe(perfil.rol));
    } catch (fallo) {
      this.error.set(aErrorApi(fallo));
    } finally {
      this.enCurso.set(false);
    }
  }
}
