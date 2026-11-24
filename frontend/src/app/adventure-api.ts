import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface Book {
  id: string;
  title: string;
  author: string;
  difficulty: Difficulty;
  description: string;
}

export interface GameView {
  title: string;
  sectionId: number;
  text: string;
  health: number;
  status: 'PLAYING' | 'COMPLETED' | 'DEAD' | 'LIMIT_REACHED';
  consequence: string | null;
  choices: { index: number; description: string }[];
}

@Injectable({ providedIn: 'root' })
export class AdventureApi {
  private readonly http = inject(HttpClient);

  list() {
    return firstValueFrom(this.http.get<Book[]>('/api/books'));
  }

  play(bookId: string, choices: readonly number[]) {
    return firstValueFrom(this.http.post<GameView>(
      `/api/books/${encodeURIComponent(bookId)}/play`, { choices },
    ));
  }

  import(file: File) {
    const data = new FormData();
    data.append('book', file);
    return firstValueFrom(this.http.post<Book>('/api/books', data));
  }
}
