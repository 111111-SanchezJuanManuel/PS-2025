import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioService } from '../../services/usuario.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-user-edit',
  standalone: true,
  templateUrl: './user-edit.component.html',
  imports: [CommonModule, FormsModule],
})
export class UserEditComponent implements OnInit {
  model = {
    username: '',
    email: '',
    password: '', 
  };

  nombre = '';
  representante = '';
  telefono_representante = '';
  red_social = '';
  idGenero: number | null = null;
  generos: any[] = [];

  selectedFile: File | null = null;
  imagenUrl: string | null = null;

  error = '';
  success = '';
  loading = true;

  private userId!: number;
  esArtistaProd = false; 

  constructor(
    private usuarioService: UsuarioService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const idFromToken = this.authService.getUserId();

    if (!idFromToken) {
      this.error = 'No se pudo obtener el usuario actual.';
      this.loading = false;
      return;
    }

    this.userId = Number(idFromToken);

    this.usuarioService.getUsuarioById(this.userId).subscribe({
      next: (u: any) => {
        this.model.username = u.username || '';
        this.model.email = u.email || '';

        const rol = u.rol || '';
        if (rol === 'ROL_ARTISTA' || rol === 'ROL_PRODUCTOR') {
          this.esArtistaProd = true;
        }

        this.nombre = u.nombre || '';
        this.representante = u.representante || '';
        this.telefono_representante = u.telefono_representante || '';
        this.red_social = u.red_social || '';
        this.idGenero = u.idGenero || u.genero?.id || null;

        if (u.id) {
          this.imagenUrl = `http://localhost:8080/auth/${u.id}/imagen`;
        }

        if (rol === 'ROL_ARTISTA') {
          this.usuarioService.getGeneros().subscribe({
            next: (gs) => (this.generos = gs),
          });
        }

        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'No se pudieron cargar los datos del usuario';
        this.loading = false;
      },
    });
  }

  esArtista(): boolean {
    return this.authService.tieneRol('ROL_ARTISTA');
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    this.selectedFile = file;
  }

  onSubmit() {
    this.error = '';
    this.success = '';

    const dto: any = {
      username: this.model.username,
      email: this.model.email,
      nombre: this.nombre,
      representante: this.representante,
      telefono_representante: this.telefono_representante,
      red_social: this.red_social,
      idGenero: this.idGenero,
    };

    if (this.model.password && this.model.password.trim() !== '') {
      dto.password = this.model.password;
    }

    this.usuarioService.updateUsuarioJson(this.userId, dto).subscribe({
      next: () => {
        if (this.selectedFile) {
          this.usuarioService.subirImagenUsuario(this.userId, this.selectedFile).subscribe({
            next: () => {
              this.success = 'Datos actualizados correctamente';
              this.model.password = '';
            },
            error: (err) => {
              console.error(err);
              this.success = 'Datos actualizados, pero no se pudo subir la imagen';
            },
          });
        } else {
          this.success = 'Datos actualizados correctamente';
          this.model.password = '';
        }
      },
      error: (err) => {
        console.error(err);
        this.error = 'No se pudo actualizar el usuario';
      },
    });
  }
}

