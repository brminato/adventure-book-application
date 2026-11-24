import { TestBed } from '@angular/core/testing';
import { AdventureApi, Book } from './adventure-api';
import { App } from './app';

const reef: Book = {
  id: 'reef', title: 'Lantern Reef', author: 'Adventure Book', difficulty: 'EASY',
  description: 'A boat is stranded.',
};

describe('Adventure library', () => {
  beforeEach(() => localStorage.clear());

  it('filters books by search and difficulty', async () => {
    TestBed.configureTestingModule({
      providers: [{ provide: AdventureApi, useValue: { list: async () => [
        reef,
        { ...reef, id: 'signal', title: 'The Last Signal', difficulty: 'HARD' },
      ] } }],
    });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const app = fixture.componentInstance;
    expect(app.filteredBooks()).toHaveLength(2);
    app.query.set(' lantern ');
    expect(app.filteredBooks()).toEqual([reef]);
    app.difficulty.set('HARD');
    expect(app.filteredBooks()).toEqual([]);
  });

  it('saves a run and can resume it after recreating the component', async () => {
    const play = vi.fn().mockResolvedValue({
      title: reef.title, sectionId: 2, text: 'The boat is near.', health: 4,
      status: 'PLAYING', consequence: null, choices: [],
    });
    TestBed.configureTestingModule({
      providers: [{ provide: AdventureApi, useValue: { list: async () => [reef], play } }],
    });
    const fixture = TestBed.createComponent(App);
    await fixture.componentInstance.play(reef, [1]);
    fixture.componentInstance.save();
    fixture.destroy();
    const restored = TestBed.createComponent(App).componentInstance;
    await restored.play(reef, restored.saves()[reef.id]);
    expect(play).toHaveBeenLastCalledWith(reef.id, [1]);
    expect(restored.game()?.view.health).toBe(4);
  });
});
