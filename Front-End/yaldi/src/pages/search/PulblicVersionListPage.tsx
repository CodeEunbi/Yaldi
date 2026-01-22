import React from "react";
import {
  useNavigate,
  useParams,
} from "react-router-dom";
// import Pagination from "../../components/common/Pagination";
// import type { PaginationInfo } from "../../types/pagination";
import { apiController } from "../../apis/apiController";
import Swal from "sweetalert2";
// import type { ApiError } from "../../types/api";

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

// interface Project {
//   "projectKey": number;
//   "teamKey": number;
//   "name": string;
//   "description": string;
//   "imageUrl": string;
//   "createdAt": string;
//   "updatedAt": string;
//   "lastActivityAt": string;
//   "isMember": boolean;
//   "role": string;
// }


const PublicVersionListPage: React.FC = () => {

  const navigate = useNavigate();
  const projectKey = useParams().projectKey || 0;
  const projectName = useParams().projectName || "";


  // const [currentPage, setCurrentPage] = React.useState<number>(1);
  const [versionList, setVersionList] = React.useState<VersionItem[]>([]);
  // const [pageInfo, setPagiInfo] = React.useState<PaginationInfo>({
  //   currentPage: 0,
  //   size: 10,
  //   totalElements: 0,
  //   hasNext: false,
  // });


  const handleItemClick = (versionKey: number) => {
    // 검색 타고 들어간 사용자가 볼 라우터로.
    navigate(`/search/${encodeURIComponent(projectKey)}/${projectName}/${versionKey}`);
  }


  React.useEffect(() => {
    // 이 프로젝트의 공개 버전 목록만 요청. api. 
    // 페이지네이션?
    const getPublicVersionList = async () => {
      try {
        const response = await apiController({
          url: `/api/v1/search/projects/${encodeURIComponent(projectKey)}/versions`,
          method: 'get',
          // params: {
          //   page: currentPage - 1,
          //   size: 10,
          // }
        })

        console.log("목록 불러오기 성공", response.data);
        setVersionList(response.data.result);
      } catch (err) {
        console.log("이 프로젝트의 버전 목록을 불러오는 데 실패.", err);
        Swal.fire({
          icon: 'error',
          text: '오류가 발생하여 정보를 불러오는 데 실패했습니다.',
          confirmButtonColor: '#1e50af',
        });
        setVersionList([]);
        // setPagiInfo(dymmyPaginationInfo);
      }
    }

    // const getProjectInfo = async () => {
    //   try {
    //     const response = await apiController({
    //       url: `/api/v1/projects/${encodeURIComponent(projectKey)}`,
    //       method: 'get',
    //     })

    //     console.log("프로젝트 정보 불러오기 성공");
    //     setProject(response.data.result);
    //   } catch (err) {

    //     const error = err as ApiError;

    //     if (error.status === 403) {

    //     }
    //     console.log("프로젝트 정보 불러오는 데 실패.", err);
    //     // setProject(dummyProject);
    //     setProject(null);
    //   }
    // }

    // getProjectInfo();
    getPublicVersionList();
  }, [projectKey]);


  return (
    <div className="relative flex flex-col w-10/12  max-w-[1187.5px] justify-self-center justify-center content-center items-center py-4 gap-4 text-my-black">

      {/* 상단 제목 및 필터 */}
      <div className="flex flex-col w-full justify-start gap-[5px]">
        <div className="text-xl font-bold">
          {/* {!project ? " " : project.name} */}
          {projectName}
        </div>
      </div>

      {/* 목록 */}
      <div className="w-full border border-my-border rounded-2xl p-1">
        <table className="w-full border-separate border-spacing-y-0.5 table-fixed">
          <colgroup>
            <col style={{ width: '15%' }} />  {/* 상태 */}
            <col style={{ width: '25%' }} />  {/* 버전명 */}
            <col style={{ width: '45%' }} />  {/* 설명 */}
            <col style={{ width: '15%' }} />  {/* 생성일 */}
          </colgroup>
          <thead className="text-md fond-semibold ">
            <tr>
              <th className="p-3 text-left border-b border-my-border" scope="col">상태</th>
              <th className="p-3 text-left border-b border-my-border" scope="col">버전명</th>
              <th className="p-3 text-left border-b border-my-border" scope="col">설명</th>
              <th className="p-3 text-left border-b border-my-border" scope="col">생성일</th>
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
                  <td className="p-2 text-left rounded-l-xl">
                    <div className="hover:underline hover:cursor-pointer truncate" onClick={() => handleItemClick(item.versionKey)} >
                      {item.designVerificationStatus}
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
                </tr>
              )
              ))}
          </tbody>
        </table>
      </div>

      {/* 페이지네이션 */}
      {/* {(pageInfo && pageInfo.totalElements > 0) && <Pagination totalPages={Math.ceil(pageInfo.totalElements / pageInfo.size)} currentPage={currentPage} onPageChange={(page: number) => setCurrentPage(page)} className="absolute top-[645px]" />} */}

    </div>
  );
};

export default PublicVersionListPage;