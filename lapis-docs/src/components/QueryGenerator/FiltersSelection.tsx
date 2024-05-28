import type { Config } from '../../config';

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
                {config.schema.metadata.map((m) =>
                    m.type === 'date' ? (
                        <>
                            <FilterField key={m.name + 'From'} name={m.name + 'From'} {...props} />
                            <FilterField key={m.name + 'To'} name={m.name + 'To'} {...props} />
                        </>
                    ) : (
                        <FilterField key={m.name} name={m.name} {...props} />
                    ),
                )}
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
