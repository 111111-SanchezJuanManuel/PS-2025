import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { EventSearchComponent } from '../event-search/event-search.component';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';
import { Subscription } from 'rxjs';
import { NotificacionService, Notificacion } from '../../services/notificacion.service';
import { NotificationBellComponent } from '../notification-bell/notification-bell.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, EventSearchComponent, NotificationBellComponent],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'],
})
export class NavbarComponent implements OnInit, OnDestroy {
  imagenUrl: string = 'assets/img/usuario.png'; 
  private subs: Subscription[] = [];
  private objectUrlToRevoke: string | null = null;
  unreadCount: number = 0;
  notificaciones: Notificacion[] = [];

  constructor(
    private router: Router,
    private usuarioService: UsuarioService,
    private authService: AuthService,
    private notificacionService: NotificacionService
  ) {}

  ngOnInit(): void {
    this.cargarImagenUsuario();

    this.subs.push(
      this.authService.avatarState$.subscribe((url) => {
        if (url) {
          this.imagenUrl = url;
        } else {
          this.imagenUrl = 'assets/img/usuario.png';
        }
      })
    );

    this.subs.push(
      this.authService.loginState$.subscribe((logged) => {
        if (logged) {
          this.cargarImagenUsuario();
        } else {
          this.imagenUrl = 'assets/img/usuario.png';
        }
      })
    );

    const usuarioId = Number(localStorage.getItem('usuarioId'));

    if (usuarioId) {
      this.usuarioService.obtenerImagen(usuarioId).subscribe({
        next: (blob) => {
          const objectURL = URL.createObjectURL(blob);
          this.imagenUrl = objectURL;
        },
        error: () => {
          this.imagenUrl = 'assets/img/usuario.png';
        },
      });

      this.notificacionService.obtenerNotificaciones(usuarioId).subscribe({
        next: (data) => {
          this.notificaciones = data;
          this.unreadCount = this.notificaciones.filter(n => !n.leido).length;
          console.log('Notificaciones cargadas:', this.notificaciones);
        },
        error: (err) => console.error('Error al cargar notificaciones:', err),
      });
    }
  }

  private cargarImagenUsuario(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;

    this.usuarioService.obtenerImagen(userId).subscribe({
      next: (blob) => {
        if (this.objectUrlToRevoke) {
          URL.revokeObjectURL(this.objectUrlToRevoke);
        }
        const objectURL = URL.createObjectURL(blob);
        this.imagenUrl = objectURL;
        this.objectUrlToRevoke = objectURL;

        this.authService.setAvatar(objectURL);
      },
      error: () => {
        this.imagenUrl = 'assets/img/usuario.png';
      },
    });
  }


  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  irAlLogin(): void {
    this.router.navigate(['/login']);
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.imagenUrl = 'assets/img/usuario.png';
    this.router.navigate(['/login']);
    this.notificaciones = [];
    this.unreadCount = 0;
  }

  abrirNotificacion(n: Notificacion) {
    console.log('Notificación abierta:', n.mensaje);

    if (!n.leido) {
      n.leido = true;
      this.unreadCount = this.notificaciones.filter(notif => !notif.leido).length;

      this.notificacionService.marcarComoLeido(n.id).subscribe({
        next: () => {
          console.log(`Notificación ${n.id} marcada como leída en el backend`);
        },
        error: (err) => {
          console.error('Error al marcar como leído:', err);
          n.leido = false;
          this.unreadCount = this.notificaciones.filter(notif => !notif.leido).length;
        },
      });
    }
  }

  verPerfil(): void {
    this.router.navigate(['/perfil']);
  }

  ngOnDestroy(): void {
    if (this.objectUrlToRevoke) {
      URL.revokeObjectURL(this.objectUrlToRevoke);
    }
    this.subs.forEach((s) => s.unsubscribe());
  }
}

