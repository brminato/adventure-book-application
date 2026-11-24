import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AdventureApi, Book, Difficulty, GameView } from './adventure-api';

const SAVE_KEY = 'adventure-book.progress.v1';

type Saves = Record<string, number[]>;
type ActiveGame = { book: Book; choices: readonly number[]; view: GameView };

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly api = inject(AdventureApi);
  readonly books = signal<Book[]>([]);
  readonly query = signal('');
  readonly difficulty = signal<Difficulty | ''>('');
  readonly saves = signal(this.readSaves());
  readonly game = signal<ActiveGame | null>(null);
  readonly busy = signal(false);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly notice = signal('');
  readonly difficulties = ['EASY', 'MEDIUM', 'HARD'] as const;

  readonly filteredBooks = computed(() => {
    const query = this.query().trim().toLowerCase();
    const difficulty = this.difficulty();
    return this.books().filter((book) =>
      (!difficulty || book.difficulty === difficulty) &&
      `${book.title} ${book.author}`.toLowerCase().includes(query),
    );
  });

  constructor() {
    void this.loadBooks();
  }

  async loadBooks() {
    this.loading.set(true);
    this.error.set('');
    try {
      this.books.set(await this.api.list());
    } catch (error) {
      this.showError(error, 'The library could not be loaded.');
    } finally {
      this.loading.set(false);
    }
  }

  async play(book: Book, choices: readonly number[] = []) {
    await this.perform(async () => {
      const view = await this.api.play(book.id, choices);
      this.game.set({ book, view, choices });
      this.focusHeading();
    }, 'The adventure could not be loaded.');
  }

  choose(index: number) {
    const game = this.game();
    if (game?.view.status === 'PLAYING') {
      void this.play(game.book, [...game.choices, index]);
    }
  }

  save() {
    const game = this.game();
    if (!game || game.view.status !== 'PLAYING') {
      return;
    }
    this.error.set('');
    this.notice.set('');
    try {
      const saves = { ...this.saves(), [game.book.id]: [...game.choices] };
      localStorage.setItem(SAVE_KEY, JSON.stringify(saves));
      this.saves.set(saves);
      this.notice.set('Progress saved on this browser.');
    } catch {
      this.error.set('Your browser could not store your progress.');
    }
  }

  leave() {
    this.game.set(null);
    this.error.set('');
    this.notice.set('');
    this.focusHeading();
  }

  async import(input: HTMLInputElement) {
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    await this.perform(async () => {
      const book = await this.api.import(file);
      this.books.update((books) => [...books, book].sort((a, b) => a.title.localeCompare(b.title)));
      this.query.set('');
      this.difficulty.set('');
      this.notice.set(`“${book.title}” was added to the library.`);
    }, 'The book could not be imported.');
  }

  private async perform(action: () => Promise<void>, fallback: string) {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    try {
      await action();
    } catch (error) {
      this.showError(error, fallback);
    } finally {
      this.busy.set(false);
    }
  }

  private showError(error: unknown, fallback: string) {
    const detail = error instanceof HttpErrorResponse ? error.error?.detail : null;
    this.error.set(typeof detail === 'string' ? detail : fallback);
  }

  private readSaves(): Saves {
    try {
      const raw: unknown = JSON.parse(localStorage.getItem(SAVE_KEY) ?? '{}');
      if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
        return Object.fromEntries(Object.entries(raw).filter(([, choices]) =>
          Array.isArray(choices) && choices.length <= 1024 &&
          choices.every((choice) => Number.isSafeInteger(choice) && choice >= 0),
        ));
      }
    } catch {
      return {};
    }
    return {};
  }

  private focusHeading() {
    requestAnimationFrame(() => document.getElementById('page-heading')?.focus());
  }
}
