import React, { Fragment, type ReactNode } from "react";
import { useNavigate, useParams } from "react-router-dom";

import FilledButton from "../../components/common/FilledButton";
import LinedButton from "../../components/common/LinedButton";
import ToggleButton from "../../components/common/ToggleButton";
import EditVersionModal from "./EditVersionModal";
import DummyDataModal from "./DummyDataModal";
import RollbackModal from "./RollbackModal";

import EditIcon from "../../assets/icons/edit_icon.svg?react";
import CopyIcon from "../../assets/icons/copy_icon.svg?react";
import CheckIcon from "../../assets/icons/check_icon.svg?react";
import SuccessIcon from '../../assets/icons/build_success_icon.svg?react';
import FailIcon from "../../assets/icons/build_fail_icon.svg?react";
import WarningIcon from "../../assets/icons/build_warning_icon.svg?react";
import CanceledIcon from "../../assets/icons/build_cancel_icon.svg?react";
import WaitingIcon from "../../assets/icons/wait_icon.svg?react";
import DownIcon from "../../assets/icons/down_icon.svg?react";
import UpIcon from "../../assets/icons/up_icon.svg?react";
import DownloadIcon from "../../assets/icons/import_icon.svg?react";
import AddIcon from "../../assets/icons/plus_icon.svg?react";
import { apiController } from "../../apis/apiController";
import DiffViewer from "./DiffViewer";
import type { Diff } from "../../utils/schemaDiffParser";
import Swal from "sweetalert2";
import type { ApiError } from "../../types/api";
// import { useAuthStore } from "../../stores/authStore";


export interface MockData {
  "mockDataKey": number;
  "status": string;
  "versionKey": number;
  "rowCount": number;
  "fileName": string;
}


export interface DummyDataItem {
  "mockDataKey": number;
  "status": string;
  "versionKey": number;
  "rowCount": number;
  "fileName": string;
  "downloadUrl": string;
}

export interface Column {
  dataType: string;
  isUnique: boolean;
  columnKey: number;
  dataDetail: (number | string)[];
  isNullable: boolean;
  logicalName: string;
  defaultValue: string | number | null;
  isForeignKey: boolean;
  isPrimaryKey: boolean;
  physicalName: string;
  isIncremental: boolean;
}

export interface Relation {
  "toTableKey": number;
  "fromTableKey": number;
  "relationType": string; //"OPTIONAL_ONE_TO_MANY",
  "constraintName": string; //"fk_posts_user",
  "onDeleteAction": string; //"CASCADE",
  "onUpdateAction": string; //"CASCADE"

}

export interface Table {
  columns: Column[];
  tableKey: number;
  logicalName: string;
  physicalName: string;
}

export interface SchemaData {
  tables: Table[];
  relations: Relation[];
}

export interface Version {
  "versionKey": number;
  "projectKey": number;
  "name": string;
  "description": string;
  "schemaData": SchemaData;
  "isPublic": boolean;
  "designVerificationStatus": string | null; //"SUCCESS",
  "verificationErrors": string[] | null;
  "verificationWarnings": string[] | null;
  "verificationMessage": string | null;
  "verificationSuggestions": string[] | null;
  "createdAt": string;
  "updatedAt": string;
}

// 1. 컬럼 레벨 변경 사항
export interface ColumnDiffItem {
  changeType: "UNCHANGED" | "MODIFIED" | "ADDED" | "REMOVED";
  columnKey: number;
  physicalName: string;
  logicalName: string;
  dataType: string; // INT, VARCHAR, TIMESTAMP 등
  dataDetail: (number | string)[]; // [255], [50] 또는 다른 타입의 상세 정보 배열
  isPrimaryKey: boolean;
  isNullable: boolean;
  isUnique: boolean;
  isForeignKey: boolean;
  isIncremental: boolean;
  defaultValue: string | number | null; // 예: "CURRENT_TIMESTAMP", null

  // 변경된 필드의 목록 (MODIFIED일 때만 존재)
  changedFields: string[] | null;

