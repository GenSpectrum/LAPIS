import { type ChangeEvent, type Dispatch, type DragEvent, type SetStateAction, useState } from 'react';
import { configSchema, type PartialConfig, type TopLevelConfig } from './configContext.tsx';
import { load } from 'js-yaml';

export type UploadConfigProps = {
    setConfig: Dispatch<SetStateAction<PartialConfig | undefined>>;
    setTopLevelConfig: Dispatch<SetStateAction<TopLevelConfig>>;
};

export function UploadConfig({ setConfig, setTopLevelConfig }: UploadConfigProps) {
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
                const { schema, ...topLevel } = config.data;
                setConfig(schema);
                setTopLevelConfig(topLevel);
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
        <label
            htmlFor='fileUploader'
            className={`flex flex-col items-center justify-center text-center h-24 px-4 cursor-pointer border border-dashed text-base-content/70 transition-colors ${
                highlight ? 'border-primary bg-base-200' : 'border-base-300 hover:border-base-content/40'
            }`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
        >
            <input hidden id='fileUploader' type='file' onChange={handleFileUpload} />
            <span className='text-base text-base-content'>Upload existing config</span>
            <span className='text-xs mt-1'>or drag and drop here</span>
        </label>
    );
}
