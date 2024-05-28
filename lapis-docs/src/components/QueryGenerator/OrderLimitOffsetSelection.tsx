import React, { type Dispatch, type SetStateAction, useEffect, useMemo, useState } from 'react';
import { LabelledInput } from './LabelledInput.tsx';
import { getResultFields, type QueryTypeSelectionState } from './QueryTypeSelectionState.ts';
import type { Config } from '../../config.ts';

export type OrderByLimitOffset = {
    orderBy: string[];
    limit: number | undefined;
    offset: number | undefined;
};

type Props = {
    config: Config;
    queryType: QueryTypeSelectionState;
    orderByLimitOffset: OrderByLimitOffset;
    onOrderByLimitOffsetChange: Dispatch<SetStateAction<OrderByLimitOffset>>;
};

export const OrderLimitOffsetSelection = ({
    config,
    queryType,
    orderByLimitOffset,
    onOrderByLimitOffsetChange,
}: Props) => {
    return (
        <div className='flex flex-col gap-4'>
            <OderBySelection
                config={config}
                queryType={queryType}
                orderBy={orderByLimitOffset.orderBy}
                onOrderByLimitOffsetChange={onOrderByLimitOffsetChange}
            />
            <LabelledInput
                label='Limit:'
                value={orderByLimitOffset.limit}
                onChange={(value) =>
                    onOrderByLimitOffsetChange((prev) => ({
                        ...prev,
                        limit: value === '' ? undefined : parseInt(value),
                    }))
                }
            />
            <LabelledInput
                label='Offset:'
                value={orderByLimitOffset.offset}
                onChange={(value) =>
                    onOrderByLimitOffsetChange((prev) => ({
                        ...prev,
                        offset: value === '' ? undefined : parseInt(value),
                    }))
                }
            />
        </div>
    );
};

type OrderBySelectionProps = {
    config: Config;
    queryType: QueryTypeSelectionState;
    orderBy: string[];
    onOrderByLimitOffsetChange: Dispatch<SetStateAction<OrderByLimitOffset>>;
};

const OderBySelection = ({ config, queryType, orderBy, onOrderByLimitOffsetChange }: OrderBySelectionProps) => {
    let unselectedOrderByFields = useMemo(
        () =>
            getResultFields(queryType, config)
                .map((resultField) => resultField.name)
                .filter((fieldName) => !orderBy.includes(fieldName)),
        [orderBy, queryType],
    );

    const [selectedOrderByField, setSelectedOrderByField] = useState(unselectedOrderByFields[0]);

    useEffect(() => {
        if (!unselectedOrderByFields.includes(selectedOrderByField)) {
            setSelectedOrderByField(unselectedOrderByFields[0]);
        }
    }, [unselectedOrderByFields, selectedOrderByField]);

    return (
        <div className='flex flex-column justify-start'>
            <label className='mr-2'>Order by:</label>

            <div>
                {orderBy.map((value, index) => (
                    <div key={index}>
                        <input readOnly className='input input-bordered' value={value} />
                        <button
                            className='btn'
                            onClick={() =>
                                onOrderByLimitOffsetChange((prev) => ({
                                    ...prev,
                                    orderBy: prev.orderBy?.filter((_, i) => i !== index),
                                }))
                            }
                        >
                            -
                        </button>
                    </div>
                ))}

                {unselectedOrderByFields.length > 0 && (
                    <>
                        <label className='mr-2'>Add a field:</label>
                        <select
                            className='input input-bordered'
                            onChange={(e) => setSelectedOrderByField(e.target.value)}
                        >
                            {unselectedOrderByFields.map((fieldName) => (
                                <option key={fieldName}>{fieldName}</option>
                            ))}
                        </select>
                        <button
                            className='btn'
                            onClick={() => {
                                onOrderByLimitOffsetChange((prev) => ({
                                    ...prev,
                                    orderBy: [...prev.orderBy, selectedOrderByField],
                                }));
                            }}
                        >
                            +
                        </button>
                    </>
                )}
            </div>
        </div>
    );
};
