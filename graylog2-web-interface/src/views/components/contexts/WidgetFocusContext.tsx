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

import { singleton } from 'views/logic/singleton';

export type FocusRequest = {
  id: string,
  editing?: boolean,
  focusing?: boolean,
}

export type WidgetFocusContextType = {
  focusedWidget: FocusRequest | undefined,
  setWidgetFocusing: (widgetId: FocusRequest['id']) => void,
  setWidgetEditing: (widgetId: FocusRequest['id']) => void,
};

const defaultContext = {
  focusedWidget: undefined,
  setWidgetFocusing: () => {},
  setWidgetEditing: () => {},
};

const WidgetFocus = React.createContext<WidgetFocusContextType>(defaultContext);

export default singleton('contexts.WidgetFocus', () => WidgetFocus);
