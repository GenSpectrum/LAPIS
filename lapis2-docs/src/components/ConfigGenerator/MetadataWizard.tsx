import { type Dispatch, type SetStateAction, useContext, useRef, useState } from 'react';
import { ConfigContext, type Metadata, type MetadataType } from './configContext.tsx';
import { AddIcon, DeleteIcon, EditIcon } from './Icons.tsx';

export function MetadataWizard() {
    const { config, addNewMetadata, deleteMetadata, removeConfigField } = useContext(ConfigContext);

    function handleDeleteMetadata(metadata: Metadata, index: number) {
        if (metadata.type === 'string' && config.primaryKey === metadata.name) {
            removeConfigField('primaryKey');
        }
        if (metadata.type === 'date' && config.dateToSortBy === metadata.name) {
            removeConfigField('dateToSortBy');
        }
        if (metadata.type === 'pango_lineage' && config.partitionBy === metadata.name) {
            removeConfigField('partitionBy');
        }
        deleteMetadata(index);
    }

    return (
        <div className='flex flex-col mb-4'>
            <h1 className='text-xl font-bold mb-4'>Metadata</h1>
            <div className='card bg-base-100 shadow-xl'>
                <table className='table '>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Type</th>
                            <th>Generate Index</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        {config.metadata.map((metadata, index) => {
                            const modal = `modal_${metadata.name.replace(' ', '')}`;

                            return (
                                <tr key={metadata.name}>
                                    <td className='break-all max-w-72'>{metadata.name}</td>
                                    <td>{metadata.type}</td>
                                    <td>{metadata.generateIndex ? 'yes' : 'no'}</td>
                                    <td>
                                        <ActionsOnMetadata
                                            handleDeleteMetadata={() => handleDeleteMetadata(metadata, index)}
                                            index={index}
                                            metadata={metadata}
                                            modal={modal}
                                        />
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
                <div className={'flex justify-end'}>
                    <button
                        className='btn m-4'
                        onClick={() => {
                            addNewMetadata();
                        }}
                    >
                        <AddIcon />
                    </button>
                </div>
            </div>
        </div>
    );
}

function ActionsOnMetadata(props: {
    handleDeleteMetadata: () => void;
    index: number;
    metadata: Metadata;
    modal: string;
}) {
    const modalRef = useRef<HTMLDialogElement>(null);

    const openModal = () => {
        if (modalRef.current) {
            modalRef.current.showModal();
        }
    };

    return (
        <>
            <div className='join'>
                <button className='btn' onClick={openModal}>
                    <EditIcon />
                </button>
                <button className='btn' onClick={props.handleDeleteMetadata}>
                    <DeleteIcon />
                </button>
            </div>

            <dialog ref={modalRef} className='modal' id={props.modal}>
                <MetadataEditModal index={props.index} metadata={props.metadata} />
            </dialog>
        </>
    );
}

export function MetadataEditModal({ index, metadata }: { index: number; metadata: Metadata }) {
    const { updateMetadata } = useContext(ConfigContext);

    const [metadataType, setMetadataType] = useState<MetadataType>(metadata.type);
    const [metadataName, setMetadataName] = useState(metadata.name);
    const [generateIndex, setGenerateIndex] = useState(metadata.generateIndex);

    const handleUpdateMetadata = () => {
        updateMetadata({ ...metadata, name: metadataName, type: metadataType, generateIndex }, index);
    };

    const handleSetMetadataType = (newType: MetadataType) => {
        setMetadataType(newType);

        switch (newType) {
            case 'string':
                break;
            case 'pango_lineage':
                setGenerateIndex(true);
                break;
            default:
                setGenerateIndex(false);
                break;
        }
    };

    return (
        <div className='modal-box'>
            <h3 className='font-bold text-lg'>Edit metadata</h3>

            <form method='dialog'>
                <div className='flex flex-col space-x-2 items-center'>
                    <table className='table'>
                        <tbody>
                            <tr>
                                <td>Name</td>
                                <td>
                                    <EditMetadataName metadataName={metadataName} setMetadataName={setMetadataName} />
                                </td>
                            </tr>

                            <tr>
                                <td>Type</td>
                                <td>
                                    <SelectMetadataType
                                        metadataType={metadataType}
                                        setMetadataType={handleSetMetadataType}
                                    />
                                </td>
                            </tr>

                            <tr>
                                <td>Generate Index</td>
                                <td>
                                    <EditGenerateIndexWrapper
                                        generateIndex={generateIndex}
                                        setGenerateIndex={setGenerateIndex}
                                        metadataType={metadataType}
                                    />
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <div className='flex justify-end'>
                    <button
                        type='submit'
                        className='btn join-item'
                        onClick={() => {
                            handleUpdateMetadata();
                        }}
                    >
                        OK
                    </button>
                    <button className='btn btn-ghost join-item'>Cancel</button>
                </div>
            </form>
        </div>
    );
}

function EditGenerateIndexWrapper({
    generateIndex,
    setGenerateIndex,
    metadataType,
}: {
    generateIndex: boolean;
    setGenerateIndex: Dispatch<SetStateAction<boolean>>;
    metadataType: MetadataType;
}) {
    switch (metadataType) {
        case 'string':
            return <EditGenerateIndex generateIndex={generateIndex} setGenerateIndex={setGenerateIndex} />;
        case 'pango_lineage':
            return <div>An index has to be generated for pango lineages</div>;
        default:
            return <div>You can only generate index for string metadata.</div>;
    }
}

function EditGenerateIndex({
    generateIndex,
    setGenerateIndex,
}: {
    generateIndex: boolean;
    setGenerateIndex: Dispatch<SetStateAction<boolean>>;
}) {
    const handleGenerateIndexChange = () => {
        setGenerateIndex(!generateIndex);
    };

    return (
        <input
            type='checkbox'
            className='toggle toggle-accent'
            checked={generateIndex}
            onChange={handleGenerateIndexChange}
        />
    );
}

function EditMetadataName({
    metadataName,
    setMetadataName,
}: {
    metadataName: string;
    setMetadataName: Dispatch<SetStateAction<string>>;
}) {
    const { config } = useContext(ConfigContext);
    const [error, setError] = useState(false);

    const handleMetadataNameChange = (event: { target: { value: string } }) => {
        if (config.metadata.filter((metadata) => metadata.name === event.target.value).length > 0) {
            if (event.target.value !== metadataName) {
                setError(true);
                return;
            }
        }

        setError(false);
        setMetadataName(event.target.value);
    };

    return (
        <input
            type='text'
            className={`input input-bordered ${error ? 'input-error' : ''}`}
            defaultValue={metadataName}
            onChange={handleMetadataNameChange}
        />
    );
}

function SelectMetadataType({
    metadataType,
    setMetadataType,
}: {
    metadataType: MetadataType;
    setMetadataType: (newType: MetadataType) => void;
}) {
    const handleMetadataTypeChange = (event: { target: { value: any } }) => {
        const newType = event.target.value;
        setMetadataType(newType);
    };

    return (
        <select className='select select-bordered mt-0' value={metadataType} onChange={handleMetadataTypeChange}>
            <option value='string'>String</option>
            <option value='date'>Date</option>
            <option value='number'>Number</option>
            <option value='boolean'>Boolean</option>
            <option value='pango_lineage'>Pango lineage</option>
            <option value='insertion'>Insertion</option>
            <option value='aaInsertion'>AA insertion</option>
        </select>
    );
}
