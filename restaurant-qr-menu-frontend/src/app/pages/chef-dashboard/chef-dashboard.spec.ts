import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChefDashboard } from './chef-dashboard';

describe('ChefDashboard', () => {
  let component: ChefDashboard;
  let fixture: ComponentFixture<ChefDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChefDashboard],
    }).compileComponents();

    fixture = TestBed.createComponent(ChefDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
