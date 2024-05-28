import { type ChangeEvent, type FC, useContext, useEffect, useMemo } from 'react';
import { ConfigContext, type MetadataType, type Schema } from './configContext.tsx';
import { sentenceCase } from 'change-case';

export function AdditionalInformationWizard() {
    return (
        <div className='mb-4'>
            <h1 className='text-xl font-bold mb-4'>Additional Information</h1>
            <MetadataDropDown filterByType={'string'} name={'primaryKey'} />
            <MetadataDropDown filterByType={'date'} name={'dateToSortBy'} />
            <MetadataDropDown filterByType={'pango_lineage'} name={'partitionBy'} />
        </div>
    );
}

type MetadataDropDownProps = {
    name: 'primaryKey' | 'dateToSortBy' | 'partitionBy';
    filterByType: MetadataType;
};
const MetadataDropDown: FC<MetadataDropDownProps> = ({ name, filterByType }) => {
    const { config, modifyConfigField, removeConfigField } = useContext(ConfigContext);

    useEffect(() => {
        function setInitialAdditionalInfo(additionalInfoName: keyof Schema, additionalInfoType: string) {
            const currentAdditionalInfo = config[additionalInfoName];
            let metadataOfAdditionalInfo;

            if (currentAdditionalInfo !== undefined) {
                metadataOfAdditionalInfo = config.metadata.find((metadata) => metadata.name === currentAdditionalInfo);
            }

            if (metadataOfAdditionalInfo === undefined) {
                const anyMetadataOfCorrectType = config.metadata.find(
                    (metadata) => metadata.type === additionalInfoType,
                );

                if (anyMetadataOfCorrectType !== undefined) {
                    modifyConfigField(additionalInfoName, anyMetadataOfCorrectType.name);
                } else {
                    removeConfigField(additionalInfoName);
                }
            }
        }

        setInitialAdditionalInfo(name, filterByType);
    }, []);

    const options = useMemo(
        () =>
            config.metadata.filter((metadata) => {
                if (filterByType === undefined) {
                    return true;
                }
                return metadata.type === filterByType;
            }),
        [config.metadata, filterByType],
    );

    const handleChange = (event: ChangeEvent<HTMLSelectElement>) => {
        modifyConfigField(name, event.target.value);
    };

    return (
        <div className='flex items-center space-x-4'>
            <div className='form-control w-full'>
                <label className='label'>
                    <div className='label w-64'>{sentenceCase(name)}</div>
                    <select
                        className='select select-bordered w-full'
                        onChange={handleChange}
                        defaultValue={config[name]}
                    >
                        {options.map((option) => (
                            <option key={option.name} value={option.name}>
                                {option.name}
                            </option>
                        ))}
                    </select>
                </label>
            </div>
        </div>
    );
};
