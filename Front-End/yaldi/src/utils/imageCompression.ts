/**
 * 이미지를 압축합니다.
 * @param file 압축할 이미지 파일
 * @param maxWidth 최대 너비 (기본값: 500px)
 * @param maxHeight 최대 높이 (기본값: 500px)
 * @param quality 압축 품질 (0-1, 기본값: 0.8)
 * @param maxFileSizeMB 최대 파일 크기 (기본값: 10MB)
 * @param outputFormat 출력 형식 ('base64' | 'blob', 기본값: 'base64')
 * @returns Base64 문자열 또는 Blob 객체
 */
export const compressImage = <T extends 'base64' | 'blob' = 'base64'>(
  file: File,
  maxWidth: number = 500,
  maxHeight: number = 500,
  quality: number = 0.8,
  maxFileSizeMB: number = 10,
  outputFormat: T = 'base64' as T
): Promise<T extends 'base64' ? string : Blob> => {
  return new Promise((resolve, reject) => {
    // ✅ 파일 크기 체크
    const fileSizeMB = file.size / (1024 * 1024);
    if (fileSizeMB > maxFileSizeMB) {
      reject(new Error(`파일 크기가 ${maxFileSizeMB}MB를 초과합니다. (현재: ${fileSizeMB.toFixed(2)}MB)`));
      return;
    }

    // ✅ 이미지 타입 체크
    if (!file.type.startsWith('image/')) {
      reject(new Error('이미지 파일만 업로드 가능합니다.'));
      return;
    }

    const reader = new FileReader();

    reader.onload = (e) => {
      const img = new Image();

      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;

        // 이미지 비율을 유지하면서 크기 조정
        if (width > height) {
          if (width > maxWidth) {
            height = Math.round((height * maxWidth) / width);
            width = maxWidth;
          }
        } else {
          if (height > maxHeight) {
            width = Math.round((width * maxHeight) / height);
            height = maxHeight;
          }
        }

        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        if (!ctx) {
          reject(new Error('Canvas context를 가져올 수 없습니다.'));
          return;
        }

        // ✅ PNG 투명 배경 처리
        const mimeType = file.type || 'image/jpeg';
        if (mimeType === 'image/png') {
          ctx.clearRect(0, 0, width, height);
        }

        ctx.drawImage(img, 0, 0, width, height);

        // ✅ 출력 형식에 따라 분기
        if (outputFormat === 'blob') {
          canvas.toBlob(
            (blob) => {
              if (blob) {
                resolve(blob as any);
              } else {
                reject(new Error('Blob 생성에 실패했습니다.'));
              }
            },
            mimeType,
            quality
          );
        } else {
          const compressedBase64 = canvas.toDataURL(mimeType, quality);
          resolve(compressedBase64 as any);
        }
      };

      img.onerror = () => {
        reject(new Error('이미지를 로드할 수 없습니다.'));
      };

      img.src = e.target?.result as string;
    };

    reader.onerror = () => {
      reject(new Error('파일을 읽을 수 없습니다.'));
    };

    reader.readAsDataURL(file);
  });
};

/**
 * 이미지를 3000자 이내의 Base64로 압축합니다.
 * @param file 압축할 이미지 파일
 * @param maxLength 최대 Base64 길이 (기본값: 3000)
 * @param maxFileSizeMB 최대 원본 파일 크기 (기본값: 10MB)
 * @returns 3000자 이내의 Base64 문자열
 */
export const compressImageToMaxLength = async (
  file: File,
  maxLength: number = 3000,
  maxFileSizeMB: number = 10
): Promise<string> => {
  // 파일 크기 체크
  const fileSizeMB = file.size / (1024 * 1024);
  if (fileSizeMB > maxFileSizeMB) {
    throw new Error(`파일 크기가 ${maxFileSizeMB}MB를 초과합니다. (현재: ${fileSizeMB.toFixed(2)}MB)`);
  }

  // 이미지 타입 체크
  if (!file.type.startsWith('image/')) {
    throw new Error('이미지 파일만 업로드 가능합니다.');
  }

  // 압축 파라미터 설정
  let width = 200;
  let quality = 0.7;
  let attempts = 0;
  const maxAttempts = 10;

  while (attempts < maxAttempts) {
    try {
      const base64 = await compressImage(file, width, width, quality, maxFileSizeMB, 'base64');

      // 3000자 이내면 성공
      if (base64.length <= maxLength) {
        console.log(`✅ 압축 성공 - 크기: ${width}px, 품질: ${quality}, 길이: ${base64.length}자`);
        return base64;
      }

      // 3000자 초과 시 파라미터 조정
      attempts++;

      if (attempts < 5) {
        // 처음 4번은 크기를 줄임
        width = Math.floor(width * 0.8);
      } else {
        // 이후에는 품질을 낮춤
        quality = Math.max(0.1, quality - 0.1);
        width = Math.max(50, Math.floor(width * 0.9));
      }

      console.log(`⚠️ ${base64.length}자 초과, 재시도 (${attempts}/${maxAttempts}) - 새 크기: ${width}px, 새 품질: ${quality}`);

    } catch (error) {
      throw new Error(`이미지 압축 실패: ${error instanceof Error ? error.message : '알 수 없는 오류'}`);
    }
  }

  throw new Error(`${maxAttempts}번 시도했지만 ${maxLength}자 이내로 압축할 수 없습니다.`);
};