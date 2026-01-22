import type { WorkspaceTable } from '../pages/workspace/WorkSpace';

type TableActionButton = {
  key: string;
  label: string;
  Icon: React.ComponentType<React.SVGProps<SVGSVGElement>>;
  bgClass: string;
  onClick?: (tableId: string) => void;
};

type TableReply = {
  id: string;
  author: string;
  content: string;
  createdAt: string;
  avatarInitial?: string;
};

type TableComment = {
  id: string;
  author: string;
  content: string;
  createdAt: string;
  avatarInitial?: string;
  replies?: TableReply[];
};

export type { TableActionButton, TableComment, TableReply };
export type { WorkspaceTable };
