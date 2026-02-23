import { Component, OnInit } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { EventService, GeneroMusical } from '../../services/event.service';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-event-search',
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './event-search.component.html',
  styleUrl: './event-search.component.css'
})
export class EventSearchComponent implements OnInit {
  searchControl = new FormControl('');

  provinciaControl = new FormControl('');
  provincias: any[] = [];

  generoControl = new FormControl('');
  generos: GeneroMusical[] = []; 

  constructor(private eventService: EventService) {}

  ngOnInit(): void {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => this.notificar());

    this.provinciaControl.valueChanges
      .subscribe(() => this.notificar());

    this.generoControl.valueChanges
      .subscribe(() => this.notificar());

    this.cargarProvincias();
    this.cargarGeneros();
  }

  private notificar(): void {
    this.eventService.actualizarFiltros({
      busqueda: this.searchControl.value || '',
      provincia: this.provinciaControl.value || '',
      genero: this.generoControl.value || ''
      
    });
  }

  cargarProvincias(): void {
    this.eventService.getProvincias().subscribe({
      next: (data) => {
        this.provincias = data;
      },
      error: (error) => {
        console.error('Error al cargar provincias:', error);
      }
    });
  }

  cargarGeneros(): void {
    this.eventService.getGenerosMusicales().subscribe({
      next: (data) => {
        this.generos = data || [];
      },
      error: (error) => {
        console.error('Error al cargar géneros musicales:', error);
      }
    });
  }
}

