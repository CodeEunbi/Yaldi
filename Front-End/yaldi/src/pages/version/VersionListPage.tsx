import React from "react";
import {
  useNavigate,
  useParams
} from "react-router-dom";
import Pagination from "../../components/common/Pagination";
import type { LongPaginationInfo } from "../../types/pagination";
import ToggleButton from "../../components/common/ToggleButton";
import { apiController } from "../../apis/apiController";

import SuccessIcon from '../../assets/icons/build_success_icon.svg?react';
import FailIcon from "../../assets/icons/build_fail_icon.svg?react";
import WarningIcon from "../../assets/icons/build_warning_icon.svg?react";
import CanceledIcon from "../../assets/icons/build_cancel_icon.svg?react";
import WaitingIcon from "../../assets/icons/wait_icon.svg?react";
import { useAuthStore } from "../../stores/authStore";
import type { ApiError } from "../../types/api";
import Swal from "sweetalert2";


interface VersionItem {
  versionKey: number;
  projectKey: number;
  name: string;
  description: string;
  isPublic: boolean;
  designVerificationStatus: string;
  createdAt: string;
  updatedAt: string;
}

const VersionListPage: React.FC = () => {

  const navigate = useNavigate();
  const projectKey = Number(useParams().projectKey) || 0;
  const setProjectKey = useAuthStore((state) => state.setProjectKey);


  const STATUS_ICON = (designVerificationStatus: string) => {
    switch (designVerificationStatus) {
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
  }; // 💡 함수를 정의하고 즉시 호출 (())

  const [currentPage, setCurrentPage] = React.useState<number>(1);
  const [versionList, setVersionList] = React.useState<VersionItem[]>([]);
  const [pageInfo, setPagiInfo] = React.useState<LongPaginationInfo>({
    "page": 0,
    "size": 10,
    "numberOfElements": 0,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true,
    "empty": true,
    "sort": {
      "sorted": false,
      "unsorted": true,
      "empty": true
    }
  });


  const handleItemClick = (versionKey: number) => {
    navigate(`/version/${versionKey}`);
  }

  // isPublic 토글
  const handleToggle = async (versionKey: number, newIsPublic: boolean) => {
    try {
      await apiController({
        url: `/api/v1/projects/${encodeURIComponent(projectKey)}/versions/${encodeURIComponent(versionKey)}/visibility`,
        method: 'patch',
        data: {
          isPublic: newIsPublic,
        }
      })
      console.log("토글 성공");
      setVersionList((prev) => prev.map(i => i.versionKey === versionKey ? { ...i, isPublic: !i.isPublic } : i));
    } catch (err) {
      console.log("토글실패", err);
      Swal.fire({
        text: "일시적인 오류로 인하여 요청에 실패했습니다.",
        icon: 'error',
        confirmButtonColor: '#1e50af',
      })
      setVersionList((prev) => prev.map(i => i.versionKey === versionKey ? { ...i, isPublic: !i.isPublic } : i));
    }
  }

  React.useEffect(() => {

    setProjectKey(projectKey);
    // 이 프로젝트의 버전 목록 다 요청 api
    const getVersionList = async () => {
      try {
        const response = await apiController({
          url: `/api/v1/projects/${encodeURIComponent(projectKey)}/versions`,
          method: 'get',
          params: {
            page: currentPage - 1,
            size: 10,
          },
        })
        console.log("목록 불러오기 성공", response.data.result);

        setVersionList(response.data.result.data);
        setPagiInfo(response.data.result.meta);
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

        //  else if (error.status === 404) {
        //   Swal.fire({
        //     icon: 'warning',
        //     text: "요청하신 정보를 찾을 수 없습니다.",
        //   })
        //   // navigate("/mypage", { replace: true });
        // }


        console.log("목록 불러오기 실패:", err);
        setVersionList([]);

      }
    };

    getVersionList();
  }, [projectKey, currentPage]);

  return (
    <div className="relative flex flex-col w-10/12  max-w-[1187.5px] justify-self-center justify-center content-center items-center py-4 gap-4 text-my-black">

      {/* 상단 제목 및 필터 */}
      <div className="flex flex-col w-full justify-start gap-[5px]">
        <div className="text-xl font-bold">
          버전
        </div>
      </div>

      {/* 목록 */}
      <div className="w-full border border-my-border rounded-2xl p-1">
        <table className="w-full border-separate border-spacing-y-0.5 table-fixed">
          <colgroup>
            <col style={{ width: '10%' }} />  {/* 상태 */}
            <col style={{ width: '20%' }} />  {/* 버전명 */}
            <col style={{ width: '45%' }} />  {/* 설명 */}
            <col style={{ width: '15%' }} />  {/* 생성일 */}
            <col style={{ width: '10%' }} />  {/* 공개여부 */}
          </colgroup>
          <thead className="text-md fond-semibold ">
            <tr>
              <th className="p-3 pl-10 text-left border-b border-my-border" scope="col">상태</th>
              <th className="p-3 text-left border-b border-my-border" scope="col">버전명</th>
              <th className="p-3 text-left border-b border-my-border" scope="col">설명</th>
              <th className="p-3 text-left border-b border-my-border" scope="col">생성일</th>
              <th className="p-3 text-left border-b border-my-border" scope="col">공개여부</th>
            </tr>
          </thead>

          <tbody className="text-sm">
            {versionList.length === 0 ? (
              <tr>
                <td colSpan={5} className="p-3 text-center">데이터가 없습니다.</td>
              </tr>
            ) : (
              versionList.map((item) => (
                <tr key={item.versionKey} className="h-[44px] overflow-hidden hover:bg-light-blue">
                  <td className="p-2 pl-10 text-left rounded-l-xl">
                    <div className="hover:underline hover:cursor-pointer truncate" onClick={() => handleItemClick(item.versionKey)} >
                      {STATUS_ICON(item.designVerificationStatus)}
                    </div>
                  </td>
                  <td className="p-2 text-left">
                    <div className="hover:underline hover:cursor-pointer truncate" onClick={() => handleItemClick(item.versionKey)} >
                      {item.name}
                    </div>
                  </td>
                  <td className="p-2 text-left">
                    <div className="hover:underline hover:cursor-pointer truncate" onClick={() => handleItemClick(item.versionKey)} >
                      {item.description}
                    </div>
                  </td>
                  <td className="p-2 text-left">
                    <div className="hover:underline hover:cursor-pointer truncate" onClick={() => handleItemClick(item.versionKey)} >
                      {new Date(item.createdAt).toLocaleDateString()}
                    </div>
                  </td>
                  <td className="p-2 text-left rounded-r-xl h-full items-center">
                    <div className="flex w-full h-full items-center justify-start">
                      <ToggleButton isOn={item.isPublic} onToggle={() => { handleToggle(item.versionKey, !item.isPublic); }} className="h-[30px]" />
                    </div>

                  </td>
                </tr>
              )
              ))}
          </tbody>
        </table>
      </div>

      {/* 페이지네이션 */}
      {pageInfo.totalElements > 0 && <Pagination totalPages={Math.ceil(pageInfo.totalElements / pageInfo.size)} currentPage={currentPage} onPageChange={(page: number) => setCurrentPage(page)} className="absolute top-[645px]" />}

    </div>
  );
};

export default VersionListPage;