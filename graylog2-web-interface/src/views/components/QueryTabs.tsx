/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useRef, useState } from 'react';
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import { SizeMe } from 'react-sizeme';

import { Col, Row, Nav, NavItem, MenuItem } from 'components/graylog';
import { ModifiedNavDropdown as NavDropdown } from 'components/graylog/NavDropdown';
import { Icon } from 'components/common';
import QueryTitle from 'views/components/queries/QueryTitle';
import QueryTitleEditModal from 'views/components/queries/QueryTitleEditModal';
import Query, { QueryId } from 'views/logic/queries/Query';
import type { TitlesMap } from 'views/stores/TitleTypes';
import ViewState from 'views/logic/views/ViewState';
import QueryDashboardTab from 'views/components/queries/QueryDashboardTab';

type Props = {
  onRemove: (queryId: string) => Promise<void> | Promise<ViewState>,
  onSelect: (queryId: string) => Promise<Query> | Promise<string>,
  onTitleChange: (queryId: string, newTitle: string) => Promise<TitlesMap>,
  queries: Array<QueryId>,
  selectedQueryId: string,
  titles: Immutable.Map<string, string>,
};

// const CLASS_HIDDEN = 'hidden';
// const CLASS_ACTIVE = 'active';
// const CLASS_LOCKED = 'locked';

const StyledRow = styled(Row)`
  margin-bottom: 0;
`;

const StyledQueryNav = styled(Nav)(({ theme }) => css`
  &.nav.nav-tabs {
    border-bottom: 0;
    display: flex;
    white-space: nowrap;
    z-index: 2; /* without it renders under widget management icons */
    position: relative;
    margin-bottom: -1px;
    padding-left: 15px;

    > li {
      > a {
        color: ${theme.colors.variant.dark.default};
        border: none;
        
        :hover,
        :active,
        :focus {
          transition: color 150ms ease-in-out;
          background: transparent;
          color: ${theme.colors.variant.dark.primary};
        }
      }
    }

    > li.active {
      z-index: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: -3px;

      > a {
        padding: 9px 15px;
        border: 1px solid ${theme.colors.variant.lighter.default};
        border-bottom: none;
        background-color: ${theme.colors.global.contentBackground};
        color: ${theme.colors.variant.darkest.primary};

        :hover,
        :active,
        :focus {
          border: 1px solid ${theme.colors.variant.lighter.default};
          border-bottom: none;
          color: ${theme.colors.variant.darkest.primary};
        }
      }
    }

    > li.query-tabs-more,
    > li.query-tabs-more a {
      cursor: pointer;
    }
  }
`);

// const adjustTabs = () => {
//   const dashboardTabs = document.querySelector('#dashboard-tabs') as HTMLElement;
//   const allTabs = dashboardTabs.querySelectorAll('li');
//   const tabItems = dashboardTabs.querySelectorAll(':scope > li:not(.dropdown):not(.query-tabs-new)');
//   const moreItems = dashboardTabs.querySelectorAll('li.dropdown .dropdown-menu li');
//   const lockedItems = dashboardTabs.querySelectorAll(`li.${CLASS_LOCKED}`);
//   const moreBtn = dashboardTabs.querySelector('li.query-tabs-more') as HTMLElement;
//   const newBtn = dashboardTabs.querySelector('li.query-tabs-new') as HTMLElement;
//   const primaryWidth = dashboardTabs.offsetWidth;
//   const hiddenItems = [];
//
//   let maxWidth = moreBtn.offsetWidth + newBtn.offsetWidth; // magic number is PageContentLayout__Container padding
//
//   if (lockedItems.length) {
//     lockedItems.forEach((tabItem: HTMLElement) => {
//       maxWidth += tabItem.offsetWidth;
//     });
//   }
//
//   allTabs.forEach((tabItem) => {
//     tabItem.classList.remove(CLASS_HIDDEN);
//   });
//
//   tabItems.forEach((tabItem: HTMLElement, idx) => {
//     if (!tabItem.classList.contains(CLASS_LOCKED)) {
//       maxWidth += tabItem.offsetWidth;
//       tabItem.classList.remove(CLASS_HIDDEN);
//
//       if (primaryWidth >= maxWidth) {
//         hiddenItems.splice(idx, 1);
//       } else {
//         tabItem.classList.add(CLASS_HIDDEN);
//         hiddenItems.push(idx);
//       }
//     }
//   });
//
//   moreItems.forEach((tabItem: HTMLElement, idx) => {
//     if (!hiddenItems.includes(idx)) {
//       tabItem.classList.add(CLASS_HIDDEN);
//     }
//
//     if (!tabItem.classList.contains(CLASS_HIDDEN) && tabItem.classList.contains(CLASS_ACTIVE)) {
//       const topTabItem = tabItems[idx] as HTMLElement;
//       const parent = moreBtn.parentNode;
//
//       topTabItem.classList.add(CLASS_LOCKED);
//       topTabItem.classList.remove(CLASS_HIDDEN);
//       tabItem.classList.add(CLASS_HIDDEN);
//
//       parent.insertBefore(moreBtn, topTabItem);
//
//       adjustTabs();
//     }
//   });
//
//   if (!hiddenItems.length) {
//     moreBtn.classList.add(CLASS_HIDDEN);
//   }
// };

