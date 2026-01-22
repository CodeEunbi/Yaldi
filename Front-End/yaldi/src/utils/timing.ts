/**
 * 타이밍 유틸리티 함수
 * - throttle: 일정 시간 간격으로만 함수 실행
 * - debounce: 마지막 호출 후 일정 시간 후에 함수 실행
 */

/**
 * Throttle 함수
 * 지정된 시간 간격으로만 함수를 실행합니다.
 *
 * @param func - 실행할 함수
 * @param delay - 시간 간격 (ms)
 * @returns throttled 함수
 *
 * @example
 * const throttledSave = throttle(() => saveData(), 1000);
 * // 1초에 한 번만 실행됨
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function throttle<T extends (...args: any[]) => void>(
  func: T,
  delay: number
): (...args: Parameters<T>) => void {
  let lastCall = 0;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return function (this: any, ...args: Parameters<T>) {
    const now = Date.now();

    if (now - lastCall >= delay) {
      lastCall = now;
      func.apply(this, args);
    }
  };
}

/**
 * Debounce 함수
 * 마지막 호출 후 지정된 시간이 지나면 함수를 실행합니다.
 *
 * @param func - 실행할 함수
 * @param delay - 대기 시간 (ms)
 * @returns debounced 함수
 *
 * @example
 * const debouncedSearch = debounce(() => search(), 500);
 * // 입력이 멈춘 후 500ms 후에 실행됨
 */
export function debounce<T extends (...args: unknown[]) => void>(
  func: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;

  return function (this: unknown, ...args: Parameters<T>) {
    if (timeoutId !== null) {
      clearTimeout(timeoutId);
    }

    timeoutId = setTimeout(() => {
      func.apply(this, args);
      timeoutId = null;
    }, delay);
  };
}

/**
 * Throttle with trailing call
 * 마지막 호출도 실행되도록 보장합니다.
 */
export function throttleWithTrailing<T extends (...args: unknown[]) => void>(
  func: T,
  delay: number
): (...args: Parameters<T>) => void {
  let lastCall = 0;
  let timeoutId: ReturnType<typeof setTimeout> | null = null;

  return function (this: unknown, ...args: Parameters<T>) {
    const now = Date.now();

    // 즉시 실행
    if (now - lastCall >= delay) {
      lastCall = now;
      func.apply(this, args);

      // 대기 중인 trailing call 취소
      if (timeoutId !== null) {
        clearTimeout(timeoutId);
        timeoutId = null;
      }
    } else {
      // trailing call 예약
      if (timeoutId !== null) {
        clearTimeout(timeoutId);
      }

      timeoutId = setTimeout(() => {
        lastCall = Date.now();
        func.apply(this, args);
        timeoutId = null;
      }, delay - (now - lastCall));
    }
  };
}
