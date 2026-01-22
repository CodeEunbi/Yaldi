# 📁 utils

## 유틸리티 함수

> 날짜 포맷팅, 문자열 처리, 검증 함수 등

```ts
// yyyy-mm-dd HH:mm:ss 형식의 문자열 반환
export const formatToKoreanDateTimeWithSeconds = (
  isoString: string
): string => {
  const date = new Date(isoString);

  const year = date.getFullYear().toString();
  const month = (date.getMonth() + 1).toString().padStart(2, "0");
  const day = date.getDate().toString().padStart(2, "0");
  const hour = date.getHours().toString().padStart(2, "0");
  const minute = date.getMinutes().toString().padStart(2, "0");
  const second = date.getSeconds().toString().padStart(2, "0");

  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
};
```
