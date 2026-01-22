import React from 'react';
import Cursor from './leftsidebar/Cursor';
import MultiSelection from './leftsidebar/MultiSelection';
import Handle from './leftsidebar/Handle';
import AddTable from './leftsidebar/AddTable';
import Memo from './leftsidebar/Memo';
import Import from './leftsidebar/Import';
import Export from './leftsidebar/Export';
import Relation013 from './leftsidebar/RelationZeroorOneorMany';
import Relation011 from './leftsidebar/RelationZeroorMany';
import Relation01 from './leftsidebar/RelationZeroorOne';
import Relation13 from './leftsidebar/RelationOneorMany';
import Relation11 from './leftsidebar/RelationOneOnly';
import Relation1 from './leftsidebar/RelationOne';
import { useWorkspace, type SidebarTool } from './WorkSpace';

const LeftSideBar: React.FC = () => {
  const { activeTool, setActiveTool, tables } = useWorkspace();

  const handleIconClick = (iconName: SidebarTool) => {
    setActiveTool(iconName);
  };

  return (
    <div className="w-12 bg-my-white border-r border-my-border flex flex-col items-center py-4 overflow-y-auto">
      <Cursor
        onClick={() => handleIconClick('cursor')}
        isActive={activeTool === 'cursor'}
      />
      <MultiSelection
        onClick={() => handleIconClick('multi-select')}
        isActive={activeTool === 'multi-select'}
      />
      <Handle
        onClick={() => handleIconClick('hand')}
        isActive={activeTool === 'hand'}
      />
      <AddTable
        onClick={() => handleIconClick('add-table')}
        isActive={activeTool === 'add-table'}
      />
      <Memo
        onClick={() => handleIconClick('memo')}
        isActive={activeTool === 'memo'}
      />

      {/* 구분선 */}
      <div className="w-6 h-px bg-my-border my-2" />

      <Relation013
        onClick={() => handleIconClick('013-relation')}
        isActive={activeTool === '013-relation'}
      />
      <Relation011
        onClick={() => handleIconClick('01-relation-1')}
        isActive={activeTool === '01-relation-1'}
      />
      <Relation01
        onClick={() => handleIconClick('01-relation')}
        isActive={activeTool === '01-relation'}
      />
      <Relation13
        onClick={() => handleIconClick('13-relation')}
        isActive={activeTool === '13-relation'}
      />
      <Relation11
        onClick={() => handleIconClick('11-relation')}
        isActive={activeTool === '11-relation'}
      />
      <Relation1
        onClick={() => handleIconClick('1-relation')}
        isActive={activeTool === '1-relation'}
      />

      <div className="mt-auto flex flex-col items-center">
        <div className="w-6 h-px bg-my-border my-2" />
        {tables.length === 0 && (
          <Import
            onClick={() => handleIconClick('import')}
            isActive={activeTool === 'import'}
          />
        )}
        <Export
          onClick={() => handleIconClick('export')}
          isActive={activeTool === 'export'}
        />
      </div>
    </div>
  );
};

export default LeftSideBar;
