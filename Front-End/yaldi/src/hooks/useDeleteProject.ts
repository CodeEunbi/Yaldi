import { useState } from 'react';
import { apiController } from '../apis/apiController';
import Swal from 'sweetalert2';

interface UseDeleteProjectReturn {
  isDeleteModalOpen: boolean;
  projectKeyToDelete: number | null;
  openDeleteModal: (projectKey: number) => void;
  closeDeleteModal: () => void;
  confirmDelete: () => void;
}

export const useDeleteProject = (onDelete?: (projectKey: number) => void): UseDeleteProjectReturn => {
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [projectKeyToDelete, setProjectKeyToDelete] = useState<number | null>(null);

  const openDeleteModal = (projectKey: number) => {
    setProjectKeyToDelete(projectKey);
    setIsDeleteModalOpen(true);
  };

  const closeDeleteModal = () => {
    setIsDeleteModalOpen(false);
    setProjectKeyToDelete(null);
  };

  const confirmDelete = async () => {
    if (projectKeyToDelete !== null) {
      try {
        await apiController({
          url: `/api/v1/projects/${projectKeyToDelete}`,
          method: 'delete',
        })

        console.log("삭제 성공");
        onDelete?.(projectKeyToDelete);
        closeDeleteModal();

      } catch (err) {
        console.log("삭제 실패", err);
        Swal.fire({
          icon: 'error',
          text: "일시적인 오류로 인하여 요청에 실패했습니다.",
          confirmButtonColor: '#1e50af',
        })
      }

    }
  };

  return {
    isDeleteModalOpen,
    projectKeyToDelete,
    openDeleteModal,
    closeDeleteModal,
    confirmDelete,
  };
};
