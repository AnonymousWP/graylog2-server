// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

// $FlowFixMe: imports from core need to be fixed in flow
import fetch from 'logic/rest/FetchProvider';
// $FlowFixMe: imports from core need to be fixed in flow
import URLUtils from 'util/URLUtils';

import FieldTypeMapping from 'enterprise/logic/fieldtypes/FieldTypeMapping';
import { QueryFiltersStore } from './QueryFiltersStore';

const fieldTypesUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/fields');

type FieldTypesActionsType = {
  all: () => Promise<void>,
};

export const FieldTypesActions: FieldTypesActionsType = Reflux.createActions({
  all: { asyncResult: true },
});

export type FieldTypesStoreState = {
  all: Immutable.List<FieldTypeMapping>,
  queryFields: Immutable.Map<String, Immutable.List<FieldTypeMapping>>,
};

export const FieldTypesStore = Reflux.createStore({
  listenables: [FieldTypesActions],

  init() {
    this.all();
    this.listenTo(QueryFiltersStore, this.onQueryFiltersUpdate, this.onQueryFiltersUpdate);
  },

  getInitialState() {
    return this._state();
  },

  onQueryFiltersUpdate(newFilters) {
    const promises = newFilters
      .filter(filter => filter !== undefined && filter !== null)
      .map(filter => filter.get('filters', Immutable.List()).filter(f => f.get('type') === 'stream').map(f => f.get('id')))
      .filter(streamFilters => streamFilters.size > 0)
      .map((filters, queryId) => this.forStreams(filters.toArray()).then(response => ({
        queryId,
        response,
      })))
      .valueSeq()
      .toJS();

    Promise.all(promises).then((results) => {
      const combinedResult = {};
      results.forEach(({ queryId, response }) => {
        combinedResult[queryId] = response;
      });
      this.queryFields = Immutable.fromJS(combinedResult);
      this._trigger();
    });
  },

  all() {
    const promise = fetch('GET', fieldTypesUrl)
      .then(this._deserializeFieldTypes)
      .then((response) => {
        this.all = Immutable.fromJS(response);
        this._trigger();
      });

    FieldTypesActions.all.promise(promise);

    return promise;
  },

  forStreams(streams) {
    return fetch('POST', fieldTypesUrl, { streams: streams })
      .then(this._deserializeFieldTypes);
  },

  _deserializeFieldTypes(response) {
    return response
      .map(fieldTypeMapping => FieldTypeMapping.fromJSON(fieldTypeMapping));
  },

  _state(): FieldTypesStoreState {
    return {
      all: this.all,
      queryFields: this.queryFields,
    };
  },
  _trigger() {
    this.trigger(this._state());
  },
});
