import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private authUrl = 'http://localhost:8080/auth';
  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  obtenerImagen(usuarioId: number): Observable<Blob> {
    return this.http.get(`${this.authUrl}/${usuarioId}/imagen`, {
      responseType: 'blob',
    });
  }

  subirImagen(usuarioId: number, file: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<void>(`${this.authUrl}/${usuarioId}/imagen`, formData);
  }

  getUsuarioById(id: number): Observable<any> {
    return this.http.get<any>(`${this.authUrl}/usuarios/${id}`);
  }

  getGeneros(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/generos/all`);
  }

  crearArtista(data: any): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/register/art-prod`, data);
  }

  crearProductor(data: any): Observable<any> {
    return this.http.post<any>(`${this.authUrl}/register/art-prod`, data);
  }

  updateUsuarioJson(id: number, dto: any) {
  return this.http.put<void>(`${this.authUrl}/usuarios/${id}`, dto);
}

subirImagenUsuario(id: number, file: File) {
  const fd = new FormData();
  fd.append('file', file);
  return this.http.post<void>(`${this.authUrl}/${id}/imagen`, fd);
}
}

