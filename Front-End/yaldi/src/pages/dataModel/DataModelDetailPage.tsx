import React, { Fragment, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import DataModelEditModal from './EditDataModelModal';
import DeleteDataModelModal from './DeleteDataModelModal';

import EditIcon from '../../assets/icons/edit_icon.svg?react';
import FilledButton from '../../components/common/FilledButton';
import LinedButton from '../../components/common/LinedButton';
import CopyIcon from '../../assets/icons/copy_icon.svg?react';
import CheckIcon from '../../assets/icons/check_icon.svg?react';
import type { DataModel } from '../../types/dataModel';

const DataModelDetailPage: React.FC = () => {

  const dummyDataModel: DataModel = {
    "modelKey": 1,
    "projectKey": 1,
    "name": "MemberEntity",
    "type": "ENTITY",
    "syncStatus": "IN_SYNC",
    "syncMessage": "동기화됨",
    "lastSyncedAt": "2025-10-26T05:41:07.800089Z",
    "code": {
      "java": "package com.example.entity;\n\nimport jakarta.persistence.*;\nimport java.time.LocalDateTime;\nimport lombok.*;\n\n/**\n * MemberEntity - 회원\n *\n * <p>테이블: members</p>\n */\n@Entity\n@Table(name = \"members\")\n@Getter\n@Setter\n@NoArgsConstructor\n@AllArgsConstructor\n@Builder\npublic class MemberEntity {\n\n    /**\n     * 회원ID - 회원 고유 식별자\n     */\n    @Id\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\n    @Column(name = \"member_id\", nullable = false, unique = true)\n    private Long memberId;\n\n    /**\n     * 이메일 - 회원 이메일 주소\n     */\n    @Column(name = \"email\", length = 255, nullable = false, unique = true)\n    private String email;\n\n    /**\n     * 이름 - 회원 이름\n     */\n    @Column(name = \"name\", length = 50, nullable = false)\n    private String name;\n\n    /**\n     * 가입일시 - 가입 일시\n     */\n    @Column(name = \"created_at\", nullable = false)\n    private LocalDateTime createdAt;\n}\n",
      "typescript": "/**\n * MemberEntity - 회원\n *\n * 테이블: members\n */\nexport interface MemberEntity {\n  /**\n   * 회원ID - 회원 고유 식별자\n   */\n  memberId: number;\n\n  /**\n   * 이메일 - 회원 이메일 주소\n   */\n  email: string;\n\n  /**\n   * 이름 - 회원 이름\n   */\n  name: string;\n\n  /**\n   * 가입일시 - 가입 일시\n   */\n  createdAt: Date;\n}\n"
    },
    "columns": [
      {
        "columnKey": 1,
        "tableKey": 1,
        "tableName": "members",
        "logicalName": "회원ID",
        "physicalName": "member_id",
        "dataType": "BIGINT",
        "dataDetail": null,
        "isNullable": false,
        "isPrimaryKey": true,
        "isForeignKey": false,
        "isUnique": true,
        "alias": "memberId"
      },
      {
        "columnKey": 2,
        "tableKey": 1,
        "tableName": "members",
        "logicalName": "이메일",
        "physicalName": "email",
        "dataType": "VARCHAR",
        "dataDetail": [
          "255"
        ],
        "isNullable": false,
        "isPrimaryKey": false,
        "isForeignKey": false,
        "isUnique": true,
        "alias": "email"
      },
      {
        "columnKey": 3,
        "tableKey": 1,
        "tableName": "members",
        "logicalName": "이름",
        "physicalName": "name",
        "dataType": "VARCHAR",
        "dataDetail": [
          "50"
        ],
        "isNullable": false,
        "isPrimaryKey": false,
        "isForeignKey": false,
        "isUnique": false,
        "alias": "name"
      },
      {
        "columnKey": 5,
        "tableKey": 1,
        "tableName": "members",
        "logicalName": "가입일시",
        "physicalName": "created_at",
        "dataType": "TIMESTAMP",
        "dataDetail": null,
        "isNullable": false,
        "isPrimaryKey": false,
        "isForeignKey": false,
        "isUnique": false,
        "alias": "createdAt"
      }
    ],
    "relatedTables": [
      {
        "tableKey": 1,
        "physicalName": "members",
        "logicalName": "회원",
        "displayName": "members (회원)"
      }
    ],
    "createdAt": "2025-10-26T05:41:07.800089Z",
    "updatedAt": "2025-10-26T05:41:07.800089Z"
  }

  const navigate = useNavigate();
  const projectKey = useParams().projectKey || 0;
  const dataModelKeyParam = useParams().dataModelKey || 0;
  const dataModelKey = Number(dataModelKeyParam);


  const [dataModel, setDataModel] = React.useState<DataModel | null>(null);
  const [isLoading, setIsLoading] = React.useState<boolean>(false);
  // const [currentPage, setCurrentPage] = React.useState<number>(1);


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

  // 타입에 따라 완성된 Tailwind 클래스를 반환하는 함수
  const getBackgroundColorClass = (dataType: string): string => {
    switch (dataType) {
      case 'entity':
        return 'bg-purple-100';
      case 'requestDto':
        return 'bg-green-100';
      case 'responseDto':
        return 'bg-orange-100';
      default:
        return 'bg-gray-100';
    }
  };

  const [isOpenEditModal, setIsOpenEditModal] = React.useState<boolean>(false);
  const [isOpenDeleteModal, setIsOpenDeleteModal] =
    React.useState<boolean>(false);
  const [selectedLanguage, setSelectedLanguage] = React.useState<
    'java' | 'typeScript'
  >('typeScript');
  const [buttonText, setButtonText] = React.useState(<CopyIcon />);

  const typeClass = dataModel
    ? getBackgroundColorClass(dataModel.type)
    : 'bg-gray-100 text-gray-800';

  const handelEditSubmit = (name: string) => {
    if (!dataModel) {
      return;
    }
    // 수정 요청
    console.log(`[데이터 모델 수정 요청] 이름: ${name}`);
  };

  const handleDeleteConfirm = () => {
    if (!dataModel) {
      return;
    }
    // 삭제 요청
    console.log(`[데이터 모델 삭제 요청] 데이터 모델 키: ${dataModelKey}`);
    navigate(`/project/${projectKey}/data-model`, { replace: true });
  };

  const handleClickDelete = () => {
    setIsOpenDeleteModal(true);
  };

  const contentToCopy = !dataModel
    ? ''
    : selectedLanguage === 'typeScript'
      ? dataModel.code?.typescript
      : dataModel.code?.java

  // 텍스트 복사 핸들러
  const handleCopy = async () => {
    if (!contentToCopy) return null;
    try {
      await navigator.clipboard.writeText(contentToCopy);
      setButtonText(<CheckIcon />); // 텍스트 변경

      // 1.5초 후 원래 텍스트로 복원
      setTimeout(() => {
        setButtonText(<CopyIcon />);
      }, 1500);
    } catch (err) {
      setButtonText(<CopyIcon />);
      console.error('텍스트 복사 실패:', err);
    }
  };

  React.useEffect(() => {
    setIsLoading(true);
    setDataModel(dummyDataModel);
    setIsLoading(false);
  }, []);

  return (
    <div className="flex flex-col w-10/12  max-w-[1187.5px] justify-self-center justify-center content-center items-center py-7 gap-10 text-my-black">
      {/* 모달들 */}
      <DataModelEditModal
        isOpen={isOpenEditModal}
        name={dataModel ? dataModel.name : ''}
        onClose={() => setIsOpenEditModal(false)}
        onSubmit={handelEditSubmit}
        onClickDelete={handleClickDelete}
      />
      <DeleteDataModelModal
        isOpen={isOpenDeleteModal}
        onClose={() => setIsOpenDeleteModal(false)}
        onConfirm={handleDeleteConfirm}
      />

      {/* 상단 제목 */}
      <div className="flex flex-col w-full gap-1">
        <div
          className={`w-fit rounded-lg px-2 py-1 text-sm font-semibold ${typeClass}`}
        >
          {dataModel ? dataModel.type : '데이터 모델 타입'}
        </div>
        <div className="flex w-full justify-start gap-[5px]">
          <div className="text-2xl font-bold">
            {dataModel ? dataModel.name : '데이터 모델 이름'}
          </div>
          <button onClick={() => setIsOpenEditModal(true)}>
            <EditIcon />
          </button>
        </div>
        <div className="text-sm text-gray-500">
          최종 수정일시: {dataModel?.updatedAt?.toLocaleString()}
        </div>
      </div>

      {/* 목록 */}
      {/* 코드 부분 */}
      {isLoading ?
        (
          // 스피너
          <div className='flex justify-center'>
            <svg aria-hidden="true" className="w-8 h-8 text-gray-200 animate-spin dark:text-gray-600 fill-blue-600" viewBox="0 0 100 101" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M100 50.5908C100 78.2051 77.6142 100.591 50 100.591C22.3858 100.591 0 78.2051 0 50.5908C0 22.9766 22.3858 0.59082 50 0.59082C77.6142 0.59082 100 22.9766 100 50.5908ZM9.08144 50.5908C9.08144 73.1895 27.4013 91.5094 50 91.5094C72.5987 91.5094 90.9186 73.1895 90.9186 50.5908C90.9186 27.9921 72.5987 9.67226 50 9.67226C27.4013 9.67226 9.08144 27.9921 9.08144 50.5908Z" fill='#CFCFCF' />
              <path d="M93.9676 39.0409C96.393 38.4038 97.8624 35.9116 97.0079 33.5539C95.2932 28.8227 92.871 24.3692 89.8167 20.348C85.8452 15.1192 80.8826 10.7238 75.2124 7.41289C69.5422 4.10194 63.2754 1.94025 56.7698 1.05124C51.7666 0.367541 46.6976 0.446843 41.7345 1.27873C39.2613 1.69328 37.813 4.19778 38.4501 6.62326C39.0873 9.04874 41.5694 10.4717 44.0505 10.1071C47.8511 9.54855 51.7191 9.52689 55.5402 10.0491C60.8642 10.7766 65.9928 12.5457 70.6331 15.2552C75.2735 17.9648 79.3347 21.5619 82.5849 25.841C84.9175 28.9121 86.7997 32.2913 88.1811 35.8758C89.083 38.2158 91.5421 39.6781 93.9676 39.0409Z" fill='#1E50AF' />
            </svg>
            <span className="sr-only">Loading...</span>
          </div>
        ) : (<div className="w-full flex flex-col gap-3">
          <div className="w-full border-b border-my-border text-xl font-bold  py-2">
            복사하기
          </div>
          {/* 타입 고르는 버튼 부분 */}
          <div className="flex items-center gap-2">
            {selectedLanguage === 'typeScript' ? (
              <FilledButton
                label="TypeScript"
                onClick={() => setSelectedLanguage('typeScript')}
                size="px-3 py-2 text-xs"
              />
            ) : (
              <LinedButton
                label="TypeScript"
                onClick={() => setSelectedLanguage('typeScript')}
                size="px-3 py-2 text-xs"
              />
            )}
            {selectedLanguage === 'java' ? (
              <FilledButton
                label="Java"
                onClick={() => setSelectedLanguage('java')}
                size="px-3 py-2 text-xs"
              />
            ) : (
              <LinedButton
                label="Java"
                onClick={() => setSelectedLanguage('java')}
                size="px-3 py-2 text-xs"
              />
            )}
          </div>
          {/* 코드 보여주고 복사하는 부분 */}
          <div className="relative flex w-full">
            <code className="bg-gray-200 rounded-xl p-4 w-full text-my-black text-md">
              {!dataModel
                ? '코드'
                : selectedLanguage === 'typeScript'
                  ? renderWithLineBreaks(dataModel.code?.typescript)
                  : renderWithLineBreaks(dataModel.code?.java)}
            </code>
            <button className="absolute top-3 right-3" onClick={handleCopy}>
              {buttonText}
            </button>
          </div>
        </div>)}

      {/* 하단 버튼 */}
      <div className="w-full justify-start">
        <FilledButton label="목록으로" onClick={() => navigate(-1)} />
      </div>
    </div >
  );
};

export default DataModelDetailPage;
