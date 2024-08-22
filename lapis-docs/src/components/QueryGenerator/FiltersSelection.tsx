import type { Config } from '../../config';
import { filtersWithFromAndTo } from '../FiltersTable/getFilters.tsx';

export type Filters = Map<string, string>;

type Props = {
    config: Config;
    filters: Filters;
    onFiltersChange: (filters: Filters) => void;
};

export const FiltersSelection = (props: Props) => {
    const { config, filters, onFiltersChange } = props;
    return (
        <div>
            <div>
                Which sequences are you interested in? You can leave everything empty to get data for all sequences.
            </div>
            <div className='flex flex-col gap-4'>
                {config.schema.metadata.map((metadata) => {
                    if (filtersWithFromAndTo.includes(metadata.type)) {
                        return (
                            <>
                                <FilterField key={metadata.name + 'From'} name={metadata.name + 'From'} {...props} />
                                <FilterField key={metadata.name + 'To'} name={metadata.name + 'To'} {...props} />
                            </>
                        );
                    }

                    if (metadata.type === 'string' && metadata.lapisAllowsRegexSearch) {
                        return (
                            <>
                                <FilterField key={metadata.name} name={metadata.name} {...props} />
                                <FilterField
                                    key={metadata.name + '.regex'}
                                    name={metadata.name + '.regex'}
                                    {...props}
                                />
                            </>
                        );
                    }

                    return <FilterField key={metadata.name} name={metadata.name} {...props} />;
                })}
            </div>
        </div>
    );
};

type FilterFieldProps = Props & {
    name: string;
};

export const FilterField = ({ name, filters, onFiltersChange }: FilterFieldProps) => {
    return (
        <div>
            {name}:{' '}
            <input
                type='text'
                className='input input-bordered w-48'
                value={filters.get(name)}
                onChange={(e) => {
                    const newFilters = new Map(filters);
                    newFilters.set(name, e.target.value);
                    onFiltersChange(newFilters);
                }}
            />
        </div>
    );
};
