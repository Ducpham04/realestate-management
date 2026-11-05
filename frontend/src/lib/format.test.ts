import { describe, expect, it } from 'vitest';
import { formatCompactVnd, formatMonth } from './format';
import { dealTransitions } from './labels';

describe('formatCompactVnd', () => {
  it.each([
    [5_200_000_000, '5,2 tỷ'],
    [850_000_000, '850 triệu'],
    [12_500_000, '12,5 triệu'],
    [null, '—'],
  ])('formats %s as %s', (value, expected) => {
    expect(formatCompactVnd(value)).toBe(expected);
  });
});

describe('formatMonth', () => {
  it('renders a short Vietnamese month label', () => {
    expect(formatMonth('2026-03')).toBe('T3/26');
  });
});

describe('dealTransitions', () => {
  it('mirrors the backend state machine', () => {
    expect(dealTransitions.CLOSED).toEqual([]);
    expect(dealTransitions.NEGOTIATING).toContain('DEPOSITED');
    expect(dealTransitions.DEPOSITED).not.toContain('NEGOTIATING');
  });
});