  // 변경 이전의 값 (MODIFIED일 때만 존재)
  previousValues: {
    physicalName?: string;
    logicalName?: string;
    dataType?: string;
    dataDetail?: (number | string)[];
    isNullable?: boolean;
    isUnique?: boolean;
    defaultValue?: string | number | null;
    // ... 변경될 수 있는 다른 컬럼 속성
  } | null;
}

// 2. 테이블 레벨 변경 사항
export interface TableDiffItem {
  changeType: "UNCHANGED" | "MODIFIED" | "ADDED" | "REMOVED";
  tableKey: number;
  physicalName: string; // users
  logicalName: string; // 회원

  // 컬럼 변경 사항 배열
  columnDiffs: ColumnDiffItem[];

  // 테이블 자체의 속성 변경 목록 (MODIFIED일 때만 존재)
  changedFields: string[] | null;

  // 테이블 자체의 이전 값 (MODIFIED일 때만 존재)
  previousValues: {
    physicalName?: string;
    logicalName?: string; // 예: "사용자"
    // ... 변경될 수 있는 다른 테이블 속성
  } | null;
}

export interface RelationDiffItem {
  changeType: "MODIFIED" | "ADDED" | "REMOVED"; // 변경 유형은 추가/제거/수정 중 하나일 수 있습니다.
  fromTableKey: number;
  toTableKey: number;
  relationType: string; // "ONE_TO_ONE" | "ONE_TO_MANY" | "MANY_TO_ONE" | "OPTIONAL_ONE_TO_MANY";
  constraintName: string; // fk_posts_user
  onDeleteAction: string; // "CASCADE" | "SET NULL" | "RESTRICT" | "NO ACTION";
  onUpdateAction: string; // "CASCADE" | "SET NULL" | "RESTRICT" | "NO ACTION";

  // 변경된 필드의 목록
  changedFields: string[] | null;

  // 이전 값 (MODIFIED일 때만 존재)
  previousValues?: {
    onDeleteAction?: string; //"CASCADE" | "SET NULL" | "RESTRICT" | "NO ACTION";
    onUpdateAction?: string; // "CASCADE" | "SET NULL" | "RESTRICT" | "NO ACTION";
    relationType?: string; // "ONE_TO_ONE" | "ONE_TO_MANY" | "MANY_TO_ONE" | "OPTIONAL_ONE_TO_MANY";
    // 여기에 변경될 수 있는 다른 필드도 포함 가능
  } | null;
}

export interface DiffSummary {
  "addedTables": number;
  "modifiedTables": number;
  "deletedTables": number;
  "addedColumns": number;
  "modifiedColumns": number;
  "deletedColumns": number;
  "addedRelations": number;
  "modifiedRelations": number;
  "deletedRelations": number;
  "hasChanges": boolean;
}

export interface SchemaDiff {
  tableDiffs: TableDiffItem[];
  relationDiffs: RelationDiffItem[];
  summary: DiffSummary;
}

export interface MockDetail {
  "mockDataKey": number;
  "status": string;
  "versionKey": number;
  "rowCount": number;
  "fileName": string;
  "downloadUrl": string;
}

