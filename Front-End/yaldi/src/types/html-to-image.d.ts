declare module 'html-to-image' {
  export type ToImageOptions = {
    filter?: (domNode: HTMLElement) => boolean;
    backgroundColor?: string;
    cacheBust?: boolean;
    width?: number;
    height?: number;
    style?: Partial<CSSStyleDeclaration>;
    pixelRatio?: number;
    quality?: number;
  };

  export function toPng(
    node: HTMLElement,
    options?: ToImageOptions,
  ): Promise<string>;

  export function toJpeg(
    node: HTMLElement,
    options?: ToImageOptions & { quality?: number },
  ): Promise<string>;
}
