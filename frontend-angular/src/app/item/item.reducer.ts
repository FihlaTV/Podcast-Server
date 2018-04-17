import { createFeatureSelector, createSelector } from '@ngrx/store';

import { Item } from '../shared/entity';

import { ItemActions, ItemAction } from './item.actions';

export interface ItemState {
	item: Item;
}

const initialState: ItemState = {
	item: null
};

export function itemReducer(state = initialState, action: ItemActions): ItemState {
	switch (action.type) {
		case ItemAction.FIND_ONE_SUCCESS: {
			return { ...state, item: action.item };
		}

		default: {
			return state;
		}
	}
}

const moduleSelector = createFeatureSelector<ItemState>('item');
export const selectItem = createSelector(moduleSelector, (s: ItemState) => s.item);