const QueryTabs = ({ onRemove, onSelect, onTitleChange, queries, selectedQueryId, titles }:Props) => {
  const queryTitleEditModal = useRef<QueryTitleEditModal | undefined | null>();
  const [openedMore, setOpenedMore] = useState<boolean>(false);
  const [hiddenTabs, setHiddenTabs] = useState<Array<React.ReactNode>>([]);
  const maxWidth = useRef<number>(0);
  const currentWidth = useRef<number>(0);

  const openTitleEditModal = (activeQueryTitle: string) => {
    if (queryTitleEditModal) {
      queryTitleEditModal.current.open(activeQueryTitle);
    }
  };

  const queryTabs = (itemType: 'navItem' | 'menuItem') => queries.map((id, index) => {
    const title = titles.get(id, `Page#${index + 1}`);
    const tabTitle = (
      <QueryTitle active={id === selectedQueryId}
                  id={id}
                  onClose={() => onRemove(id)}
                  openEditModal={openTitleEditModal}
                  key={`menutitle-${id}`}
                  title={title} />
    );

    const output = {
      menuItem: <MenuItem eventKey={`menu-${id}`} key={id} onClick={() => onSelect(id)}>{tabTitle}</MenuItem>,
    };

    return (
      <SizeMe>
        {() => {
          return output[itemType];
        }}
      </SizeMe>
    );
  });

  const newTab = (
    <NavItem key="new"
             eventKey="new"
             onClick={() => onSelect('new')}
             className="query-tabs-new">
      <Icon name="plus" />
    </NavItem>
  );

  const onTabSize = (id) => (size) => {
    currentWidth.current += size.width;

    if (currentWidth.current > maxWidth.current) {
      setHiddenTabs([...hiddenTabs, id]);
    }
  };

  return (
    <StyledRow>
      <Col>
        <SizeMe>
          {(wrapperSize) => {
            maxWidth.current = wrapperSize.size.width;

            return (
              <StyledQueryNav bsStyle="tabs" activeKey={selectedQueryId} id="dashboard-tabs">

                {queries.map((id, index) => {
                  const title = titles.get(id, `Page#${index + 1}`);
                  const tabTitle = (
                    <QueryTitle active={id === selectedQueryId}
                                id={id}
                                onClose={() => onRemove(id)}
                                openEditModal={openTitleEditModal}
                                key={`navtitle-${id}`}
                                title={title} />
                  );

                  if (hiddenTabs.includes(id)) { return null; }

                  return (
                    <QueryDashboardTab key={`nav-${id}`}
                                       id={id}
                                       onSelect={onSelect}
                                       tabTitle={tabTitle}
                                       onSize={onTabSize(id)} />
                  );
                })}

                <NavDropdown eventKey="more"
                             title={<Icon name="ellipsis-h" />}
                             className="query-tabs-more"
                             id="query-tabs-more"
                             noCaret
                             pullRight
                             active={openedMore}
                             open={openedMore}
                             onToggle={(isOpened) => setOpenedMore(isOpened)}>
                  {queryTabs('menuItem')}
                </NavDropdown>

                {newTab}
              </StyledQueryNav>
            );
          }}
        </SizeMe>

        {/*
          The title edit modal can't be part of the QueryTitle component,
          due to the react bootstrap tabs keybindings.
          The input would always lose the focus when using the arrow keys.
        */}
        <QueryTitleEditModal onTitleChange={(newTitle: string) => onTitleChange(selectedQueryId, newTitle)}
                             ref={queryTitleEditModal} />
      </Col>
    </StyledRow>
  );
};

QueryTabs.propTypes = {
  onRemove: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  onTitleChange: PropTypes.func.isRequired,
  queries: PropTypes.object.isRequired,
  selectedQueryId: PropTypes.string.isRequired,
  titles: PropTypes.object.isRequired,
};

export default QueryTabs;
