/* eslint-disable react-refresh/only-export-components */
import React from 'react';
import DtoIcon from '../../../assets/icons/dto-icon.svg?react';
import { apiController } from '../../../apis/apiController';

// DTO 관련 타입
export interface DtoSelectedColumn {
  columnKey: number;
  tableKey: number;
}

export interface DtoRequestPayload {
  name: string;
  type: 'DTO_REQUEST' | 'DTO_RESPONSE';
  selectedColumns: DtoSelectedColumn[];
}

export interface DtoRelatedTable {
  tableKey: number;
  physicalName: string;
  logicalName: string;
  displayName: string;
}

export interface DtoResult {
  modelKey: number;
  projectKey: number;
  name: string;
  type: 'DTO_REQUEST' | 'DTO_RESPONSE';
  syncStatus: string;
  syncMessage: string;
  lastSyncedAt: string;
  relatedTables: DtoRelatedTable[];
  columnCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface DtoResponse {
  isSuccess: boolean;
  code: string;
  message: string;
  result: DtoResult;
}

interface DtoProps {
  onClick?: () => void;
  isActive?: boolean;
}

// DTO API 함수
export const createDto = async (projectKey: number, payload: DtoRequestPayload): Promise<DtoResponse> => {
  try {
    const response = await apiController({
      url: `/api/v1/projects/${projectKey}/data-models/dto`,
      method: 'post',
      data: payload
    });

    console.log("DTO 생성 성공", response.data);
    return response.data;

  } catch (error) {
    console.error("DTO API 호출 실패:", error);
    throw error;
  }
};

const Dto: React.FC<DtoProps> = ({ onClick, isActive = false }) => {
  const handleClick = () => {
    if (onClick) {
      onClick();
    }
  };

  return (
    <button
      onClick={handleClick}
      className={`w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors border ${
        isActive
          ? 'bg-blue/10 border-blue text-blue'
          : 'border-transparent hover:bg-ai-chat text-my-black'
      }`}
      aria-label="DTO"
      aria-pressed={isActive}
      title="DTO"
    >
      <DtoIcon
        className={`w-5 h-5 ${isActive ? 'text-blue' : 'text-my-black'}`}
      />
    </button>
  );
};

export default Dto;
