import { type ChangeEvent, type Dispatch, type DragEvent, type SetStateAction, useState } from 'react';
import { configSchema, type PartialConfig } from './configContext.tsx';
import { load } from 'js-yaml';

export type UploadConfigProps = {
    setConfig: Dispatch<SetStateAction<PartialConfig | undefined>>;
};

export function UploadConfig({ setConfig }: UploadConfigProps) {
    const [highlight, setHighlight] = useState(false);

    const readAndSetConfigFile = (file: File): void => {
        const fileReader = new FileReader();
        fileReader.onloadend = () => {
            if (!fileReader.result) {
                return;
            }
            const fileContent = fileReader.result.toString();
            const config = configSchema.safeParse(load(fileContent));

            if (config.success) {
                setConfig(config.data.schema);
            } else {
                alert('Invalid config file: ' + config.error.message);
            }
        };
        fileReader.readAsText(file);
    };

    const handleFileUpload = (event: ChangeEvent<HTMLInputElement>) => {
        if (event.target.files) {
            readAndSetConfigFile(event.target.files[0]);
        }
    };

    const handleDragOver = (event: DragEvent) => {
        event.preventDefault();
        setHighlight(true);
    };

    const handleDragLeave = () => {
        setHighlight(false);
    };

    const handleDrop = (event: DragEvent) => {
        event.preventDefault();
        if (event.dataTransfer.items) {
            const item = event.dataTransfer.items[0];
            if (item.kind === 'file') {
                const file = item.getAsFile();
                if (file) {
                    readAndSetConfigFile(file);
                }
            }
        }
        setHighlight(false);
    };

    return (
        <div
            className={`p-5 ${highlight ? 'border-2 border-dashed border-primary' : 'border-2 border-transparent'}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
        >
            <input hidden id='fileUploader' type='file' onChange={handleFileUpload} />
            <label htmlFor='fileUploader' className='btn btn-primary cursor-pointer'>
                Upload Config
            </label>
            <div className='mt-2'>or drag and drop file here.</div>
        </div>
    );
}