const VersionDetailPage: React.FC = () => {

  const navigate = useNavigate();
  // const projectKey = useParams().projectKey || 0;
  // const projectKey = useAuthStore((state) => state.projectKey);
  const versionKey = Number(useParams().versionKey) || 0;


  // 줄바꿈 문자를 <br/>로 변환하는 함수
  const renderWithLineBreaks = (text: string | null | undefined): ReactNode => {
    if (!text) {
      return null;
    }

    // 1. 문자열을 '\n'을 기준으로 분리하여 배열을 만듭니다.
    const lines = text.split('\n');

    // 2. 분리된 배열을 순회하며 각 라인 뒤에 <br />을 삽입합니다.
    return lines.map((line, index) => (
      <Fragment key={index}>
        {/* 현재 라인 텍스트 */}
        {line}

        {/* 마지막 줄이 아닐 경우에만 <br /> 삽입 */}
        {index < lines.length - 1 && <br />}
      </Fragment>
    ));
  };


  const [version, setVersion] = React.useState<Version | null>(null);
  const [mockDatas, setMockDatas] = React.useState<MockData[] | null>(null);
  const [diff, setDiff] = React.useState<Diff | null>(null);
  const [isOpenEditModal, setIsOpenEditModal] = React.useState<boolean>(false);
  const [isOpenDummyModal, setIsOpenDummyModal] = React.useState<boolean>(false);
  const [isOpenRollbackModal, setIsOpenRollbackModal] = React.useState<boolean>(false);
  // const [isOpenDeleteModal, setIsOpenDeleteModal] = React.useState<boolean>(false);
  const [selectedLanguage, setSelectedLanguage] = React.useState<'mySql' | 'postgres'>('mySql');
  const [buttonText, setButtonText] = React.useState(<CopyIcon />);
  const [isAiContentOpen, setIsAiContentOpen] = React.useState<boolean>(false);
  const [isVersionLoading, setIsVersionLoading] = React.useState<boolean>(false);
  const [isMockLoading, setIsMockLoading] = React.useState<boolean>(false);
  const [isDiffLoading, setIsDiffLoading] = React.useState<boolean>(false);


  // 버전 상세 정보 요청
  const getVersionInfo = React.useCallback(async () => {

    setIsVersionLoading(true);
    try {
      const response = await apiController({
        url: `/api/v1/versions/${encodeURIComponent(versionKey)}`,
        method: 'get',
      })
      setVersion(response.data.result);
      console.log("버전 정보 성공적으로 가져옴.");
    } catch (err) {
      const error = err as ApiError;

      if (error.status === 403) {
        Swal.fire({
          icon: 'warning',
          text: "이 프로젝트에 권한이 없습니다.",
          confirmButtonColor: '#1e50af',
        })
        navigate("/mypage", { replace: true });
        return;
      }

      console.log("버전 정보 못 가져옴.:", err);
      Swal.fire({
        icon: 'error',
        text: "일시적인 오류로 인하여 요청에 실패했습니다.",
        confirmButtonColor: '#1e50af',
      })
      setVersion(null);
    } finally {
      setIsVersionLoading(false);
    }
  }, []);

  // 목데이터 목록 요청
  const getMockDataList = React.useCallback(async () => {
    setIsMockLoading(true)
    try {
      const response = await apiController({
        url: `/api/v1/versions/${encodeURIComponent(versionKey)}/mock-data`,
        method: "get",
      })

      console.log("성공적으로 목데이터 불러 옴.");
      setMockDatas(response.data.result);

    } catch (err) {
      const error = err as ApiError;

      if (error.status === 403) {
        Swal.fire({
          icon: 'warning',
          text: "이 프로젝트에 권한이 없습니다.",
          confirmButtonColor: '#1e50af',
        })
        navigate("/mypage", { replace: true });
        return;
      }

      console.log("목 데이터 목록 못 불러옴: ", err);
      Swal.fire({
        icon: 'error',
        text: "일시적인 오류로 인하여 요청에 실패했습니다.",
        confirmButtonColor: '#1e50af',
      })
      setMockDatas([]);
    } finally {
      setIsMockLoading(false);
    }
  }, []);


  // 이전 버전과 비교 요청
  const getVersionDiff = React.useCallback(async () => {
    setIsDiffLoading(true);
    try {
      const response = await apiController({
        url: `/api/v1/versions/${encodeURIComponent(versionKey)}/compare`,
        method: 'get',
      })
      console.log("성공적으로 diff 불러 옴.");
      setDiff(response.data.result);
    } catch (err) {
      console.log("diff 로딩 실패:", err);
      Swal.fire({
        icon: 'error',
        text: "일시적인 오류로 인하여 요청에 실패했습니다.",
        confirmButtonColor: '#1e50af',
      })

      setDiff(null);
    } finally {
      setIsDiffLoading(false);
    }
  }, []);


  // 버전 정보 수정 요청
  const handelEditSubmit = async (name: string, description: string) => {
    if (!version?.projectKey) return null;

    console.log(`[버전 수정 요청] 이름: ${name}, 설명: ${description}`);
    try {
      await apiController({
        url: `/api/v1/versions/${encodeURIComponent(versionKey)}`,
        method: 'patch',
        data: {
          name,
          description,
        }
      })
      getVersionInfo();
      console.log("버전 정보 수정 성공");
    } catch (err) {
      console.log("버전 정보 수정 실패", err);
      Swal.fire({
        text: "수정에 실패했습니다. 잠시 후 다시 시도해 주세요.",
        confirmButtonColor: '#1e50af',
        icon: 'error',
      })
    }
  };

  // 삭제 요청
  // const handleDeleteConfirm = async () => {
  //   console.log(`[데이터 모델 삭제 요청] 데이터 모델 키: ${versionKey}`);
  //   try {
  //     await apiController({
  //       url: ``,
  //       method: 'delete',
  //     })
  //     console.log(versionKey, '버전 삭제');
  //     // 목록 페이지로 이동
  //     navigate(`/project/${version?.projectKey}/version`, { replace: true });
  //   } catch (err) {
  //     console.log("삭제 요청 에러 발생:", err);
  //     Swal.fire({
  //       text: "삭제 실패했습니다. 잠시 후 다시 시도해 주세요.",
  //       icon: 'error',
  //     })
  //   };
  // }

  // isPublic 토글 요청
  const handleToggle = async () => {
    if (!version) return;

    try {
      await apiController({
        url: `/api/v1/projects/${encodeURIComponent(version?.projectKey)}/versions/${encodeURIComponent(versionKey)}/visibility`,
        method: 'patch',
        data: {
          isPublic: !version?.isPublic,
        }
      })
    } catch (err) {
      console.log("토글실패", err);
      // Swal.fire({
      //   text: "일시적인 오류로 인하여 요청에 실패했습니다.",
      // })
      // setVersion((prev) => {...prev, isPublic: !prev.isPublic});
    }
  }



  const STATUS_ICON: React.ReactNode = (() => {
    if (!version) return;
    switch (version.designVerificationStatus) {
      case "SUCCESS":
        return (<SuccessIcon />);
      case "WARNING":
        return (<WarningIcon />);
      case "FAILED":
        return (<FailIcon />);
      case "CANCELED":
        return (<CanceledIcon />);
      default:
        return (<WaitingIcon />);
    }
  })();


  // table 정보로부터 mySQL 뽑는 함수
  function generateMySQL(tables: Table[], relations: Relation[]): string {
    let sql = '';

    // 테이블 생성
    tables.forEach(table => {
      sql += `CREATE TABLE ${table.physicalName} (\n`;

      const columnDefs = table.columns.map(col => {
        let def = `    ${col.physicalName} `;

        // 데이터 타입
        if (col.dataType === 'VARCHAR' && col.dataDetail.length > 0) {
          def += `VARCHAR(${col.dataDetail[0]})`;
        } else if (col.dataType === 'ENUM' && col.dataDetail.length > 0) {
          def += `ENUM(${col.dataDetail.map(v => `'${v}'`).join(', ')})`;
        } else {
          def += col.dataType;
        }

        // AUTO_INCREMENT
        if (col.isIncremental) {
          def += ' AUTO_INCREMENT';
        }

        // NULL 여부
        def += col.isNullable ? ' NULL' : ' NOT NULL';

        // 기본값
        if (col.defaultValue) {
          def += ` DEFAULT ${col.defaultValue === 'CURRENT_TIMESTAMP' ? 'CURRENT_TIMESTAMP' : `'${col.defaultValue}'`}`;
        }

        // UNIQUE
        if (col.isUnique && !col.isPrimaryKey) {
          def += ' UNIQUE';
        }

        return def;
      });

      sql += columnDefs.join(',\n');

      // Primary Key
      const pkColumns = table.columns.filter(c => c.isPrimaryKey);
      if (pkColumns.length > 0) {
        sql += `,\n    PRIMARY KEY (${pkColumns.map(c => c.physicalName).join(', ')})`;
      }

      sql += '\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;\n\n';
    });

    // Foreign Key 추가
    relations.forEach(rel => {
      const fromTable = tables.find(t => t.tableKey === rel.fromTableKey);
      const toTable = tables.find(t => t.tableKey === rel.toTableKey);
      const fkColumn = fromTable?.columns.find(c => c.isForeignKey);
      const pkColumn = toTable?.columns.find(c => c.isPrimaryKey);

      if (fromTable && toTable && fkColumn && pkColumn) {
        sql += `ALTER TABLE ${fromTable.physicalName}\n`;
        sql += `    ADD CONSTRAINT ${rel.constraintName}\n`;
        sql += `    FOREIGN KEY (${fkColumn.physicalName})\n`;
        sql += `    REFERENCES ${toTable.physicalName}(${pkColumn.physicalName})\n`;
        sql += `    ON DELETE ${rel.onDeleteAction}\n`;
        sql += `    ON UPDATE ${rel.onUpdateAction};\n\n`;
      }
    });

    return sql;
  }

  // table 정보로부터 postgreSQL 뽑는 함수
  function generatePostgreSQL(tables: Table[], relations: Relation[]): string {
    let sql = '';

    // ENUM 타입 생성
    const enumColumns = tables.flatMap(t =>
      t.columns.filter(c => c.dataType === 'ENUM')
    );

    const uniqueEnums = new Set(
      enumColumns.map(c => `user_${c.physicalName}`)
    );

    uniqueEnums.forEach(enumName => {
      const enumCol = enumColumns.find(c => `user_${c.physicalName}` === enumName);
      if (enumCol) {
        sql += `CREATE TYPE ${enumName} AS ENUM (${enumCol.dataDetail.map(v => `'${v}'`).join(', ')});\n`;
      }
    });

    if (uniqueEnums.size > 0) sql += '\n';

    // 테이블 생성 (MySQL과 유사하지만 SERIAL 사용)
    tables.forEach(table => {
      sql += `CREATE TABLE ${table.physicalName} (\n`;

      const columnDefs = table.columns.map(col => {
        let def = `    ${col.physicalName} `;

        // 데이터 타입
        if (col.isIncremental) {
          def += 'SERIAL';
        } else if (col.dataType === 'VARCHAR' && col.dataDetail.length > 0) {
          def += `VARCHAR(${col.dataDetail[0]})`;
        } else if (col.dataType === 'ENUM') {
          def += `user_${col.physicalName}`;
        } else {
          def += col.dataType;
        }

        // NULL 여부 (SERIAL이 아닌 경우만)
        if (!col.isIncremental) {
          def += col.isNullable ? ' NULL' : ' NOT NULL';
        }

        // 기본값
        if (col.defaultValue && !col.isIncremental) {
          def += ` DEFAULT ${col.defaultValue === 'CURRENT_TIMESTAMP' ? 'CURRENT_TIMESTAMP' : `'${col.defaultValue}'`}`;
        }

        // UNIQUE
        if (col.isUnique && !col.isPrimaryKey) {
          def += ' UNIQUE';
        }

        return def;
      });

      sql += columnDefs.join(',\n');

      // Primary Key
      const pkColumns = table.columns.filter(c => c.isPrimaryKey);
      if (pkColumns.length > 0) {
        sql += `,\n    PRIMARY KEY (${pkColumns.map(c => c.physicalName).join(', ')})`;
      }

      sql += '\n);\n\n';
    });

    // Foreign Key는 CREATE TABLE에 포함하거나 ALTER TABLE 사용 가능
    // 여기서는 ALTER TABLE 방식 사용
    relations.forEach(rel => {
      const fromTable = tables.find(t => t.tableKey === rel.fromTableKey);
      const toTable = tables.find(t => t.tableKey === rel.toTableKey);
      const fkColumn = fromTable?.columns.find(c => c.isForeignKey);
      const pkColumn = toTable?.columns.find(c => c.isPrimaryKey);

      if (fromTable && toTable && fkColumn && pkColumn) {
        sql += `ALTER TABLE ${fromTable.physicalName}\n`;
        sql += `    ADD CONSTRAINT ${rel.constraintName}\n`;
        sql += `    FOREIGN KEY (${fkColumn.physicalName})\n`;
        sql += `    REFERENCES ${toTable.physicalName}(${pkColumn.physicalName})\n`;
        sql += `    ON DELETE ${rel.onDeleteAction}\n`;
        sql += `    ON UPDATE ${rel.onUpdateAction};\n\n`;
      }
    });

    return sql;
  }



  const contentToCopy = !version
    ? ""
    : selectedLanguage === 'mySql'
      ? generateMySQL(version.schemaData.tables, version.schemaData.relations)
      : generatePostgreSQL(version.schemaData.tables, version.schemaData.relations);


  // 텍스트 복사 핸들러
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(contentToCopy);
      setButtonText(<CheckIcon />); // 텍스트 변경

      // 1.5초 후 원래 텍스트로 복원
      setTimeout(() => {
        setButtonText(<CopyIcon />);
      }, 1500);

    } catch (err) {
      setButtonText(<CopyIcon />);
      console.error("텍스트 복사 실패:", err);
    }
  };


  // 더미데이터 생성 요청 후 목록에 추가
  const handleSubmitDummydata = async (count: number) => {

    // 1. API 요청 시뮬레이션: 비동기 처리
    console.log(`[API 요청] 더미 데이터 ${count}개 생성 요청 시작`);
    try {

      const response = await apiController({
        url: `/api/v1/projects/${version?.projectKey}/versions/${versionKey}/mock-data`,
        method: "post",
        data: {
          rowCount: count,
        }
      })

      const newDummydata = response.data.result;
      console.log(`[API 응답 완료] 더미 데이터 ${count}개 추가됨.`);
      setMockDatas(prev => {
        // prev가 null (또는 undefined)이면 상태를 변경하지 않고 그대로 반환합니다.
        if (!prev) {
          return prev;
        }

        return {
          ...prev, // 기존 version 객체의 다른 속성 유지
          // 💡 스프레드 연산자(...)를 사용하여 새 배열을 생성하고 newDummydata를 추가
          dummyDataList: [...prev, newDummydata]
        };
      });

    } catch (err) {
      console.log("목데이터 생성 요청 실패", err);
      Swal.fire({
        icon: 'error',
        text: "일시적인 오류로 인하여 요청에 실패했습니다.",
        confirmButtonColor: '#1e50af',
      })
    }

  }

  // 더미 데이터 다운로드
  const onClickDownloadLink = React.useCallback((name: string, mockdataKey: number) => {

    const getSrcUrl = async () => {

      try {
        const response = await apiController({
          url: `/api/v1/projects/versions/${encodeURIComponent(versionKey)}/mock-data/${encodeURIComponent(mockdataKey)}`,
          // url: `/api/v1/projects/${encodeURIComponent(projectKey ?? "")}/versions/${encodeURIComponent(versionKey)}/mock-data/${encodeURIComponent(mockdataKey)}`,
          method: 'get',
        })
        const srcUrl: string = response.data.result.downloadUrl;
        return srcUrl;
      } catch (err) {
        console.log("더미 데이터 다운로드 링크 가져오기 실패", err);
        Swal.fire({
          icon: 'error',
          text: "일시적인 오류로 인하여 요청에 실패했습니다.",
          confirmButtonColor: '#1e50af',
        })
        return null;
      }
    }

    getSrcUrl().then((srcUrl) => {
      if (!srcUrl) return;
      fetch(srcUrl, { method: 'GET' }).then((res) => res.blob()).then((blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = name;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
          window.URL.revokeObjectURL(url);
        }, 1000);
        a.remove();
      }).catch((err) => {
        console.error('err', err);
      });

    })

  }, []);


  // 롤백 요청 (projectKey, versionKey);
  const handleRollbackConfirm = async () => {
    try {
      await apiController({
        url: ``,
        method: 'post',
      })

    } catch (err) {
      console.log("롤백 실패:", err);
      Swal.fire({
        icon: 'error',
        text: "일시적인 오류로 인하여 요청에 실패했습니다.",
        confirmButtonColor: '#1e50af',
      })

    }
    console.log(`version ${versionKey} 로 rollback`);
    // workspace로 이동?
  }


  React.useEffect(() => {
    getVersionInfo()
      .then(() => {
        getMockDataList();
        getVersionDiff();
      })
  }, [version, versionKey, getVersionInfo, getMockDataList, getVersionDiff]);


  React.useEffect(() => {
    // dataModelKey로 상세 정보 요청
    // setDataModel(요청_결과);
  }, [versionKey]);



  if (!version || isVersionLoading) {
    return (
      <div className="flex flex-col w-10/12  max-w-[1187.5px] pt-96 justify-self-center justify-center content-center items-center py-7 gap-10 text-my-black">
        <div role="status">
          <svg aria-hidden="true" className="w-8 h-8 text-gray-200 animate-spin dark:text-gray-600 fill-blue-600" viewBox="0 0 100 101" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M100 50.5908C100 78.2051 77.6142 100.591 50 100.591C22.3858 100.591 0 78.2051 0 50.5908C0 22.9766 22.3858 0.59082 50 0.59082C77.6142 0.59082 100 22.9766 100 50.5908ZM9.08144 50.5908C9.08144 73.1895 27.4013 91.5094 50 91.5094C72.5987 91.5094 90.9186 73.1895 90.9186 50.5908C90.9186 27.9921 72.5987 9.67226 50 9.67226C27.4013 9.67226 9.08144 27.9921 9.08144 50.5908Z" fill='#CFCFCF' />
            <path d="M93.9676 39.0409C96.393 38.4038 97.8624 35.9116 97.0079 33.5539C95.2932 28.8227 92.871 24.3692 89.8167 20.348C85.8452 15.1192 80.8826 10.7238 75.2124 7.41289C69.5422 4.10194 63.2754 1.94025 56.7698 1.05124C51.7666 0.367541 46.6976 0.446843 41.7345 1.27873C39.2613 1.69328 37.813 4.19778 38.4501 6.62326C39.0873 9.04874 41.5694 10.4717 44.0505 10.1071C47.8511 9.54855 51.7191 9.52689 55.5402 10.0491C60.8642 10.7766 65.9928 12.5457 70.6331 15.2552C75.2735 17.9648 79.3347 21.5619 82.5849 25.841C84.9175 28.9121 86.7997 32.2913 88.1811 35.8758C89.083 38.2158 91.5421 39.6781 93.9676 39.0409Z" fill='#1E50AF' />
          </svg>
          <span className="sr-only">Loading...</span>
        </div>
      </div>
    )
  }


  return (
    <div className="flex flex-col w-10/12  max-w-[1187.5px] justify-self-center justify-center content-center items-center py-7 gap-10 text-my-black">

      {/* 모달들 */}
      <EditVersionModal
        isOpen={isOpenEditModal}
        name={version ? version.name : ""}
        description={version ? version.description : ""}
        onClose={() => setIsOpenEditModal(false)}
        onSubmit={handelEditSubmit}
      // onClickDelete={() => setIsOpenDeleteModal(true)}
      />
      {/* <DeleteVersionModal
        isOpen={isOpenDeleteModal}
        onClose={() => setIsOpenDeleteModal(false)}
        onConfirm={handleDeleteConfirm}
      /> */}
      <DummyDataModal
        isOpen={isOpenDummyModal}
        onClose={() => setIsOpenDummyModal(false)}
        onSubmit={handleSubmitDummydata}
      />
      <RollbackModal
        isOpen={isOpenRollbackModal}
        onClose={() => setIsOpenRollbackModal(false)}
        onConfirm={handleRollbackConfirm}
      />


      {/* 상단 제목 */}
      <div className="flex flex-col w-full gap-1">
        <div className="flex w-full justify-start gap-[5px]">
          <div className="text-2xl font-bold">
            {version ? version.name : "버전 이름"}
          </div>
          <button
            onClick={() => setIsOpenEditModal(true)}
          >
            <EditIcon />
          </button>
        </div>
        <div className="text-sm text-gray-500">최종 수정일시: {new Date(version?.updatedAt).toLocaleString()}</div>
      </div>

      {/* 목록 */}

      {/* AI 검증 부분 */}
      <div className="w-full flex flex-col gap-0">
        <div className="w-full border-b border-my-border text-xl font-bold  py-2">
          AI 검증 결과
        </div>
        <div className="w-full py-4 text-my-black text-md flex gap-1">
          {STATUS_ICON} {version.verificationMessage}
        </div>
        {(version.designVerificationStatus && ["WARNING", "FAILED"].includes(version.designVerificationStatus)) && <div className="flex w-full max-w-[1153px] rounded-2xl border border-my-border p-4 text-base justify-between items-start">
          <div className={`w-11/12 break-all ${isAiContentOpen ? "break-all" : "truncate"}`}>
            {version.verificationErrors ? version.verificationErrors : version.verificationWarnings ? version.verificationWarnings : ""}
          </div>
          <button onClick={() => setIsAiContentOpen(prev => !prev)}>
            {isAiContentOpen ? <UpIcon /> : <DownIcon />}
          </button>
        </div>}
      </div>

      {/* 설명 부분 */}
      {version.description && <div className="w-full flex flex-col gap-1">
        <div className="w-full border-b border-my-border text-xl font-bold  py-2">
          설명
        </div>
        <div className="w-full py-4 text-my-black text-md">
          {version.description}
        </div>
      </div>
      }

      {/* 공개 여부 부분 */}
      {version.description && <div className="w-full flex flex-col gap-1">
        <div className="w-full border-b border-my-border text-xl font-bold  py-2">
          공개 여부
        </div>
        <div className="w-full py-4 text-my-black text-md">
          <div className="flex w-full h-full items-center justify-start">
            <ToggleButton isOn={version.isPublic} onToggle={handleToggle} className="h-[30px]" />
          </div>
        </div>
      </div>
      }

      {/* diff 부분 */}
      <div className="w-full flex flex-col gap-3">
        <div id="titleDiff" className="flex w-full border-b border-my-border items-center py-2">
          <div className="text-xl font-bold">
            변경사항
          </div>
        </div>
        {!isDiffLoading && <div>
          {diff && <DiffViewer diff={diff} />}
        </div>
        }
      </div>

      {/* 더미 데이터 부분 */}
      <div className="w-full flex flex-col gap-3">
        <div className="flex w-full border-b border-my-border items-center gap-2 py-2">
          <div className="text-xl font-bold">
            더미 데이터
          </div>
          <button onClick={() => setIsOpenDummyModal(true)}>
            <AddIcon />
          </button>
        </div>
        {/* 더미데이터 목록 */}
        {!isMockLoading &&
          <div className="relative flex w-full flex-col gap-2 justify-center">
            {mockDatas ? (mockDatas.map((item) => (
              <a key={item.mockDataKey} href="" onClick={() => onClickDownloadLink(item.fileName, item.mockDataKey)} className="flex gap-2 text-gray-600 text-sm underline">
                <DownloadIcon className="h-5 w-5" />
                {item.fileName}
              </a>
            ))) : "더미데이터가 없습니다."}
          </div>
        }
      </div>


      {/* 코드 부분 */}
      <div className="w-full flex flex-col gap-3">
        <div className="w-full border-b border-my-border text-xl font-bold  py-2">
          복사하기
        </div>
        {/* 타입 고르는 버튼 부분 */}
        <div className="flex items-center gap-2">
          {selectedLanguage === 'mySql' ? (
            <FilledButton
              label="mySql"
              onClick={() => setSelectedLanguage('mySql')}
              size="px-3 py-2 text-xs"
            />
          ) : (
            <LinedButton
              label="mySql"
              onClick={() => setSelectedLanguage('mySql')}
              size="px-3 py-2 text-xs"
            />
          )}
          {selectedLanguage === 'postgres' ? (
            <FilledButton
              label="postgres"
              onClick={() => setSelectedLanguage('postgres')}
              size="px-3 py-2 text-xs"
            />
          ) : (
            <LinedButton
              label="postgres"
              onClick={() => setSelectedLanguage('postgres')}
              size="px-3 py-2 text-xs"
            />
          )}

        </div>
        {/* 코드 보여주고 복사하는 부분 */}
        <div className="relative flex w-full">
          <code className="bg-gray-200 rounded-xl p-4 w-full text-my-black text-md">
            {!version ? "코드" : renderWithLineBreaks((contentToCopy))}
          </code>
          <button className="absolute top-3 right-3"
            onClick={handleCopy}>
            {buttonText}
          </button>
        </div>
      </div>

      {/* 하단 버튼 */}
      <div className="w-full justify-start flex gap-2">
        <FilledButton
          label="목록으로"
          onClick={() => navigate(-1)}
        />
        <FilledButton
          label="이 버전으로 되돌리기"
          onClick={() => setIsOpenRollbackModal(true)}
        />
      </div>

    </div >
  );
};

export default VersionDetailPage;