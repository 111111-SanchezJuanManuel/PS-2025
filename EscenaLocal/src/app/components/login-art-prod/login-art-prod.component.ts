import {
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  AfterViewInit
} from '@angular/core';
import { AuthRequest, AuthService, Rol } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login-art-prod',
  imports: [FormsModule, CommonModule],
  templateUrl: './login-art-prod.component.html',
  styleUrl: './login-art-prod.component.css'
})
export class LoginArtProdComponent implements OnInit, AfterViewInit {
  model: AuthRequest = { username: '', password: '', rol: '' };
  error: string | null = null;
  @ViewChild('usernameInput') usernameInput!: ElementRef<HTMLInputElement>;
  roles: Rol[] = [];

  constructor(
    private authService: AuthService,
    private usuarioService: UsuarioService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarRoles();
  }

  ngAfterViewInit(): void {
    this.focusInput();

    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.focusInput());
  }

  private focusInput(): void {
    setTimeout(() => {
      this.usernameInput?.nativeElement.focus();
    }, 0);
  }

  

    onSubmit() {
  this.error = null;

  if (!this.model.rol) {
    this.error = 'Seleccione un rol.';
    return;
  }

  this.authService.login(this.model).subscribe({
    next: (res) => {
      this.authService.saveToken(res.token);
      localStorage.setItem('usuarioId', res.userId.toString());

      const ok = this.authService.validateSelectedRole(this.model.rol);
      console.log('rol coincide?', ok);

      if (!ok) {
        this.authService.clearToken();
        localStorage.removeItem('usuarioId');
        this.error =
          'El rol seleccionado no coincide con el rol asignado al usuario.';
        return; 
      }

      this.usuarioService.obtenerImagen(res.userId).subscribe({
        next: (blob) => {
          const objectURL = URL.createObjectURL(blob);
          localStorage.setItem('imagenUsuario', objectURL);
        },
        error: () => {
          localStorage.removeItem('imagenUsuario');
        },
        complete: () => {
          this.router.navigate(['/']);
        }
      });
    },
    error: (err) => {
      console.error('Error en login:', err);
      this.error = err?.error?.message || 'Error en login';
    }
  });
}


  irAlLogin(event: Event) {
    event.preventDefault();
    this.router.navigate(['/login']);
  }

  irARegistrar() {
    this.router.navigate(['/login/art-prod/nuevo']);
  }

  cargarRoles() {
    this.authService.getRoles().subscribe({
      next: (data) => {
        this.roles = data;
      },
      error: (error) => {
        console.error('Error al cargar roles:', error);
      }
    });
  }
}

