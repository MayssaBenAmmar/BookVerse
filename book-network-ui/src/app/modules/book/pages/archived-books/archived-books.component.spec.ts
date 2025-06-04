import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArchivedBooksComponent } from './archived-books.component';

describe('ArchivedBooksComponent', () => {
  let component: ArchivedBooksComponent;
  let fixture: ComponentFixture<ArchivedBooksComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ArchivedBooksComponent]
    });
    fixture = TestBed.createComponent(ArchivedBooksComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
