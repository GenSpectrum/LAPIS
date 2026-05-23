import { type Dispatch, type SetStateAction, useContext, useEffect, useRef, useState } from 'react';
import { ConfigContext, type Metadata, type MetadataType } from './configContext.tsx';
import { HelpTooltip } from './HelpTooltip.tsx';
import { Field, SectionHeading } from './FormLayout.tsx';

export function MetadataWizard() {
    const { config, addNewMetadata, deleteMetadata, removeConfigField } = useContext(ConfigContext);
    const [autoOpenIndex, setAutoOpenIndex] = useState<number | null>(null);

    function handleDeleteMetadata(metadata: Metadata, index: number) {
        if (metadata.type === 'string' && config.primaryKey === metadata.name) {
            removeConfigField('primaryKey');
        }
        deleteMetadata(index);
    }

    function handleAddMetadata() {
        // The new row is appended; its index is the current length.
        setAutoOpenIndex(config.metadata.length);
        addNewMetadata();
    }

    const hasRows = config.metadata.length > 0;

    return (
        <div className='space-y-4'>
            <SectionHeading>Metadata fields</SectionHeading>
            <div className='border border-base-300 rounded-sm'>
                <table className='w-full text-sm border-collapse'>
                    <thead className='bg-base-200 text-left'>
                        <tr>
                            <th className='px-3 py-2 font-medium whitespace-nowrap'>
                                Name
                                <HelpTooltip
                                    text='Must be unique and must not contain a "." (which LAPIS reserves for derived filters such as "<name>.regex" and "<name>.isNull").'
                                    docsHref='/maintainer-docs/references/database-configuration#the-metadata-object'
                                />
                            </th>
                            <th className='px-3 py-2 font-medium whitespace-nowrap w-24'>
                                Type
                                <HelpTooltip
                                    text='Data type of the field. One of string, date, int, float, or boolean.'
                                    docsHref='/maintainer-docs/references/database-configuration#metadata-types'
                                />
                            </th>
                            <th className='px-3 py-2 font-medium text-center whitespace-nowrap w-20'>
                                Indexed
                                <HelpTooltip
                                    text='When enabled, SILO precomputes bitmaps for the field. Recommended for low-cardinality string fields like country or host.'
                                    docsHref='/maintainer-docs/references/database-configuration#generating-an-index'
                                />
                            </th>
                            <th className='px-3 py-2 w-20'></th>
                        </tr>
                    </thead>
                    <tbody>
                        {!hasRows && (
                            <tr>
                                <td colSpan={4} className='px-3 py-6 text-center text-sm text-base-content/60'>
                                    No metadata fields yet. Add one to start.
                                </td>
                            </tr>
                        )}
                        {config.metadata.map((metadata, index) => {
                            const modal = `modal_${metadata.name.replace(/\s+/g, '_')}_${index}`;
                            return (
                                <tr key={index} className='border-t border-base-300'>
                                    <td className='px-3 py-2 font-mono truncate max-w-xs' title={metadata.name}>
                                        {metadata.name}
                                    </td>
                                    <td className='px-3 py-2 font-mono whitespace-nowrap'>{metadata.type}</td>
                                    <td className='px-3 py-2 text-center'>
                                        {metadata.generateIndex ? (
                                            <span aria-label='indexed' title='indexed'>
                                                ✓
                                            </span>
                                        ) : (
                                            <span className='text-base-content/40' aria-label='not indexed'>
                                                —
                                            </span>
                                        )}
                                    </td>
                                    <td className='px-3 py-2 text-right whitespace-nowrap'>
                                        <ActionsOnMetadata
                                            handleDeleteMetadata={() => handleDeleteMetadata(metadata, index)}
                                            index={index}
                                            metadata={metadata}
                                            modal={modal}
                                            shouldAutoOpen={autoOpenIndex === index}
                                            onAutoOpened={() => setAutoOpenIndex(null)}
                                        />
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                    <tfoot>
                        <tr className='border-t border-base-300 bg-base-100'>
                            <td colSpan={4} className='px-3 py-2'>
                                <button
                                    type='button'
                                    className='text-sm text-base-content/80 hover:text-base-content underline'
                                    onClick={handleAddMetadata}
                                >
                                    + Add field
                                </button>
                            </td>
                        </tr>
                    </tfoot>
                </table>
            </div>
        </div>
    );
}

function ActionsOnMetadata(props: {
    handleDeleteMetadata: () => void;
    index: number;
    metadata: Metadata;
    modal: string;
    shouldAutoOpen: boolean;
    onAutoOpened: () => void;
}) {
    const modalRef = useRef<HTMLDialogElement>(null);
    // Bumping this key on each open re-mounts MetadataEditModal so that
    // its local form state is re-initialised from the current metadata
    // prop — i.e. Cancel actually cancels and discards typed-but-unsaved
    // edits.
    const [sessionKey, setSessionKey] = useState(0);

    const openModal = () => {
        setSessionKey((k) => k + 1);
        modalRef.current?.showModal();
    };

    const { shouldAutoOpen, onAutoOpened } = props;
    useEffect(() => {
        if (shouldAutoOpen) {
            openModal();
            onAutoOpened();
        }
    }, [shouldAutoOpen, onAutoOpened]);

    return (
        <>
            <button
                type='button'
                className='text-xs text-base-content/80 hover:text-base-content underline mr-3'
                onClick={openModal}
            >
                Edit
            </button>
            <button
                type='button'
                className='text-xs text-error/80 hover:text-error underline'
                onClick={props.handleDeleteMetadata}
            >
                Delete
            </button>
            <dialog ref={modalRef} className='modal' id={props.modal}>
                <MetadataEditModal key={sessionKey} index={props.index} metadata={props.metadata} />
            </dialog>
        </>
    );
}

export function MetadataEditModal({ index, metadata }: { index: number; metadata: Metadata }) {
    const { updateMetadata } = useContext(ConfigContext);

    const [metadataType, setMetadataType] = useState<MetadataType>(metadata.type);
    const [metadataName, setMetadataName] = useState(metadata.name);
    const [generateIndex, setGenerateIndex] = useState(!!metadata.generateIndex);

    const handleUpdateMetadata = () => {
        updateMetadata({ ...metadata, name: metadataName, type: metadataType, generateIndex }, index);
    };

    const handleSetMetadataType = (newType: MetadataType) => {
        setMetadataType(newType);

        if (newType !== 'string') {
            setGenerateIndex(false);
        }
    };

    return (
        <div className='modal-box rounded-sm'>
            <h3 className='font-semibold text-base mb-4'>Edit metadata field</h3>
            <form method='dialog' className='space-y-4'>
                <Field label='Name' required>
                    <EditMetadataName metadataName={metadataName} setMetadataName={setMetadataName} />
                </Field>
                <Field label='Type' required>
                    <SelectMetadataType metadataType={metadataType} setMetadataType={handleSetMetadataType} />
                </Field>
                <Field
                    label='Generate index'
                    description={
                        metadataType === 'string'
                            ? 'Best for low-cardinality string fields.'
                            : 'Indexing is only available for string fields.'
                    }
                >
                    <EditGenerateIndex
                        generateIndex={generateIndex}
                        setGenerateIndex={setGenerateIndex}
                        disabled={metadataType !== 'string'}
                    />
                </Field>
                <div className='flex justify-end gap-2 pt-2'>
                    <button type='submit' className='btn btn-sm btn-ghost'>
                        Cancel
                    </button>
                    <button
                        type='submit'
                        className='btn btn-sm btn-primary'
                        onClick={() => {
                            handleUpdateMetadata();
                        }}
                    >
                        Save
                    </button>
                </div>
            </form>
        </div>
    );
}

function EditGenerateIndex({
    generateIndex,
    setGenerateIndex,
    disabled,
}: {
    generateIndex: boolean;
    setGenerateIndex: Dispatch<SetStateAction<boolean>>;
    disabled?: boolean;
}) {
    return (
        <input
            type='checkbox'
            className='toggle toggle-sm'
            checked={generateIndex}
            onChange={() => setGenerateIndex(!generateIndex)}
            disabled={disabled}
        />
    );
}

function validateMetadataName(name: string, metadataName: string, existing: { name: string }[]): string | null {
    const trimmed = name.trim();
    if (trimmed.length === 0) {
        return 'Name cannot be empty.';
    }
    if (name.includes('.')) {
        return 'Name must not contain the reserved character ".". LAPIS uses it to generate derived filters and will refuse to start otherwise.';
    }
    if (name !== metadataName && existing.some((metadata) => metadata.name === name)) {
        return `Another metadata field is already called "${name}".`;
    }
    return null;
}

function EditMetadataName({
    metadataName,
    setMetadataName,
}: {
    metadataName: string;
    setMetadataName: Dispatch<SetStateAction<string>>;
}) {
    const { config } = useContext(ConfigContext);
    const [error, setError] = useState<string | null>(null);

    const handleMetadataNameChange = (event: { target: { value: string } }) => {
        const value = event.target.value;
        const validationError = validateMetadataName(value, metadataName, config.metadata);

        if (validationError !== null) {
            setError(validationError);
            return;
        }

        setError(null);
        setMetadataName(value);
    };

    return (
        <div className='flex flex-col gap-1'>
            <input
                type='text'
                className={`input input-bordered input-sm font-mono ${error ? 'input-error' : ''}`}
                defaultValue={metadataName}
                onChange={handleMetadataNameChange}
            />
            {error !== null && <span className='text-error text-xs'>{error}</span>}
        </div>
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
        <select
            className='select select-bordered select-sm font-mono'
            value={metadataType}
            onChange={handleMetadataTypeChange}
        >
            <option value='string'>string</option>
            <option value='date'>date</option>
            <option value='int'>int</option>
            <option value='float'>float</option>
            <option value='boolean'>boolean</option>
        </select>
    );
}
